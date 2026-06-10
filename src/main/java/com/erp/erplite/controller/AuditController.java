package com.erp.erplite.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.erplite.common.RequireRole;
import com.erp.erplite.common.Result;
import com.erp.erplite.common.SystemConstants;
import com.erp.erplite.entity.SysAuditTask;
import com.erp.erplite.service.AuditService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Data
    public static class AuditParam {
        private String module;
        private Long entityId;
        private String originalData;
        private String newData;
        private String type; // UPDATE, DELETE, REVERT
    }

    /**
     * 提交修改/删除/撤销审核
     */
    @PostMapping("/submit")
    public Result<String> submitAuditTask(@RequestBody AuditParam param) {
        boolean autoApproved = auditService.submitAuditTask(param.getModule(), param.getEntityId(), param.getOriginalData(), param.getNewData(), param.getType());
        if (autoApproved) {
            return Result.success("您拥有主管/管理员权限，操作已直接生效");
        }
        return Result.success("已提交审核请求，等待主管审批");
    }

    /**
     * 获取待审批列表
     */
    @RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @GetMapping("/pending")
    public Result<IPage<SysAuditTask>> getPendingTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(auditService.getPendingTasks(pageNum, pageSize));
    }

    /**
     * 获取全部审核历史
     */
    @RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @GetMapping("/history")
    public Result<IPage<SysAuditTask>> getHistoryTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(auditService.getHistoryTasks(pageNum, pageSize));
    }

    /**
     * 审批通过
     */
    @RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @PostMapping("/approve/{taskId}")
    public Result<String> approveTask(@PathVariable Long taskId) {
        try {
            auditService.approveTask(taskId);
            return Result.success("审批通过，数据已更新");
        } catch (Exception e) {
            return Result.error("审批失败: " + e.getMessage());
        }
    }

    /**
     * 审批驳回
     */
    @RequireRole({SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
    @PostMapping("/reject/{taskId}")
    public Result<String> rejectTask(@PathVariable Long taskId) {
        auditService.rejectTask(taskId);
        return Result.success("审批已驳回");
    }
}
