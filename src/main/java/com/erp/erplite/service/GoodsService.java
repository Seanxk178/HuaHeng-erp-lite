package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.Goods;
import com.erp.erplite.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品业务逻辑服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor // 配合 final 关键字，替代 @Autowired 进行依赖注入
public class GoodsService {

    private final GoodsMapper goodsMapper;

    /**
     * 查询所有商品列表
     */
    public List<Goods> getAllGoods() {
        log.info("开始查询所有商品列表");
        // selectList(null) 表示无条件查询所有
        return goodsMapper.selectList(null);
    }

    /**
     * 新增商品
     * 业务风险点：商品编码必须唯一
     */
    public void addGoods(Goods goods) {
        log.info("准备新增商品: {}", goods.getName());

        // 1. 校验编码是否已存在
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", goods.getCode());
        boolean exists = goodsMapper.exists(queryWrapper);

        if (exists) {
            log.warn("新增商品失败，编码已存在: {}", goods.getCode());
            // 抛出运行时异常，会被我们的 GlobalExceptionHandler 拦截并返回给前端
            throw new RuntimeException("商品编码已存在，请更换编码！");
        }

        // 2. 插入数据库
        goodsMapper.insert(goods);
        log.info("新增商品成功, ID: {}", goods.getId());
    }

    /**
     * 更新商品信息
     */
    public void updateGoods(Goods goods) {
        if (goods.getId() == null) throw new RuntimeException("商品ID不能为空");
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", goods.getCode()).ne("id", goods.getId());
        if (goodsMapper.exists(queryWrapper)) {
            throw new RuntimeException("商品编码已存在，请更换编码！");
        }
        goodsMapper.updateById(goods);
    }
}