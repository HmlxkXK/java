package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.User;

/**
 * 社员数据访问层。继承 BaseMapper 后即拥有通用 CRUD，
 * 邮箱查询、存在性判断等用 LambdaQueryWrapper 在调用方拼装。
 */
public interface UserMapper extends BaseMapper<User> {
}
