package com.erp.erplite.entity;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinanceDashboardVO {
    // 总应付账款 (欠供应商的钱)
    private BigDecimal totalPayable;
    // 总应收账款 (客户欠我们的钱)
    private BigDecimal totalReceivable;
    // 总销售额
    private BigDecimal totalSales;
    // 总毛利润 (销售额 - 结转出库成本)
    private BigDecimal totalProfit;
}