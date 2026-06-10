package com.erp.erplite.service;

import com.erp.erplite.common.SystemConstants;
import com.erp.erplite.common.UserContext;
import com.erp.erplite.entity.AnalysisVO;
import com.erp.erplite.entity.OrderVO;
import com.erp.erplite.entity.TodoVO;
import com.erp.erplite.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final OrderService orderService;
    private final FinanceService financeService;

    /**
     * 根据当前登录用户的角色，动态聚合待办任务
     */
    public List<TodoVO> getMyTodos() {
        User user = UserContext.get();
        if (user == null || user.getRole() == null) {
            return new ArrayList<>();
        }

        String role = user.getRole();
        List<TodoVO> todos = new ArrayList<>();

        // 财务和管理员拥有核心单据审批权和财务催收职责
        boolean isFinanceOrAdmin = SystemConstants.ROLE_ADMIN.equals(role) || SystemConstants.ROLE_FINANCE.equals(role);

        if (isFinanceOrAdmin) {
            // 1. 聚合：待审核的业务单据 (最多拉取前500条待办)
            List<OrderVO> pendingOrders = orderService.getPendingOrders(1, 500).getRecords();
            for (OrderVO order : pendingOrders) {
                TodoVO todo = new TodoVO();
                todo.setId(order.getOrderNo());
                todo.setModule("ORDER");
                todo.setCreateTime(order.getCreateTime());
                todo.setUrgency("NORMAL");

                if (order.getType() != null && order.getType() == 1) {
                    todo.setTitle("🚨 待审核：采购入库单");
                    todo.setActionUrl("/order/inbound/approve");
                } else {
                    todo.setTitle("📦 待发货：销售出库单");
                    todo.setActionUrl("/order/outbound/approve");
                }
                
                todo.setDescription(String.format("往来单位: %s | 涉及商品: %s | 业务总额: ¥%s", 
                        order.getPartnerName() != null ? order.getPartnerName() : "-",
                        order.getGoodsName() != null ? order.getGoodsName() : "-",
                        order.getTotalAmount()));
                todos.add(todo);
            }

            // 2. 聚合：财务高风险账龄预警 (超30天提醒，超90天高危)
            List<AnalysisVO> debts = financeService.analyzeDebtAge();
            for (AnalysisVO debt : debts) {
                if (debt.getDays() >= 30) {
                    TodoVO todo = new TodoVO();
                    todo.setId(debt.getCode());
                    todo.setTitle(debt.getDays() >= 90 ? "🔴 坏账高危预警" : "🟡 账款催收预警");
                    todo.setModule("FINANCE");
                    todo.setActionUrl("/finance/account");
                    todo.setCreateTime(new java.util.Date()); // 实时分析，时间取当前
                    todo.setUrgency(debt.getDays() >= 90 ? "HIGH" : "WARNING");
                    todo.setDescription(String.format("%s | 已逾期天数: %d 天", debt.getDescription(), debt.getDays()));
                    todos.add(todo);
                }
            }
        }
        
        // 按照紧急程度和时间排序（高危优先，时间早的优先）
        todos.sort((t1, t2) -> {
            int u1 = getUrgencyScore(t1.getUrgency());
            int u2 = getUrgencyScore(t2.getUrgency());
            if (u1 != u2) {
                return Integer.compare(u2, u1); // 降序，分高在前
            }
            if (t1.getCreateTime() != null && t2.getCreateTime() != null) {
                return t1.getCreateTime().compareTo(t2.getCreateTime()); // 升序，早的在前
            }
            return 0;
        });

        return todos;
    }

    private int getUrgencyScore(String urgency) {
        if ("HIGH".equals(urgency)) return 3;
        if ("WARNING".equals(urgency)) return 2;
        return 1; // NORMAL
    }
}
