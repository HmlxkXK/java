package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.service.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 视频流接口。
 * <p>GET /api/videos/{episodeId} —— 无 Range 返回 200 整文件，带 Range 返回
 * 206 区间。
 * <b>故意不走 Result 包装、直接返回二进制流</b>：video 标签期望的是媒体
 * 响应，JSON 包装会让播放器解析失败；鉴权语义上这是公开接口（演示项目
 * 不做会员墙），以后要加会员限制在这前面挂拦截器即可。
 * <p>浏览器 video 标签会自动带上 Range 头做分段拉取，Range 头原样透传给
 * {@code VideoService} 处理；写流过程中客户端中断（拖进度条/关页面）抛出的
 * IOException 由容器按客户端中断记录，不影响其它请求。
 */
@RestController
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/api/videos/{episodeId}")
    public void stream(
            @PathVariable Long episodeId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletResponse response) throws IOException {
        videoService.stream(episodeId, rangeHeader, response);
    }
}
