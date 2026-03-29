# ⚔ AION2 Quest Checker

아이온2 일일·주간 퀘스트 관리, 파티 모집, 서버 랭킹을 제공하는 웹 서비스입니다.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![MariaDB](https://img.shields.io/badge/MariaDB-10.11-blue?style=flat-square&logo=mariadb)
![AWS](https://img.shields.io/badge/AWS-EC2+RDS+S3-orange?style=flat-square&logo=amazonaws)

🔗 **레포지토리**: [github.com/wjsskagur/AION2_QuestCheck](https://github.com/wjsskagur/AION2_QuestCheck)

---

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [프로젝트 구조](#-프로젝트-구조)
- [로컬 개발 환경 세팅](#-로컬-개발-환경-세팅-windows)
- [환경변수 설정](#-환경변수-설정)
- [DB 스키마](#-db-스키마)
- [API 명세](#-api-명세)
- [AWS 초기 세팅부터 배포까지](#-aws-초기-세팅부터-배포까지)
- [퀘스트 초기화 로직](#-퀘스트-초기화-로직)
- [월 운영 비용](#-월-운영-비용)
- [주의사항](#-주의사항)

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| **퀘스트 체커** | 캐릭터별 일일·주간 퀘스트 완료 현황 관리 및 진행률 표시 |
| **퀘스트 자동 초기화** | 퀘스트별 초기화 요일·시각 개별 설정 (기본: 수요일 오전 5시) |
| **파티 모집 게시판** | 1차·2차 카테고리 구조. 관리자가 카테고리 추가·수정·삭제 가능 |
| **캐릭터 인증** | 아이온2 공식 게시글 댓글로 캐릭터 소유권 검증 |
| **서버 랭킹** | 등록 캐릭터 기준 서버별 전투력·레벨 랭킹 (비로그인 공개) |
| **공지 자동 요약** | 공식 공지 3시간마다 수집, Claude API로 3줄 자동 요약 |
| **일반 회원가입** | 아이디·비밀번호로 직접 가입 |
| **SNS 로그인** | 카카오·네이버·구글 OAuth2 로그인 |
| **회원 유형 구분** | LOCAL / KAKAO / NAVER / GOOGLE / ADMIN 구분 표시 |
| **관리자 패널** | 계정·퀘스트·파티 카테고리·인증URL 관리 |

---

## 🛠 기술 스택

### 백엔드
- **Java 17** + **Spring Boot 3.3** + **Gradle**
- **Spring Security** — JWT Stateless 인증 + OAuth2 Client
- **Spring Data JPA** + **MariaDB**
- **Jsoup** — 공홈 크롤링 (인증 검증, 공지 수집)
- **Claude API (Haiku)** — 공지 자동 요약

### 프론트엔드
- **React 18** + **Vite**
- **React Router v6** — 공개/보호/관리자 라우트 분리
- **Google AdSense** — 광고 수익화 (공개 페이지)

### 인프라 (AWS ap-northeast-2 서울)
- **EC2 t4g.small** — Graviton2 ARM, Spring Boot 서버
- **RDS db.t4g.micro** — MariaDB 10.11, Single-AZ
- **S3 + CloudFront** — React 정적 파일 서빙
- **SSM Parameter Store** — 비밀값 관리 (DB비번·JWT·API키)
- **CodeDeploy** — EC2 In-Place 자동 배포
- **GitHub Actions** — CI/CD 파이프라인

---

## 🏗 아키텍처

```
브라우저
  │
  ├── /* ──────────→ CloudFront ──→ S3 (React 빌드)
  └── /api/* ──────→ CloudFront ──→ EC2 (Spring Boot :8080) ──→ RDS (MariaDB)
                                         │
                                         ├── SSM (비밀값)
                                         ├── Claude API (공지 요약)
                                         └── 아이온2 공홈 (크롤링)

OAuth2 흐름:
  브라우저 → /oauth2/authorization/{provider}
           → 카카오/네이버/구글 인증
           → /login/oauth2/code/{provider}
           → JWT 발급
           → 프론트 /oauth2/callback?token=JWT
```

---

## 📁 프로젝트 구조

```
AION2_QuestCheck/
├── .github/workflows/deploy.yml    # CI/CD
├── .gitattributes                  # LF 강제 (Windows CRLF 방지)
├── .gitignore
├── appspec.yml                     # CodeDeploy 설정
├── README.md
│
├── backend/                        # Spring Boot
│   ├── build.gradle
│   ├── settings.gradle
│   ├── scripts/
│   │   ├── stop.sh
│   │   ├── start.sh               # SSM 비밀값 주입 + JVM 기동
│   │   └── health_check.sh        # 배포 검증 (실패 시 자동 롤백)
│   └── src/main/
│       ├── java/com/aion2/
│       │   ├── Aion2QuestApplication.java
│       │   ├── security/          SecurityConfig · JwtProvider · JwtAuthFilter
│       │   │                      CustomOAuth2UserService · OAuth2SuccessHandler
│       │   ├── entity/            Account · Character · QuestTemplate · QuestCheck
│       │   │                      VerificationCode · Post · SystemConfig
│       │   │                      PartyCategory · PartySubcategory · PartyPost · PartyComment
│       │   ├── repository/        JpaRepository 인터페이스들
│       │   ├── dto/               요청/응답 DTO
│       │   ├── service/           Auth · Admin · Character · QuestCheck · Verification
│       │   │                      Crawler · ClaudeApi · PartyBoard
│       │   └── controller/        Auth · Character · QuestCheck · Verification
│       │                          Admin · Public · PartyBoard · PartyAdmin
│       └── resources/
│           ├── application.yml
│           └── schema.sql
│
└── frontend/                       # React 18 + Vite
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx                 # 라우팅 + Auth Context
        ├── index.css               # 다크 테마 전역 스타일
        ├── api/index.js            # fetch 래퍼 (JWT 자동 주입)
        ├── hooks/useAuth.js
        ├── components/
        │   └── Components.jsx      # AdBanner · VerificationModal · Pagination · ProviderBadge
        └── pages/
            ├── LandingPage.jsx     # / · 로그인 · OAuth2 콜백 포함
            ├── Dashboard.jsx       # 대시보드 · 캐릭터 관리 · 캐릭터 상세 포함
            ├── RankingPage.jsx     # 랭킹 · 공지 · 가이드 · 개인정보처리방침 포함
            ├── PartyBoardPage.jsx  # 파티 모집 목록
            ├── PartyDetailPage.jsx # 파티 모집 상세 + 댓글
            ├── PartyWritePage.jsx  # 파티 모집 글 작성/수정
            └── AdminPage.jsx       # 관리자 패널 (계정·퀘스트·파티카테고리·인증설정)
```

---

## 💻 로컬 개발 환경 세팅 (Windows)

### 사전 준비

| 도구 | 버전 | 설치 |
|---|---|---|
| JDK (Corretto) | 17 LTS | [다운로드](https://aws.amazon.com/corretto/) |
| Node.js | 20 LTS | [다운로드](https://nodejs.org/) |
| Docker Desktop | 최신 | [다운로드](https://www.docker.com/products/docker-desktop/) |
| AWS CLI v2 | 최신 | [MSI 다운로드](https://awscli.amazonaws.com/AWSCLIV2.msi) |
| Git | 최신 | [다운로드](https://git-scm.com/) |

> PowerShell 7 이상 사용 권장. SSH, SCP 명령어가 Mac/Linux와 동일하게 동작합니다.

### 1. 레포 클론

```powershell
git clone https://github.com/wjsskagur/AION2_QuestCheck.git
cd AION2_QuestCheck
```

### 2. 로컬 MariaDB 실행 (Docker)

```powershell
docker run -d --name aion2-db `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=aion2_quest `
  -e MYSQL_USER=aion2_user `
  -e MYSQL_PASSWORD=aion2_pass `
  -p 3306:3306 `
  mariadb:10.11
```

### 3. DB 스키마 초기화

```powershell
# Docker를 통해 schema.sql 실행
Get-Content backend\src\main\resources\schema.sql | docker exec -i aion2-db mariadb -u aion2_user -paion2_pass aion2_quest
```

### 4. 백엔드 실행

```powershell
cd backend
.\gradlew bootRun
# http://localhost:8080/api/actuator/health → {"status":"UP"} 확인
```

### 5. 프론트엔드 실행

```powershell
cd ..\frontend
npm install
npm run dev
# http://localhost:5173 에서 확인
```

---

## 🔐 환경변수 설정

### 로컬 개발

`application.yml`의 기본값(`${변수명:기본값}` 형식)으로 자동 사용됩니다. 별도 설정 불필요합니다.

### 운영 환경 (AWS SSM Parameter Store)

`start.sh`가 배포 시 SSM에서 값을 가져와 환경변수로 주입합니다.

```powershell
# 로컬 PowerShell에서 AWS CLI로 등록
aws ssm put-parameter --name "/aion2/db/host"                --value "RDS엔드포인트"        --type SecureString
aws ssm put-parameter --name "/aion2/db/username"            --value "admin"                --type SecureString
aws ssm put-parameter --name "/aion2/db/password"            --value "DB비밀번호"            --type SecureString
aws ssm put-parameter --name "/aion2/jwt/secret"             --value "32자이상랜덤문자열"    --type SecureString
aws ssm put-parameter --name "/aion2/claude/api-key"         --value "sk-ant-..."           --type SecureString
aws ssm put-parameter --name "/aion2/oauth/kakao/client-id"  --value "카카오ClientID"       --type SecureString
aws ssm put-parameter --name "/aion2/oauth/kakao/client-secret" --value "카카오Secret"      --type SecureString
aws ssm put-parameter --name "/aion2/oauth/naver/client-id"  --value "네이버ClientID"       --type SecureString
aws ssm put-parameter --name "/aion2/oauth/naver/client-secret" --value "네이버Secret"      --type SecureString
aws ssm put-parameter --name "/aion2/oauth/google/client-id" --value "구글ClientID"         --type SecureString
aws ssm put-parameter --name "/aion2/oauth/google/client-secret" --value "구글Secret"       --type SecureString
```

> ⚠ `.env` 파일이나 코드에 비밀값을 절대 작성하지 마세요. `.gitignore`에 포함되어 있습니다.

---

## 🗄 DB 스키마

| 테이블 | 역할 | 주요 컬럼 |
|---|---|---|
| `account` | 사용자 계정 | username · password · role · **auth_provider** · provider_id |
| `character` | 캐릭터 정보 | server · name · level · combat_power · verified |
| `quest_template` | 퀘스트 템플릿 | type · **reset_day** · **reset_hour** |
| `quest_check` | 완료 기록 | character_id · quest_id · **date_key(DATETIME)** |
| `verification_code` | 인증 코드 | code · status · expires_at |
| `post` | 공지 게시판 | title · content(요약) · source_url |
| `system_config` | 동적 설정 | config_key · config_value |
| `party_category` | 파티 1차 카테고리 | name · icon · **sort_order** |
| `party_subcategory` | 파티 2차 카테고리 | category_id · name · min_level · sort_order |
| `party_post` | 파티 모집 글 | category_id · subcategory_id · title · server · max_members · status |
| `party_comment` | 파티 댓글 | post_id · account_id · content |

> DDL 전체: [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql)

---

## 📡 API 명세

### 공개 API (인증 불필요)

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/auth/login` | 로그인 → JWT 발급 |
| `POST` | `/api/auth/signup` | 일반 회원가입 |
| `GET` | `/api/auth/me` | 내 정보 조회 (OAuth2 콜백용) |
| `GET` | `/api/public/ranking?server=카이나토스` | 서버별 캐릭터 랭킹 |
| `GET` | `/api/public/posts` | 공지 게시판 |
| `GET` | `/api/public/stats` | 서비스 통계 |
| `GET` | `/api/party/categories` | 파티 카테고리 목록 (1차+2차) |
| `GET` | `/api/party/posts` | 파티 모집 글 목록 (필터·페이징) |
| `GET` | `/api/party/posts/{id}` | 파티 모집 글 상세 |

### 로그인 필요 API

| Method | URL | 설명 |
|---|---|---|
| `GET/POST` | `/api/characters` | 캐릭터 조회/추가 |
| `PUT` | `/api/characters/{id}/refresh` | 캐릭터 정보 갱신 |
| `DELETE` | `/api/characters/{id}` | 캐릭터 삭제 |
| `GET` | `/api/quest-checks/dashboard` | 대시보드 |
| `POST` | `/api/quest-checks/{cid}/toggle/{qid}` | 퀘스트 체크 토글 |
| `POST` | `/api/characters/{id}/verification/issue` | 인증 코드 발급 |
| `POST` | `/api/characters/{id}/verification/verify` | 인증 확인 |
| `POST` | `/api/party/posts` | 파티 모집 글 작성 |
| `PUT` | `/api/party/posts/{id}` | 파티 모집 글 수정 |
| `PATCH` | `/api/party/posts/{id}/close` | 모집 마감 |
| `DELETE` | `/api/party/posts/{id}` | 파티 모집 글 삭제 |
| `POST` | `/api/party/posts/{id}/comments` | 댓글 작성 |
| `DELETE` | `/api/party/comments/{id}` | 댓글 삭제 |

### 관리자 전용 API

| Method | URL | 설명 |
|---|---|---|
| `GET/POST` | `/api/admin/accounts` | 계정 목록/생성 |
| `GET/POST` | `/api/admin/quests` | 퀘스트 템플릿 목록/생성 |
| `GET/PUT` | `/api/admin/config/verify` | 인증 설정 |
| `POST` | `/api/admin/party/categories` | 파티 1차 카테고리 생성 |
| `PUT/DELETE` | `/api/admin/party/categories/{id}` | 수정/삭제 |
| `POST` | `/api/admin/party/subcategories` | 파티 2차 카테고리 생성 |
| `PUT/DELETE` | `/api/admin/party/subcategories/{id}` | 수정/삭제 |

---

## 🚀 AWS 초기 세팅부터 배포까지

### 전체 흐름

```
STEP 1  AWS 계정 & IAM 세팅          ~20분  (최초 1회)
STEP 2  EC2 인스턴스 생성             ~15분  (최초 1회)
STEP 3  RDS (MariaDB) 생성           ~10분  (최초 1회)
STEP 4  S3 버킷 2개 생성              ~10분  (최초 1회)
STEP 5  CloudFront 배포 생성          ~20분  (최초 1회)
STEP 6  Route 53 도메인 연결          ~10분  (최초 1회)
STEP 7  CodeDeploy 앱 & 배포 그룹     ~10분  (최초 1회)
STEP 8  SSM Parameter Store 등록     ~10분  (최초 1회)
STEP 9  EC2 SSH: 패키지 설치          ~15분  (최초 1회)
STEP 10 EC2 SSH: Nginx + SSL 설정    ~15분  (최초 1회)
STEP 11 EC2 SSH: DB 스키마 초기화     ~5분   (최초 1회)
STEP 12 GitHub Secrets 등록          ~5분   (최초 1회)
STEP 13 SNS 앱 등록 (카카오/네이버/구글) ~45분 (최초 1회)
STEP 14 첫 배포 실행 & 검증           ~10분
────────────────────────────────────────────
총 소요: 약 3~3.5시간 (이후 배포는 git push 1번)
```

---

### STEP 1. AWS 계정 & IAM 세팅

**1-1. IAM User 생성 (GitHub Actions 배포 전용)**

```
AWS 콘솔 → IAM → 사용자 → [사용자 생성]
이름: aion2-github-actions
권한 정책 (직접 연결):
  - AWSCodeDeployFullAccess
  - AmazonS3FullAccess
  - CloudFrontFullAccess
생성 후 → 보안 자격 증명 → 액세스 키 만들기 → [서드파티 서비스]
→ Access Key ID, Secret Access Key 메모 (GitHub Secrets에 등록)
```

**1-2. EC2용 IAM Role 생성**

```
IAM → 역할 → [역할 생성]
신뢰할 수 있는 엔터티: AWS 서비스 → EC2
권한 정책:
  - AmazonSSMReadOnlyAccess
  - AmazonS3ReadOnlyAccess
  - AWSCodeDeployRole
역할 이름: aion2-ec2-role
```

---

### STEP 2. EC2 인스턴스 생성

```
EC2 → [인스턴스 시작]
이름:             aion2-quest-server
AMI:              Amazon Linux 2023 (64비트 ARM) ← 반드시 ARM 선택
인스턴스 유형:    t4g.small
키 페어:          [새 키 페어 생성] → aion2-keypair.pem 다운로드 후 보관
보안 그룹 (새로 생성):
  - SSH(22):   내 IP
  - HTTP(80):  0.0.0.0/0
  - HTTPS(443):0.0.0.0/0
스토리지:         gp3 20GB
고급 세부 정보 → IAM 인스턴스 프로파일: aion2-ec2-role
```

생성 후 **Elastic IP** 할당 및 EC2에 연결합니다.

```
EC2 → 탄력적 IP → [탄력적 IP 주소 할당] → [할당]
생성된 IP → [탄력적 IP 주소 연결] → aion2-quest-server 선택
```

---

### STEP 3. RDS 생성

```
RDS → [데이터베이스 생성]
엔진:           MariaDB 10.11
인스턴스 클래스: db.t4g.micro
스토리지:        gp3 20GB (자동 확장 OFF)
DB 식별자:       aion2-quest-db
마스터 사용자:   admin
마스터 암호:     강력한 비밀번호 (SSM에 저장)
퍼블릭 액세스:   아니요 (중요!)
초기 DB 이름:    aion2_quest
```

생성 후 **보안 그룹** 수정 — EC2 보안 그룹에서만 3306 허용:

```
RDS 보안 그룹 → 인바운드 규칙 편집
MySQL/Aurora, 포트 3306, 소스: EC2 보안 그룹 ID
```

---

### STEP 4. S3 버킷 2개 생성

```
# 프론트엔드용
S3 → [버킷 만들기]
이름:   aion2-quest-frontend
리전:   ap-northeast-2
퍼블릭 액세스 차단: 모두 차단 (CloudFront OAC만 허용)

# CodeDeploy 배포 아티팩트용
S3 → [버킷 만들기]
이름:   aion2-deploy-artifacts
리전:   ap-northeast-2
퍼블릭 액세스 차단: 모두 차단
```

---

### STEP 5. CloudFront 배포 생성

**오리진 1: S3 (React 정적 파일)**

```
CloudFront → [배포 생성]
오리진 도메인: aion2-quest-frontend.s3.ap-northeast-2.amazonaws.com
S3 버킷 액세스: Origin access control (OAC) → [OAC 생성]
→ 배포 생성 후 "버킷 정책 업데이트" 배너 → [정책 복사]
→ S3 버킷 → 권한 → 버킷 정책에 붙여넣기
```

**오리진 2 추가: EC2 (Spring Boot API)**

```
[오리진 추가]
도메인: EC2 퍼블릭 DNS
프로토콜: HTTP만 (포트 80)
```

**경로 동작 추가 (/api/*)**

```
경로 패턴: /api/*
오리진:    EC2 오리진
캐시 정책: CachingDisabled
원본 요청 정책: AllViewerExceptHostHeader
```

**오류 페이지 설정 (React Router 필수)**

```
배포 선택 → [오류 페이지] 탭 → [사용자 정의 오류 응답 생성]
HTTP 오류 코드: 403 → 응답 페이지: /index.html → HTTP 응답: 200
HTTP 오류 코드: 404 → 응답 페이지: /index.html → HTTP 응답: 200
```

**ACM 인증서 (HTTPS용) — us-east-1에서 발급**

```
리전을 us-east-1로 변경
Certificate Manager → [인증서 요청] → 퍼블릭
도메인: your-domain.com 및 *.your-domain.com
DNS 검증 → Route 53에서 레코드 자동 생성 → 발급 완료 대기 (5~10분)

CloudFront 배포 → [편집]
대체 도메인(CNAME): your-domain.com
SSL 인증서: 방금 발급한 인증서
→ 배포 ID 메모 (GitHub Secrets에 필요)
```

---

### STEP 6. Route 53 도메인 연결

```
Route 53 → 호스팅 영역 → 해당 도메인 → [레코드 생성]
레코드 유형: A
별칭: 켜기
트래픽 라우팅 대상: CloudFront 배포에 대한 별칭 → 방금 만든 배포 선택
[레코드 생성]
```

---

### STEP 7. CodeDeploy 앱 & 배포 그룹 생성

**CodeDeploy 서비스 역할 생성**

```
IAM → 역할 → [역할 생성]
신뢰할 수 있는 엔터티: AWS 서비스 → CodeDeploy
사용 사례: CodeDeploy
정책: AWSCodeDeployRole (자동 선택)
역할 이름: aion2-codedeploy-role
```

**애플리케이션 & 배포 그룹**

```
CodeDeploy → 애플리케이션 → [애플리케이션 생성]
이름: aion2-quest
컴퓨팅 플랫폼: EC2/온프레미스

→ aion2-quest 클릭 → [배포 그룹 생성]
이름:           aion2-prod
서비스 역할:    aion2-codedeploy-role
배포 유형:      현재 위치 (In-Place)
환경 구성:      Amazon EC2 인스턴스
  태그: Name = aion2-quest-server
배포 구성:      CodeDeployDefault.AllAtOnce
로드 밸런서:    비활성화
```

---

### STEP 8. SSM Parameter Store 등록

```powershell
# Windows PowerShell에서 실행
# AWS CLI 인증 먼저: aws configure

aws ssm put-parameter --name "/aion2/db/host"     --value "RDS엔드포인트주소" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/db/username" --value "admin"             --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/db/password" --value "DB비밀번호"         --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/jwt/secret"  --value "최소32자이상랜덤키" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/claude/api-key" --value "sk-ant-api03-..."  --type SecureString --region ap-northeast-2

# SNS 로그인 키 (STEP 13 완료 후 등록)
aws ssm put-parameter --name "/aion2/oauth/kakao/client-id"     --value "" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/oauth/kakao/client-secret" --value "" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/oauth/naver/client-id"     --value "" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/oauth/naver/client-secret" --value "" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/oauth/google/client-id"    --value "" --type SecureString --region ap-northeast-2
aws ssm put-parameter --name "/aion2/oauth/google/client-secret" --value "" --type SecureString --region ap-northeast-2
```

---

### STEP 9. EC2 SSH 접속 & 패키지 설치

```powershell
# Windows PowerShell에서 SSH 접속
# 키 파일 권한 설정 (Windows)
icacls C:\Users\YourName\aion2-keypair.pem /inheritance:r /grant:r "$($env:USERNAME):R"

ssh -i C:\Users\YourName\aion2-keypair.pem ec2-user@EC2_퍼블릭_IP
```

EC2 접속 후:

```bash
# 패키지 업데이트
sudo dnf update -y

# Java 17 (ARM)
sudo dnf install -y java-17-amazon-corretto
java -version  # openjdk 17 확인

# Nginx
sudo dnf install -y nginx
sudo systemctl enable nginx && sudo systemctl start nginx

# Certbot (Let's Encrypt)
sudo dnf install -y certbot python3-certbot-nginx

# CodeDeploy Agent
sudo dnf install -y ruby wget
cd /tmp
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x install
sudo ./install auto
sudo systemctl enable codedeploy-agent && sudo systemctl start codedeploy-agent
sudo systemctl status codedeploy-agent  # active (running) 확인

# 앱 디렉토리
sudo mkdir -p /opt/app && sudo chown ec2-user:ec2-user /opt/app
```

---

### STEP 10. Nginx + SSL 설정

```bash
sudo vi /etc/nginx/conf.d/aion2.conf
```

아래 내용 입력 (`your-domain.com`을 실제 도메인으로 교체):

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api/ {
        proxy_pass         http://localhost:8080;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    location /health {
        proxy_pass http://localhost:8080/api/actuator/health;
        access_log off;
    }
}
```

```bash
sudo nginx -t        # syntax is ok 확인
sudo systemctl restart nginx

# SSL 발급 (도메인 DNS가 이 EC2를 가리킨 후 실행)
sudo certbot --nginx -d your-domain.com
# 이메일 입력 → 약관 동의 → 완료
```

---

### STEP 11. DB 스키마 초기화

```bash
# EC2에서 MariaDB 클라이언트 설치
sudo dnf install -y mariadb105

# RDS 접속 테스트
mysql -h RDS엔드포인트 -u admin -p aion2_quest
# 비밀번호 입력 → MariaDB 프롬프트 확인
```

schema.sql 파일을 EC2로 복사:

```powershell
# Windows PowerShell에서 실행
scp -i C:\Users\YourName\aion2-keypair.pem `
    .\backend\src\main\resources\schema.sql `
    ec2-user@EC2_퍼블릭_IP:~/
```

```bash
# EC2에서 실행
mysql -h RDS엔드포인트 -u admin -p aion2_quest < ~/schema.sql
mysql -h RDS엔드포인트 -u admin -p aion2_quest -e "SHOW TABLES;"
# 11개 테이블 확인
```

---

### STEP 12. GitHub Secrets 등록

```
GitHub → 레포지토리 → Settings → Secrets and variables → Actions → [New repository secret]

AWS_ACCESS_KEY_ID      = STEP 1에서 발급한 IAM User Access Key
AWS_SECRET_ACCESS_KEY  = STEP 1에서 발급한 IAM User Secret Key
CF_DIST_ID             = CloudFront 배포 ID (STEP 5에서 메모)
```

---

### STEP 13. SNS 앱 등록 (카카오·네이버·구글)

**카카오 (https://developers.kakao.com)**

```
1. [내 애플리케이션] → [애플리케이션 추가하기]
2. 앱 이름: AION2 퀘스트 체커
3. [앱 키] → REST API 키 메모 (= Client ID)
4. [카카오 로그인] → 활성화 ON
5. Redirect URI 등록:
   https://your-domain.com/api/login/oauth2/code/kakao
6. [동의항목] → 닉네임(필수), 이메일(선택)
7. [보안] → Client Secret 발급
→ SSM /aion2/oauth/kakao/client-id, /aion2/oauth/kakao/client-secret 업데이트
```

**네이버 (https://developers.naver.com/apps)**

```
1. [Application 등록]
2. 사용 API: 네이버 로그인
3. 권한: 이름(필수), 이메일(필수), 프로필 사진(선택)
4. 서비스 URL: https://your-domain.com
5. Callback URL: https://your-domain.com/api/login/oauth2/code/naver
→ SSM /aion2/oauth/naver/client-id, /aion2/oauth/naver/client-secret 업데이트
```

**구글 (https://console.cloud.google.com)**

```
1. 새 프로젝트 생성
2. [API 및 서비스] → [OAuth 동의 화면] → 외부
   범위: email, profile, openid
3. [사용자 인증 정보] → [OAuth 2.0 클라이언트 ID] → 웹 애플리케이션
   승인된 리디렉션 URI: https://your-domain.com/api/login/oauth2/code/google
→ SSM /aion2/oauth/google/client-id, /aion2/oauth/google/client-secret 업데이트
```

---

### STEP 14. start.sh 도메인 변경 & 첫 배포

`backend/scripts/start.sh`의 `FRONTEND_URL`을 실제 도메인으로 변경:

```bash
export FRONTEND_URL=https://your-domain.com
```

```powershell
# 첫 배포
git add .
git commit -m "feat: initial deployment"
git push origin main
```

배포 확인:

| 항목 | 확인 방법 | 기대 결과 |
|---|---|---|
| GitHub Actions | 레포 → Actions 탭 | 모든 단계 ✓ |
| CodeDeploy | AWS → CodeDeploy → 배포 | 배포 성공 |
| 헬스체크 | `curl https://your-domain.com/health` | `{"status":"UP"}` |
| 브라우저 | `https://your-domain.com` 접속 | 랜딩 페이지 표시 |
| 관리자 로그인 | 아이디: admin / 비밀번호: admin1234 | 로그인 성공 후 즉시 비밀번호 변경 |

---

## 🔄 퀘스트 초기화 로직

`quest_check.date_key(DATETIME)`에 "현재 초기화 주기의 시작 시점"을 저장합니다.

```
일일 퀘스트 (reset_hour=5)
  오전 4시 접속 → date_key = 어제 오전 5시  (전날 주기, 미체크 상태)
  오전 6시 접속 → date_key = 오늘 오전 5시  (오늘 주기, 미체크 상태)

주간 퀘스트 (reset_day=3 수요일, reset_hour=5)
  수요일 오전 4시 → date_key = 지난 주 수요일 오전 5시  (이전 주기)
  수요일 오전 6시 → date_key = 이번 주 수요일 오전 5시  (이번 주기)
```

스케줄러 없이 조회 시점에 계산되므로 별도 크론 작업이 필요 없습니다.
관리자 패널 → 퀘스트 관리에서 퀘스트별로 요일·시각을 독립 설정할 수 있습니다.

---

## 💰 월 운영 비용

> AWS ap-northeast-2 (서울) · On-Demand 기준

| 항목 | 스펙 | 월 비용 |
|---|---|---|
| EC2 | t4g.small (2vCPU/2GB) Graviton2 | $12.26 |
| EBS | gp3 20GB | $1.60 |
| RDS | db.t4g.micro / MariaDB Single-AZ | $21.90 |
| RDS 스토리지 | 20GB gp3 | $2.30 |
| S3 + CloudFront | 1TB 무료 티어 내 | ~$0.01 |
| Route 53 | 호스팅 영역 1개 | $0.50 |
| Claude API | Haiku · 월 ~100건 | ~$0.10 |
| **합계** | | **~$38.67** |

> 1년 No-Upfront RI 전환 시 **~$25/월** (35% 절감). 서비스 안정화 3~6개월 후 전환 권장.

---

## ⚠ 주의사항

### 크롤링
- `https://aion2.plaync.com/robots.txt` → `Allow: /` 확인됨 (크롤링 허용)
- 공지 본문 전체 저장 금지 → Claude API 요약 후 요약문 + 원문 링크만 저장
- `CrawlerService.java`의 CSS selector는 사이트 출시 후 실제 HTML 구조 확인 후 수정 필요

### 보안
- DB 비밀번호, JWT Secret, API Key는 절대 코드에 직접 작성 금지
- 모든 비밀값은 AWS SSM Parameter Store에 저장
- `.gitignore`에 `.env`, `*.pem` 포함되어 있음 — 퍼블릭 레포 안전

### Windows 개발 환경
- `.sh` 파일을 Windows에서 수정하면 CRLF 줄바꿈이 되어 EC2 Linux에서 실행 오류 발생
- `.gitattributes`에 `*.sh text eol=lf` 설정으로 자동 처리됨

---

## 📄 라이선스

개인 프로젝트 · 비상업적 사용 목적
