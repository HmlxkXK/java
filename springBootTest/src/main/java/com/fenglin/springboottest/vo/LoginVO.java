package com.fenglin.springboottest.vo;

/**
 * 登录成功返回体。前端从 {@code data.token} 取凭证并存 localStorage。
 */
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 固定为 Bearer，方便前端拼 Authorization 头 */
    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    /** 当前登录用户信息 */
    private UserVO user;

    public LoginVO() {
    }

    public LoginVO(String token, long expiresIn, UserVO user) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
