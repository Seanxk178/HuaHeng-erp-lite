package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.Partner;
import com.erp.erplite.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import com.erp.erplite.dto.PartnerExcelDTO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

import com.erp.erplite.common.SystemConstants;

@RestController
@RequestMapping("/partner")
@RequiredArgsConstructor
@com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_SALES, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * 获取往来单位列表
     * 请求方式: GET /partner/list
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_SALES, SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @GetMapping("/list")
    public Result<List<Partner>> getList() {
        List<Partner> list = partnerService.getAllPartners();
        return Result.success(list);
    }

    /**
     * 新增往来单位
     * 请求方式: POST /partner/add
     */
    @PostMapping("/add")
    public Result<String> addPartner(@RequestBody Partner partner) {
        partnerService.addPartner(partner);
        return Result.success("往来单位添加成功");
    }

    /**
     * 更新往来单位
     */
    @PostMapping("/update")
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_ADMIN})
    public Result<String> updatePartner(@RequestBody Partner partner) {
        partnerService.updatePartner(partner);
        return Result.success("往来单位修改成功");
    }

    /**
     * 下载往来单位导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("往来单位导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), PartnerExcelDTO.class).sheet("模板").doWrite(new java.util.ArrayList<>());
    }

    /**
     * 批量导入往来单位
     */
    @PostMapping("/import")
    public Result<String> importPartners(@RequestParam("file") MultipartFile file) {
        try {
            List<PartnerExcelDTO> list = EasyExcel.read(file.getInputStream()).head(PartnerExcelDTO.class).sheet().doReadSync();
            if (list == null || list.isEmpty()) {
                return Result.error("导入文件为空或无数据");
            }
            try {
                String resultMsg = partnerService.batchAddPartners(list);
                return Result.success(resultMsg);
            } catch (RuntimeException e) {
                // Return as an error so the frontend highlights it in red, but some records might have been imported.
                return Result.error(e.getMessage());
            }
        } catch (Exception e) {
            return Result.error("Excel解析失败: " + e.getMessage());
        }
    }
}