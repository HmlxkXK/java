package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Anime;

/**
 * 番剧数据访问层。排序、上下架过滤等用 LambdaQueryWrapper 在调用方拼装。
 */
public interface AnimeMapper extends BaseMapper<Anime> {
}
