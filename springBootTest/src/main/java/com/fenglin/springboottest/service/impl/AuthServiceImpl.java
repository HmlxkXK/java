package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.ProfileRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.util.JwtUtil;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 认证服务实现。
 * <p>数据访问层已从 JPA 迁移到 MyBatis-Plus：
 * <ul>
 *   <li>existsByAccount -> selectCount（存在即 &gt;0）；</li>
 *   <li>findByAccount   -> selectOne（eq email 列存昵称账号）；</li>
 *   <li>save           -> insert（新纪录均无主键，无需 saveOrUpdate 分支）。</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** "记住我" 时的 token 有效期：7 天 */
    private static final long REMEMBER_ME_TTL_MILLIS = Duration.ofDays(7).toMillis();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String sendCode(String account) {
        // 图片验证码改由前端 canvas 生成，此接口保留仅为兼容旧调用方，不再发送邮件。
        // 若调用方传入的是昵称账号，仅做一次占用提示。
        String normalized = normalize(account);
        if (existsByAccount(normalized)) {
            throw new BizException("该昵称已被注册，请直接登录");
        }
        log.info("收到验证码请求（注册已改为前端图片验证码，无需后端发送）-> account={}", normalized);
        return "";
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        // 昵称即登录账号，统一小写去空格后作为唯一账号
        String account = normalize(request.getNickname());

        if (existsByAccount(account)) {
            throw new BizException("该昵称已被注册，请换一个");
        }

        User user = new User();
        // email 列保持表结构不变，实际存放登录账号（昵称）
        user.setEmail(account);
        user.setNickname(request.getNickname().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userMapper.insert(user);
        log.info("新社员注册成功 -> id={}, account={}", user.getId(), account);
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

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, ProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "用户不存在或已注销");
        }

        String newNickname = request.getNickname();
        boolean wantNickname = newNickname != null && !newNickname.isBlank();
        String newPassword = request.getNewPassword();
        boolean wantPassword = newPassword != null && !newPassword.isBlank();

        if (!wantNickname && !wantPassword) {
            throw new BizException("请先填写要修改的内容");
        }

        // 改昵称：昵称即登录账号（存于 email 列），需校验唯一
        if (wantNickname) {
            String account = normalize(newNickname);
            User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account));
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BizException("该昵称已被注册，请换一个");
            }
            user.setNickname(newNickname.trim());
            user.setEmail(account);
        }

        // 改密码：先校验原密码
        if (wantPassword) {
            if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
                throw new BizException("修改密码需先填写原密码");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BizException("原密码不正确");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userMapper.updateById(user);
        log.info("社员资料更新 -> id={}, nickname={}", user.getId(), user.getNickname());
        return UserVO.from(user);
    }

    /** 注册时判断登录账号（昵称，存于 email 列）是否已占用 */
    private boolean existsByAccount(String account) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, account)) > 0;
    }

    /** 邮箱统一按小写去空格处理，避免大小写导致的重复注册 */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
