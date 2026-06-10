package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.AnalysisVO;
import com.erp.erplite.entity.StockVO;
import com.erp.erplite.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.erp.erplite.common.RequireRole;

import java.util.List;

import com.erp.erplite.common.SystemConstants;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@com.erp.erplite.common.RequireRole({SystemConstants.ROLE_SALES, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
public class StockController {

    private final StockService stockService;

    /**
     * 获取库存台账列表
     * 请求方式: GET /stock/ledger
     */
    @GetMapping("/ledger")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<StockVO>> getStockLedger(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(stockService.getStockLedger(pageNum, pageSize, warehouseId));
    }

    @GetMapping("/ageAnalysis")
    public Result<java.util.List<AnalysisVO>> analyzeStockAge() {
        return Result.success(stockService.analyzeStockAge());
    }

    /**
     * 开单时的联想搜索与实时库存透出
     * 请求方式: GET /stock/search?warehouseId=1&keyword=测试
     */
    @GetMapping("/search")
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_SALES, SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER}) // 允许业务员调用
    public Result<java.util.List<StockVO>> searchGoodsWithStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword) {
        return Result.success(stockService.searchGoodsWithStock(warehouseId, keyword));
    }

    /**
     * 导出库存台账 Excel
     * 请求方式: GET /stock/export
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
    @GetMapping("/export")
    public void exportStockLedger(
            @RequestParam(required = false) Long warehouseId,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            // 1. 设置响应头，告诉浏览器这是一个 Excel 下载文件
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 文件名进行 URLEncoder 编码，防止中文乱码
            String fileName = java.net.URLEncoder.encode("实时库存台账报表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 2 & 3. 分批拉取数据并写入，防止数据量大时OOM或数据截断
            try (com.alibaba.excel.ExcelWriter excelWriter = com.alibaba.excel.EasyExcel.write(response.getOutputStream(), com.erp.erplite.entity.StockVO.class).build()) {
                com.alibaba.excel.write.metadata.WriteSheet writeSheet = com.alibaba.excel.EasyExcel.writerSheet("库存台账").build();
                int pageNum = 1;
                int pageSize = 1000;
                while (true) {
                    java.util.List<com.erp.erplite.entity.StockVO> dataList = stockService.getStockLedger(pageNum, pageSize, warehouseId).getRecords();
                    if (dataList == null || dataList.isEmpty()) {
                        break;
                    }
                    excelWriter.write(dataList, writeSheet);
                    if (dataList.size() < pageSize) {
                        break;
                    }
                    pageNum++;
                }
            }
        } catch (Exception e) {
            // 导出失败时不返回文件，而是返回错误信息 JSON
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            try {
                response.getWriter().println("{\"code\": 500, \"message\": \"导出Excel失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}