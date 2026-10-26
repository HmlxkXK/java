package com.fenglin.springboottest.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.AdminAccount;
import com.fenglin.springboottest.entity.Anime;
import com.fenglin.springboottest.entity.AnimeEpisode;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AdminAccountMapper;
import com.fenglin.springboottest.mapper.AnimeEpisodeMapper;
import com.fenglin.springboottest.mapper.AnimeMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 启动时预置演示数据：
 * <ul>
 *   <li>两个前台社员账号（原有）；</li>
 *   <li>一个后台管理员账号 admin / admin123456（后台登录页无注册，只能靠这里或数据库建）；</li>
 *   <li>六条演示番剧，对应首页「本周热门番剧」，仅当表为空时写入。</li>
 * </ul>
 * 已存在的记录一律跳过，不会覆盖手动改过的数据。
 * <p>数据访问层已从 JPA 迁移到 MyBatis-Plus：存在性判断用 selectCount，
 * 插入用 insert（create_time / update_time 由 MetaObjectHandler 自动填充）。
 * <p>同等的建库建表与种子 SQL 另见 {@code sql/reigeki_anime.sql}，
 * 该脚本可直接导入 MySQL 手动初始化，效果与本预置逻辑一致。
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * 显式声明为最低优先级，保证排在 {@link DataSourceDiagnostics} 之后执行：
     * 数据库连不上时先由自检抛出可读的错误，而不是在这里写数据时失败。
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ApplicationRunner initDemoData(UserMapper userMapper,
                                          AdminAccountMapper adminAccountMapper,
                                          AnimeMapper animeMapper,
                                          AnimeEpisodeMapper episodeMapper,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            seedUser(userMapper, passwordEncoder, "燃尽小队长", "reigeki@demo.com", "reigeki123");
            seedUser(userMapper, passwordEncoder, "管理员", "admin@demo.com", "admin123456");
            seedAdmin(adminAccountMapper, passwordEncoder, "admin", "admin123456", "站点管理员");
            seedAnimes(animeMapper);
            seedEpisodes(animeMapper, episodeMapper);
        };
    }

    private void seedUser(UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          String nickname,
                          String email,
                          String rawPassword) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            return;
        }
        User user = new User();
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.insert(user);
        log.info("预置演示账号 -> {} / {}", email, rawPassword);
    }

    private void seedAdmin(AdminAccountMapper mapper,
                           PasswordEncoder passwordEncoder,
                           String username,
                           String rawPassword,
                           String nickname) {
        if (mapper.selectCount(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getUsername, username)) > 0) {
            return;
        }
        AdminAccount admin = new AdminAccount();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setNickname(nickname);
        mapper.insert(admin);
        log.info("预置后台管理员 -> {} / {}", username, rawPassword);
    }

    private void seedAnimes(AnimeMapper mapper) {
        if (mapper.selectCount(null) > 0) {
            return;
        }
        seedAnime(mapper, "碧海之章", "少年与深海共生，在潮汐尽头点亮最后的光。", "热血", "9.2", 12,
                "linear-gradient(135deg,#1ea7e8,#0b6fd0)", 0);
        seedAnime(mapper, "潮汐之歌", "深海少女用歌声唤醒沉睡的古城。", "奇幻", "8.9", 8,
                "linear-gradient(135deg,#36d1dc,#5b86e5)", 1);
        seedAnime(mapper, "雷鸣剑客", "以雷为刃的浪人，斩尽世间不公。", "动作", "9.0", 20,
                "linear-gradient(135deg,#5fd0ff,#2b86e0)", 2);
        seedAnime(mapper, "樱落之时", "转学少女与一段被遗忘的约定。", "治愈", "8.7", 5,
                "linear-gradient(135deg,#9fd8ff,#cfeeff)", 3);
        seedAnime(mapper, "星轨彼岸", "跨越光年的通讯，连接两个文明的少年。", "科幻", "9.1", 15,
                "linear-gradient(135deg,#7f53ff,#647dee)", 4);
        seedAnime(mapper, "荒野炊烟", "末日废土里，美食是最温柔的抵抗。", "日常", "8.8", 30,
                "linear-gradient(135deg,#11998e,#38ef7d)", 5);
        log.info("预置演示番剧 x6 -> 首页「本周热门番剧」数据源");
    }

    /**
     * 演示剧集：给《碧海之章》挂两集，指向 videos/ 目录下的样例视频。
     * 仅当该番剧还没有剧集时写入；视频文件由仓库提供（videos/demo-ep*.mp4），
     * 换成自己的片源时把文件放进去、改 anime_episode.video_path 即可。
     */
    private void seedEpisodes(AnimeMapper animeMapper, AnimeEpisodeMapper episodeMapper) {
        Anime demo = animeMapper.selectOne(new LambdaQueryWrapper<Anime>()
                .eq(Anime::getTitle, "碧海之章")
                .last("LIMIT 1"));
        if (demo == null) {
            return;
        }
        if (episodeMapper.selectCount(new LambdaQueryWrapper<AnimeEpisode>()
                .eq(AnimeEpisode::getAnimeId, demo.getId())) > 0) {
            return;
        }
        insertEpisode(episodeMapper, demo.getId(), 1, "第 1 话 · 潮声", "demo-ep1.mp4", 6);
        insertEpisode(episodeMapper, demo.getId(), 2, "第 2 话 · 灯塔", "demo-ep2.mp4", 6);
        log.info("预置演示剧集 x2 -> 《碧海之章》播放页数据源");
    }

    private void insertEpisode(AnimeEpisodeMapper mapper, Long animeId, int episodeNo,
                               String title, String videoPath, int durationSeconds) {
        AnimeEpisode episode = new AnimeEpisode();
        episode.setAnimeId(animeId);
        episode.setEpisodeNo(episodeNo);
        episode.setTitle(title);
        episode.setVideoPath(videoPath);
        episode.setDurationSeconds(durationSeconds);
        mapper.insert(episode);
    }

    private void seedAnime(AnimeMapper mapper,
                           String title,
                           String description,
                           String tag,
                           String rating,
                           Integer episode,
                           String cover,
                           Integer sort) {
        Anime anime = new Anime();
        anime.setTitle(title);
        anime.setDescription(description);
        anime.setTag(tag);
        anime.setRating(rating);
        anime.setEpisode(episode);
        anime.setCover(cover);
        anime.setSort(sort);
        anime.setVisible(true);
        mapper.insert(anime);
    }
}
