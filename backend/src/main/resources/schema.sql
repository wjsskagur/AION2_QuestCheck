CREATE DATABASE IF NOT EXISTS aion2_quest
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aion2_quest;

-- ── account ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS account (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NULL UNIQUE,
    password       VARCHAR(255) NULL,
    role           ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    auth_provider  ENUM('LOCAL','KAKAO','NAVER','GOOGLE','ADMIN') NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255) NULL,
    email          VARCHAR(255) NULL,
    nickname       VARCHAR(100) NULL,
    profile_image  VARCHAR(500) NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_provider (auth_provider, provider_id)
);

-- ── character ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `character` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    BIGINT      NOT NULL,
    server        VARCHAR(50) NOT NULL,
    name          VARCHAR(50) NOT NULL,
    class_name    VARCHAR(30),
    level         INT         NOT NULL DEFAULT 1,
    combat_power  BIGINT      NOT NULL DEFAULT 0,
    grade         VARCHAR(20),
    grade_color   VARCHAR(10),
    wings         VARCHAR(30),
    honor         BIGINT DEFAULT 0,
    raw_data      JSON,
    verified      TINYINT(1)  NOT NULL DEFAULT 0,
    verified_at   DATETIME,
    fetched_at    DATETIME,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_char (account_id, server, name),
    INDEX idx_server_cp (server, combat_power),
    CONSTRAINT fk_char_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE
);

-- ── quest_template ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quest_template (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    type       ENUM('DAILY','WEEKLY','SPECIFIC') NOT NULL,
    min_level  INT,
    max_level  INT,
    days       VARCHAR(20),
    reset_day  INT NOT NULL DEFAULT 3,
    reset_hour INT NOT NULL DEFAULT 5,
    is_active  TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reset_day  CHECK (reset_day  BETWEEN 1 AND 7),
    CONSTRAINT chk_reset_hour CHECK (reset_hour BETWEEN 0 AND 23),
    CONSTRAINT fk_qt_account FOREIGN KEY (created_by) REFERENCES account(id)
);

-- ── quest_check ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quest_check (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id BIGINT NOT NULL,
    quest_id     BIGINT NOT NULL,
    date_key     DATETIME NOT NULL,
    is_done      TINYINT(1) NOT NULL DEFAULT 1,
    checked_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_check (character_id, quest_id, date_key),
    CONSTRAINT fk_qc_char  FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE,
    CONSTRAINT fk_qc_quest FOREIGN KEY (quest_id)     REFERENCES quest_template(id) ON DELETE CASCADE
);

-- ── verification_code ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS verification_code (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT      NOT NULL,
    character_id BIGINT      NOT NULL,
    code         VARCHAR(12) NOT NULL UNIQUE,
    status       ENUM('PENDING','VERIFIED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    expires_at   DATETIME    NOT NULL,
    verified_at  DATETIME,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vc_code (code),
    CONSTRAINT fk_vc_account FOREIGN KEY (account_id)   REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_vc_char    FOREIGN KEY (character_id) REFERENCES `character`(id) ON DELETE CASCADE
);

-- ── post (공지 게시판) ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS post (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    source_url VARCHAR(500),
    category   ENUM('NOTICE','UPDATE','EVENT') NOT NULL,
    created_by BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_account FOREIGN KEY (created_by) REFERENCES account(id)
);

-- ── system_config ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ────────────────────────────────────────────────────────────────────────────
-- 파티 모집 게시판
-- ────────────────────────────────────────────────────────────────────────────

-- ── party_category (1차 카테고리) ──────────────────────────────────────────
-- 예: 던전, 레이드, PvP, 기타
CREATE TABLE IF NOT EXISTS party_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200) NULL,
    icon        VARCHAR(10)  NULL,          -- 이모지 또는 짧은 아이콘 문자
    sort_order  INT NOT NULL DEFAULT 0,     -- 표시 순서
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pcat_account FOREIGN KEY (created_by) REFERENCES account(id)
);

-- ── party_subcategory (2차 카테고리) ──────────────────────────────────────
-- 예: 던전 > 일반던전, 던전 > 하드던전 / 레이드 > 주간레이드, 레이드 > 공격대
CREATE TABLE IF NOT EXISTS party_subcategory (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200) NULL,
    min_level   INT          NULL,          -- 최소 레벨 (선택)
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   TINYINT(1) NOT NULL DEFAULT 1,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_subcat (category_id, name),
    CONSTRAINT fk_psubcat_cat     FOREIGN KEY (category_id) REFERENCES party_category(id) ON DELETE CASCADE,
    CONSTRAINT fk_psubcat_account FOREIGN KEY (created_by)  REFERENCES account(id)
);

