package com.fenglin.springboottest.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置。
 * <ul>
 *   <li>Mapper 扫描：等价于在每个 Mapper 上写 @Mapper；</li>
 *   <li>分页插件：为后续列表分页预留，当前不影响既有查询；</li>
 *   <li>字段自动填充：替代原 JPA 的 @PrePersist / @PreUpdate ——
 *       create_time 在插入时填充，update_time 在插入和更新时填充。</li>
 * </ul>
 */
@Configuration
public class MybatisPlusConfig {

    /** Mapper 接口所在包，启动时为它们注册代理实现 */
    public static final String MAPPER_PACKAGE = "com.fenglin.springboottest.mapper";

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                // strictInsertFill 只在字段为 null 且确有 @TableField(fill=INSERT) 时写入，
                // 不会覆盖调用方显式设置的值
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
