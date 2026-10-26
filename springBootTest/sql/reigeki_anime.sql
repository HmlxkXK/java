-- =====================================================================
-- reigeki-anime 数据库初始化脚本
-- 适用：MySQL 8.0+
-- 用法：mysql -uroot -p < reigeki_anime.sql
--       （或在 IDEA / Navicat 等 Database 面板中直接执行整个文件）
--
-- 说明：
--   1. 本脚本幂等：库、表均已存在时不会报错，种子数据只在对应表为空
--      （或账号不存在）时写入，可放心重复执行。
--   2. 原项目用 JPA ddl-auto=update 建表；数据层迁移到 MyBatis-Plus 后，
--      表结构统一由本脚本管理，应用启动不再改表。
--   3. 种子密码均为 BCrypt($2a$10) 密文，与下列明文一一对应，
--      供演示登录使用，请勿在生产环境保留。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS reigeki_anime
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE reigeki_anime;

-- ---------------------------------------------------------------------
-- 社员（用户）表。表名 t_user：user 是 MySQL 保留字。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    nickname    VARCHAR(50)  NOT NULL COMMENT '昵称',
    email       VARCHAR(100) NOT NULL COMMENT '登录账号（邮箱），唯一',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    create_time DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='社员账号';

-- ---------------------------------------------------------------------
-- 后台管理员表。管理员无自助注册入口，只能从这里或应用启动预置产生。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名，唯一',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname    VARCHAR(50)  NULL COMMENT '展示用昵称',
    create_time DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台管理员账号';

-- ---------------------------------------------------------------------
-- 番剧表。首页「本周热门番剧」板块的数据来源。
--   sort    展示顺序，越小越靠前
--   visible 上下架开关，下架后不出现在首页
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anime (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title       VARCHAR(50)  NOT NULL COMMENT '番剧名',
    description VARCHAR(100) NULL COMMENT '一句话简介',
    tag         VARCHAR(20)  NULL COMMENT '标签：热血/奇幻/治愈…',
    rating      VARCHAR(10)  NULL COMMENT '评分，如 9.2',
    episode     INT          NULL COMMENT '更新至第几话',
    cover       VARCHAR(200) NULL COMMENT '封面渐变（CSS background 值）',
    `sort`      INT          NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前',
    visible     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否在首页展示',
    create_time DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    update_time DATETIME(6)  NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='番剧';

-- ---------------------------------------------------------------------
-- 番剧集数（视频）表。播放页选集 + 视频流接口的数据来源。
--   video_path 只存相对 app.video.dir 的文件名，服务端会做路径穿越校验。
--   anime_id 上建索引：剧集列表、下一集、级联删除都按它查。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anime_episode (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    anime_id        BIGINT       NOT NULL COMMENT '所属番剧 id（anime.id）',
    episode_no      INT          NOT NULL COMMENT '集数，从 1 开始',
    title           VARCHAR(100) NULL COMMENT '集标题',
    video_path      VARCHAR(200) NOT NULL COMMENT '视频文件名（相对 app.video.dir）',
    duration_seconds INT         NULL COMMENT '时长（秒），仅展示用',
    create_time     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_anime_episode_anime_id (anime_id, episode_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='番剧集数';

-- =====================================================================
-- 种子数据
-- =====================================================================

-- 前台演示社员：
--   reigeki@demo.com / reigeki123
--   admin@demo.com   / admin123456
INSERT INTO t_user (nickname, email, password)
SELECT * FROM (
    SELECT '燃尽小队长' AS nickname, 'reigeki@demo.com' AS email,
           '$2a$10$0j4cy4j0iqJQ53JRai9DWeWKXMV9mbbuq/tJMnW/vBy091/8qz.OC' AS password
    UNION ALL
    SELECT '管理员', 'admin@demo.com',
           '$2a$10$u7N1g/r15/d8mCOuYcTuyOinh2g3GVFq0c34frMkbja33rYfbCRTi'
) seed
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE email IN ('reigeki@demo.com', 'admin@demo.com'));

-- 后台演示管理员：admin / admin123456
INSERT INTO admin_account (username, password, nickname)
SELECT 'admin',
       '$2a$10$S/5am6kw32MEDa6kOmpKpu0txFBDYViUZoKmkvlGTImBk2yUtn6HK',
       '站点管理员'
WHERE NOT EXISTS (SELECT 1 FROM admin_account WHERE username = 'admin');

-- 首页演示番剧 x6，仅当表为空时写入
INSERT INTO anime (title, description, tag, rating, episode, cover, `sort`, visible)
SELECT * FROM (
    SELECT '碧海之章' AS title, '少年与深海共生，在潮汐尽头点亮最后的光。' AS description,
           '热血' AS tag, '9.2' AS rating, 12 AS episode,
           'linear-gradient(135deg,#1ea7e8,#0b6fd0)' AS cover, 0 AS `sort`, 1 AS visible
    UNION ALL SELECT '潮汐之歌', '深海少女用歌声唤醒沉睡的古城。', '奇幻', '8.9', 8,
           'linear-gradient(135deg,#36d1dc,#5b86e5)', 1, 1
    UNION ALL SELECT '雷鸣剑客', '以雷为刃的浪人，斩尽世间不公。', '动作', '9.0', 20,
           'linear-gradient(135deg,#5fd0ff,#2b86e0)', 2, 1
    UNION ALL SELECT '樱落之时', '转学少女与一段被遗忘的约定。', '治愈', '8.7', 5,
           'linear-gradient(135deg,#9fd8ff,#cfeeff)', 3, 1
    UNION ALL SELECT '星轨彼岸', '跨越光年的通讯，连接两个文明的少年。', '科幻', '9.1', 15,
           'linear-gradient(135deg,#7f53ff,#647dee)', 4, 1
    UNION ALL SELECT '荒野炊烟', '末日废土里，美食是最温柔的抵抗。', '日常', '8.8', 30,
           'linear-gradient(135deg,#11998e,#38ef7d)', 5, 1
) seed
WHERE NOT EXISTS (SELECT 1 FROM anime);

-- 演示剧集 x2：挂在《碧海之章》下，指向 videos/ 目录里的样例视频。
-- 换成自己的片源：把文件放进 app.video.dir 指向的目录，改 video_path 即可。
INSERT INTO anime_episode (anime_id, episode_no, title, video_path, duration_seconds)
SELECT a.id, t.episode_no, t.title, t.video_path, t.duration_seconds
FROM (SELECT id FROM anime WHERE title = '碧海之章' LIMIT 1) a
CROSS JOIN (
    SELECT 1 AS episode_no, '第 1 话 · 潮声' AS title, 'demo-ep1.mp4' AS video_path, 6 AS duration_seconds
    UNION ALL
    SELECT 2, '第 2 话 · 灯塔', 'demo-ep2.mp4', 6
) t
WHERE NOT EXISTS (
    SELECT 1 FROM anime_episode e
    WHERE e.anime_id = a.id AND e.video_path IN ('demo-ep1.mp4', 'demo-ep2.mp4')
);
