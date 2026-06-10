package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.OrderVO;
import com.erp.erplite.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.erp.erplite.common.RequireRole;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.erp.erplite.common.SystemConstants;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@RequireRole({SystemConstants.ROLE_SALES, SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_PURCHASE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN, SystemConstants.ROLE_PURCHASE_MANAGER, SystemConstants.ROLE_SALES_MANAGER})
public class OrderController {

    private final OrderService orderService;

    /**
     * 接收前端传来的出库参数 (可以复用入库的类，但为了语义清晰重新建一个)
     */
    @Data
    public static class OutboundParam {
        private Long partnerId; // 客户ID
        private Long goodsId;
        private Long warehouseId; // 出库仓库
        private java.math.BigDecimal quantity;
        private java.math.BigDecimal unitPrice; // 销售单价
        private String contractNo; // 关联合同号
        private String orderRemark; // 整单备注
        private String detailRemark; // 订单明细备注
    }

    /**
     * 出库接口
     * 请求方式: POST /order/outbound
     */
    @PostMapping("/outbound")
    public Result<String> outbound(@RequestBody OutboundParam param) {
        orderService.submitOutboundDraft(param.getPartnerId(), param.getWarehouseId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice(), param.getContractNo(), param.getOrderRemark(), param.getDetailRemark());
        return Result.success("出库申请已提交，等待财务审批发货");
    }
    /**
     * 驳回单据
     * 请求方式: POST /order/reject/{orderId}
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
    @PostMapping("/reject/{orderId}")
    public Result<String> rejectOrder(@PathVariable Long orderId) {
        orderService.rejectOrder(orderId);
        return Result.success("单据已驳回");
    }

    /**
     * 重新提交驳回的单据
     * 请求方式: POST /order/update/{orderId}
     */
    @PostMapping("/update/{orderId}")
    public Result<String> updateRejectedOrder(@PathVariable Long orderId, @RequestBody OutboundParam param) {
        orderService.updateRejectedOrder(orderId, param.getPartnerId(), param.getWarehouseId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice(), param.getContractNo(), param.getOrderRemark(), param.getDetailRemark());
        return Result.success("修改已保存，单据重新提交审核");
    }

    /**
     * 接收前端传来的入库参数
     */
    @Data
    public static class InboundParam {
        private Long partnerId; // 供应商ID
        private Long goodsId;
        private Long warehouseId; // 入库仓库
        private java.math.BigDecimal quantity;
        private java.math.BigDecimal unitPrice; // 采购单价
        private String contractNo; // 关联合同号
        private String orderRemark; // 整单备注
        private String detailRemark; // 订单明细备注
    }

    /**
     * 入库接口
     * 请求方式: POST /order/inbound
     */
    @PostMapping("/inbound")
    public Result<String> inbound(@RequestBody InboundParam param) {
        // 方法名改成我们刚才新写的 submitInboundDraft
        orderService.submitInboundDraft(param.getPartnerId(), param.getWarehouseId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice(), param.getContractNo(), param.getOrderRemark(), param.getDetailRemark());
        return Result.success("入库申请已提交，等待审核");
    }

    /**
     *  审核入库单
     * 请求方式: POST /order/approve/inbound/{orderId}
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
    @PostMapping("/approve/inbound/{orderId}")
    public Result<String> approveInbound(@PathVariable Long orderId) {
        orderService.approveInboundOrder(orderId);
        return Result.success("单据审核通过，已生效");
    }

    /**
     * 审核并执行出库单 (支持部分发货)
     * 请求方式: POST /order/approve/outbound/{orderId}?actualQuantity=xxx
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
    @PostMapping("/approve/outbound/{orderId}")
    public Result<String> approveOutbound(@PathVariable Long orderId, @RequestParam java.math.BigDecimal actualQuantity) {
        orderService.approveOutboundOrder(orderId, actualQuantity);
        return Result.success("发货完成");
    }

    @GetMapping("/pending")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.erp.erplite.entity.OrderVO>> getPendingList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.getPendingOrders(pageNum, pageSize));
    }

    /**
     * 获取单据操作轨迹时间轴
     * 请求方式: GET /order/timeline/{orderId}
     */
    @GetMapping("/timeline/{orderId}")
    public Result<java.util.List<com.erp.erplite.entity.TimelineVO>> getOrderTimeline(@PathVariable Long orderId) {
        return Result.success(orderService.getOrderTimeline(orderId));
    }

