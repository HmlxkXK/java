package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.AdminAccount;
import com.fenglin.springboottest.dto.AdminLoginRequest;
import com.fenglin.springboottest.mapper.AdminAccountMapper;
import com.fenglin.springboottest.util.JwtUtil;
import com.fenglin.springboottest.vo.AdminLoginVO;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 后台管理员认证接口。
 * <p>只有登录 / 会话查询 / 退出，<b>刻意没有注册</b>：管理员账号
 * 由数据库维护（启动时预置 admin / admin123456），不存在自助注册入口。
 * <ul>
 *   <li>POST /api/admin/login   登录，签发带 role=admin 的 JWT</li>
 *   <li>GET  /api/admin/me      查询当前管理员（需管理端 token）</li>
 *   <li>POST /api/admin/logout  退出（无状态方案，前端清 token 即可）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    /** 记住我：签发 7 天令牌 */
    private static final long REMEMBER_TTL_MILLIS = Duration.ofDays(7).toMillis();

    private final AdminAccountMapper adminAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminAuthController(AdminAccountMapper adminAccountMapper,
                               PasswordEncoder passwordEncoder,
                               JwtUtil jwtUtil) {
        this.adminAccountMapper = adminAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** 登录。用户名统一转小写比较；账号不存在与密码错误返回同一句提示 */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        AdminAccount admin = adminAccountMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getUsername, username));

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return Result.fail(400, "用户名或密码错误");
        }

        long ttl = request.isRemember() ? REMEMBER_TTL_MILLIS : jwtUtil.getExpireMillis();
        String nickname = admin.getNickname() == null || admin.getNickname().isBlank()
                ? admin.getUsername() : admin.getNickname();
        String token = jwtUtil.generateAdmin(username, nickname, ttl);

        return Result.ok("登录成功", AdminLoginVO.of(
                token, "Bearer", ttl / 1000, AdminLoginVO.AdminUserVO.of(username, nickname)));
    }

    /** 查询当前管理员。拦截器已确保 token 有效且为管理端令牌 */
    @GetMapping("/me")
    public Result<AdminLoginVO.AdminUserVO> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var claims = jwtUtil.parse(jwtUtil.resolveToken(authorization));
        String username = claims != null ? claims.getSubject() : null;
        String nickname = claims != null ? claims.get("nickname", String.class) : null;
        return Result.ok(AdminLoginVO.AdminUserVO.of(username, nickname));
    }

    /** 退出。JWT 无状态，服务端无会话可销毁，前端清掉本地 token 即可 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok("已退出后台登录", null);
    }
}
