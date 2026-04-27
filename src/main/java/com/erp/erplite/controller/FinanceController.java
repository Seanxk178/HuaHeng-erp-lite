package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.AnalysisVO;
import com.erp.erplite.entity.FinAccountVO;
import com.erp.erplite.entity.FinanceDashboardVO;
import com.erp.erplite.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/dashboard")
    public Result<FinanceDashboardVO> getDashboard() {
        FinanceDashboardVO data = financeService.getDashboardData();
        return Result.success(data);
    }

    /**
     * 获取账款明细列表
     * GET /finance/accountList?type=1 (可选参数)
     */
    @GetMapping("/accountList")
    public Result<java.util.List<FinAccountVO>> getAccountList(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer type) {
        return Result.success(financeService.getAccountList(type));
    }

    /**
     * 核销账款
     * POST /finance/settle/{accountId}
     */
    @PostMapping("/settle/{accountId}")
    public Result<String> settleAccount(@PathVariable Long accountId) {
        financeService.settleAccount(accountId);
        return Result.success("账款核销成功");
    }

    @GetMapping("/ageAnalysis")
    public Result<java.util.List<AnalysisVO>> analyzeDebtAge() {
        return Result.success(financeService.analyzeDebtAge());
    }
}