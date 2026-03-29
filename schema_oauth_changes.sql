-- ============================================================
-- [변경 1] 기존 account 테이블에 SNS 로그인 컬럼 추가
-- 이미 배포된 DB라면 이 ALTER TABLE을 EC2에서 직접 실행
-- ============================================================
ALTER TABLE account
  ADD COLUMN auth_provider ENUM('LOCAL','KAKAO','NAVER','GOOGLE','ADMIN')
                           NOT NULL DEFAULT 'LOCAL' AFTER role,
  ADD COLUMN provider_id   VARCHAR(255) NULL AFTER auth_provider,
  ADD COLUMN email         VARCHAR(255) NULL AFTER provider_id,
  ADD COLUMN nickname      VARCHAR(100) NULL AFTER email,
  ADD COLUMN profile_image VARCHAR(500) NULL AFTER nickname,
  MODIFY COLUMN username   VARCHAR(50)  NULL,   -- SNS 가입은 username 없어도 됨
  MODIFY COLUMN password   VARCHAR(255) NULL,   -- SNS 가입은 password 없어도 됨
  ADD UNIQUE KEY uq_provider (auth_provider, provider_id);

-- ============================================================
-- [신규 설치용] schema.sql의 account 테이블 전체 교체본
-- ============================================================
/*
CREATE TABLE IF NOT EXISTS account (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NULL UNIQUE,    -- LOCAL만 필수, SNS는 NULL 허용
    password       VARCHAR(255) NULL,           -- LOCAL만 존재, SNS는 NULL
    role           ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    auth_provider  ENUM('LOCAL','KAKAO','NAVER','GOOGLE','ADMIN')
                   NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255) NULL,           -- SNS 플랫폼 고유 사용자 ID
    email          VARCHAR(255) NULL,           -- SNS에서 받은 이메일
    nickname       VARCHAR(100) NULL,           -- SNS 닉네임 (표시용)
    profile_image  VARCHAR(500) NULL,           -- SNS 프로필 이미지 URL
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_provider (auth_provider, provider_id)  -- 플랫폼+ID 중복 방지
);
*/

-- ============================================================
-- auth_provider 값 설명
-- ============================================================
-- LOCAL  : 아이디/비밀번호로 직접 가입한 일반 회원
-- KAKAO  : 카카오 로그인으로 가입한 회원
-- NAVER  : 네이버 로그인으로 가입한 회원
-- GOOGLE : 구글 로그인으로 가입한 회원
-- ADMIN  : 관리자가 직접 생성한 계정 (기존 관리자 생성 방식)
