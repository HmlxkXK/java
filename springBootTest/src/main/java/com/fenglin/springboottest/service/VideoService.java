package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.AnimeEpisode;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 视频流式播放服务。
 * <p>视频文件体积大，绝不能一次性读进内存再整包吐出。播放器（video 标签）
 * 会先发一个不带 Range 的请求，之后每次拖动进度条、预读缓冲都会带
 * {@code Range: bytes=start-end} 继续请求；服务端对应返回
 * <b>200 整文件</b> 或 <b>206 Partial Content</b> + {@code Content-Range}。
 * <p>实现直接往 {@link HttpServletResponse} 写字节（FileChannel 零拷贝），
 * 而不是走 ResponseEntity<ResourceRegion>：Spring 6.1 起经框架处理过的
 * 响应头是只读的，ResourceRegionHttpMessageConverter 内部往里 add 头会抛
 * UnsupportedOperationException（实测踩坑），手动写流则完全可控。
 * <p>安全由实现保证：episodeId 必须真实存在，且解析出的视频文件必须落在
 * 视频根目录之内（防路径穿越）。
 */
public interface VideoService {

    /**
     * 按剧集 id 流式输出视频（直接写 response，不返回值）。
     *
     * @param episodeId   剧集 id
     * @param rangeHeader 浏览器带来的 Range 头，可为 null
     * @param response    Servlet 响应，由本方法写入状态码、头与字节流
     * @throws IOException 写出失败（客户端中断/磁盘错误）时向上抛
     */
    void stream(Long episodeId, String rangeHeader, HttpServletResponse response) throws IOException;
}
