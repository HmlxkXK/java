package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 后台管理员账号。
 * <p>与前台社员（{@link User}）完全分开：两张表、两套登录接口、
 * 前端也用两份互不干扰的本地存储。管理员只能由数据库 / 启动预置产生，
 * 后台不提供注册入口。
 */
@TableName("admin_account")
public class AdminAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一 */
    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 展示用的昵称 */
    private String nickname;

    /** 创建时间，插入时由 MetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
