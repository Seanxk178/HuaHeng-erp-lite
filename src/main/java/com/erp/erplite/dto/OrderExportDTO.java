package com.erp.erplite.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 历史订单导出专用 DTO，将数字状态/类型转为中文
 */
@Data
public class OrderExportDTO {

    @ExcelProperty("单据编号")
    private String orderNo;

    @ExcelProperty("单据类型")
    private String typeStr;

    @ExcelProperty("往来单位")
    private String partnerName;

    @ExcelProperty("商品名称")
    private String goodsName;

    @ExcelProperty("业务数量")
    private BigDecimal quantity;

    @ExcelProperty("总金额(元)")
    private BigDecimal totalAmount;

    @ExcelProperty("单据状态")
    private String statusStr;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
