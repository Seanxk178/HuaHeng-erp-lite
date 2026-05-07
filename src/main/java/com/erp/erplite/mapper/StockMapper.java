package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.Stock;
import com.erp.erplite.entity.StockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT " +
            "g.id AS goodsId, " +
            "g.code AS goodsCode, " +
            "g.name AS goodsName, " +
            "g.unit AS unit, " +
            "s.warehouse_id AS warehouseId, " +
            "IFNULL(s.quantity, 0) AS quantity, " +
            "IFNULL(s.total_cost, 0.00) AS totalCost " +
            "FROM base_goods g " +
            "LEFT JOIN inv_stock s ON g.id = s.goods_id")
    com.baomidou.mybatisplus.core.metadata.IPage<StockVO> getStockLedger(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<StockVO> page);
}