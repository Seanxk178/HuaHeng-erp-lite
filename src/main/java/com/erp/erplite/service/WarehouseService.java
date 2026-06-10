package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.common.BusinessException;
import com.erp.erplite.common.Log;
import com.erp.erplite.entity.Warehouse;
import com.erp.erplite.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {
    
    private final WarehouseMapper warehouseMapper;

    public List<Warehouse> getWarehouseList() {
        QueryWrapper<Warehouse> query = new QueryWrapper<>();
        query.orderByDesc("id");
        return warehouseMapper.selectList(query);
    }

    @Log("执行了添加仓库操作")
    public void addWarehouse(Warehouse warehouse) {
        // 校验编码是否存在
        QueryWrapper<Warehouse> query = new QueryWrapper<>();
        query.eq("code", warehouse.getCode());
        if (warehouseMapper.selectCount(query) > 0) {
            throw new BusinessException("仓库编码已存在: " + warehouse.getCode());
        }
        warehouse.setCreateTime(new Date());
        warehouse.setStatus(1); // 默认启用
        warehouseMapper.insert(warehouse);
    }

    @Log("执行了修改仓库操作")
    public void updateWarehouse(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            throw new BusinessException("仓库ID不能为空");
        }
        
        // 校验编码冲突
        if (warehouse.getCode() != null) {
            QueryWrapper<Warehouse> query = new QueryWrapper<>();
            query.eq("code", warehouse.getCode()).ne("id", warehouse.getId());
            if (warehouseMapper.selectCount(query) > 0) {
                throw new BusinessException("仓库编码已存在: " + warehouse.getCode());
            }
        }
        
        warehouseMapper.updateById(warehouse);
    }

    @Log("执行了删除仓库操作")
    public void deleteWarehouse(Long id) {
        // 在实际业务中可能需要校验仓库下是否有库存或单据，此处为简化版
        warehouseMapper.deleteById(id);
    }
}
