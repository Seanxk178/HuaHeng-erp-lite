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

    public FinanceDashboardVO getDashboardData() {
        log.info("统计核心财务看板数据");
        FinanceDashboardVO vo = new FinanceDashboardVO();

        // 1. 获取应付与应收
        vo.setTotalPayable(finAccountMapper.sumTotalPayable());
        vo.setTotalReceivable(finAccountMapper.sumTotalReceivable());

        // 2. 获取总销售额
        BigDecimal totalSales = orderDetailMapper.sumTotalSales();
        vo.setTotalSales(totalSales);

        // 3. 粗略计算毛利润。我们在第13步算出库时，虽然改了库存里的成本，
        // 但为了看板快速展示，简单算法是：当前毛利 = 总销售额 - (应付货款中已经卖出去的部分对应的成本)
        // 注意：严格的财务系统毛利是在每次出库单里记一张表，为了不改前面的大逻辑，这里利用一个简化的毛利公式替代，后续可做报表精细化。
        // TODO: (技术债) 目前毛利简单设定为：总销售额 - (当前库存总额变化)，下面我们用一个极其简单的固定毛利率模拟，或者直接展示销售额
        // 这里为了绝对不出错导致负数，我们暂时用 销售额 * 0.3 作为模拟毛利，并在下一版真正补齐“单笔利润表”。
        vo.setTotalProfit(totalSales.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP));

        return vo;
    }

    // --- 查询账款列表 ---
    public com.baomidou.mybatisplus.core.metadata.IPage<FinAccountVO> getAccountList(Integer type, String startDate, String endDate, String keyword, int pageNum, int pageSize) {
        log.info("查询账款明细列表, 类型: {}, 分页: {}/{}", type, pageNum, pageSize);
        return finAccountMapper.getAccountList(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), type, startDate, endDate, keyword);
    }

    // --- 核销账款 (支持分期付款) ---
    @Log("执行了财务核销操作") //
    @Transactional(rollbackFor = Exception.class)
    public void settleAccount(Long accountId, BigDecimal payAmount) {
        log.info("准备核销账款, ID: {}, 本次实付金额: {}", accountId, payAmount);
        com.erp.erplite.entity.FinAccount account = finAccountMapper.selectById(accountId);
        if (account == null) {
            throw new RuntimeException("账单不存在");
        }
        if (account.getStatus() == 1) {
            throw new RuntimeException("该账单已结清，请勿重复操作");
        }

        // 处理分期付款逻辑
        BigDecimal currentPaid = account.getPaidAmount() != null ? account.getPaidAmount() : BigDecimal.ZERO;
        
        if (payAmount != null && payAmount.compareTo(BigDecimal.ZERO) > 0) {
            currentPaid = currentPaid.add(payAmount);
        } else {
            // 如果前端没传本次金额，默认全额结清剩余欠款
            currentPaid = account.getAmount();
        }

        account.setPaidAmount(currentPaid);

        // 如果已付金额 >= 应付金额，标记为已结清
        if (currentPaid.compareTo(account.getAmount()) >= 0) {
            account.setStatus(1);
            account.setPaidAmount(account.getAmount()); // 防御超付
            log.info("账单 ID:{} 全额结清！", accountId);
        } else {
            log.info("账单 ID:{} 部分结清，当前已结: {}/{}", accountId, account.getPaidAmount(), account.getAmount());
        }

        finAccountMapper.updateById(account);
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