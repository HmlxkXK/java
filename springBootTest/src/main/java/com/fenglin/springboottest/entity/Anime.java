package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 番剧。
 * <p>首页「本周热门番剧」板块的数据来源：管理员在后台维护，
 * 前端通过公开接口 GET /api/animes 读取 {@code visible=true} 的记录。
 * <p>字段说明：
 * <ul>
 *   <li>cover —— 封面渐变背景的 CSS 值（如 linear-gradient(...)），前端直接绑到 style，
 *       便于不引入图片上传也能自定义每张卡的视觉；</li>
 *   <li>sort —— 展示顺序，越小越靠前；</li>
 *   <li>visible —— 上下架开关，下架后不出现在首页。</li>
 * </ul>
 * <p>数据访问层已从 JPA 迁移到 MyBatis-Plus，create_time / update_time
 * 由 {@code MybatisPlusMetaObjectHandler} 在插入与更新时自动填充。
 */
@TableName("anime")
public class Anime {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 番剧名，必填 */
    private String title;

    /** 一句话简介 */
    private String description;

    /** 标签：热血 / 奇幻 / 治愈 …… */
    private String tag;

    /** 评分，存字符串以兼容 "9.2" 这种展示形式 */
    private String rating;

    /** 更新至第几话 */
    private Integer episode;

    /** 封面渐变（CSS background 值） */
    private String cover;

    /** 展示顺序，越小越靠前 */
    private Integer sort = 0;

    /** 是否在首页展示（上下架） */
    private Boolean visible = true;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入与更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
