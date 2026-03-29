# SNS 로그인 앱 등록 가이드
# 각 플랫폼에서 앱을 등록하고 Client ID / Secret을 발급받아야 합니다.

# ================================================================
# 1. 카카오 (developers.kakao.com)
# ================================================================
# 1. https://developers.kakao.com 로그인
# 2. [내 애플리케이션] → [애플리케이션 추가하기]
# 3. 앱 이름: AION2 퀘스트 체커
# 4. [앱 키] 탭 → REST API 키 메모 (= Client ID)
# 5. [카카오 로그인] → 활성화 ON
# 6. [Redirect URI 등록]:
#    https://your-domain.com/api/login/oauth2/code/kakao
# 7. [동의항목] → 닉네임(필수), 이메일(선택) 체크
# 8. [보안] → Client Secret 코드 발급
#
# SSM 등록:
# aws ssm put-parameter --name /aion2/oauth/kakao/client-id     --value "REST_API_키" --type SecureString
# aws ssm put-parameter --name /aion2/oauth/kakao/client-secret --value "Client_Secret" --type SecureString


# ================================================================
# 2. 네이버 (developers.naver.com)
# ================================================================
# 1. https://developers.naver.com/apps 로그인
# 2. [Application 등록] 클릭
# 3. 애플리케이션 이름: AION2 퀘스트 체커
# 4. [사용 API] → 네이버 로그인 선택
# 5. 권한: 이름(필수), 이메일(필수), 프로필 사진(선택)
# 6. 서비스 URL: https://your-domain.com
# 7. Callback URL: https://your-domain.com/api/login/oauth2/code/naver
# 8. Client ID, Client Secret 메모
#
# 주의: 네이버는 검수 없이 개발용으로 바로 사용 가능 (멤버 5명 제한)
#       서비스 오픈 전 검수 신청 필요
#
# SSM 등록:
# aws ssm put-parameter --name /aion2/oauth/naver/client-id     --value "Client_ID" --type SecureString
# aws ssm put-parameter --name /aion2/oauth/naver/client-secret --value "Client_Secret" --type SecureString


# ================================================================
# 3. 구글 (console.cloud.google.com)
# ================================================================
# 1. https://console.cloud.google.com 로그인
# 2. 새 프로젝트 생성: aion2-quest-checker
# 3. [API 및 서비스] → [OAuth 동의 화면]
#    - 사용자 유형: 외부
#    - 앱 이름, 이메일 입력
#    - 범위: .../auth/userinfo.email, .../auth/userinfo.profile, openid
# 4. [사용자 인증 정보] → [사용자 인증 정보 만들기] → [OAuth 2.0 클라이언트 ID]
#    - 애플리케이션 유형: 웹 애플리케이션
#    - 승인된 리디렉션 URI: https://your-domain.com/api/login/oauth2/code/google
# 5. 클라이언트 ID, 클라이언트 보안 비밀 메모
#
# 주의: 구글은 HTTPS 필수. HTTP로는 OAuth 동작 안 함 (로컬 개발은 localhost만 허용)
#
# SSM 등록:
# aws ssm put-parameter --name /aion2/oauth/google/client-id     --value "Client_ID" --type SecureString
# aws ssm put-parameter --name /aion2/oauth/google/client-secret --value "Client_Secret" --type SecureString


# ================================================================
# 로컬 개발 시 Redirect URI
# ================================================================
# 각 플랫폼에 개발용 URI도 등록:
# 카카오: http://localhost:8080/login/oauth2/code/kakao
# 네이버: http://localhost:8080/login/oauth2/code/naver
# 구글:   http://localhost:8080/login/oauth2/code/google
#         http://localhost:5173/oauth2/callback  (프론트 개발 서버)

# ================================================================
# 로컬 개발용 .env (절대 Git에 올리지 않기 — .gitignore에 포함)
# ================================================================
# KAKAO_CLIENT_ID=your_kakao_client_id
# KAKAO_CLIENT_SECRET=your_kakao_client_secret
# NAVER_CLIENT_ID=your_naver_client_id
# NAVER_CLIENT_SECRET=your_naver_client_secret
# GOOGLE_CLIENT_ID=your_google_client_id
# GOOGLE_CLIENT_SECRET=your_google_client_secret
