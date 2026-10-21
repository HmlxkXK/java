package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体，对应前端 {@code POST /api/auth/register} 的 JSON body：
 * <pre>{"nickname":"...","email":"...","password":"...","code":"123456"}</pre>
 */
public class RegisterRequest {

    @NotBlank(message = "请填写昵称")
    @Size(max = 50, message = "昵称最长 50 个字符")
    private String nickname;

    @NotBlank(message = "请填写邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 个字符")
    private String email;

    @NotBlank(message = "请填写密码")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    private String password;

    @NotBlank(message = "请填写邮箱验证码")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
