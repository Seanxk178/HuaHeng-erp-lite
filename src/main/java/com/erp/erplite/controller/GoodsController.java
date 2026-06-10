package com.erp.erplite.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.erp.erplite.common.Log;
import com.erp.erplite.common.Result;
import com.erp.erplite.entity.Goods;
import com.erp.erplite.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.erp.erplite.common.SystemConstants;

/**
 * 商品管理 API 接口
 */
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor // 依赖注入
@com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_PURCHASE_MANAGER}) // 需要管理员或采购权限
public class GoodsController {

    private final GoodsService goodsService;

    /**
     * 获取商品列表接口
     * 请求方式: GET /goods/list
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_SALES, SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @GetMapping("/list")
    public Result<List<Goods>> getList() {
        List<Goods> list = goodsService.getAllGoods();
        return Result.success(list);
    }

    /**
     * 新增商品接口
     * 请求方式: POST /goods/add
     * @RequestBody: 将前端传来的 JSON 数据自动转换为 Goods 对象
     */
    @Log("新增了商品基础数据")
    @PostMapping("/add")
    public Result<String> addGoods(@RequestBody Goods goods) {
        goodsService.addGoods(goods);
        return Result.success("商品添加成功");
    }

    /**
     * 更新商品接口
     */
    @Log("修改了商品基础数据")
    @PostMapping("/update")
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN})
    public Result<String> updateGoods(@RequestBody Goods goods) {
        goodsService.updateGoods(goods);
        return Result.success("商品修改成功");
    }

    /**
     * 导出商品基础数据模板/列表
     */
    @GetMapping("/export")
    public void exportGoods(jakarta.servlet.http.HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("商品基础数据表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            try (com.alibaba.excel.ExcelWriter excelWriter = com.alibaba.excel.EasyExcel.write(response.getOutputStream(), Goods.class).build()) {
                com.alibaba.excel.write.metadata.WriteSheet writeSheet = com.alibaba.excel.EasyExcel.writerSheet("商品资料").build();
                int pageNum = 1;
                int pageSize = 1000;
                while (true) {
                    com.baomidou.mybatisplus.core.metadata.IPage<Goods> iPage = goodsService.getGoodsPage(pageNum, pageSize);
                    List<Goods> dataList = iPage.getRecords();
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

    /**
     * 导入商品基础数据
     */
    @Log("批量导入了商品数据")
    @PostMapping("/import")
    public Result<String> importGoods(@RequestParam("file") MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(), Goods.class, new ReadListener<Goods>() {
                private static final int BATCH_COUNT = 100;
                private List<Goods> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                @Override
                public void invoke(Goods data, AnalysisContext context) {
                    cachedDataList.add(data);
                    if (cachedDataList.size() >= BATCH_COUNT) {
                        goodsService.importGoodsBatch(cachedDataList);
                        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    if (!cachedDataList.isEmpty()) {
                        goodsService.importGoodsBatch(cachedDataList);
                    }
                }
            }).sheet().doRead();
            return Result.success("商品数据导入成功");
        } catch (Exception e) {
            return Result.error("Excel导入失败: " + e.getMessage());
        }
    }
}