package com.fenglin.springboottest.config;

import com.fenglin.springboottest.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 管理端接口鉴权拦截器。
 *
 * 拦截 {@code /api/admin/**}（登录接口除外），要求请求携带
 * {@code Authorization: Bearer <token>}，且 token 载荷含 {@code role=admin}。
 *
 * 响应约定与项目其它错误一致：HTTP 状态码仍是 200，错误写在响应体的
 * {@code code} 字段里（见 common/Result.java），前端据此统一处理。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AdminAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 浏览器预检请求不带 Authorization，必须放行，否则 CORS 会挂
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Claims claims = jwtUtil.parse(jwtUtil.resolveToken(request.getHeader("Authorization")));
        if (!jwtUtil.isAdminClaims(claims)) {
            deny(response);
            return false;
        }

        // 后续 controller 可直接取当前管理员
        request.setAttribute("adminUsername", claims.getSubject());
        request.setAttribute("adminNickname", claims.get("nickname", String.class));
        return true;
    }

    private void deny(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":401,\"message\":\"后台登录已失效，请重新登录\",\"data\":null}");
    }
}
