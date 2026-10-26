package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.dto.AnimeRequest;
import com.fenglin.springboottest.entity.Anime;
import com.fenglin.springboottest.entity.AnimeEpisode;
import com.fenglin.springboottest.mapper.AnimeEpisodeMapper;
import com.fenglin.springboottest.mapper.AnimeMapper;
import com.fenglin.springboottest.service.AnimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 番剧业务服务实现。
 * <p>评分格式等「值校验」仍留在控制器（属于 HTTP 请求的形状约束），
 * 这里负责数据库操作与业务规则（如 sort 自动补尾、存在性判断）。
 */
@Service
public class AnimeServiceImpl implements AnimeService {

    private final AnimeMapper animeMapper;
    private final AnimeEpisodeMapper episodeMapper;

    public AnimeServiceImpl(AnimeMapper animeMapper, AnimeEpisodeMapper episodeMapper) {
        this.animeMapper = animeMapper;
        this.episodeMapper = episodeMapper;
    }

    @Override
    public List<Anime> listVisible() {
        return animeMapper.selectList(new LambdaQueryWrapper<Anime>()
                .eq(Anime::getVisible, true)
                .orderByAsc(Anime::getSort)
                .orderByAsc(Anime::getId));
    }

    @Override
    public Optional<Anime> getById(Long id) {
        return Optional.ofNullable(animeMapper.selectById(id));
    }

    @Override
    public List<Anime> listAllForAdmin() {
        return animeMapper.selectList(new LambdaQueryWrapper<Anime>()
                .orderByAsc(Anime::getSort)
                .orderByAsc(Anime::getId));
    }

    @Override
    @Transactional
    public Anime create(AnimeRequest request) {
        Anime anime = new Anime();
        apply(anime, request);
        if (request.getSort() == null) {
            anime.setSort(Math.toIntExact(animeMapper.selectCount(null)));
        }
        animeMapper.insert(anime);
        return anime;
    }

    @Override
    @Transactional
    public Optional<Anime> update(Long id, AnimeRequest request) {
        Anime anime = animeMapper.selectById(id);
        if (anime == null) {
            return Optional.empty();
        }
        apply(anime, request);
        animeMapper.updateById(anime);
        return Optional.of(anime);
    }

    @Override
    @Transactional
    public Optional<Anime> toggleVisible(Long id, boolean visible) {
        Anime anime = animeMapper.selectById(id);
        if (anime == null) {
            return Optional.empty();
        }
        anime.setVisible(visible);
        animeMapper.updateById(anime);
        return Optional.of(anime);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (animeMapper.selectById(id) == null) {
            return false;
        }
        // 番剧下架时连同剧集一起删，避免留下指向已消失番剧的孤儿记录
        episodeMapper.delete(new LambdaQueryWrapper<AnimeEpisode>().eq(AnimeEpisode::getAnimeId, id));
        animeMapper.deleteById(id);
        return true;
    }

    @Override
    public List<AnimeEpisode> listEpisodes(Long animeId) {
        return episodeMapper.selectList(new LambdaQueryWrapper<AnimeEpisode>()
                .eq(AnimeEpisode::getAnimeId, animeId)
                .orderByAsc(AnimeEpisode::getEpisodeNo));
    }

    @Override
    public AnimeEpisode getEpisode(Long episodeId) {
        return episodeMapper.selectById(episodeId);
    }

    @Override
    public Optional<AnimeEpisode> nextEpisode(Long animeId, int currentEpisodeNo) {
        return Optional.ofNullable(episodeMapper.selectOne(new LambdaQueryWrapper<AnimeEpisode>()
                .eq(AnimeEpisode::getAnimeId, animeId)
                .gt(AnimeEpisode::getEpisodeNo, currentEpisodeNo)
                .orderByAsc(AnimeEpisode::getEpisodeNo)
                .last("LIMIT 1")));
    }

    /** 把请求体字段套到实体上（null 字段不覆盖已有值） */
    private void apply(Anime anime, AnimeRequest request) {
        anime.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) anime.setDescription(request.getDescription().trim());
        if (request.getTag() != null) anime.setTag(request.getTag().trim());
        if (request.getRating() != null) anime.setRating(request.getRating().trim());
        if (request.getEpisode() != null) anime.setEpisode(request.getEpisode());
        if (request.getCover() != null) anime.setCover(request.getCover().trim());
        if (request.getSort() != null) anime.setSort(request.getSort());
        if (request.getVisible() != null) anime.setVisible(request.getVisible());
    }
}
