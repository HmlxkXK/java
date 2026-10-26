package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Anime;
import com.fenglin.springboottest.entity.AnimeEpisode;
import com.fenglin.springboottest.service.AnimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页公开数据接口。
 * <ul>
 *   <li>GET /api/animes               —— 首页「本周热门番剧」板块；</li>
 *   <li>GET /api/animes/{id}          —— 播放页头部信息（单部番剧）；</li>
 *   <li>GET /api/animes/{id}/episodes —— 播放页剧集列表，按集数升序。</li>
 * </ul>
 * 数据层已收拢到 {@link AnimeService}，控制器只做校验与包装。
 */
@RestController
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @GetMapping("/api/animes")
    public Result<List<Anime>> list() {
        return Result.ok(animeService.listVisible());
    }

    /** 单部番剧详情（播放页用）。不存在时返回业务码 400 与提示文案 */
    @GetMapping("/api/animes/{id}")
    public Result<Anime> detail(@PathVariable Long id) {
        return animeService.getById(id)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(400, "番剧不存在或已下架"));
    }

    /** 某部番剧的剧集列表（播放页选集用），按集数升序 */
    @GetMapping("/api/animes/{id}/episodes")
    public Result<List<AnimeEpisode>> episodes(@PathVariable Long id) {
        return Result.ok(animeService.listEpisodes(id));
    }
}
