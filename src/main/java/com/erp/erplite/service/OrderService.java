package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.Log;
import com.erp.erplite.entity.*;
import com.erp.erplite.mapper.FinAccountMapper;
import com.erp.erplite.mapper.OrderDetailMapper;
import com.erp.erplite.mapper.OrderMapper;
import com.erp.erplite.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final StockMapper stockMapper;
    private final FinAccountMapper finAccountMapper;

    /**
     * 1. 提交采购入库申请 (仅生成草稿单据，不动库存和财务)
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitInboundDraft(Long partnerId, Long goodsId, Integer quantity, java.math.BigDecimal unitPrice) {
        log.info("提交采购入库草稿, 供应商:{}, 商品:{}, 数量:{}", partnerId, goodsId, quantity);

        if (quantity == null || quantity <= 0 || unitPrice == null) {
            throw new RuntimeException("入库数量或单价不合法");
        }

        // 计算总金额
        java.math.BigDecimal totalAmount = unitPrice.multiply(new java.math.BigDecimal(quantity));

        // 生成单据主表 (初始状态为 0-待审核)
        Order order = new Order();
        order.setOrderNo("IN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setType(1);
        order.setPartnerId(partnerId);
        order.setStatus(0); // 核心修改：状态标记为待审核
        order.setCreateBy(1L);
        orderMapper.insert(order);

        // 生成单据明细表
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(totalAmount);
        orderDetailMapper.insert(detail);

        log.info("入库单草稿已生成，单号: {}", order.getOrderNo());
    }

    /**
     * 2. 审核入库单 (核心逻辑移到这里：加库存、生成应付账款)
     * @param orderId 单据主表的ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveInboundOrder(Long orderId) {
        log.info("准备审核入库单, 单据ID: {}", orderId);

        // 1. 查询单据并防重审
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getType() != 1) {
            throw new RuntimeException("非法的入库单");
        }
        if (order.getStatus() == 1) {
            throw new RuntimeException("该单据已审核，请勿重复操作！");
        }

        // 2. 获取单据明细（MVP里默认一单一明细，取第一条）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderDetail> detailQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        detailQuery.eq("order_id", orderId);
        OrderDetail detail = orderDetailMapper.selectOne(detailQuery);

        if (detail == null) {
            throw new RuntimeException("单据明细丢失！");
        }

        Long goodsId = detail.getGoodsId();
        Integer quantity = detail.getQuantity();
        java.math.BigDecimal totalAmount = detail.getTotalAmount();

        // 3. 联动更新库存表 (库存增加 + 成本增加) - 和以前一样
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock> stockQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        stockQuery.eq("goods_id", goodsId);
        Stock existStock = stockMapper.selectOne(stockQuery);

        if (existStock == null) {
            Stock newStock = new Stock();
            newStock.setGoodsId(goodsId);
            newStock.setQuantity(quantity);
            newStock.setTotalCost(totalAmount);
            newStock.setVersion(0);
            stockMapper.insert(newStock);
        } else {
            existStock.setQuantity(existStock.getQuantity() + quantity);
            java.math.BigDecimal currentCost = existStock.getTotalCost() != null ? existStock.getTotalCost() : java.math.BigDecimal.ZERO;
            existStock.setTotalCost(currentCost.add(totalAmount));
            stockMapper.updateById(existStock);
        }

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
        orderMapper.updateById(order);

        log.info("单号 {} 审核通过！库存和财务已更新。", order.getOrderNo());
    }

    /**
     * 添加出库单 (销售出库，控制并发防超卖)
     */
    @Transactional(rollbackFor = Exception.class)
    public void addOutboundOrder(Long partnerId, Long goodsId, Integer quantity, java.math.BigDecimal unitPrice) {
        log.info("处理销售出库, 客户: {}, 商品: {}, 数量: {}, 售价: {}", partnerId, goodsId, quantity, unitPrice);

        if (quantity == null || quantity <= 0 || unitPrice == null) {
            throw new RuntimeException("出库参数不合法");
        }

        // 1. 查询当前库存
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.eq("goods_id", goodsId);
        Stock existStock = stockMapper.selectOne(query);

        if (existStock == null || existStock.getQuantity() < quantity) {
            throw new RuntimeException("库存不足，无法出库！");
        }

        // --- 核心财务逻辑开始 ---
        // 2. 计算本次出库的成本 (当前总成本 / 当前总数量 * 出库数量)
        java.math.BigDecimal currentAvgCost = existStock.getTotalCost().divide(new java.math.BigDecimal(existStock.getQuantity()), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal outboundCost = currentAvgCost.multiply(new java.math.BigDecimal(quantity)); // 本次结转的成本

        // 3. 计算销售总金额 与 毛利润
        java.math.BigDecimal salesAmount = unitPrice.multiply(new java.math.BigDecimal(quantity));
        java.math.BigDecimal profit = salesAmount.subtract(outboundCost); // 毛利 = 售价 - 成本
        log.info("出库财务核算: 结转成本={}, 销售额={}, 毛利润={}", outboundCost, salesAmount, profit);
        // --- 核心财务逻辑结束 ---

        // 4. 扣减库存 与 扣减总成本 (乐观锁依然生效)
        existStock.setQuantity(existStock.getQuantity() - quantity);
        existStock.setTotalCost(existStock.getTotalCost().subtract(outboundCost));

        int updateRows = stockMapper.updateById(existStock);
        if (updateRows == 0) {
            throw new RuntimeException("系统繁忙，请重试！");
        }

        // 5. 生成单据主表与明细
        Order order = new Order();
        order.setOrderNo("OUT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setType(2);
        order.setPartnerId(partnerId);
        order.setCreateBy(1L);
        orderMapper.insert(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setTotalAmount(salesAmount);
        orderDetailMapper.insert(detail);

        // 6. 生成应收账款 (财务欠款)
        FinAccount finAccount = new FinAccount();
        finAccount.setPartnerId(partnerId);
        finAccount.setOrderId(order.getId());
        finAccount.setType(2); // 2-应收账款
        finAccount.setAmount(salesAmount);
        finAccount.setStatus(0);
        finAccountMapper.insert(finAccount);
    }

    public java.util.List<OrderVO> getPendingOrders() {
        return orderMapper.getPendingInboundOrders();
    }

    /**
     * 3. 采购退货单
     * 业务场景：把从供应商买来的货退回去。
     */
    @Log("执行了采购退货")
    @Transactional(rollbackFor = Exception.class)
    public void returnInboundOrder(Long partnerId, Long goodsId, Integer quantity, java.math.BigDecimal unitPrice) {
        log.info("处理采购退货, 供应商:{}, 商品:{}, 退货数量:{}, 退货单价:{}", partnerId, goodsId, quantity, unitPrice);

        if (quantity == null || quantity <= 0 || unitPrice == null) {
            throw new RuntimeException("退货数量或单价不合法");
        }

        // 1. 查询当前库存，确保有足够的货能退给别人
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock> stockQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        stockQuery.eq("goods_id", goodsId);
        Stock existStock = stockMapper.selectOne(stockQuery);

        if (existStock == null || existStock.getQuantity() < quantity) {
            throw new RuntimeException("当前库存不足，无法完成退货操作！");
        }

        // 计算退货总金额
        java.math.BigDecimal totalAmount = unitPrice.multiply(new java.math.BigDecimal(quantity));

        // 2. 扣减库存，并扣除这部分货物的成本 (相当于从仓库里拿走)
        existStock.setQuantity(existStock.getQuantity() - quantity);

        // 成本扣减逻辑：原成本 - 本次退货的金额 (防范成本扣成负数)
        java.math.BigDecimal newTotalCost = existStock.getTotalCost().subtract(totalAmount);
        if (newTotalCost.compareTo(java.math.BigDecimal.ZERO) < 0) {
            newTotalCost = java.math.BigDecimal.ZERO; // 保底，成本不为负
        }
        existStock.setTotalCost(newTotalCost);

        // 乐观锁扣库存
        int updateRows = stockMapper.updateById(existStock);
        if (updateRows == 0) {
            throw new RuntimeException("系统繁忙，请重试！");
        }

        // 3. 生成单据主表 (类型为 3: 采购退货)
        Order order = new Order();
        order.setOrderNo("RET-IN-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setType(3); // 3-采购退货单
        order.setPartnerId(partnerId);
        order.setStatus(1); // 退货单直接生效
        order.setCreateBy(1L);
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
    public void inventoryCheck(Long goodsId, Integer actualQuantity) {
        log.info("开始库存盘点, 商品ID:{}, 实盘数量:{}", goodsId, actualQuantity);

        if (actualQuantity == null || actualQuantity < 0) {
            throw new RuntimeException("实盘数量不能为负数");
        }

        // 1. 查询当前系统的账面库存
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Stock> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        query.eq("goods_id", goodsId);
        Stock stock = stockMapper.selectOne(query);

        int bookQuantity = (stock == null) ? 0 : stock.getQuantity();

        // 2. 对比差异 (如果没差，直接结束)
        if (bookQuantity == actualQuantity) {
            log.info("商品 {} 账实相符，无需平账", goodsId);
            return;
        }

        int diffQuantity = actualQuantity - bookQuantity; // 差异数量
        boolean isProfit = diffQuantity > 0; // 是否盘盈
        String checkTypeDesc = isProfit ? "盘盈" : "盘亏";

        log.warn("发现库存差异! 账面:{}, 实际:{}, {}: {}", bookQuantity, actualQuantity, checkTypeDesc, Math.abs(diffQuantity));

        // 3. 计算盘点对应的金额损失/收益 (使用当前的加权平均单价计算)
        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        if (stock != null && stock.getQuantity() > 0 && stock.getTotalCost() != null) {
            java.math.BigDecimal avgPrice = stock.getTotalCost().divide(new java.math.BigDecimal(stock.getQuantity()), 4, java.math.RoundingMode.HALF_UP);
            totalAmount = avgPrice.multiply(new java.math.BigDecimal(Math.abs(diffQuantity)));
        }

        // 4. 强制修正库存
        if (stock == null) {
            // 如果压根没这商品，属于无中生有（纯盘盈）
            stock = new Stock();
            stock.setGoodsId(goodsId);
            stock.setQuantity(actualQuantity);
            stock.setTotalCost(java.math.BigDecimal.ZERO); // TODO: 真实业务中无头盘盈需要财务定一个初始价入库
            stock.setVersion(0);
            stockMapper.insert(stock);
        } else {
            stock.setQuantity(actualQuantity); // 直接将数量覆盖为实盘数

            // 修正总成本 (盘盈就加钱，盘亏就减钱)
            java.math.BigDecimal currentCost = stock.getTotalCost() != null ? stock.getTotalCost() : java.math.BigDecimal.ZERO;
            if (isProfit) {
                stock.setTotalCost(currentCost.add(totalAmount));
            } else {
                stock.setTotalCost(currentCost.subtract(totalAmount));
            }
            int rows = stockMapper.updateById(stock);
            if (rows == 0) throw new RuntimeException("系统繁忙，请重试！");
        }

        // 5. 留痕：生成一张专门的盘点损益单据 (type=5)
        Order order = new Order();
        order.setOrderNo("CHK-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setType(5); // 5-盘点损益单
        // 盘点单没有 partnerId
        order.setStatus(1); // 盘点结果直接生效
        order.setCreateBy(1L);
        orderMapper.insert(order);

        // 生成盘点明细（记录这笔单子是因为哪个商品发生了差异）
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setGoodsId(goodsId);
        // 注意：明细里存的是差异值（带正负号），这样查明细表就能知道是亏是盈
        detail.setQuantity(diffQuantity);
        detail.setTotalAmount(isProfit ? totalAmount : totalAmount.negate());
        orderDetailMapper.insert(detail);

        log.info("库存平账完成，生成盘点单: {}", order.getOrderNo());
    }
}