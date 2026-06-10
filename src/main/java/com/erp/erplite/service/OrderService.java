package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.Log;
import com.erp.erplite.common.UserContext;
import com.erp.erplite.entity.*;
import com.erp.erplite.mapper.GoodsMapper;
import com.erp.erplite.mapper.FinAccountMapper;
import com.erp.erplite.mapper.OrderDetailMapper;
import com.erp.erplite.mapper.OrderMapper;
import com.erp.erplite.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final StockMapper stockMapper;
    private final FinAccountMapper finAccountMapper;
    private final GoodsMapper goodsMapper;
    private final com.erp.erplite.mapper.StockLogMapper stockLogMapper;

    private Long getCurrentUserId() {
        User user = UserContext.get();
        return user != null ? user.getId() : 1L; // 兜底给系统管理员
    }

    /**
     * 1. 提交采购入库申请 (仅生成草稿单据，不动库存和财务)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitInboundDraft(Long partnerId, Long warehouseId, Long goodsId, java.math.BigDecimal quantity,
            java.math.BigDecimal unitPrice, String contractNo, String orderRemark, String detailRemark) {
        log.info("提交采购入库草稿, 供应商:{}, 仓库:{}, 商品:{}, 数量:{}", partnerId, warehouseId, goodsId, quantity);

        if (quantity == null || quantity.compareTo(java.math.BigDecimal.ZERO) <= 0 || unitPrice == null || unitPrice.compareTo(java.math.BigDecimal.ZERO) <= 0 || warehouseId == null) {
            throw new com.erp.erplite.common.BusinessException("参数不合法，数量和单价必须大于0且必须指定仓库");
        }

        // 计算总金额
        java.math.BigDecimal totalAmount = unitPrice.multiply(quantity);

        // 生成单据主表 (初始状态为 0-待审核)
        Order order = new Order();
        order.setOrderNo("IN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setType(1);
        order.setPartnerId(partnerId);
        order.setWarehouseId(warehouseId);
        order.setStatus(0); // 核心修改：状态标记为待审核
        order.setCreateBy(getCurrentUserId());
        order.setContractNo(contractNo);
        order.setRemark(orderRemark);
        orderMapper.insert(order);

        // 生成单据明细表
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(totalAmount);
        detail.setCostAmount(totalAmount); // 入库成本等于采购总额
        detail.setRemark(detailRemark);
        orderDetailMapper.insert(detail);

        log.info("入库单草稿已生成，单号: {}", order.getOrderNo());
    }

    /**
     * 2. 审核入库单 (核心逻辑移到这里：加库存、生成应付账款)
     * 
     * @param orderId 单据主表的ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveInboundOrder(Long orderId) {
        log.info("准备审核入库单, 单据ID: {}", orderId);

        // 1. 查询单据并防重审
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getType() != 1) {
            throw new com.erp.erplite.common.BusinessException("非法的入库单");
        }
        if (order.getStatus() == 1) {
            throw new com.erp.erplite.common.BusinessException("该单据已审核，请勿重复操作！");
        }

        // 2. 获取单据明细（MVP里默认一单一明细，取第一条）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderDetail> detailQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        detailQuery.eq("order_id", orderId);
        OrderDetail detail = orderDetailMapper.selectOne(detailQuery);

        if (detail == null) {
            throw new com.erp.erplite.common.BusinessException("单据明细丢失！");
        }

        Long goodsId = detail.getGoodsId();
        java.math.BigDecimal quantity = detail.getQuantity();
        java.math.BigDecimal totalAmount = detail.getTotalAmount();
        Long warehouseId = order.getWarehouseId();

        // 3. 联动更新库存表 (分仓核算) - 使用原子 SQL 更新
        QueryWrapper<Stock> stockQuery = new QueryWrapper<>();
        stockQuery.eq("goods_id", goodsId).eq("warehouse_id", warehouseId);
        Stock existStock = stockMapper.selectOne(stockQuery);
        BigDecimal beforeQty = existStock != null ? existStock.getQuantity() : BigDecimal.ZERO;

        int updated = stockMapper.addStock(goodsId, warehouseId, quantity, totalAmount);
        
        if (updated == 0) {
            // 没有更新到记录，说明是第一次入库该商品，使用细粒度锁防并发重复插入
            synchronized (("stock_init_" + warehouseId + "_" + goodsId).intern()) {
                Stock exist = stockMapper.selectOne(new QueryWrapper<Stock>().eq("goods_id", goodsId).eq("warehouse_id", warehouseId));
                if (exist == null) {
                    Stock newStock = new Stock();
                    newStock.setGoodsId(goodsId);
                    newStock.setWarehouseId(warehouseId);
                    newStock.setQuantity(quantity);
                    newStock.setTotalCost(totalAmount);
                    newStock.setVersion(0);
                    stockMapper.insert(newStock);
                } else {
                    // 如果其他线程抢先插入了，则回退为更新逻辑
                    stockMapper.addStock(goodsId, warehouseId, quantity, totalAmount);
                }
            }
        }
        
        insertStockLog(goodsId, warehouseId, beforeQty, quantity, beforeQty.add(quantity), order.getOrderNo(), 1);


        // 4. 自动生成财务应付账款
        FinAccount finAccount = new FinAccount();
        finAccount.setPartnerId(order.getPartnerId());
        finAccount.setOrderId(order.getId());
        finAccount.setType(1); // 1-应付账款
        finAccount.setAmount(totalAmount);
        finAccount.setStatus(0);
        finAccountMapper.insert(finAccount);

        // 5. 更新单据状态为 1 (已审核)
        order.setStatus(1);
        int updateRow = orderMapper.updateById(order);
        if (updateRow == 0) {
            throw new com.erp.erplite.common.BusinessException("单据状态已变更，请勿重复操作！");
        }

        log.info("单号 {} 审核通过！库存和财务已更新。", order.getOrderNo());
    }

    /**
     * 提交销售出库草稿单 (不扣库存，不记财务)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitOutboundDraft(Long partnerId, Long warehouseId, Long goodsId, java.math.BigDecimal quantity,
            java.math.BigDecimal unitPrice, String contractNo, String orderRemark, String detailRemark) {
        log.info("提交销售出库草稿, 客户: {}, 仓库: {}, 商品: {}, 数量: {}, 售价: {}", partnerId, warehouseId, goodsId, quantity,
                unitPrice);

        if (quantity == null || quantity.compareTo(java.math.BigDecimal.ZERO) <= 0 || unitPrice == null || unitPrice.compareTo(java.math.BigDecimal.ZERO) <= 0 || warehouseId == null) {
            throw new com.erp.erplite.common.BusinessException("出库参数不合法，数量和单价必须大于0且必须指定仓库");
        }

        java.math.BigDecimal salesAmount = unitPrice.multiply(quantity);

        // 生成单据主表与明细 (状态 0-待发货)
        Order order = new Order();
        order.setOrderNo("OUT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setType(2);
        order.setPartnerId(partnerId);
        order.setWarehouseId(warehouseId);
        order.setStatus(0);
        order.setCreateBy(getCurrentUserId());
        order.setContractNo(contractNo);
        order.setRemark(orderRemark);
        orderMapper.insert(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(salesAmount);
        detail.setRemark(detailRemark);
        orderDetailMapper.insert(detail);

        log.info("销售出库草稿已生成，单号: {}", order.getOrderNo());
    }

    /**
     * 审核并执行销售出库 (真正的扣库存、记账、裂变)
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveOutboundOrder(Long orderId, java.math.BigDecimal actualQuantity) {
        log.info("准备审核出库发货, 单据ID: {}, 实际发货数量: {}", orderId, actualQuantity);

        // 防线1：拦截无效裂变 (防止死循环生成 0 数量的单据)
        if (actualQuantity == null || actualQuantity.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.erp.erplite.common.BusinessException("实际发货数量必须大于0！不发货请直接取消单据。");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getType() != 2 || order.getStatus() == 1) {
            throw new com.erp.erplite.common.BusinessException("非法的待发出库单");
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderDetail> detailQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        detailQuery.eq("order_id", orderId);
        OrderDetail detail = orderDetailMapper.selectOne(detailQuery);

        java.math.BigDecimal originalQuantity = detail.getQuantity();
        if (actualQuantity.compareTo(originalQuantity) > 0) {
            throw new com.erp.erplite.common.BusinessException("实际发货数量不能大于原单据需求数量！");
        }

        Long goodsId = detail.getGoodsId();
        Long warehouseId = order.getWarehouseId();

        // 1. 查询当前仓库的库存 (计算成本需要读)
        QueryWrapper<Stock> query = new QueryWrapper<>();
        query.eq("goods_id", goodsId).eq("warehouse_id", warehouseId);
        Stock existStock = stockMapper.selectOne(query);

        if (existStock == null || existStock.getQuantity().compareTo(actualQuantity) < 0) {
            throw new com.erp.erplite.common.BusinessException("库存不足，无法完成本次实际发货！");
        }
        
        BigDecimal beforeQty = existStock.getQuantity();
        
        // 防线2：精细成本结转
        java.math.BigDecimal currentAvgCost = existStock.getTotalCost()
                .divide(existStock.getQuantity(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal outboundCost = currentAvgCost.multiply(actualQuantity);

        // 2. 原子扣减库存
        int updated = stockMapper.reduceStock(goodsId, warehouseId, actualQuantity, outboundCost);
        if (updated == 0) {
             throw new com.erp.erplite.common.BusinessException("并发扣减库存失败，可能库存不足，请重试！");
        }
        insertStockLog(goodsId, warehouseId, beforeQty, actualQuantity.negate(), beforeQty.subtract(actualQuantity), order.getOrderNo(), 2);


        // 更新当前单据状态和明细数量
        order.setStatus(1);
        int updateRow = orderMapper.updateById(order);
        if (updateRow == 0) {
            throw new com.erp.erplite.common.BusinessException("单据状态已变更，请勿重复操作！");
        }

        java.math.BigDecimal actualSalesAmount = detail.getUnitPrice().multiply(actualQuantity);
        detail.setQuantity(actualQuantity);
        detail.setTotalAmount(actualSalesAmount);
        detail.setCostAmount(outboundCost); // 记录出库时扣减的真实加权成本
        orderDetailMapper.updateById(detail);

        // 生成应收账款
        FinAccount finAccount = new FinAccount();
        finAccount.setPartnerId(order.getPartnerId());
        finAccount.setOrderId(order.getId());
        finAccount.setType(2); // 2-应收账款
        finAccount.setAmount(actualSalesAmount);
        finAccount.setStatus(0);
        finAccountMapper.insert(finAccount);

        // === 核心：欠货单裂变 (Backorder) ===
        if (actualQuantity.compareTo(originalQuantity) < 0) {
            java.math.BigDecimal remainQty = originalQuantity.subtract(actualQuantity);

            Order bOrder = new Order();
            // 如果原单已经是 B1，可能会变成 B1-B1，为了简单，目前MVP允许拼接
            bOrder.setOrderNo(order.getOrderNo() + "-B1");
            bOrder.setType(4); // 4-欠货单 (Backorder)
            bOrder.setPartnerId(order.getPartnerId());
            bOrder.setWarehouseId(order.getWarehouseId());
            bOrder.setStatus(0); // 待发货
            bOrder.setCreateBy(order.getCreateBy());
            orderMapper.insert(bOrder);

            OrderDetail bDetail = new OrderDetail();
            bDetail.setOrderId(bOrder.getId());
            bDetail.setGoodsId(goodsId);
            bDetail.setQuantity(remainQty);
            bDetail.setUnitPrice(detail.getUnitPrice());
            bDetail.setTotalAmount(detail.getUnitPrice().multiply(remainQty));
            orderDetailMapper.insert(bDetail);

            log.info("触发裂变！生成欠货单: {}", bOrder.getOrderNo());
        }
    }

    /**
     * 驳回单据
     * @param orderId 单据ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long orderId) {
        log.info("驳回单据, ID: {}", orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new com.erp.erplite.common.BusinessException("单据不存在");
        }
        if (order.getStatus() != 0) {
            throw new com.erp.erplite.common.BusinessException("只有待审核状态的单据可以被驳回");
        }
        order.setStatus(4); // 4-已驳回
        orderMapper.updateById(order);
        log.info("单据 {} 已驳回", order.getOrderNo());
    }

    public com.baomidou.mybatisplus.core.metadata.IPage<OrderVO> getPendingOrders(int pageNum, int pageSize) {
        return orderMapper.getPendingOrders(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize));
    }

    /**
     * 3. 采购退货单
     * 业务场景：把从供应商买来的货退回去。
     */
    @Log("执行了采购退货")
    @Transactional(rollbackFor = Exception.class)
    public void returnInboundOrder(Long partnerId, Long warehouseId, Long goodsId, java.math.BigDecimal quantity,
            java.math.BigDecimal unitPrice) {
        log.info("处理采购退货, 供应商:{}, 仓库:{}, 商品:{}, 退货数量:{}, 退货单价:{}", partnerId, warehouseId, goodsId, quantity,
                unitPrice);

        if (quantity == null || quantity.compareTo(java.math.BigDecimal.ZERO) <= 0 || unitPrice == null || unitPrice.compareTo(java.math.BigDecimal.ZERO) <= 0 || warehouseId == null) {
            throw new com.erp.erplite.common.BusinessException("参数不合法，数量和单价必须大于0且必须指定仓库");
        }

        String orderNo = "RET-IN-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        java.math.BigDecimal totalAmount = unitPrice.multiply(quantity); // 财务退款总额

        QueryWrapper<Stock> stockQuery = new QueryWrapper<>();
        stockQuery.eq("goods_id", goodsId).eq("warehouse_id", warehouseId);
        Stock existStock = stockMapper.selectOne(stockQuery);
        
        if (existStock == null || existStock.getQuantity().compareTo(quantity) < 0) {
             throw new com.erp.erplite.common.BusinessException("当前库存不足，无法完成退货操作！");
        }
        BigDecimal beforeQty = existStock.getQuantity();

        // 核心修复：物理库存退货必须按照当前实际的加权平均成本扣减，不能用退货单价扣减，否则会击穿成本池
        java.math.BigDecimal currentAvgCost = existStock.getTotalCost()
                .divide(existStock.getQuantity(), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal costReduction = currentAvgCost.multiply(quantity);

        int updated = stockMapper.reduceStock(goodsId, warehouseId, quantity, costReduction);
        if(updated == 0) {
             throw new com.erp.erplite.common.BusinessException("并发扣减库存失败，可能库存不足，请重试！");
        }
        insertStockLog(goodsId, warehouseId, beforeQty, quantity.negate(), beforeQty.subtract(quantity), orderNo, 2);


        // 3. 生成单据主表 (类型为 3: 采购退货)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setType(3); // 3-采购退货单
        order.setPartnerId(partnerId);
        order.setWarehouseId(warehouseId);
        order.setStatus(1); // 退货单直接生效
        order.setCreateBy(getCurrentUserId());
        orderMapper.insert(order);

        // 4. 生成单据明细表
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(totalAmount);
        orderDetailMapper.insert(detail);

        // 5. 生成反向财务账款 (负的应付账款，意思是供应商要倒找给我们钱，冲抵货款)
        FinAccount finAccount = new FinAccount();
        finAccount.setPartnerId(partnerId);
        finAccount.setOrderId(order.getId());
        finAccount.setType(1); // 依然是采购应付池子里的钱
        // 【核心】生成一个负数金额的账单，存入数据库
        finAccount.setAmount(totalAmount.negate());
        finAccount.setStatus(0);
        finAccountMapper.insert(finAccount);

        log.info("采购退货单处理完成，退货金额: -{}", totalAmount);
    }

    /**
     * 4. 仓库库存盘点引擎 (自动平账)
     * 业务场景：库管员录入实际盘点数量，系统对比账面数量，自动生成盘盈/盘亏单，修正当前库存。
     */
    @com.erp.erplite.common.Log("执行了库存盘点")
    @Transactional(rollbackFor = Exception.class)
    public void inventoryCheck(Long warehouseId, Long goodsId, java.math.BigDecimal actualQuantity) {
        log.info("开始库存盘点, 仓库:{}, 商品ID:{}, 实盘数量:{}", warehouseId, goodsId, actualQuantity);

        if (actualQuantity == null || actualQuantity.compareTo(java.math.BigDecimal.ZERO) < 0 || warehouseId == null) {
            throw new com.erp.erplite.common.BusinessException("实盘数量不能为负数且必须指定仓库");
        }

        String orderNo = "CHK-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        boolean isProfit = false;
        java.math.BigDecimal diffQuantity = java.math.BigDecimal.ZERO;
        
        QueryWrapper<Stock> query = new QueryWrapper<>();
        query.eq("goods_id", goodsId).eq("warehouse_id", warehouseId);
        Stock stock = stockMapper.selectOne(query);

        java.math.BigDecimal bookQuantity = (stock == null) ? java.math.BigDecimal.ZERO : stock.getQuantity();

        if (bookQuantity.compareTo(actualQuantity) == 0) {
            log.info("商品 {} 账实相符，无需平账", goodsId);
            return;
        }

        diffQuantity = actualQuantity.subtract(bookQuantity); 
        isProfit = diffQuantity.compareTo(java.math.BigDecimal.ZERO) > 0; 
        String checkTypeDesc = isProfit ? "盘盈" : "盘亏";

        log.warn("发现库存差异! 账面:{}, 实际:{}, {}: {}", bookQuantity, actualQuantity, checkTypeDesc, diffQuantity.abs());

        if (stock != null && stock.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0 && stock.getTotalCost() != null) {
            java.math.BigDecimal avgPrice = stock.getTotalCost().divide(stock.getQuantity(),
                    4, java.math.RoundingMode.HALF_UP);
            totalAmount = avgPrice.multiply(diffQuantity.abs());
        } else if (stock != null) {
            // 原库存为 0，或者总成本为空，无法计算加权平均，按照商品基础资料的默认采购价计算盘盈价值
            Goods goods = goodsMapper.selectById(goodsId);
            java.math.BigDecimal defaultPrice = (goods != null && goods.getDefaultPrice() != null)
                    ? goods.getDefaultPrice()
                    : java.math.BigDecimal.ZERO;
            totalAmount = defaultPrice.multiply(diffQuantity.abs());
        }

        if (stock == null) {
            Goods goods = goodsMapper.selectById(goodsId);
            java.math.BigDecimal defaultPrice = (goods != null && goods.getDefaultPrice() != null)
                    ? goods.getDefaultPrice()
                    : java.math.BigDecimal.ZERO;

            java.math.BigDecimal initialCost = defaultPrice.multiply(actualQuantity);
            totalAmount = initialCost; 

            Stock newStock = new Stock();
            newStock.setGoodsId(goodsId);
            newStock.setWarehouseId(warehouseId);
            newStock.setQuantity(actualQuantity);
            newStock.setTotalCost(initialCost); 
            newStock.setVersion(0);
            stockMapper.insert(newStock);
            
            insertStockLog(goodsId, warehouseId, bookQuantity, diffQuantity, actualQuantity, orderNo, 3);
        } else {
            java.math.BigDecimal currentCost = stock.getTotalCost() != null ? stock.getTotalCost()
                    : java.math.BigDecimal.ZERO;
            BigDecimal newCost = isProfit ? currentCost.add(totalAmount) : currentCost.subtract(totalAmount);
            
            // 使用更稳健的直接 UPDATE 来盘点
            stock.setQuantity(actualQuantity);
            stock.setTotalCost(newCost);
            int rows = stockMapper.updateById(stock);
            if (rows == 0) {
                throw new com.erp.erplite.common.BusinessException("并发盘点库存状态已变更，请刷新后重试！");
            }
            insertStockLog(goodsId, warehouseId, bookQuantity, diffQuantity, actualQuantity, orderNo, 3);
        }


        // 5. 留痕：生成一张专门的盘点损益单据 (type=5)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setType(5); // 5-盘点损益单
        order.setWarehouseId(warehouseId);
        // 盘点单没有 partnerId
        order.setStatus(1); // 盘点结果直接生效
        order.setCreateBy(getCurrentUserId());
        orderMapper.insert(order);

        // 生成盘点明细（记录这笔单子是因为哪个商品发生了差异）
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);

        // 注意：明细里存的是差异值（带正负号），这样查明细表就能知道是亏是盈
        detail.setQuantity(diffQuantity);
        detail.setTotalAmount(isProfit ? totalAmount : totalAmount.negate());
        orderDetailMapper.insert(detail);

        // 6. 财务平账 (记录财务损失或收益)
        FinAccount finAccount = new FinAccount();
        finAccount.setPartnerId(0L); // 内部账户(如管理费用科目)
        finAccount.setOrderId(order.getId());
        finAccount.setType(3); // 3-内部损益账款(盘盈盘亏)
        finAccount.setAmount(isProfit ? totalAmount : totalAmount.negate());
        finAccount.setPaidAmount(java.math.BigDecimal.ZERO);
        finAccount.setUnpaidAmount(java.math.BigDecimal.ZERO);
        finAccount.setStatus(1); // 内部损益直接结清
        finAccountMapper.insert(finAccount);

        log.info("库存平账完成，生成盘点单: {}", order.getOrderNo());
    }

    public com.baomidou.mybatisplus.core.metadata.IPage<OrderVO> getHistoryOrders(Integer type, String startDate, String endDate, String keyword, int pageNum, int pageSize) {
        return orderMapper.getHistoryOrders(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), type, startDate, endDate, keyword);
    }

    /**
     * 获取单据的操作轨迹 (生命周期时间轴)
     */
    public List<com.erp.erplite.entity.TimelineVO> getOrderTimeline(Long orderId) {
        List<com.erp.erplite.entity.TimelineVO> timeline = new java.util.ArrayList<>();
        Order order = orderMapper.selectById(orderId);
        if (order == null) return timeline;

        // 1. 创建节点
        com.erp.erplite.entity.TimelineVO createNode = new com.erp.erplite.entity.TimelineVO();
        createNode.setTitle("单据已创建 (草稿)");
        createNode.setContent(order.getType() == 1 ? "业务员提交了采购入库申请" : "业务员提交了销售出库申请");
        createNode.setTime(order.getCreateTime() != null ? order.getCreateTime() : new java.util.Date());
        createNode.setType("PRIMARY");
        createNode.setOperator("业务员"); // 真实系统可关联 User 表取名字
        timeline.add(createNode);

        // 2. 状态节点 (如果是已驳回)
        if (order.getStatus() == 4) {
            com.erp.erplite.entity.TimelineVO rejectNode = new com.erp.erplite.entity.TimelineVO();
            rejectNode.setTitle("单据被驳回");
            rejectNode.setContent("财务/管理员驳回了该申请");
            rejectNode.setTime(new java.util.Date()); // 由于没存更新时间，用当前时间或日志时间替代
            rejectNode.setType("DANGER");
            rejectNode.setOperator("审批人");
            timeline.add(rejectNode);
            return timeline;
        }

        // 3. 审核与库存变动节点
        QueryWrapper<StockLog> stockLogQuery = new QueryWrapper<>();
        stockLogQuery.eq("related_order_no", order.getOrderNo());
        List<StockLog> stockLogs = stockLogMapper.selectList(stockLogQuery);
        
        for (StockLog slog : stockLogs) {
            com.erp.erplite.entity.TimelineVO stockNode = new com.erp.erplite.entity.TimelineVO();
            stockNode.setTitle(slog.getType() == 4 ? "单据被冲销" : "库存已更新 (单据生效)");
            stockNode.setContent(String.format("库存变动: %s 件，当前结余: %s 件", slog.getChangeQuantity(), slog.getAfterQuantity()));
            stockNode.setTime(slog.getCreateTime());
            stockNode.setType(slog.getType() == 4 ? "DANGER" : "SUCCESS");
            stockNode.setOperator("仓管/财务");
            timeline.add(stockNode);
        }

        // 4. 财务账款节点
        QueryWrapper<FinAccount> finQuery = new QueryWrapper<>();
        finQuery.eq("order_id", orderId);
        FinAccount finAcc = finAccountMapper.selectOne(finQuery);
        if (finAcc != null) {
            com.erp.erplite.entity.TimelineVO finNode = new com.erp.erplite.entity.TimelineVO();
            finNode.setTitle(finAcc.getType() == 1 ? "应付账款已生成" : "应收账款已生成");
            finNode.setContent(String.format("产生挂账金额: ¥%s", finAcc.getAmount()));
            finNode.setTime(finAcc.getCreateTime() != null ? finAcc.getCreateTime() : new java.util.Date());
            finNode.setType("WARNING");
            finNode.setOperator("系统自动");
            timeline.add(finNode);
            
            // 如果有结清流水，再加一个节点
            if (finAcc.getStatus() == 1) {
                com.erp.erplite.entity.TimelineVO settleNode = new com.erp.erplite.entity.TimelineVO();
                settleNode.setTitle("财务已核销结清");
                settleNode.setContent(String.format("实收/实付金额: ¥%s", finAcc.getPaidAmount()));
                settleNode.setTime(new java.util.Date()); // 同样用当前时间做示例
                settleNode.setType("SUCCESS");
                settleNode.setOperator("财务出纳");
                timeline.add(settleNode);
            }
        }

        // 按时间排序
        timeline.sort(java.util.Comparator.comparing(com.erp.erplite.entity.TimelineVO::getTime));
        return timeline;
    }

    /**
     * 6. 作废/冲销单据
     */
    @Log("作废或冲销了单据")
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        log.info("开始处理单据作废/冲销: {}", orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new com.erp.erplite.common.BusinessException("单据不存在！");
        if (order.getStatus() == 3) throw new com.erp.erplite.common.BusinessException("该单据已被作废，无需重复操作！");

        int currentStatus = order.getStatus();
        
        // 利用乐观锁抢占状态
        order.setStatus(3);
        int row = orderMapper.updateById(order);
        if (row == 0) throw new com.erp.erplite.common.BusinessException("单据已被其他人处理，操作失败！");

        if (currentStatus == 0) {
            log.info("草稿单据直接作废成功");
            return; 
        }

        if (currentStatus == 1) {
            log.info("单据已生效，开始执行红字冲销补偿...");
            
            // 雷区一防线：校验财务流水是否已付款/回款
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FinAccount> fq = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            fq.eq("order_id", orderId);
            FinAccount existFa = finAccountMapper.selectOne(fq);
            if (existFa != null && existFa.getPaidAmount() != null && existFa.getPaidAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                throw new com.erp.erplite.common.BusinessException("该单据已产生实际收付款，严禁直接作废，请走标准的售后退款流程！");
            }

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderDetail> dq = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            dq.eq("order_id", orderId);
            List<OrderDetail> details = orderDetailMapper.selectList(dq);
            
            Order reverseOrder = new Order();
            reverseOrder.setOrderNo(order.getOrderNo() + "-REV");
            reverseOrder.setType(order.getType()); 
            reverseOrder.setPartnerId(order.getPartnerId());
            reverseOrder.setWarehouseId(order.getWarehouseId());
            reverseOrder.setStatus(1); 
            reverseOrder.setCreateBy(getCurrentUserId());
            orderMapper.insert(reverseOrder);

            java.math.BigDecimal totalReverseAmount = java.math.BigDecimal.ZERO;

            for (OrderDetail d : details) {
                java.math.BigDecimal revQty = d.getQuantity().negate();
                java.math.BigDecimal revAmt = d.getTotalAmount().negate();
                totalReverseAmount = totalReverseAmount.add(revAmt);

                OrderDetail rd = new OrderDetail();
                rd.setOrderId(reverseOrder.getId());
                rd.setGoodsId(d.getGoodsId());
                rd.setQuantity(revQty);
                rd.setUnitPrice(d.getUnitPrice());
                rd.setTotalAmount(revAmt);
                orderDetailMapper.insert(rd);

                // 冲销库存 - 原子更新
                QueryWrapper<Stock> sq = new QueryWrapper<>();
                sq.eq("goods_id", d.getGoodsId()).eq("warehouse_id", order.getWarehouseId());
                Stock stock = stockMapper.selectOne(sq);
                
                if (stock != null) {
                    java.math.BigDecimal beforeQty = stock.getQuantity();
                    java.math.BigDecimal realChange = order.getType() == 1 ? revQty : d.getQuantity(); 
                    
                    // 雷区二修复：严格使用出库时的“原始扣减成本”，杜绝成本漂移
                    java.math.BigDecimal originalCost = d.getCostAmount() != null ? d.getCostAmount() : d.getTotalAmount();
                    java.math.BigDecimal costChange = order.getType() == 1 ? originalCost.negate() : originalCost;

                    if (realChange.compareTo(java.math.BigDecimal.ZERO) > 0) {
                         stockMapper.addStock(d.getGoodsId(), order.getWarehouseId(), realChange, costChange);
                    } else {
                         int reduced = stockMapper.reduceStock(d.getGoodsId(), order.getWarehouseId(), realChange.negate(), costChange.negate());
                         if (reduced == 0) {
                             throw new com.erp.erplite.common.BusinessException("作废失败：当前库存已不足以冲销该入库单（商品已被消耗出库），请走正常的销售退货或盘点流程！");
                         }
                    }

                    insertStockLog(d.getGoodsId(), order.getWarehouseId(), beforeQty, realChange, beforeQty.add(realChange), reverseOrder.getOrderNo(), 4); 
                }
            }

            FinAccount fa = new FinAccount();
            fa.setPartnerId(order.getPartnerId() != null ? order.getPartnerId() : 0L);
            fa.setOrderId(reverseOrder.getId());
            
            // 正确映射红字单的财务账单类型
            int faType = 1;
            if (order.getType() == 2) faType = 2; // 销售出库 -> 应收
            else if (order.getType() == 5) faType = 3; // 盘点损益 -> 内部损益
            fa.setType(faType); 
            
            fa.setAmount(totalReverseAmount); 
            fa.setStatus(0);
            finAccountMapper.insert(fa);
            
            log.info("红字冲销完成，生成对冲单号: {}", reverseOrder.getOrderNo());
        }
    }

    private void insertStockLog(Long goodsId, Long warehouseId, java.math.BigDecimal beforeQty, java.math.BigDecimal changeQty, java.math.BigDecimal afterQty, String orderNo, Integer type) {
        StockLog slog = new StockLog();
        slog.setGoodsId(goodsId);
        slog.setWarehouseId(warehouseId);
        slog.setBeforeQuantity(beforeQty);
        slog.setChangeQuantity(changeQty);
        slog.setAfterQuantity(afterQty);
        slog.setRelatedOrderNo(orderNo);
        slog.setType(type);
        slog.setCreateBy(getCurrentUserId());
        slog.setCreateTime(new java.util.Date());
        stockLogMapper.insert(slog);
    }

    /**
     * 重新提交被驳回的单据 (状态从 4 -> 0)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRejectedOrder(Long orderId, Long partnerId, Long warehouseId, Long goodsId, java.math.BigDecimal quantity, java.math.BigDecimal unitPrice, String contractNo, String orderRemark, String detailRemark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 4) {
            throw new com.erp.erplite.common.BusinessException("只能编辑已被驳回的单据");
        }
        if (!order.getCreateBy().equals(getCurrentUserId())) {
            throw new com.erp.erplite.common.BusinessException("只能编辑自己提交的单据");
        }
        
        // Update Order - 严格安全校验：后端禁止修改往来单位和仓库，只能改备注和合同号
        order.setContractNo(contractNo);
        order.setRemark(orderRemark);
        order.setStatus(0); // 重新变为待审批
        orderMapper.updateById(order);
        
        // Update Detail - 严格安全校验：后端禁止修改商品类别，仅允许修改数量和单价
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderDetail> dq = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dq.eq("order_id", orderId);
        OrderDetail detail = orderDetailMapper.selectOne(dq);
        if (detail != null) {
            if (quantity != null) detail.setQuantity(quantity);
            if (unitPrice != null) detail.setUnitPrice(unitPrice);
            if (quantity != null && unitPrice != null) {
                detail.setTotalAmount(quantity.multiply(unitPrice));
            }
            detail.setRemark(detailRemark);
            orderDetailMapper.updateById(detail);
        }
    }
}