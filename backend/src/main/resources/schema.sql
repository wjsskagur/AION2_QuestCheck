CREATE DATABASE IF NOT EXISTS aion2_quest
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aion2_quest;

CREATE TABLE IF NOT EXISTS account (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

-- reset_day : 초기화 요일 (1=월 2=화 3=수 4=목 5=금 6=토 7=일), DAILY는 무시
-- reset_hour: 초기화 시각 (0~23, 기본 5 = 오전 5시), DAILY/WEEKLY 모두 적용
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

-- date_key: DATETIME (시각 포함) — 초기화 시점의 "기준 날짜+시각" 저장
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

CREATE TABLE IF NOT EXISTS system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO system_config (config_key, config_value) VALUES
    ('verify.article_url',         'https://aion2.plaync.com/ko-kr/board/free/view?articleId=694b271e7883017e5d359427'),
    ('verify.code_expire_minutes', '10'),
    ('verify.code_prefix',         'AION'),
    ('crawler.notice_url',         'https://aion2.plaync.com/board/notice');

INSERT IGNORE INTO account (username, password, role) VALUES
    ('admin', '$2a$12$nGWvz1rULVCerG5M6p7b4eSRGJ0v53M3LL7TjEE1X2VnlbGl3dRTW', 'ADMIN');

-- 샘플 퀘스트 (reset_day=3 수요일, reset_hour=5 오전5시)
INSERT IGNORE INTO quest_template (name, type, min_level, reset_day, reset_hour, created_by) VALUES
    ('일일 임무 완료',   'DAILY',  1,  1, 5, 1),
    ('일일 던전 입장',   'DAILY',  10, 1, 5, 1),
    ('주간 보스 처치',   'WEEKLY', 20, 3, 5, 1),
    ('주간 레이드 참여', 'WEEKLY', 30, 3, 5, 1);
