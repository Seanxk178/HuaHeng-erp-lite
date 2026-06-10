package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.Order;
import com.erp.erplite.entity.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // 联合主表、明细表、商品表、往来单位表，查出所有的待审单据 (包含入库和出库)
    @Select("SELECT " +
            "o.id AS orderId, o.order_no AS orderNo, o.status AS status, o.create_time AS createTime, " +
            "o.type AS type, " +
            "p.name AS partnerName, g.name AS goodsName, d.quantity AS quantity, d.total_amount AS totalAmount " +
            "FROM doc_order o " +
            "JOIN doc_order_detail d ON o.id = d.order_id " +
            "JOIN base_goods g ON d.goods_id = g.id " +
            "JOIN base_partner p ON o.partner_id = p.id " +
            "WHERE o.type IN (1, 2, 4) AND o.status = 0 ORDER BY o.create_time DESC")
    com.baomidou.mybatisplus.core.metadata.IPage<OrderVO> getPendingOrders(com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderVO> page);

    // 查询历史生效单据 (status = 1)
    @Select("<script>" +
            "SELECT " +
            "o.id AS orderId, o.order_no AS orderNo, o.type AS type, o.status AS status, o.create_time AS createTime, o.create_by AS createBy, " +
            "p.name AS partnerName, g.name AS goodsName, d.quantity AS quantity, " +
            "d.unit_price AS unitPrice, d.total_amount AS totalAmount " +
            "FROM doc_order o " +
            "LEFT JOIN doc_order_detail d ON o.id = d.order_id " +
            "LEFT JOIN base_goods g ON d.goods_id = g.id " +
            "LEFT JOIN base_partner p ON o.partner_id = p.id " +
            "WHERE o.status != 0 " +
            "<if test='type != null'> AND o.type = #{type} </if> " +
            "<if test='startDate != null and startDate != \"\"'> AND o.create_time &gt;= #{startDate} </if> " +
            "<if test='endDate != null and endDate != \"\"'> AND o.create_time &lt;= #{endDate} </if> " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%') OR p.name LIKE CONCAT('%', #{keyword}, '%') OR o.contract_no LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY o.create_time DESC" +
            "</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<OrderVO> getHistoryOrders(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderVO> page,
            @org.apache.ibatis.annotations.Param("type") Integer type,
            @org.apache.ibatis.annotations.Param("startDate") String startDate,
            @org.apache.ibatis.annotations.Param("endDate") String endDate,
            @org.apache.ibatis.annotations.Param("keyword") String keyword);
}


