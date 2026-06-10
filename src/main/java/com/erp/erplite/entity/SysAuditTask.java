package com.erp.erplite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_audit_task")
public class SysAuditTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String module; // 业务模块，如 "goods", "partner"
    private Long entityId; // 目标实体ID
    
    private String originalData; // 原数据快照JSON
    private String newData; // 修改后数据JSON
    
    private String type; // UPDATE-修改, DELETE-删除, REVERT-撤销单据
    private Integer status; // 0-待审核, 1-已通过, 2-已驳回
    
    @com.baomidou.mybatisplus.annotation.TableField("applicant_id")
    private Long applicantUser; // 申请人ID
    private String applicantName; // 申请人姓名（冗余）
    
    @com.baomidou.mybatisplus.annotation.TableField("approver_id")
    private Long approverUser; // 审批人ID
    
    private Date createTime;
    private Date updateTime;
}
