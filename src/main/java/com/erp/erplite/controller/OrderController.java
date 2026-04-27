package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 接收前端传来的出库参数 (可以复用入库的类，但为了语义清晰重新建一个)
     */
    @Data
    public static class OutboundParam {
        private Long partnerId; // 客户ID
        private Long goodsId;
        private Integer quantity;
        private java.math.BigDecimal unitPrice; // 销售单价
    }

    /**
     * 出库接口
     * 请求方式: POST /order/outbound
     */
    @PostMapping("/outbound")
    public Result<String> outbound(@RequestBody OutboundParam param) {
        orderService.addOutboundOrder(param.getPartnerId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice());
        return Result.success("出库成功，已生成应收账款");
    }
    /**
     * 接收前端传来的入库参数
     */
    @Data
    public static class InboundParam {
        private Long partnerId; // 供应商ID
        private Long goodsId;
        private Integer quantity;
        private java.math.BigDecimal unitPrice; // 采购单价
    }

    /**
     * 入库接口
     * 请求方式: POST /order/inbound
     */
    @PostMapping("/inbound")
    public Result<String> inbound(@RequestBody InboundParam param) {
        // 方法名改成我们刚才新写的 submitInboundDraft
        orderService.submitInboundDraft(param.getPartnerId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice());
        return Result.success("入库申请已提交，等待审核");
    }

    /**
     *  审核入库单
     * 请求方式: POST /order/approve/inbound/{orderId}
     */
    @PostMapping("/approve/inbound/{orderId}")
    public Result<String> approveInbound(@PathVariable Long orderId) {
        orderService.approveInboundOrder(orderId);
        return Result.success("单据审核通过，已生效");
    }

    @GetMapping("/pending")
    public Result<java.util.List<com.erp.erplite.entity.OrderVO>> getPendingList() {
        return Result.success(orderService.getPendingOrders());
    }

    /**
     * 3. 采购退货
     * 请求方式: POST /order/return/inbound
     */
    @PostMapping("/return/inbound")
    public Result<String> returnInbound(@RequestBody InboundParam param) {
        orderService.returnInboundOrder(param.getPartnerId(), param.getGoodsId(), param.getQuantity(), param.getUnitPrice());
        return Result.success("退货成功，库存已扣减，并生成红字冲销账款");
    }

    @Data
    public static class InventoryCheckParam {
        private Long goodsId;
        private Integer actualQuantity; // 库管员数出来的真实数量
    }

    /**
     * 4. 提交库存盘点结果
     * 请求方式: POST /order/inventory/check
     */
    @PostMapping("/inventory/check")
    public Result<String> inventoryCheck(@RequestBody InventoryCheckParam param) {
        orderService.inventoryCheck(param.getGoodsId(), param.getActualQuantity());
        return Result.success("盘点完成，系统账面已自动更新平账");
    }
}