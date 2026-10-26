package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.AnimeEpisode;
import com.fenglin.springboottest.mapper.AnimeEpisodeMapper;
import com.fenglin.springboottest.service.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 视频流式播放实现（手动写 Servlet 流）。
 * <ul>
 *   <li><b>零拷贝</b>：FileChannel.transferTo 直接由操作系统把文件区间
 *       送到 socket，全程不经过 JVM 堆，内存占用与文件大小无关；</li>
 *   <li><b>单次分块</b>：每个响应最多吐 1MB（MAX_CHUNK）。播放器拿到
 *       Content-Range 里完整的资源长度后会自己排队发后续 Range 请求，
 *       客户端断开时服务端能立刻感知，不会白传几百 MB；</li>
 *   <li><b>Range 解析宽容</b>：接受 bytes=start-end / bytes=start- /
 *       bytes=-n 三种形式；非法值降级为整文件 200 而不是报错 ——
 *       播放器的首要目标是能播；</li>
 *   <li><b>路径安全</b>：videoPath 归一化后必须仍在视频根目录内，
 *       否则拒绝（防 ../ 路径穿越）。</li>
 * </ul>
 */
@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoServiceImpl.class);

    /** 单次响应最多吐 1MB，长视频由播放器分多次 Range 请求取完 */
    private static final long MAX_CHUNK = 1024 * 1024;

    /** 视频根目录，默认取项目下的 videos/，可用环境变量 VIDEO_DIR 覆盖 */
    private final Path videoRoot;
    private final AnimeEpisodeMapper episodeMapper;

    public VideoServiceImpl(@Value("${app.video.dir:videos}") String videoDir,
                            AnimeEpisodeMapper episodeMapper) {
        this.videoRoot = Paths.get(videoDir).toAbsolutePath().normalize();
        this.episodeMapper = episodeMapper;
        try {
            Files.createDirectories(videoRoot);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建视频目录: " + videoRoot, e);
        }
        log.info("视频根目录 -> {}", videoRoot);
    }

    @Override
    public void stream(Long episodeId, String rangeHeader, HttpServletResponse response) throws IOException {
        AnimeEpisode episode = episodeMapper.selectById(episodeId);
        if (episode == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "剧集不存在");
            return;
        }

        Path file = resolveSafe(episode.getVideoPath());
        long contentLength = Files.size(file);

        // 解析 Range；解析失败或没带就退化为整文件
        long start = 0;
        long end = contentLength - 1;
        boolean partial = false;

        if (rangeHeader != null && rangeHeader.matches("bytes=\\d*-\\d*")) {
            String[] parts = rangeHeader.substring("bytes=".length()).split("-", -1);
            try {
                if (!parts[0].isBlank()) {
                    // bytes=start-end / bytes=start-
                    start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        end = Math.min(Long.parseLong(parts[1]), contentLength - 1);
                    }
                    partial = true;
                } else if (parts.length > 1 && !parts[1].isBlank()) {
                    // bytes=-n：最后 n 个字节
                    start = Math.max(contentLength - Long.parseLong(parts[1]), 0);
                    partial = true;
                }
            } catch (NumberFormatException ignored) {
                // 非法数字 -> 退化整文件
                start = 0;
                end = contentLength - 1;
                partial = false;
            }
        }

        // 越界修正：起点越界按整文件处理（播放器常拿旧长度来请求）
        if (start >= contentLength) {
            start = 0;
            end = contentLength - 1;
            partial = false;
        }
        if (end < start) {
            end = start;
        }

        // 分块只在 206 部分响应时生效；200 整文件响应必须给完整长度，
        // 否则播放器以为文件被截断。transferTo 是流式拷贝，不占堆内存。
        long regionLength = partial ? Math.min(end - start + 1, MAX_CHUNK) : contentLength;
        long regionEnd = start + regionLength - 1;

        response.setStatus(partial ? HttpServletResponse.SC_PARTIAL_CONTENT : HttpServletResponse.SC_OK);
        response.setContentType(mediaTypeOf(file).toString());
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getFileName() + "\"");
        if (partial) {
            response.setHeader("Content-Range", "bytes " + start + "-" + regionEnd + "/" + contentLength);
        }
        response.setContentLengthLong(regionLength);

        try (FileChannel channel = FileChannel.open(file, java.nio.file.StandardOpenOption.READ);
             WritableByteChannel out = Channels.newChannel(response.getOutputStream())) {
            channel.transferTo(start, regionLength, out);
        } catch (IOException e) {
            // 播放器拖动进度条会掐断当前请求，属正常现象，不必当错误告警
            log.debug("视频流传输中断 -> episodeId={}, range={}", episodeId, rangeHeader);
            throw e;
        }
    }

    /** 把相对文件名解析为视频根目录内的绝对路径；越界一律拒绝 */
    private Path resolveSafe(String videoPath) {
        if (videoPath == null || videoPath.isBlank()) {
            throw new BizException("该剧集未配置视频文件");
        }
        Path resolved = videoRoot.resolve(videoPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(videoRoot)) {
            log.warn("拒绝越界视频路径 -> {}", videoPath);
            throw new BizException("视频文件路径不合法");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new BizException("视频文件不存在，请管理员检查 " + videoPath);
        }
        return resolved;
    }

    /** 按扩展名给出 Content-Type，未识别时退回 application/octet-stream */
    private MediaType mediaTypeOf(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "mp4", "m4v" -> MediaType.parseMediaType("video/mp4");
            case "webm" -> MediaType.parseMediaType("video/webm");
            case "ogv" -> MediaType.parseMediaType("video/ogg");
            case "mkv" -> MediaType.parseMediaType("video/x-matroska");
            case "mov" -> MediaType.parseMediaType("video/quicktime");
            default -> MediaTypeFactory.getMediaType(name)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
        };
    }
}
