package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密器。
 * <p>只引入 spring-security-crypto 这一个包（不是整套 Spring Security），
 * 所以不会给项目加上默认的登录拦截，原有 Thymeleaf 页面不受影响。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 自带随机盐，同一个密码每次加密结果都不同
        return new BCryptPasswordEncoder();
    }
}
