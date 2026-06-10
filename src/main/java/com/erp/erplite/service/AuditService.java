package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.erplite.common.BusinessException;
import com.erp.erplite.common.SystemConstants;
import com.erp.erplite.common.UserContext;
import com.erp.erplite.entity.*;
import com.erp.erplite.mapper.SysAuditTaskMapper;
import com.erp.erplite.mapper.GoodsMapper;
import com.erp.erplite.mapper.PartnerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SysAuditTaskMapper auditTaskMapper;
    private final GoodsMapper goodsMapper;
    private final PartnerMapper partnerMapper;
    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS sys_audit_task (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "module VARCHAR(50), " +
                "entity_id BIGINT, " +
                "original_data TEXT, " +
                "new_data TEXT, " +
                "type VARCHAR(20), " +
                "status INT DEFAULT 0, " +
                "applicant_user BIGINT, " +
                "applicant_name VARCHAR(50), " +
                "approver_user BIGINT, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        try {
            jdbcTemplate.execute(createTableSql);
            log.info("sys_audit_task table initialized.");

            // 初始化主管角色
            jdbcTemplate.execute(
                    "INSERT INTO sys_role (role_key, role_name) SELECT 'purchase_manager', '采购主管' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'purchase_manager')");
            jdbcTemplate.execute(
                    "INSERT INTO sys_role (role_key, role_name) SELECT 'sales_manager', '销售主管' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'sales_manager')");

            // 为主管分配基础菜单
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 1 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 2 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 3 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 4 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 6 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");
            jdbcTemplate.execute(
                    "INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT id, 7 FROM sys_role WHERE role_key IN ('purchase_manager', 'sales_manager')");

            log.info("Manager roles seeded.");
        } catch (Exception e) {
            log.error("Failed to initialize sys_audit_task table or manager roles", e);
        }
    }

    private Long getCurrentUserId() {
        User user = UserContext.get();
        return user != null ? user.getId() : 1L;
    }

    private String getCurrentUserName() {
        User user = UserContext.get();
        return user != null ? user.getUsername() : "Admin";
    }

    /**
     * 提交审核任务
     */
    public boolean submitAuditTask(String module, Long entityId, String originalData, String newData, String type) {
        SysAuditTask task = new SysAuditTask();
        task.setModule(module);
        task.setEntityId(entityId);
        task.setOriginalData(originalData);
        task.setNewData(newData);
        task.setType(type);
        task.setStatus(0); // 0-待审核
        task.setApplicantUser(getCurrentUserId());
        task.setApplicantName(getCurrentUserName());
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        auditTaskMapper.insert(task);

        User user = UserContext.get();
        if (user != null && ("admin".equals(user.getRole()) ||
                "ROLE_ADMIN".equals(user.getRole()) ||
                "ROLE_FINANCE".equals(user.getRole()) ||
                "finance".equals(user.getRole()) ||
                "purchase_manager".equals(user.getRole()) ||
                "sales_manager".equals(user.getRole()))) {
            try {
                approveTask(task.getId());
                return true;
            } catch (Exception e) {
                log.error("Admin auto approve failed", e);
                throw new com.erp.erplite.common.BusinessException("管理员直接执行失败: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * 获取待审核列表
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuditTask> getPendingTasks(int pageNum, int pageSize) {
        QueryWrapper<SysAuditTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0).orderByDesc("create_time");
        
        // 部门隔离级别的数据过滤 (商务与采购合并为同一大部门)
        User currentUser = UserContext.get();
        if (currentUser != null && !SystemConstants.ROLE_ADMIN.equals(currentUser.getRole()) && !"admin".equals(currentUser.getRole())) {
            if (SystemConstants.ROLE_SALES_MANAGER.equals(currentUser.getRole()) || SystemConstants.ROLE_PURCHASE_MANAGER.equals(currentUser.getRole())) {
                queryWrapper.inSql("applicant_id", "SELECT id FROM sys_user WHERE role IN ('ROLE_SALES', 'sales', 'ROLE_PURCHASE', 'purchase', 'ROLE_WAREHOUSE', 'warehouse')");
            } else {
                queryWrapper.eq("id", -1); // 其他角色不允许看
            }
        }

        return auditTaskMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), queryWrapper);
    }

    /**
     * 获取全部审核历史
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuditTask> getHistoryTasks(int pageNum, int pageSize) {
        QueryWrapper<SysAuditTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        
        // 部门隔离级别的数据过滤 (商务与采购合并为同一大部门)
        User currentUser = UserContext.get();
        if (currentUser != null && !SystemConstants.ROLE_ADMIN.equals(currentUser.getRole()) && !"admin".equals(currentUser.getRole())) {
            if (SystemConstants.ROLE_SALES_MANAGER.equals(currentUser.getRole()) || SystemConstants.ROLE_PURCHASE_MANAGER.equals(currentUser.getRole())) {
                queryWrapper.inSql("applicant_id", "SELECT id FROM sys_user WHERE role IN ('ROLE_SALES', 'sales', 'ROLE_PURCHASE', 'purchase', 'ROLE_WAREHOUSE', 'warehouse')");
            } else {
                queryWrapper.eq("id", -1);
            }
        }
        
        return auditTaskMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), queryWrapper);
    }

    /**
     * 审核通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long taskId) throws Exception {
        SysAuditTask task = auditTaskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new BusinessException("任务不存在或已被处理");
        }

        // 执行具体的应用逻辑
        if ("UPDATE".equals(task.getType())) {
            if ("goods".equals(task.getModule())) {
                Goods goods = objectMapper.readValue(task.getNewData(), Goods.class);
                goods.setId(task.getEntityId());
                goodsMapper.updateById(goods);
            } else if ("partner".equals(task.getModule())) {
                Partner partner = objectMapper.readValue(task.getNewData(), Partner.class);
                partner.setId(task.getEntityId());
                partnerMapper.updateById(partner);
            }
        } else if ("DELETE".equals(task.getType())) {
            if ("goods".equals(task.getModule())) {
                goodsMapper.deleteById(task.getEntityId());
            } else if ("partner".equals(task.getModule())) {
                partnerMapper.deleteById(task.getEntityId());
            }
        } else if ("REVERT".equals(task.getType()) && "order".equals(task.getModule())) {
            orderService.cancelOrder(task.getEntityId());
        }

        task.setStatus(1); // 1-已通过
        task.setApproverUser(getCurrentUserId());
        task.setUpdateTime(new Date());
        auditTaskMapper.updateById(task);
    }

    /**
     * 驳回
     */
    public void rejectTask(Long taskId) {
        SysAuditTask task = auditTaskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new BusinessException("任务不存在或已被处理");
        }
        task.setStatus(2); // 2-已驳回
        task.setApproverUser(getCurrentUserId());
        task.setUpdateTime(new Date());
        auditTaskMapper.updateById(task);
    }
}
