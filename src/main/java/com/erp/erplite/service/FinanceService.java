package com.erp.erplite.service;

import com.erp.erplite.common.Log;
import com.erp.erplite.entity.AnalysisVO;
import com.erp.erplite.entity.FinAccount;
import com.erp.erplite.entity.FinAccountVO;
import com.erp.erplite.entity.FinanceDashboardVO;
import com.erp.erplite.mapper.FinAccountMapper;
import com.erp.erplite.mapper.OrderDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinAccountMapper finAccountMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final com.erp.erplite.mapper.StockMapper stockMapper;

    private com.erp.erplite.entity.FinanceDashboardVO cachedDashboardData = null;
    private long cachedDashboardDataTime = 0;

    public com.erp.erplite.entity.FinanceDashboardVO getDashboardData() {
        if (cachedDashboardData != null && System.currentTimeMillis() - cachedDashboardDataTime < 3 * 60 * 1000) {
            log.info("返回缓存的核心财务看板数据");
            return cachedDashboardData;
        }

        log.info("重新统计核心财务看板数据");
        com.erp.erplite.entity.FinanceDashboardVO vo = new com.erp.erplite.entity.FinanceDashboardVO();

        // 1. 获取应付与应收
        vo.setTotalPayable(finAccountMapper.sumTotalPayable());
        vo.setTotalReceivable(finAccountMapper.sumTotalReceivable());

        // 2. 获取总销售额与总出库成本 (只统计已生效发货的单据)
        BigDecimal totalSales = orderDetailMapper.sumTotalSales();
        vo.setTotalSales(totalSales);
        
        BigDecimal totalCost = orderDetailMapper.sumTotalCost();

        // 3. 计算真实毛利润 = 总销售额 - 出库商品的真实加权平均成本
        // 彻底去除了写死的 30% 比例，反映公司真实的经营利润状况
        BigDecimal actualProfit = totalSales.subtract(totalCost).setScale(2, java.math.RoundingMode.HALF_UP);
        vo.setTotalProfit(actualProfit);

        // 4. 获取全局库存占用 Top 5
        vo.setTopStocks(stockMapper.getTop5Stock());

        cachedDashboardData = vo;
        cachedDashboardDataTime = System.currentTimeMillis();
        return vo;
    }

    // --- 查询账款列表 ---
    public com.baomidou.mybatisplus.core.metadata.IPage<FinAccountVO> getAccountList(Integer type, String startDate, String endDate, String keyword, int pageNum, int pageSize) {
        log.info("查询账款明细列表, 类型: {}, 分页: {}/{}", type, pageNum, pageSize);
        return finAccountMapper.getAccountList(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), type, startDate, endDate, keyword);
    }

    // --- 核销账款 (支持分期付款) ---
    @Log("执行了财务核销操作")
    @Transactional(rollbackFor = Exception.class)
    public void settleAccount(Long accountId, BigDecimal payAmount) {
        log.info("准备核销账款, ID: {}, 本次实付金额: {}", accountId, payAmount);
        
        com.erp.erplite.entity.FinAccount account = finAccountMapper.selectById(accountId);
        if (account == null) {
            throw new com.erp.erplite.common.BusinessException("账单不存在");
        }
        if (account.getStatus() == 1) {
            throw new com.erp.erplite.common.BusinessException("该账单已结清，请勿重复操作");
        }

        // 处理分期付款逻辑（兼容退货产生的负数账单）
        BigDecimal currentPaid = account.getPaidAmount() != null ? account.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal oldPaidAmount = currentPaid;
        
        if (payAmount != null && payAmount.abs().compareTo(BigDecimal.ZERO) > 0) {
            // 无论前端传正数还是负数，统一按绝对值加上正确的符号方向进行累加
            BigDecimal sign = account.getAmount().signum() >= 0 ? BigDecimal.ONE : new BigDecimal("-1");
            currentPaid = currentPaid.add(payAmount.abs().multiply(sign));
        } else {
            // 如果前端没传本次金额（或者传了0），默认全额结清剩余款项
            currentPaid = account.getAmount();
        }

        account.setPaidAmount(currentPaid);

        // 统一使用绝对值判断是否已达到结清标准
        if (currentPaid.abs().compareTo(account.getAmount().abs()) >= 0) {
            account.setStatus(1);
            account.setPaidAmount(account.getAmount()); // 防御超付
            log.info("账单 ID:{} 全额结清！", accountId);
        } else {
            log.info("账单 ID:{} 部分结清，当前已结: {}/{}", accountId, account.getPaidAmount(), account.getAmount());
        }

        // 乐观锁更新
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<com.erp.erplite.entity.FinAccount> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("id", accountId);
        if (oldPaidAmount.compareTo(BigDecimal.ZERO) == 0) {
            updateWrapper.and(w -> w.eq("paid_amount", 0).or().isNull("paid_amount"));
        } else {
            updateWrapper.eq("paid_amount", oldPaidAmount);
        }

        int rows = finAccountMapper.update(account, updateWrapper);
        if (rows == 0) {
            // 并发冲突，直接熔断抛错，禁止重试累加
            throw new com.erp.erplite.common.BusinessException("操作冲突：当前账款金额已被其他操作刷新，请刷新页面后重试");
        }
    }

    /**
     * 账龄分析 (分析客户欠我们多久钱了)
     */
    public java.util.List<AnalysisVO> analyzeDebtAge() {
        log.info("执行应收账龄分析");
        java.util.List<com.erp.erplite.entity.AnalysisVO> result = new java.util.ArrayList<>();

        // 查出所有未结清 (status=0) 的应收账款 (type=2)
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FinAccount> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.eq("type", 2).eq("status", 0);
        java.util.List<FinAccount> debtList = finAccountMapper.selectList(query);

        long currentTime = System.currentTimeMillis();

        for (FinAccount debt : debtList) {
            AnalysisVO vo = new AnalysisVO();
            vo.setCode("客户ID: " + debt.getPartnerId());
            BigDecimal remain = debt.getAmount().subtract(debt.getPaidAmount() != null ? debt.getPaidAmount() : BigDecimal.ZERO);
            vo.setDescription("拖欠货款: ¥" + remain);

            // 账单产生时间到现在过了多久
            long createTime = debt.getCreateTime() != null ? debt.getCreateTime().getTime() : currentTime;
            int days = (int) ((currentTime - createTime) / (1000 * 3600 * 24));
            vo.setDays(days);

            // 评级
            if (days < 30) {
                vo.setLevel("🟢 信用期内 (0-30天)");
            } else if (days < 90) {
                vo.setLevel("🟡 催收预警 (30-90天)");
            } else {
                vo.setLevel("🔴 坏账高风险 (>90天)");
            }
            result.add(vo);
        }
        return result;
    }

}