package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台新增 / 编辑番剧的请求体。
 */
public class AnimeRequest {

    @NotBlank(message = "番剧名不能为空")
    @Size(max = 50, message = "番剧名最长 50 字")
    private String title;

    @Size(max = 100, message = "简介最长 100 字")
    private String description;

    @Size(max = 20, message = "标签最长 20 字")
    private String tag;

    /** 展示形式如 "9.2"，后台做格式校验 */
    @Size(max = 10, message = "评分格式不正确")
    private String rating;

    @Min(value = 0, message = "话数不能为负数")
    @Max(value = 100000, message = "话数超出合理范围")
    private Integer episode;

    @Size(max = 200, message = "封面样式过长")
    private String cover;

    @Min(value = 0, message = "排序值不能为负数")
    private Integer sort = 0;

    private Boolean visible = true;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public Integer getEpisode() { return episode; }
    public void setEpisode(Integer episode) { this.episode = episode; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
}
