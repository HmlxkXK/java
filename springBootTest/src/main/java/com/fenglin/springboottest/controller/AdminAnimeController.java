package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.AnimeRequest;
import com.fenglin.springboottest.entity.Anime;
import com.fenglin.springboottest.service.AnimeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 番剧管理接口（需管理端 token，见 AdminAuthInterceptor）。
 * <p>对首页「本周热门番剧」板块的数据做全量增删改查与上下架。
 * <ul>
 *   <li>GET    /api/admin/animes              全量列表</li>
 *   <li>POST   /api/admin/animes              新增</li>
 *   <li>PUT    /api/admin/animes/{id}         编辑</li>
 *   <li>PUT    /api/admin/animes/{id}/visible 上下架切换</li>
 *   <li>DELETE /api/admin/animes/{id}         删除</li>
 * </ul>
 * <p>本次优化：CRUD 全部下沉到 {@link AnimeService}，控制器只做「评分格式
 * 校验 + Result 包装」，业务规则（sort 补尾、级联删剧集、事务）都在 service。
 */
@RestController
@RequestMapping("/api/admin/animes")
public class AdminAnimeController {

    private final AnimeService animeService;

    public AdminAnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    /** 全量列表（含下架的），按 sort 升序 */
    @GetMapping
    public Result<List<Anime>> list() {
        return Result.ok(animeService.listAllForAdmin());
    }

    /** 新增。新建记录默认排在最后 */
    @PostMapping
    public Result<Anime> create(@Valid @RequestBody AnimeRequest request) {
        if (badRating(request)) {
            return Result.fail(400, "评分格式应为 0-10 的数字，如 9.2");
        }
        return Result.ok("新增成功", animeService.create(request));
    }

    /** 编辑 */
    @PutMapping("/{id}")
    public Result<Anime> update(@PathVariable Long id, @Valid @RequestBody AnimeRequest request) {
        if (badRating(request)) {
            return Result.fail(400, "评分格式应为 0-10 的数字，如 9.2");
        }
        return animeService.update(id, request)
                .map(anime -> Result.ok("保存成功", anime))
                .orElseGet(() -> Result.fail(400, "番剧不存在或已被删除"));
    }

    /** 上下架切换：下架后首页不再展示该番剧 */
    @PutMapping("/{id}/visible")
    public Result<Anime> toggleVisible(@PathVariable Long id, @RequestParam("visible") boolean visible) {
        return animeService.toggleVisible(id, visible)
                .map(anime -> Result.ok(visible ? "已上架" : "已下架", anime))
                .orElseGet(() -> Result.fail(400, "番剧不存在或已被删除"));
    }

    /** 删除。已配置的剧集会一并删除，避免孤儿记录 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return animeService.delete(id)
                ? Result.ok("删除成功", null)
                : Result.fail(400, "番剧不存在或已被删除");
    }

    /** 评分非空时必须是 "9.2" 这类 0-10 数字 */
    private boolean badRating(AnimeRequest request) {
        return request.getRating() != null && !request.getRating().isBlank()
                && !request.getRating().matches("\\d{1,2}(\\.\\d)?");
    }
}
