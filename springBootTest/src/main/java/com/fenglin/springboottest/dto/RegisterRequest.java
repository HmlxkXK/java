package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体，对应前端 {@code POST /api/auth/register} 的 JSON body：
 * <pre>{"nickname":"...","password":"..."}</pre>
 * <p>注册不再需要邮箱：昵称即登录账号（须唯一）。图片验证码由前端 canvas
 * 生成并校验，后端不接收验证码字段。
 */
public class RegisterRequest {

    /** 昵称同时作为登录账号，需唯一 */
    @NotBlank(message = "请填写昵称")
    @Size(min = 2, max = 50, message = "昵称长度需在 2-50 个字符之间")
    @Pattern(regexp = "^[\\w\\u4e00-\\u9fa5-]+$", message = "昵称只能包含字母、数字、下划线、中文和连字符")
    private String nickname;

    @NotBlank(message = "请填写密码")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    private String password;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
