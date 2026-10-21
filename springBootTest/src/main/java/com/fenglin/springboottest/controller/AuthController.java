package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.util.JwtUtil;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 * <p>路径与响应结构与前端 reigeki-anime 已写好的调用完全对齐：
 * <ul>
 *   <li>POST /api/auth/send-code?email=xxx     获取邮箱验证码</li>
 *   <li>POST /api/auth/register               注册</li>
 *   <li>POST /api/auth/login                  登录</li>
 *   <li>GET  /api/auth/me                     查询当前登录用户</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /** 获取邮箱验证码，演示环境直接把验证码放在 data 里返回 */
    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestParam("email") String email) {
        String code = authService.sendCode(email);
        return Result.ok("验证码已发送", code);
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO user = authService.register(request);
        return Result.ok("注册成功，欢迎加入燃尽动漫", user);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO loginVO = authService.login(request);
        return Result.ok("登录成功", loginVO);
    }

    /** 查询当前登录用户，需要带 Authorization: Bearer &lt;token&gt; */
    @GetMapping("/me")
    public Result<UserVO> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserVO user = authService.currentUser(jwtUtil.resolveToken(authorization));
        return Result.ok(user);
    }
}
