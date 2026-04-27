package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.FinAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

@Mapper
public interface FinAccountMapper extends BaseMapper<FinAccount> {

    // 统计总应付账款 (type = 1 且未结清 status = 0)
    @Select("SELECT IFNULL(SUM(amount), 0.00) FROM fin_account WHERE type = 1 AND status = 0")
    BigDecimal sumTotalPayable();

    // 统计总应收账款 (type = 2 且未结清 status = 0)
    @Select("SELECT IFNULL(SUM(amount), 0.00) FROM fin_account WHERE type = 2 AND status = 0")
    BigDecimal sumTotalReceivable();

    // 查询账款明细列表 (可根据类型筛选)
    @Select("<script>" +
            "SELECT " +
            "a.id AS accountId, p.name AS partnerName, o.order_no AS orderNo, " +
            "a.type AS type, a.amount AS amount, a.status AS status, a.create_time AS createTime " +
            "FROM fin_account a " +
            "LEFT JOIN base_partner p ON a.partner_id = p.id " +
            "LEFT JOIN doc_order o ON a.order_id = o.id " +
            "WHERE 1=1 " +
            "<if test='type != null'> AND a.type = #{type} </if> " +
            "ORDER BY a.status ASC, a.create_time DESC" +
            "</script>")
    java.util.List<com.erp.erplite.entity.FinAccountVO> getAccountList(@org.apache.ibatis.annotations.Param("type") Integer type);
}