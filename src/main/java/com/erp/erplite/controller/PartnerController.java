package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.Partner;
import com.erp.erplite.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * 获取往来单位列表
     * 请求方式: GET /partner/list
     */
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
}