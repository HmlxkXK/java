package com.fenglin.springboottest.vo;

import com.fenglin.springboottest.entity.User;

import java.time.format.DateTimeFormatter;

/**
 * 对外暴露的用户信息。刻意不含密码字段，避免任何形式的泄露。
 */
public class UserVO {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String nickname;
    private String email;
    /** 注册时间，格式化好的字符串，前端拿来即用 */
    private String createTime;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setCreateTime(user.getCreateTime() == null ? null : user.getCreateTime().format(FORMATTER));
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
