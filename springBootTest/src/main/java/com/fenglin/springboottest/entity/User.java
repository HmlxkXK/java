package com.fenglin.springboottest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 社员（用户）实体。
 * <p>表名用 {@code t_user}，因为 {@code user} 是 MySQL 的保留字。
 */
@Entity
@Table(name = "t_user")
public class User {

    /** 主键，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 昵称 */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /** 登录账号（邮箱），唯一 */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 加密后的密码，绝不明文存储 */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /** 注册时间 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
