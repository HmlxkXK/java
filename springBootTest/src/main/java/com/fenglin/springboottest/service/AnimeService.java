package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.AnimeRequest;
import com.fenglin.springboottest.entity.Anime;
import com.fenglin.springboottest.entity.AnimeEpisode;

import java.util.List;
import java.util.Optional;

/**
 * 番剧业务服务。
 * <p>本次优化把番剧 CRUD 与剧集查询从控制器收拢到 service 层：
 * <ul>
 *   <li>控制器只做参数校验与响应包装，不再直接持有 Mapper ——
 *       事务边界、缓存、权限之类的横切逻辑以后都加在这一层；</li>
 *   <li>两个写操作（新增/编辑）标记 {@code @Transactional}，
 *       后续如果一次请求要写多张表（如番剧+剧集），已经预留好原子性。</li>
 * </ul>
 */
public interface AnimeService {

    /** 首页公开列表：只取上架的，按 sort 升序、id 升序稳定排列 */
    List<Anime> listVisible();

    /** 按 id 查单部番剧（公开详情用） */
    Optional<Anime> getById(Long id);

    /** 后台全量列表（含下架），按 sort 升序、id 升序 */
    List<Anime> listAllForAdmin();

    /** 新增。sort 为空时自动排到当前末尾 */
    Anime create(AnimeRequest request);

    /** 编辑，番剧不存在返回 Optional.empty() */
    Optional<Anime> update(Long id, AnimeRequest request);

    /** 上下架切换，番剧不存在返回 Optional.empty() */
    Optional<Anime> toggleVisible(Long id, boolean visible);

    /** 删除，返回是否真的删了（false = 本就不存在） */
    boolean delete(Long id);

    /** 某部番剧的剧集列表，按集数升序 */
    List<AnimeEpisode> listEpisodes(Long animeId);

    /** 按剧集 id 取单集（视频流接口用） */
    AnimeEpisode getEpisode(Long episodeId);

    /** 下一集：同番剧中集数大于当前的最近一集，没有则 empty（播放页连播用） */
    Optional<AnimeEpisode> nextEpisode(Long animeId, int currentEpisodeNo);
}
