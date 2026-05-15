package com.erp.erplite.service;

import com.erp.erplite.entity.Stock;
import com.erp.erplite.entity.StockVO;
import com.erp.erplite.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;

    /**
     * 获取实时库存台账
     */
    public com.baomidou.mybatisplus.core.metadata.IPage<StockVO> getStockLedger(int pageNum, int pageSize) {
        log.info("查询实时库存台账(含财务), 分页: {}/{}", pageNum, pageSize);
        com.baomidou.mybatisplus.core.metadata.IPage<StockVO> page = stockMapper
                .getStockLedger(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize));

        // 遍历计算移动加权平均单价
        for (StockVO vo : page.getRecords()) {
            if (vo.getQuantity() != null && vo.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0
                    && vo.getTotalCost() != null) {
                // 单价 = 总成本 / 数量。注意：除法必须指定保留几位小数和舍入规则，否则遇到除不尽会报错！
                // ROUND_HALF_UP 就是四舍五入
                java.math.BigDecimal avg = vo.getTotalCost().divide(vo.getQuantity(), 2,
                        java.math.RoundingMode.HALF_UP);
                vo.setAvgPrice(avg);
            } else {
                vo.setAvgPrice(java.math.BigDecimal.ZERO);
            }
        }
        return page;
    }

    /**
     * 库龄分析 (分析商品在仓库里躺了多久没动过)
     * 注意：严格的 ERP 库龄是用批次管理的（先进先出）。
     * 为了不把系统改得过于庞大，我们这里采用一种实用的近似算法：
     * 用 inv_stock 表的 update_time (最后一次发生活动的时间) 到现在相隔的天数作为该商品的“整体滞销天数”。
     */
    public java.util.List<com.erp.erplite.entity.AnalysisVO> analyzeStockAge() {
        log.info("执行库龄分析");
        java.util.List<com.erp.erplite.entity.AnalysisVO> result = new java.util.ArrayList<>();

        // 查询所有有库存的商品 (必须是有库存的)
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.gt("quantity", 0);
        java.util.List<Stock> stockList = stockMapper.selectList(query);

        long currentTime = System.currentTimeMillis();

        for (Stock stock : stockList) {
            com.erp.erplite.entity.AnalysisVO vo = new com.erp.erplite.entity.AnalysisVO();

            // 拿到商品名称(这里简单起见，如果需要名称可以连表查，这里先演示计算逻辑)
            vo.setCode("商品ID: " + stock.getGoodsId());

            // 计算天数差: (当前毫秒 - 最后更新毫秒) / (1天的毫秒数)
            long lastTime = stock.getUpdateTime() != null ? stock.getUpdateTime().getTime() : currentTime;
            int days = (int) ((currentTime - lastTime) / (1000 * 3600 * 24));
            vo.setDays(days);
            vo.setDescription("库存滞压: " + stock.getQuantity() + "件");

            // 评级
            if (days < 30) {
                vo.setLevel("🟢 活跃流转 (0-30天)");
            } else if (days < 90) {
                vo.setLevel("🟡 滞销预警 (30-90天)");
            } else {
                vo.setLevel("🔴 死库存危险 (>90天)");
            }
            result.add(vo);
        }
        return result;
    }
}