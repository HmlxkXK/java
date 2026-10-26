package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.VerificationCodeStore;
import com.fenglin.springboottest.util.JwtUtil;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 认证服务实现。
 * <p>数据访问层已从 JPA 迁移到 MyBatis-Plus：
 * <ul>
 *   <li>existsByEmail  -> selectCount（存在即 &gt;0）；</li>
 *   <li>findByEmail    -> selectOne（eq email）；</li>
 *   <li>save           -> insert（新纪录均无主键，无需 saveOrUpdate 分支）。</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 与前端保持一致的邮箱格式校验 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** "记住我" 时的 token 有效期：7 天 */
    private static final long REMEMBER_ME_TTL_MILLIS = Duration.ofDays(7).toMillis();

    private final UserMapper userMapper;
    private final VerificationCodeStore codeStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper,
                           VerificationCodeStore codeStore,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.codeStore = codeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String sendCode(String email) {
        String normalized = normalize(email);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BizException("邮箱格式不正确");
        }
        if (existsByEmail(normalized)) {
            throw new BizException("该邮箱已注册，请直接登录");
        }

        String code = codeStore.generate(normalized);

        // 真实项目这里应调用邮件服务把验证码发出去；
        // 演示环境直接返回明文，前端 alert 出来方便自测。
        log.info("生成注册验证码 -> email={}, code={}（演示环境明文返回）", normalized, code);
        return code;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        String email = normalize(request.getEmail());

        if (existsByEmail(email)) {
            throw new BizException("该邮箱已被注册");
        }
        // 先校验验证码，通过后立即消费，避免一次验证码反复注册
        if (!codeStore.verifyAndConsume(email, request.getCode())) {
            throw new BizException("验证码错误或已过期");
        }

        User user = new User();
        user.setNickname(request.getNickname().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userMapper.insert(user);
        log.info("新社员注册成功 -> id={}, email={}", user.getId(), user.getEmail());
        return UserVO.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginVO login(LoginRequest request) {
        String account = normalize(request.getAccount());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account));
        // 账号不存在与密码错误返回同一提示，避免暴露账号是否注册
        if (user == null) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "账号或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "账号或密码错误");
        }

        boolean rememberMe = Boolean.TRUE.equals(request.getRemember());
        long ttlMillis = rememberMe ? REMEMBER_ME_TTL_MILLIS : jwtUtil.getExpireMillis();
        String token = jwtUtil.generate(user, ttlMillis);

        log.info("社员登录成功 -> id={}, email={}, rememberMe={}", user.getId(), user.getEmail(), rememberMe);
        return new LoginVO(token, ttlMillis / 1000, UserVO.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO currentUser(String token) {
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "登录已过期，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "用户不存在或已注销");
        }
        return UserVO.from(user);
    }

    /** 注册时判断邮箱是否已占用 */
    private boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    /** 邮箱统一按小写去空格处理，避免大小写导致的重复注册 */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
