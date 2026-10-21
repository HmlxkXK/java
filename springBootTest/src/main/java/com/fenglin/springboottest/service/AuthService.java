package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.UserVO;

/**
 * 社员认证服务。
 */
public interface AuthService {

    /**
     * 为邮箱生成验证码。
     *
     * @return 验证码明文（演示环境直接回传，便于前端自测）
     */
    String sendCode(String email);

    /** 注册 */
    UserVO register(RegisterRequest request);

    /** 登录，成功返回 token 与用户信息 */
    LoginVO login(LoginRequest request);

    /** 根据 token 获取当前登录用户 */
    UserVO currentUser(String token);
}
