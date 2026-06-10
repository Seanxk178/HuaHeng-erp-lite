package com.erp.erplite.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TodoVO {
    private String id;          // 业务ID (如单据编号、客户ID)
    private String title;       // 任务标题 (如：待审核入库单)
    private String description; // 任务描述摘要
    private String module;      // 所属模块 (ORDER, FINANCE 等)
    private String actionUrl;   // 前端跳转路由或操作标识
    private String urgency;     // 紧急程度 (NORMAL, WARNING, HIGH)
    private Date createTime;    // 任务产生时间
}