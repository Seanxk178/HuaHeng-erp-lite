package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.Stock;
import com.erp.erplite.entity.StockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("<script>" +
            "SELECT " +
            "g.id AS goodsId, " +
            "g.code AS goodsCode, " +
            "g.name AS goodsName, " +
            "g.unit AS unit, " +
            "s.warehouse_id AS warehouseId, " +
            "w.name AS warehouseName, " +
            "IFNULL(s.quantity, 0) AS quantity, " +
            "IFNULL(s.total_cost, 0.00) AS totalCost " +
            "FROM base_goods g " +
            "LEFT JOIN inv_stock s ON g.id = s.goods_id " +
            "LEFT JOIN base_warehouse w ON s.warehouse_id = w.id " +
            "<where> " +
            "<if test='warehouseId != null'> AND s.warehouse_id = #{warehouseId} </if> " +
            "</where>" +
            "ORDER BY s.warehouse_id, g.code" +
            "</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<StockVO> getStockLedger(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<StockVO> page,
            @Param("warehouseId") Long warehouseId);

    @Select("SELECT " +
            "g.id AS goodsId, " +
            "g.code AS goodsCode, " +
            "g.name AS goodsName, " +
            "g.unit AS unit, " +
            "s.warehouse_id AS warehouseId, " +
            "IFNULL(s.quantity, 0) AS quantity, " +
            "IFNULL(s.total_cost, 0.00) AS totalCost " +
            "FROM inv_stock s " +
            "JOIN base_goods g ON g.id = s.goods_id " +
            "ORDER BY s.total_cost DESC " +
            "LIMIT 5")
    java.util.List<StockVO> getTop5Stock();

    // 前端开单时的联想搜索与库存透出
    @Select("<script>" +
            "SELECT " +
            "g.id AS goodsId, " +
            "g.code AS goodsCode, " +
            "g.name AS goodsName, " +
            "g.spec AS spec, " +
            "g.unit AS unit, " +
            "g.default_price AS defaultPrice, " +
            "IFNULL(s.quantity, 0) AS quantity " +
            "FROM base_goods g " +
            // 注意：把 warehouse_id 放在 ON 条件里，这样即使仓库里从没放过这个商品，也会查出来显示为 0
            "LEFT JOIN inv_stock s ON g.id = s.goods_id " +
            "<if test='warehouseId != null'> AND s.warehouse_id = #{warehouseId} </if> " +
            "WHERE g.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'> AND (g.name LIKE CONCAT('%', #{keyword}, '%') OR g.code LIKE CONCAT('%', #{keyword}, '%')) </if> " +
            "ORDER BY g.code " +
            "LIMIT 20" +
            "</script>")
    java.util.List<StockVO> searchGoodsWithStock(@Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    // 原子增加库存（入库用）
    @Update("UPDATE inv_stock SET quantity = quantity + #{quantity}, total_cost = total_cost + #{cost} " +
            "WHERE goods_id = #{goodsId} AND warehouse_id = #{warehouseId}")
    int addStock(@Param("goodsId") Long goodsId, @Param("warehouseId") Long warehouseId, 
                 @Param("quantity") BigDecimal quantity, @Param("cost") BigDecimal cost);

    // 原子扣减库存（出库/退货用），带数量校验防超卖
    @Update("UPDATE inv_stock SET quantity = quantity - #{quantity}, total_cost = GREATEST(0, total_cost - #{cost}) " +
            "WHERE goods_id = #{goodsId} AND warehouse_id = #{warehouseId} AND quantity >= #{quantity}")
    int reduceStock(@Param("goodsId") Long goodsId, @Param("warehouseId") Long warehouseId, 
                    @Param("quantity") BigDecimal quantity, @Param("cost") BigDecimal cost);
}
