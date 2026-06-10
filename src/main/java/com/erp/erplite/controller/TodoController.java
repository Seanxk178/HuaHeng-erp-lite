package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.entity.TodoVO;
import com.erp.erplite.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 待办任务 API 接口
 * 负责为工作台看板聚合展示用户的待处理任务
 */
@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    /**
     * 获取当前登录人的所有待办事项
     * 请求方式: GET /todo/my
     */
    @GetMapping("/my")
    public Result<List<TodoVO>> getMyTodos() {
        return Result.success(todoService.getMyTodos());
    }
}
