package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品表 Mapper 接口
 * 继承 BaseMapper 即可自动获得所有的单表 CRUD 方法
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
}