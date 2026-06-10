package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.Warehouse;
import com.erp.erplite.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {
    
    private final WarehouseService warehouseService;

    @GetMapping("/list")
    public Result<List<Warehouse>> getList() {
        return Result.success(warehouseService.getWarehouseList());
    }

    @PostMapping("/add")
    public Result<String> addWarehouse(@RequestBody Warehouse warehouse) {
        warehouseService.addWarehouse(warehouse);
        return Result.success("添加成功");
    }

    @PostMapping("/update")
    public Result<String> updateWarehouse(@RequestBody Warehouse warehouse) {
        warehouseService.updateWarehouse(warehouse);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success("删除成功");
    }
}
