package com.erp.erplite.controller;

import com.erp.erplite.common.Log;
import com.erp.erplite.common.Result;
import com.erp.erplite.entity.Goods;
import com.erp.erplite.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理 API 接口
 */
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor // 依赖注入
public class GoodsController {

    private final GoodsService goodsService;

    /**
     * 获取商品列表接口
     * 请求方式: GET /goods/list
     */
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
}