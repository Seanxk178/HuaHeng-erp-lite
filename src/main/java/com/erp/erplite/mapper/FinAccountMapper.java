package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.FinAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

@Mapper
public interface FinAccountMapper extends BaseMapper<FinAccount> {

    // 统计总应付账款 (type = 1 且未结清 status = 0)
    @Select("SELECT IFNULL(SUM(amount - IFNULL(paid_amount, 0)), 0.00) FROM fin_account WHERE type = 1 AND status = 0")
    BigDecimal sumTotalPayable();

    // 统计总应收账款 (type = 2 且未结清 status = 0)
    @Select("SELECT IFNULL(SUM(amount - IFNULL(paid_amount, 0)), 0.00) FROM fin_account WHERE type = 2 AND status = 0")
    BigDecimal sumTotalReceivable();

    // 查询账款明细列表 (可根据类型筛选)
    @Select("<script>" +
            "SELECT " +
            "a.id AS accountId, p.name AS partnerName, o.order_no AS orderNo, " +
            "a.type AS type, a.amount AS amount, IFNULL(a.paid_amount, 0) AS paidAmount, a.status AS status, a.create_time AS createTime " +
            "FROM fin_account a " +
            "LEFT JOIN base_partner p ON a.partner_id = p.id " +
            "LEFT JOIN doc_order o ON a.order_id = o.id " +
            "WHERE 1=1 " +
            "<if test='type != null'> AND a.type = #{type} </if> " +
            "<if test='startDate != null and startDate != \"\"'> AND a.create_time &gt;= #{startDate} </if> " +
            "<if test='endDate != null and endDate != \"\"'> AND a.create_time &lt;= #{endDate} </if> " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  AND (o.order_no LIKE CONCAT('%', #{keyword}, '%') OR p.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY a.status ASC, a.create_time DESC" +
            "</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<com.erp.erplite.entity.FinAccountVO> getAccountList(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.erp.erplite.entity.FinAccountVO> page,
            @org.apache.ibatis.annotations.Param("type") Integer type,
            @org.apache.ibatis.annotations.Param("startDate") String startDate,
            @org.apache.ibatis.annotations.Param("endDate") String endDate,
            @org.apache.ibatis.annotations.Param("keyword") String keyword);
}