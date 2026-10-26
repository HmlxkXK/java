package com.fenglin.springboottest.vo;

/**
 * 后台登录返回值。
 */
public class AdminLoginVO {

    private String token;
    private String tokenType;
    private long expiresIn;
    private AdminUserVO user;

    public static AdminLoginVO of(String token, String tokenType, long expiresIn, AdminUserVO user) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.token = token;
        vo.tokenType = tokenType;
        vo.expiresIn = expiresIn;
        vo.user = user;
        return vo;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public AdminUserVO getUser() { return user; }
    public void setUser(AdminUserVO user) { this.user = user; }

    /** 管理员的对外信息，刻意不含密码等敏感字段 */
    public static class AdminUserVO {
        private String username;
        private String nickname;

        public static AdminUserVO of(String username, String nickname) {
            AdminUserVO vo = new AdminUserVO();
            vo.username = username;
            vo.nickname = nickname;
            return vo;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }
}
