package com.fenglin.springboottest.util;

import com.fenglin.springboottest.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 签发与校验工具（jjwt 0.12.x）。
 * <p>载荷里只放 userId / email / nickname 这类非敏感信息，密码永不入 token。
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** HS256 要求密钥不少于 256 bit，即 32 个 ASCII 字符 */
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours:24}") long expireHours) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret 至少需要 " + MIN_SECRET_LENGTH + " 个字符，当前只有 " + secretBytes.length + " 个");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expireMillis = Duration.ofHours(expireHours).toMillis();
    }

    /** 默认有效期 */
    public long getExpireMillis() {
        return expireMillis;
    }

    /** 按默认有效期签发 */
    public String generate(User user) {
        return generate(user, expireMillis);
    }

    /**
     * 按指定有效期签发。
     *
     * @param ttlMillis 有效期（毫秒），"记住我" 场景可传更长的值
     */
    public String generate(User user, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 签发后台管理员令牌。
     * <p>与社员令牌的区别是载荷里多一个 {@code role=admin}，
     * 管理端接口（/api/admin/**）只认带这个标记的 token。
     *
     * @param ttlMillis 有效期（毫秒），"记住我" 场景可传更长的值
     */
    public String generateAdmin(String username, String nickname, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .subject(username)
                .claim("role", "admin")
                .claim("nickname", nickname)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 token。
     *
     * @return 解析成功返回 Claims；签名错误、过期、格式非法统一返回 null
     */
    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 校验失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从 token 中取用户 id，失败返回 null */
    public Long parseUserId(String token) {
        Claims claims = parse(token);
        if (claims == null || claims.getSubject() == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断 Claims 是否为管理员令牌（role=admin）。
     */
    public boolean isAdminClaims(Claims claims) {
        return claims != null && "admin".equals(claims.get("role", String.class));
    }

    /**
     * 从 {@code Authorization: Bearer xxx} 请求头中剥出 token。
     */
    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authorizationHeader.substring(prefix.length()).trim();
        }
        return authorizationHeader.trim();
    }
}
