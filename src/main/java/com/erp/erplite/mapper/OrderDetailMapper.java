package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    // 统计总销售额 (关联主表 doc_order, 筛选 type = 2 销售出库单)
    @Select("SELECT IFNULL(SUM(d.total_amount), 0.00) " +
            "FROM doc_order_detail d " +
            "JOIN doc_order o ON d.order_id = o.id " +
            "WHERE o.type = 2")
    BigDecimal sumTotalSales();
}