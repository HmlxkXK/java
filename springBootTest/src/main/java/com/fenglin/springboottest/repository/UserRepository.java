package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 社员数据访问层。继承 JpaRepository，常规 CRUD 无需手写实现。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按邮箱（登录账号）查询 */
    Optional<User> findByEmail(String email);

    /** 注册时判断邮箱是否已占用 */
    boolean existsByEmail(String email);
}
