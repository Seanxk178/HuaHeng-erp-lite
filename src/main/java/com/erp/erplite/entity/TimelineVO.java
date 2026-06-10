package com.erp.erplite.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TimelineVO {
    private String title;       // 事件节点名称 (例如: 提交采购申请)
    private String content;     // 事件详情描述 (例如: 申请入库 50 件)
    private String operator;    // 操作人
    private Date time;          // 发生时间
    private String type;        // 节点类型 (PRIMARY, SUCCESS, WARNING, DANGER) 用于前端渲染颜色
}
