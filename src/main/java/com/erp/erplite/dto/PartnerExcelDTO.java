package com.erp.erplite.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class PartnerExcelDTO {
    
    @ExcelProperty("编码")
    private String code;
    
    @ExcelProperty("名称")
    private String name;
    
    @ExcelProperty("类型(填:供应商 或 客户)")
    private String typeStr;
    
    @ExcelProperty("联系人")
    private String contact;
    
    @ExcelProperty("电话")
    private String phone;
    
    @ExcelProperty("地址")
    private String address;
    
    @ExcelProperty("主营产品")
    private String mainProduct;
}
