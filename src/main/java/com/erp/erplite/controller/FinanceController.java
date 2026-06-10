package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.AnalysisVO;
import com.erp.erplite.entity.FinAccountVO;
import com.erp.erplite.entity.FinanceDashboardVO;
import com.erp.erplite.service.FinanceService;
import com.erp.erplite.common.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.erp.erplite.common.SystemConstants;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@com.erp.erplite.common.RequireRole({SystemConstants.ROLE_FINANCE, SystemConstants.ROLE_ADMIN})
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/dashboard")
    public Result<FinanceDashboardVO> getDashboard() {
        FinanceDashboardVO data = financeService.getDashboardData();
        return Result.success(data);
    }

    /**
     * 获取账款明细列表
     */
    @GetMapping("/accountList")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<FinAccountVO>> getAccountList(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(financeService.getAccountList(type, startDate, endDate, keyword, pageNum, pageSize));
    }

    /**
     * 核销账款
     * POST /finance/settle/{accountId}
     */
    @PostMapping("/settle/{accountId}")
    public Result<String> settleAccount(@PathVariable Long accountId, 
                                      @RequestParam(required = false) java.math.BigDecimal payAmount) {
        financeService.settleAccount(accountId, payAmount);
        return Result.success("账款核销成功");
    }

    @GetMapping("/ageAnalysis")
    public Result<java.util.List<AnalysisVO>> analyzeDebtAge() {
        return Result.success(financeService.analyzeDebtAge());
    }
}