package com.erp.erplite.entity;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderVO {
    @ExcelIgnore
    private Long orderId;

    @ExcelProperty("单据编号")
    private String orderNo;

    @ExcelProperty("往来单位")
    private String partnerName; // 供应商名称/客户名称

    @ExcelProperty("商品名称")
    private String goodsName;   // 商品名称

    @ExcelProperty("业务数量")
    private java.math.BigDecimal quantity;   // 数量

    @ExcelProperty("总金额(元)")
    private BigDecimal totalAmount; // 总金额

    @ExcelProperty("单据状态")
    private Integer status;     // 审核状态
    
    @ExcelProperty("单据类型")
    private Integer type;

    @ExcelProperty("创建人ID")
    private Long createBy;

    @ExcelProperty("创建时间")
    private Date createTime;
}