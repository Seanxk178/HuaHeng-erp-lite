package com.erp.erplite.controller;

import com.erp.erplite.common.Result;
import com.erp.erplite.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Data
    public static class LoginParam {
        private String username;
        private String password;
    }

    /**
     * 登录 API
     * 请求方式: POST /user/login
     */
    @PostMapping("/login")
    public Result<com.erp.erplite.entity.LoginVO> login(@RequestBody LoginParam param) {
        if (param.getUsername() == null || param.getPassword() == null) {
            return Result.error("账号和密码不能为空");
        }
        return Result.success(userService.login(param.getUsername(), param.getPassword()));
    }
}