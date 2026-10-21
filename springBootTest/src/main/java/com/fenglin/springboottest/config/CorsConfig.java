package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。
 * <p>前端 Vite 开发服务器跑在 5173，后端在 8081，属于不同源，必须放行，
 * 否则浏览器会拦掉请求。这里只对 /api/** 生效，不影响原有页面。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 用 allowedOriginPatterns 而不是 allowedOrigins，
                // 这样在 allowCredentials=true 时也能匹配通配端口
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                // 预检请求缓存 1 小时，减少 OPTIONS 往返
                .maxAge(3600);
    }
}
