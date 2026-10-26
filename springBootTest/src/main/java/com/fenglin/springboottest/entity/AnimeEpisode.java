package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 番剧集数（视频）。
 * <p>一条番剧（{@link Anime}）对应多集，每集指向视频目录里的一个文件：
 * <ul>
 *   <li>animeId —— 所属番剧，外键语义上关联 anime.id，查询走
 *       {@code idx_anime_episode_anime_id} 索引；</li>
 *   <li>episodeNo —— 集数，播放页按它升序展示；</li>
 *   <li>videoPath —— 相对视频根目录（app.video.dir）的文件名。
 *       <b>只存文件名不存绝对路径</b>，且服务端解析时校验真实路径必须
 *       落在根目录内，防止 ../ 之类路径穿越；</li>
 *   <li>durationSeconds —— 时长（秒），可空，仅用于列表展示。</li>
 * </ul>
 * 视频内容本身不进数据库，通过 GET /api/videos/{episodeId} 由
 * {@code VideoService} 以 HTTP Range 流式输出。
 */
@TableName("anime_episode")
public class AnimeEpisode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属番剧 id（anime.id） */
    private Long animeId;

    /** 集数，从 1 开始 */
    private Integer episodeNo;

    /** 集标题，如「第 1 话 · 潮声」；空则前端展示默认文案 */
    private String title;

    /** 视频文件名（相对 app.video.dir），如 demo-ep1.mp4 */
    private String videoPath;

    /** 时长（秒），可空 */
    private Integer durationSeconds;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAnimeId() { return animeId; }
    public void setAnimeId(Long animeId) { this.animeId = animeId; }

    public Integer getEpisodeNo() { return episodeNo; }
    public void setEpisodeNo(Integer episodeNo) { this.episodeNo = episodeNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