    /**
     * 3. 采购退货
     * 请求方式: POST /order/return/inbound
     */
    @PostMapping("/return/inbound")
    public Result<String> returnInbound(@RequestBody InboundParam param) {
        orderService.returnInboundOrder(param.getPartnerId(), param.getWarehouseId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice());
        return Result.success("退货成功，库存已扣减，并生成红字冲销账款");
    }

    @Data
    public static class InventoryCheckParam {
        private Long goodsId;
        private Long warehouseId; // 盘点仓库
        private java.math.BigDecimal actualQuantity; // 库管员数出来的真实数量
    }

    /**
     * 4. 提交库存盘点结果
     * 请求方式: POST /order/inventory/check
     */
    @com.erp.erplite.common.RequireRole({SystemConstants.ROLE_WAREHOUSE, SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
    @PostMapping("/inventory/check")
    public Result<String> inventoryCheck(@RequestBody InventoryCheckParam param) {
        orderService.inventoryCheck(param.getWarehouseId(), param.getGoodsId(), param.getActualQuantity());
        return Result.success("盘点完成，系统账面已自动更新平账");
    }

    @GetMapping("/history")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<OrderVO>> getHistoryOrders(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.getHistoryOrders(type, startDate, endDate, keyword, pageNum, pageSize));
    }

    /**
     * 导出历史单据 Excel
     * 请求方式: GET /order/history/export
     */
    @GetMapping("/history/export")
    public void exportHistoryOrders(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("历史单据报表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 获取前10000条数据用于导出
            java.util.List<OrderVO> dataList = orderService.getHistoryOrders(type, startDate, endDate, keyword, 1, 10000).getRecords();

            // 转换为导出 DTO（中文可读）
            java.util.List<com.erp.erplite.dto.OrderExportDTO> exportList = new java.util.ArrayList<>();
            for (OrderVO vo : dataList) {
                com.erp.erplite.dto.OrderExportDTO dto = new com.erp.erplite.dto.OrderExportDTO();
                dto.setOrderNo(vo.getOrderNo());
                dto.setPartnerName(vo.getPartnerName());
                dto.setGoodsName(vo.getGoodsName());
                dto.setQuantity(vo.getQuantity());
                dto.setTotalAmount(vo.getTotalAmount());
                dto.setCreateTime(vo.getCreateTime());
                // 类型转中文
                if (vo.getType() != null) {
                    switch (vo.getType()) {
                        case 1: dto.setTypeStr("采购入库"); break;
                        case 2: dto.setTypeStr("销售出库"); break;
                        case 3: dto.setTypeStr("采购退货"); break;
                        case 5: dto.setTypeStr("库存盘点"); break;
                        default: dto.setTypeStr("其他(" + vo.getType() + ")");
                    }
                }
                // 状态转中文
                if (vo.getStatus() != null) {
                    switch (vo.getStatus()) {
                        case 0: dto.setStatusStr("待审批"); break;
                        case 1: dto.setStatusStr("已生效"); break;
                        case 4: dto.setStatusStr("已驳回"); break;
                        default: dto.setStatusStr("已作废");
                    }
                }
                exportList.add(dto);
            }

            com.alibaba.excel.EasyExcel.write(response.getOutputStream(), com.erp.erplite.dto.OrderExportDTO.class)
                    .sheet("历史单据")
                    .doWrite(exportList);

        } catch (Exception e) {
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            try {
                response.getWriter().println("{\"code\": 500, \"message\": \"导出Excel失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}