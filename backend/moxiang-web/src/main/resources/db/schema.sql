-- ============================================================
-- 墨香论坛 (Moxiang Forum) Database Schema
-- MySQL 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS moxiang_forum
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE moxiang_forum;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)     NOT NULL                COMMENT '用户名',
    password    VARCHAR(100)    NOT NULL                COMMENT '密码(BCrypt)',
    email       VARCHAR(100)    NOT NULL                COMMENT '邮箱',
    avatar      VARCHAR(500)    DEFAULT NULL            COMMENT '头像URL',
    bio         VARCHAR(500)    DEFAULT NULL            COMMENT '个人简介',
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
    status      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '状态: 0=正常, 1=禁用',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0=正常, 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- Forums (Sections / 版块)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_forum (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '版块ID',
    name        VARCHAR(50)     NOT NULL                COMMENT '版块名称',
    description VARCHAR(500)    DEFAULT NULL            COMMENT '版块描述',
    icon        VARCHAR(500)    DEFAULT NULL            COMMENT '版块图标URL',
    sort_order  INT             NOT NULL DEFAULT 0      COMMENT '排序(升序)',
    post_count  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '帖子数量',
    status      TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '状态: 0=正常, 1=禁用',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版块表';

-- ------------------------------------------------------------
-- Posts (帖子)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_post (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    forum_id      BIGINT UNSIGNED NOT NULL               COMMENT '所属版块ID',
    user_id       BIGINT UNSIGNED NOT NULL               COMMENT '作者ID',
    title         VARCHAR(100)    NOT NULL               COMMENT '帖子标题',
    content       MEDIUMTEXT      NOT NULL               COMMENT '帖子内容',
    view_count    BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '浏览数',
    like_count    BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '点赞数',
    comment_count BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '评论数',
    is_top        TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '是否置顶: 0=否, 1=是',
    is_featured   TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '是否精华: 0=否, 1=是',
    status        TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '状态: 0=正常, 1=锁定',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_forum_id (forum_id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at),
    KEY idx_view_count (view_count),
    FULLTEXT KEY ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- ------------------------------------------------------------
-- Comments (评论)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_comment (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    post_id     BIGINT UNSIGNED NOT NULL               COMMENT '帖子ID',
    user_id     BIGINT UNSIGNED NOT NULL               COMMENT '评论者ID',
    parent_id   BIGINT UNSIGNED DEFAULT NULL           COMMENT '父评论ID(回复时使用)',
    content     TEXT            NOT NULL               COMMENT '评论内容',
    like_count  BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '点赞数',
    status      TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '状态: 0=正常, 1=隐藏',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post_id (post_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ------------------------------------------------------------
-- Novels (小说)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_novel (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '小说ID',
    user_id       BIGINT UNSIGNED NOT NULL               COMMENT '作者ID',
    title         VARCHAR(100)    NOT NULL               COMMENT '小说标题',
    description   TEXT            DEFAULT NULL           COMMENT '小说简介',
    cover         VARCHAR(500)    DEFAULT NULL           COMMENT '封面图片URL',
    category      VARCHAR(50)     NOT NULL               COMMENT '分类(玄幻/都市/武侠/言情等)',
    word_count    BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '总字数',
    chapter_count INT UNSIGNED    NOT NULL DEFAULT 0     COMMENT '章节数',
    view_count    BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '总浏览数',
    collect_count BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '收藏数',
    status        TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '状态: 0=连载中, 1=已完结, 2=暂停',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_category (category),
    KEY idx_created_at (created_at),
    FULLTEXT KEY ft_title_desc (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说表';

-- ------------------------------------------------------------
-- Novel Chapters (章节)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_novel_chapter (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    novel_id       BIGINT UNSIGNED NOT NULL               COMMENT '所属小说ID',
    chapter_number INT UNSIGNED    NOT NULL               COMMENT '章节序号',
    title          VARCHAR(200)    NOT NULL               COMMENT '章节标题',
    content        LONGTEXT        NOT NULL               COMMENT '章节内容',
    word_count     BIGINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '本章字数',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted     TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_novel_id (novel_id),
    UNIQUE KEY uk_novel_chapter (novel_id, chapter_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说章节表';

-- ------------------------------------------------------------
-- Tags (标签)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_tag (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    name       VARCHAR(30)     NOT NULL                COMMENT '标签名',
    color      VARCHAR(20)     DEFAULT '#409EFF'       COMMENT '标签颜色(HEX)',
    use_count  BIGINT UNSIGNED NOT NULL DEFAULT 0      COMMENT '使用次数',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ------------------------------------------------------------
-- Post-Tag Many-to-Many
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_post_tag (
    post_id BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    tag_id  BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    PRIMARY KEY (post_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签关联表';

-- ------------------------------------------------------------
-- User Like Post (用户点赞帖子)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_like_post (
    user_id    BIGINT UNSIGNED NOT NULL,
    post_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞帖子记录';

-- ------------------------------------------------------------
-- User Collect Novel (用户收藏小说)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user_collect_novel (
    user_id    BIGINT UNSIGNED NOT NULL,
    novel_id   BIGINT UNSIGNED NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, novel_id),
    KEY idx_novel_id (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏小说记录';

-- ------------------------------------------------------------
-- Seed Data
-- ------------------------------------------------------------

-- Default admin account (password: Admin@123 encoded with BCrypt)
-- Password: Admin@123  (BCrypt $2b$10, cost=10 — change immediately in production)
INSERT INTO t_user (username, password, email, role, status)
VALUES ('admin', '$2b$10$qtYS01OKNfvG01krOp75JetnTmfNRbL5fJRSwCTX8Ue0RVBzbNABq', 'admin@moxiang.com', 'ADMIN', 0)
ON DUPLICATE KEY UPDATE username = username;

-- Default forums
INSERT INTO t_forum (name, description, icon, sort_order) VALUES
('综合讨论', '什么都可以讨论的地方', '/icons/general.png', 1),
('玄幻奇幻', '玄幻、奇幻、仙侠类小说讨论', '/icons/fantasy.png', 2),
('都市言情', '都市、言情、现实类小说讨论', '/icons/romance.png', 3),
('武侠历史', '武侠、历史、军事类小说讨论', '/icons/wuxia.png', 4),
('科幻末世', '科幻、末世、机甲类小说讨论', '/icons/scifi.png', 5),
('新书推荐', '推荐好书，发现新作', '/icons/recommend.png', 6)
ON DUPLICATE KEY UPDATE name = name;

-- Default tags
INSERT INTO t_tag (name, color) VALUES
('热门', '#FF4500'),
('推荐', '#409EFF'),
('完结', '#67C23A'),
('连载', '#E6A23C'),
('精品', '#F56C6C'),
('新书', '#909399')
ON DUPLICATE KEY UPDATE name = name;
