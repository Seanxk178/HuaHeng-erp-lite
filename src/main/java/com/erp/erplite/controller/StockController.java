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

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@RequireRole({"warehouse", "finance", "admin"})
public class StockController {

    private final StockService stockService;

    /**
     * 获取库存台账列表
     * 请求方式: GET /stock/ledger
     */
    @GetMapping("/ledger")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<StockVO>> getStockLedger(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(stockService.getStockLedger(pageNum, pageSize));
    }

    @GetMapping("/ageAnalysis")
    public Result<java.util.List<AnalysisVO>> analyzeStockAge() {
        return Result.success(stockService.analyzeStockAge());
    }

    /**
     * 导出库存台账 Excel
     * 请求方式: GET /stock/export
     */
    @GetMapping("/export")
    public void exportStockLedger(jakarta.servlet.http.HttpServletResponse response) {
        try {
            // 1. 设置响应头，告诉浏览器这是一个 Excel 下载文件
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 文件名进行 URLEncoder 编码，防止中文乱码
            String fileName = java.net.URLEncoder.encode("实时库存台账报表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 2. 获取业务数据 (复用之前写好的台账查询逻辑，全量导出这里取前1000条或后续改为流式)
            java.util.List<com.erp.erplite.entity.StockVO> dataList = stockService.getStockLedger(1, 1000).getRecords();

            // 3. 使用 EasyExcel 写入数据并输出到浏览器
            com.alibaba.excel.EasyExcel.write(response.getOutputStream(), com.erp.erplite.entity.StockVO.class)
                    .sheet("库存台账")
                    .doWrite(dataList);

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