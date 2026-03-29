# ⚔ AION2 Quest Checker

아이온2 일일·주간 퀘스트 완료 현황을 캐릭터별로 관리하는 웹 서비스입니다.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)
![MariaDB](https://img.shields.io/badge/MariaDB-10.11-blue?style=flat-square&logo=mariadb)
![AWS](https://img.shields.io/badge/AWS-EC2_+_RDS_+_S3-orange?style=flat-square&logo=amazonaws)

---

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [프로젝트 구조](#-프로젝트-구조)
- [로컬 개발 환경 세팅](#-로컬-개발-환경-세팅)
- [환경변수 설정](#-환경변수-설정)
- [DB 스키마](#-db-스키마)
- [API 명세](#-api-명세)
- [배포](#-배포)
- [퀘스트 초기화 로직](#-퀘스트-초기화-로직)
- [월 운영 비용](#-월-운영-비용)

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| **퀘스트 체커** | 캐릭터별 일일·주간 퀘스트 완료 현황 관리 및 진행률 표시 |
| **퀘스트 자동 초기화** | 퀘스트별 초기화 요일·시각 설정 (기본: 수요일 오전 5시) |
| **캐릭터 인증** | 아이온2 공식 게시글 댓글로 캐릭터 소유권 검증 |
| **서버 랭킹** | 등록된 캐릭터 기준 서버별 전투력·레벨 랭킹 (비로그인 공개) |
| **공지 자동 요약** | 공식 공지를 3시간마다 수집, Claude API로 3줄 자동 요약 |
| **관리자 패널** | 계정·퀘스트 템플릿·인증 URL 관리 |

---

## 🛠 기술 스택

### 백엔드
- **Java 17** + **Spring Boot 3.3**
- **Spring Security** — JWT Stateless 인증
- **Spring Data JPA** + **MariaDB**
- **Jsoup** — 공홈 크롤링 (인증 검증, 공지 수집)
- **Claude API (Haiku)** — 공지 자동 요약
- **Gradle**

### 프론트엔드
- **React 18** + **Vite**
- **React Router v6** — 공개/보호 라우트 분리
- **Google AdSense** — 광고 수익화 (공개 페이지)

### 인프라 (AWS)
- **EC2 t4g.small** — Spring Boot 서버 (Graviton2 ARM)
- **RDS db.t4g.micro** — MariaDB 10.11
- **S3 + CloudFront** — React 정적 파일 서빙
- **SSM Parameter Store** — 비밀값 관리
- **CodeDeploy** — EC2 In-Place 자동 배포
- **GitHub Actions** — CI/CD 파이프라인

---

## 🏗 아키텍처

```
브라우저
  │
  ├── /* ──────────→ CloudFront ──→ S3 (React 빌드)
  │
  └── /api/* ──────→ CloudFront ──→ EC2 (Spring Boot :8080)
                                         │
                                         └──→ RDS (MariaDB)
                                         └──→ SSM (비밀값)
                                         └──→ Claude API (공지 요약)
                                         └──→ 아이온2 공식 사이트 (크롤링)
```

---

## 📁 프로젝트 구조

```
aion2-quest-checker/
├── .github/
│   └── workflows/
│       └── deploy.yml          # GitHub Actions CI/CD
├── .gitattributes              # LF 강제 (Windows CRLF 방지)
├── .gitignore
├── appspec.yml                 # CodeDeploy 설정
│
├── backend/                    # Spring Boot (Java 17)
│   ├── build.gradle
│   ├── settings.gradle
│   ├── scripts/
│   │   ├── stop.sh             # CodeDeploy: 기존 프로세스 종료
│   │   ├── start.sh            # CodeDeploy: JVM 기동 (SSM 비밀값 주입)
│   │   └── health_check.sh     # CodeDeploy: /actuator/health 폴링
│   └── src/main/
│       ├── java/com/aion2/
│       │   ├── Aion2QuestApplication.java
│       │   ├── config/         SecurityConfig.java
│       │   ├── security/       JwtProvider · JwtAuthFilter
│       │   ├── entity/         Account · Character · QuestTemplate
│       │   │                   QuestCheck · VerificationCode · Post · SystemConfig
│       │   ├── repository/     JpaRepository 인터페이스 7개
│       │   ├── dto/            요청/응답 DTO
│       │   ├── service/        Auth · Admin · Character · QuestCheck
│       │   │                   Verification · Crawler · ClaudeApi
│       │   └── controller/     Auth · Character · QuestCheck
│       │                       Verification · Admin · Public
│       └── resources/
│           ├── application.yml
│           └── schema.sql
│
└── frontend/                   # React 18 + Vite
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx             # 라우팅 + Auth Context
        ├── index.css           # 다크 테마 전역 스타일
        ├── api/index.js        # fetch 래퍼 (JWT 자동 주입)
        ├── hooks/useAuth.js    # 로그인 상태 관리
        ├── components/
        │   └── Components.jsx  # AdBanner · VerificationModal · Pagination
        └── pages/
            ├── PublicPages.jsx     # Landing · Login · Ranking · Notice · Guide · Privacy
            ├── ProtectedPages.jsx  # Dashboard · CharactersPage · CharDetailPage
            └── AdminPage.jsx       # 계정·퀘스트·인증 설정 관리
```

---

## 💻 로컬 개발 환경 세팅

### 사전 준비

| 도구 | 버전 | 설치 |
|---|---|---|
| JDK (Corretto) | 17 LTS | [다운로드](https://aws.amazon.com/corretto/) |
| Gradle | 8.x | JDK 설치 시 `./gradlew` 사용 가능 |
| Node.js | 20 LTS | [다운로드](https://nodejs.org/) |
| Docker Desktop | 최신 | [다운로드](https://www.docker.com/products/docker-desktop/) |

> **Windows 사용자** — PowerShell 7 이상을 사용하세요. 명령어는 Mac/Linux와 동일합니다.

### 1. 레포 클론

```bash
git clone https://github.com/your-username/aion2-quest-checker.git
cd aion2-quest-checker
```

### 2. 로컬 MariaDB 실행 (Docker)

```bash
docker run -d --name aion2-db \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=aion2_quest \
  -e MYSQL_USER=aion2_user \
  -e MYSQL_PASSWORD=aion2_pass \
  -p 3306:3306 \
  mariadb:10.11
```

### 3. DB 스키마 초기화

```bash
# Windows PowerShell
docker exec -i aion2-db mariadb -u aion2_user -paion2_pass aion2_quest `
  < backend/src/main/resources/schema.sql

# Mac/Linux
docker exec -i aion2-db mariadb -u aion2_user -paion2_pass aion2_quest \
  < backend/src/main/resources/schema.sql
```

### 4. 백엔드 실행

```bash
cd backend

# Windows PowerShell
.\gradlew bootRun

# Mac/Linux
./gradlew bootRun
```

> `http://localhost:8080/api/actuator/health` 에서 `{"status":"UP"}` 확인

### 5. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

> `http://localhost:5173` 에서 확인

---

## 🔐 환경변수 설정

### 로컬 개발 (`application.yml` 기본값 사용)

로컬에서는 `application.yml`에 지정된 기본값으로 실행됩니다. 별도 설정 불필요합니다.

```yaml
# 기본값 확인 (backend/src/main/resources/application.yml)
spring.datasource.username: ${DB_USERNAME:aion2_user}   # : 뒤가 기본값
spring.datasource.password: ${DB_PASSWORD:aion2_pass}
```

### 운영 환경 (AWS SSM Parameter Store)

운영 서버에서는 `start.sh`가 SSM에서 값을 가져와 환경변수로 주입합니다.

```bash
# SSM 파라미터 등록 (최초 1회)
aws ssm put-parameter --name "/aion2/db/host"       --value "RDS엔드포인트"     --type SecureString
aws ssm put-parameter --name "/aion2/db/username"   --value "admin"              --type SecureString
aws ssm put-parameter --name "/aion2/db/password"   --value "DB비밀번호"         --type SecureString
aws ssm put-parameter --name "/aion2/jwt/secret"    --value "32자이상랜덤문자열" --type SecureString
aws ssm put-parameter --name "/aion2/claude/api-key" --value "sk-ant-..."        --type SecureString
```

> ⚠ **절대 코드에 직접 작성하지 마세요.** `.env` 파일도 `.gitignore`에 포함되어 있습니다.

---

## 🗄 DB 스키마

| 테이블 | 역할 | 핵심 컬럼 |
|---|---|---|
| `account` | 사용자 계정 | username · password(BCrypt) · role |
| `character` | 캐릭터 정보 | server · name · level · combat_power · verified |
| `quest_template` | 퀘스트 템플릿 | type · **reset_day** · **reset_hour** |
| `quest_check` | 완료 기록 | character_id · quest_id · **date_key(DATETIME)** |
| `verification_code` | 인증 코드 | code · status · expires_at |
| `post` | 공지 게시판 | title · content(요약) · source_url |
| `system_config` | 동적 설정 | config_key · config_value |

> DDL 전체: [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql)

---

## 📡 API 명세

### 공개 API (인증 불필요)

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/auth/login` | 로그인 → JWT 발급 |
| `GET` | `/api/public/ranking?server=카이나토스` | 서버별 캐릭터 랭킹 |
| `GET` | `/api/public/posts?category=UPDATE&page=0` | 공지 게시판 |
| `GET` | `/api/public/stats` | 서비스 통계 (랜딩 페이지용) |

### 로그인 필요 API

| Method | URL | 설명 |
|---|---|---|
| `GET` | `/api/characters` | 내 캐릭터 목록 |
| `POST` | `/api/characters` | 캐릭터 추가 |
| `PUT` | `/api/characters/{id}/refresh` | 캐릭터 정보 갱신 |
| `DELETE` | `/api/characters/{id}` | 캐릭터 삭제 |
| `GET` | `/api/quest-checks/dashboard` | 대시보드 (전 캐릭터 퀘스트 현황) |
| `POST` | `/api/quest-checks/{charId}/toggle/{questId}` | 퀘스트 체크 토글 |
| `POST` | `/api/characters/{id}/verification/issue` | 인증 코드 발급 |
| `POST` | `/api/characters/{id}/verification/verify` | 댓글 크롤링 검증 |

### 관리자 전용 API

| Method | URL | 설명 |
|---|---|---|
| `GET/POST` | `/api/admin/accounts` | 계정 목록 조회 / 생성 |
| `GET/POST` | `/api/admin/quests` | 퀘스트 템플릿 조회 / 생성 |
| `GET/PUT` | `/api/admin/config/verify` | 인증 URL·만료시간 조회/변경 |

---

## 🚀 배포

### 사전 조건

1. [초기 세팅 가이드](SETUP.md) 완료 (AWS 인프라 구성)
2. GitHub Repository Secrets 등록

```
AWS_ACCESS_KEY_ID      = IAM User Access Key
AWS_SECRET_ACCESS_KEY  = IAM User Secret Key
CF_DIST_ID             = CloudFront 배포 ID
```

### 배포 방법

```bash
git push origin main
```

GitHub Actions가 자동으로 아래 순서로 실행됩니다.

```
① Gradle 빌드 & 테스트
② React npm 빌드
③ React dist/ → S3 업로드 + CloudFront 캐시 무효화
④ JAR → S3 업로드 → CodeDeploy 트리거
⑤ EC2: stop.sh → start.sh → health_check.sh
⑥ /actuator/health 200 확인 → 배포 완료
```

> 전체 소요: **약 4~6분** · 실제 다운타임: **약 15~30초** (JVM 기동 시간)
>
> `health_check.sh`가 실패하면 CodeDeploy가 자동으로 이전 버전으로 롤백합니다.

---

## 🔄 퀘스트 초기화 로직

퀘스트마다 `reset_day`(초기화 요일)와 `reset_hour`(초기화 시각)를 독립적으로 설정할 수 있습니다.

### 기본값

| 항목 | 값 | 설명 |
|---|---|---|
| `reset_day` | `3` | 수요일 (아이온2 주간 초기화 기준) |
| `reset_hour` | `5` | 오전 5시 |

### 동작 원리

`quest_check` 테이블의 `date_key(DATETIME)`에 "현재 초기화 주기의 시작 시점"을 저장합니다. 같은 주기 안에서는 `date_key`가 동일하므로 체크 상태가 유지되고, 다음 초기화 시점이 지나면 `date_key`가 바뀌어 자동으로 리셋됩니다. **별도 스케줄러 없이** 조회 시 자동 리셋 효과를 얻습니다.

```
일일 퀘스트 (reset_hour=5)
  현재 오전 4시  → date_key = 어제 오전 5시  (전날 주기)
  현재 오전 6시  → date_key = 오늘 오전 5시  (오늘 주기)

주간 퀘스트 (reset_day=3 수요일, reset_hour=5)
  수요일 오전 4시 → date_key = 지난 주 수요일 오전 5시  (이전 주기)
  수요일 오전 6시 → date_key = 이번 주 수요일 오전 5시  (이번 주기)
```

### 관리자 패널에서 퀘스트 설정

```
관리자 패널 → 퀘스트 관리 탭
  - 유형: 일일 / 주간 / 특정
  - 초기화 시각: 0~23시 선택
  - 초기화 요일: 월~일 선택 (주간만 표시)
```

---

## 💰 월 운영 비용

> AWS ap-northeast-2 (서울) · On-Demand 기준

| 항목 | 스펙 | 월 비용 |
|---|---|---|
| EC2 | t4g.small (2vCPU / 2GB) | $12.26 |
| EBS | gp3 20GB | $1.60 |
| RDS | db.t4g.micro / MariaDB Single-AZ | $21.90 |
| RDS 스토리지 | 20GB gp3 | $2.30 |
| S3 + CloudFront | React 빌드 (1TB 무료 티어 내) | ~$0.01 |
| Route 53 | 호스팅 영역 1개 | $0.50 |
| Claude API | Haiku · 월 ~100건 공지 요약 | ~$0.10 |
| **합계** | | **~$38.67** |

> 1년 No-Upfront RI 전환 시: **~$25/월** (35% 절감)

---

## 📝 주의사항

### 크롤링 관련

- 아이온2 출시 후 `https://aion2.plaync.com/robots.txt` 크롤링 허용 여부를 반드시 확인하세요
- 공지 본문 전체 저장은 저작권 침해 소지가 있어 Claude API 요약 후 **요약문 + 원문 링크**만 저장합니다
- `CrawlerService.java`의 CSS selector는 실제 사이트 HTML 구조를 보고 수정이 필요합니다

### 보안

- DB 비밀번호, JWT Secret, API Key는 절대 코드에 직접 작성하지 마세요
- 모든 비밀값은 AWS SSM Parameter Store에 저장합니다
- 퍼블릭 레포로 운영 가능하나 `.env` 파일이 올라가지 않도록 주의하세요

---

## 📄 라이선스

개인 프로젝트 · 비상업적 사용 목적