-- ── party_post (파티 모집 글) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS party_post (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT       NOT NULL,
    subcategory_id  BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    character_id    BIGINT       NULL,          -- 대표 캐릭터 (선택)
    title           VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    server          VARCHAR(50)  NOT NULL,      -- 서버 (카이나토스 등)
    min_level       INT          NULL,
    max_members     INT NOT NULL DEFAULT 4,     -- 모집 인원
    current_members INT NOT NULL DEFAULT 1,     -- 현재 인원 (작성자 포함)
    status          ENUM('OPEN','CLOSED','DELETED') NOT NULL DEFAULT 'OPEN',
    schedule_time   DATETIME     NULL,          -- 예정 시간 (선택)
    views           INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pp_cat     (category_id, subcategory_id),
    INDEX idx_pp_server  (server),
    INDEX idx_pp_status  (status),
    INDEX idx_pp_created (created_at),
    CONSTRAINT fk_pp_cat    FOREIGN KEY (category_id)    REFERENCES party_category(id),
    CONSTRAINT fk_pp_subcat FOREIGN KEY (subcategory_id) REFERENCES party_subcategory(id),
    CONSTRAINT fk_pp_acct   FOREIGN KEY (account_id)     REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT fk_pp_char   FOREIGN KEY (character_id)   REFERENCES `character`(id) ON DELETE SET NULL
);

-- ── party_comment (댓글) ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS party_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT   NOT NULL,
    account_id  BIGINT   NOT NULL,
    content     TEXT     NOT NULL,
    is_deleted  TINYINT(1) NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pc_post FOREIGN KEY (post_id)    REFERENCES party_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_pc_acct FOREIGN KEY (account_id) REFERENCES account(id)    ON DELETE CASCADE
);

-- ────────────────────────────────────────────────────────────────────────────
-- 초기 데이터
-- ────────────────────────────────────────────────────────────────────────────
INSERT IGNORE INTO system_config (config_key, config_value) VALUES
    ('verify.article_url',         'https://aion2.plaync.com/ko-kr/board/free/view?articleId=694b271e7883017e5d359427'),
    ('verify.code_expire_minutes', '10'),
    ('verify.code_prefix',         'AION'),
    ('crawler.notice_url',         'https://aion2.plaync.com/board/notice');

-- 관리자 계정 (비밀번호: admin1234 — 첫 로그인 후 반드시 변경)
INSERT IGNORE INTO account (username, password, role, auth_provider) VALUES
    ('admin', '$2a$12$nGWvz1rULVCerG5M6p7b4eSRGJ0v53M3LL7TjEE1X2VnlbGl3dRTW', 'ADMIN', 'ADMIN');

-- 샘플 파티 카테고리
INSERT IGNORE INTO party_category (name, description, icon, sort_order, created_by) VALUES
    ('던전',  '인스턴스 던전 파티 모집',    '⚔',  1, 1),
    ('레이드', '레이드 및 공격대 모집',      '🏰', 2, 1),
    ('PvP',   'PvP 및 아비스 파티 모집',    '🛡',  3, 1),
    ('기타',  '기타 컨텐츠 파티 모집',      '🎮', 4, 1);

-- 샘플 2차 카테고리 (아이온2 출시 후 실제 콘텐츠에 맞게 수정)
INSERT IGNORE INTO party_subcategory (category_id, name, description, min_level, sort_order, created_by) VALUES
    (1, '일반 던전',  '일반 난이도 던전',   10, 1, 1),
    (1, '하드 던전',  '하드 난이도 던전',   30, 2, 1),
    (1, '카오스 던전','카오스 난이도 던전', 50, 3, 1),
    (2, '주간 레이드','주간 레이드 공략',   40, 1, 1),
    (2, '공격대',    '대규모 공격대',       50, 2, 1),
    (3, '아비스',    '아비스 PvP',          20, 1, 1),
    (3, '전장',      '전장 파티',           20, 2, 1),
    (4, '레벨업',    '레벨업 파티',          1, 1, 1),
    (4, '거래',      '아이템 거래 파티',     1, 2, 1);

-- 샘플 퀘스트
INSERT IGNORE INTO quest_template (name, type, min_level, reset_day, reset_hour, created_by) VALUES
    ('일일 임무 완료',   'DAILY',  1,  1, 5, 1),
    ('일일 던전 입장',   'DAILY',  10, 1, 5, 1),
    ('주간 보스 처치',   'WEEKLY', 20, 3, 5, 1),
    ('주간 레이드 참여', 'WEEKLY', 30, 3, 5, 1);
