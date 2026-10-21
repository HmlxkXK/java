package com.fenglin.springboottest.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 数据库连接启动自检。
 * <p>在任何一个 Repository 被使用之前跑一次真实的 {@code getConnection()}，把
 * 连接池实际生效的参数、MySQL 版本、握手耗时打到日志里。
 * <p>目的是把"连不上数据库"这类问题在启动阶段就暴露成一句能看懂的话：
 * 以前只会在第一次请求时抛出一串 {@code SQLTransientConnectionException} 堆栈，
 * 现在会在启动时直接点明是服务没起、账号密码不对，还是 URL 写错了。
 * <p>用 {@code @Order(HIGHEST_PRECEDENCE)} 保证它排在
 * {@link DataInitializer} 的演示账号初始化之前——库都连不上时，不该继续写数据。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSourceDiagnostics.class);

    /** 校验连接是否可用的超时（秒） */
    private static final int VALIDATION_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;

    public DataSourceDiagnostics(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        reportPoolSettings();
        probe();
    }

    /** 打印真正生效的池参数。配置写错了（比如被 profile 覆盖）一眼就能看出来。 */
    private void reportPoolSettings() {
        if (dataSource instanceof HikariDataSource hikari) {
            log.info("HikariCP 生效参数 -> pool={}, maximumPoolSize={}, minimumIdle={}, "
                            + "connectionTimeout={}ms, validationTimeout={}ms, "
                            + "maxLifetime={}s, idleTimeout={}s, keepaliveTime={}s, "
                            + "leakDetectionThreshold={}ms",
                    hikari.getPoolName(),
                    hikari.getMaximumPoolSize(),
                    hikari.getMinimumIdle(),
                    hikari.getConnectionTimeout(),
                    hikari.getValidationTimeout(),
                    hikari.getMaxLifetime() / 1000,
                    hikari.getIdleTimeout() / 1000,
                    hikari.getKeepaliveTime() / 1000,
                    hikari.getLeakDetectionThreshold());
        } else {
            log.warn("当前 DataSource 不是 HikariCP（{}），跳过连接池参数打印",
                    dataSource.getClass().getName());
        }
    }

    /** 真实取一次连接，确认握手能走通。 */
    private void probe() {
        long start = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            long handshakeMillis = System.currentTimeMillis() - start;

            log.info("数据库连接正常 -> {} {}（驱动 {} {}）, 连接耗时 {}ms, URL={}",
                    meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion(),
                    meta.getDriverName(),
                    meta.getDriverVersion(),
                    handshakeMillis,
                    meta.getURL());

            // HikariCP 内部就是用 isValid() 探活的，这里显式跑一次，
            // 避免"拿到连接对象但底层其实已经断开"的假成功。
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new IllegalStateException("连接对象已获取但 isValid() 返回 false，"
                        + "通常是 MySQL 侧主动断开了该连接");
            }
        } catch (SQLException e) {
            // 把常见成因翻译成人话，再抛出原异常保留堆栈。
            throw new IllegalStateException(buildHint(e), e);
        }
    }

    /**
     * 根据异常内容给出可执行的排查方向。
     * <p>不吞掉原始异常，调用方仍能拿到完整堆栈。
     */
    private String buildHint(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        StringBuilder hint = new StringBuilder("数据库连接失败，应用终止启动。");

        if (message.contains("Access denied")) {
            hint.append(" 账号或密码不正确：检查 application-dev.properties 的 "
                    + "spring.datasource.username / password，"
                    + "或设置环境变量 DB_USERNAME / DB_PASSWORD。");
        } else if (state.startsWith("08") || message.contains("Connection refused")
                || message.contains("Communications link failure")) {
            hint.append(" MySQL 可能没有启动或端口不对：确认服务在 listen 3306，"
                    + "必要时用 DB_URL 覆盖连接串。");
        } else if (message.contains("Unknown database")) {
            hint.append(" 目标库不存在且未允许自动创建：给账号 CREATE 权限，"
                    + "或先手工执行 CREATE DATABASE。");
        } else if (message.contains("Public Key Retrieval is not allowed")) {
            hint.append(" MySQL 8 的 caching_sha2_password 需要 URL 带 "
                    + "allowPublicKeyRetrieval=true（或改用 useSSL=true）。");
        } else if (message.contains("Unknown character set")
                || message.contains("Unsupported character encoding")) {
            hint.append(" characterEncoding 应填 Java 字符集名 UTF-8，"
                    + "不是 MySQL 的排序规则名 utf8mb4。");
        } else {
            hint.append(" 请检查 MySQL 是否运行、连接串与账号权限。");
        }
        hint.append(" 原始错误：").append(message);
        return hint.toString();
    }
}
