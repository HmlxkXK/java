package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 社员（用户）实体。
 * <p>表名用 {@code t_user}，因为 {@code user} 是 MySQL 的保留字。
 * <p>数据访问层已从 JPA 迁移到 MyBatis-Plus：
 * <ul>
 *   <li>{@code @TableName} 对应原 JPA 的 {@code @Table}；</li>
 *   <li>{@code @TableId(type = AUTO)} 对应原来的数据库自增主键；</li>
 *   <li>原 {@code @PrePersist} 的注册时间兜底改由
 *       {@code MybatisPlusMetaObjectHandler} 在 insert 时统一填充；</li>
 *   <li>字段名与列名的驼峰/下划线映射由全局 {@code map-underscore-to-camel-case} 完成，
 *       无需逐列标注。</li>
 * </ul>
 */
@TableName("t_user")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 登录账号（邮箱），唯一 */
    private String email;

    /** BCrypt 加密后的密码，绝不明文存储 */
    private String password;

    /** 注册时间，插入时由 MetaObjectHandler 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

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
