package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 后台登录请求体。只有用户名 + 密码，不存在注册流程。
 */
public class AdminLoginRequest {

    @NotBlank(message = "请输入用户名")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 勾选后签发 7 天令牌，否则用默认有效期 */
    private boolean remember;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isRemember() { return remember; }
    public void setRemember(boolean remember) { this.remember = remember; }
}
