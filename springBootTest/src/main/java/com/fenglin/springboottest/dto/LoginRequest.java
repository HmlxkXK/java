package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体，对应前端 {@code POST /api/auth/login} 的 JSON body：
 * <pre>{"account":"账号或邮箱","password":"...","remember":false}</pre>
 */
public class LoginRequest {

    @NotBlank(message = "请输入账号或邮箱")
    private String account;

    @NotBlank(message = "请输入密码")
    private String password;

    /** 记住我：为 true 时服务端签发更长有效期的 token（前端可选传） */
    private Boolean remember;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRemember() {
        return remember;
    }

    public void setRemember(Boolean remember) {
        this.remember = remember;
    }
}
