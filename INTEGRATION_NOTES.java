// ============================================================
// App.jsx 라우트 추가 — /oauth2/callback 경로 추가
// ============================================================

// 기존 import에 추가:
// import { OAuth2CallbackPage } from './pages/LoginPage.jsx'

// Routes 안에 추가:
// <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

/*
<Routes>
  <Route path="/"               element={<LandingPage />} />
  <Route path="/login"          element={<LoginPage />} />
  <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />  ← 추가
  <Route path="/ranking"        element={<RankingPage />} />
  ...
</Routes>
*/

// ============================================================
// AdminPage.jsx — 계정 목록에 auth_provider 배지 추가
// ============================================================

// AccountsTab의 테이블 tbody 부분 교체:
/*
<tbody>
  {accounts.map(a => (
    <tr key={a.id}>
      <td>{a.id}</td>
      <td style={{ fontWeight: 500 }}>
        {a.nickname || a.username || '(이름 없음)'}
      </td>
      <td>
        <span className={`badge ${a.role === 'ADMIN' ? 'badge-gold' : 'badge-gray'}`}>
          {a.role}
        </span>
      </td>

      // ── 추가: 가입 경로 배지 ──
      <td>
        <span className={`badge ${
          a.authProvider === 'KAKAO'  ? 'badge-gold'   :
          a.authProvider === 'NAVER'  ? 'badge-green'  :
          a.authProvider === 'GOOGLE' ? 'badge-red'    :
          a.authProvider === 'ADMIN'  ? 'badge-purple' :
          'badge-blue'  // LOCAL
        }`}>
          {a.authProvider === 'LOCAL'  ? '일반가입' :
           a.authProvider === 'ADMIN'  ? '관리자생성' :
           a.authProvider}
        </span>
      </td>

      <td className="text-muted text-small">
        {new Date(a.createdAt).toLocaleDateString('ko-KR')}
      </td>
    </tr>
  ))}
</tbody>
*/

// ============================================================
// AccountDto.Summary 에 authProvider 필드 추가 (백엔드)
// ============================================================
/*
@Getter @Builder
public static class Summary {
    private Long id;
    private String username;
    private String nickname;       // 추가
    private String role;
    private String authProvider;   // 추가
    private LocalDateTime createdAt;

    public static Summary from(Account a) {
        return Summary.builder()
            .id(a.getId())
            .username(a.getUsername())
            .nickname(a.getNickname())
            .role(a.getRole().name())
            .authProvider(a.getAuthProvider().name())  // 추가
            .createdAt(a.getCreatedAt())
            .build();
    }
}
*/

// ============================================================
// /auth/me 엔드포인트 추가 (백엔드 AuthController)
// OAuth2 콜백 페이지에서 사용자 정보 조회용
// ============================================================
/*
@GetMapping("/me")
public AuthDto.LoginResponse getMe(@AuthenticationPrincipal Account account) {
    return AuthDto.LoginResponse.builder()
        .id(account.getId())
        .username(account.getUsername())
        .nickname(account.getDisplayName())
        .role(account.getRole().name())
        .authProvider(account.getAuthProvider().name())
        .profileImage(account.getProfileImage())
        .build();
}
*/
