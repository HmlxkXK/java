package com.fenglin.springboottest.config;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 启动时预置两个演示账号，方便直接跑通登录流程。
 * <p>已存在的邮箱会跳过，不会覆盖你手动注册的数据。
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * 显式声明为最低优先级，保证排在 {@link DataSourceDiagnostics} 之后执行：
     * 数据库连不上时先由自检抛出可读的错误，而不是在这里写数据时失败。
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ApplicationRunner initDemoUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            seed(userRepository, passwordEncoder, "燃尽小队长", "reigeki@demo.com", "reigeki123");
            seed(userRepository, passwordEncoder, "管理员", "admin@demo.com", "admin123456");
        };
    }

    private void seed(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      String nickname,
                      String email,
                      String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        log.info("预置演示账号 -> {} / {}", email, rawPassword);
    }
}
