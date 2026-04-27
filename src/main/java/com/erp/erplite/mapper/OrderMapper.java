package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.Order;
import com.erp.erplite.entity.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // 联合主表、明细表、商品表、往来单位表，查出所有的待审单据
    @Select("SELECT " +
            "o.id AS orderId, o.order_no AS orderNo, o.status AS status, o.create_time AS createTime, " +
            "p.name AS partnerName, g.name AS goodsName, d.quantity AS quantity, d.total_amount AS totalAmount " +
            "FROM doc_order o " +
            "JOIN doc_order_detail d ON o.id = d.order_id " +
            "JOIN base_goods g ON d.goods_id = g.id " +
            "JOIN base_partner p ON o.partner_id = p.id " +
            "WHERE o.type = 1 AND o.status = 0 ORDER BY o.create_time DESC")
    List<OrderVO> getPendingInboundOrders();
}