package com.aion2.security;

import com.aion2.entity.Account;
import com.aion2.repository.AccountRepository;
import com.aion2.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

// ── SecurityConfig (OAuth2 추가) ──────────────────────────────────────────────
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // OAuth2 로그인 설정
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                // /oauth2/authorization/{provider} 로 SNS 로그인 시작
                // /api/auth/oauth2/callback/{provider} 로 콜백 수신
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

// ── CustomOAuth2UserService ───────────────────────────────────────────────────
/**
 * SNS 인증 성공 후 사용자 정보 처리
 * 신규 SNS 사용자 → DB 자동 생성 (회원가입)
 * 기존 SNS 사용자 → DB 조회 (로그인)
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AccountRepository accountRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId().toUpperCase();
        Account.AuthProvider authProvider = Account.AuthProvider.valueOf(provider);

        // 플랫폼별 사용자 정보 추출
        OAuthUserInfo userInfo = extractUserInfo(authProvider, oAuth2User.getAttributes());
        log.info("OAuth2 로그인: provider={} providerId={}", provider, userInfo.providerId());

        // DB에서 기존 회원 조회, 없으면 신규 생성
        Account account = accountRepo
            .findByAuthProviderAndProviderId(authProvider, userInfo.providerId())
            .orElseGet(() -> {
                log.info("신규 SNS 회원 생성: {}", userInfo.nickname());
                return accountRepo.save(Account.builder()
                    .authProvider(authProvider)
                    .providerId(userInfo.providerId())
                    .email(userInfo.email())
                    .nickname(userInfo.nickname())
                    .profileImage(userInfo.profileImage())
                    .build());
            });

        // 프로필 정보 업데이트 (매 로그인마다 최신 정보 반영)
        account.setEmail(userInfo.email());
        account.setNickname(userInfo.nickname());
        account.setProfileImage(userInfo.profileImage());
        accountRepo.save(account);

        // account를 Attribute에 담아서 SuccessHandler로 전달
        return new OAuth2AccountUser(account, oAuth2User.getAttributes());
    }

    /**
     * 플랫폼별로 응답 구조가 달라서 각각 파싱
     */
    private OAuthUserInfo extractUserInfo(Account.AuthProvider provider, Map<String, Object> attrs) {
        return switch (provider) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
                Map<String, Object> profile = kakaoAccount != null
                    ? (Map<String, Object>) kakaoAccount.get("profile") : Map.of();
                yield new OAuthUserInfo(
                    String.valueOf(attrs.get("id")),
                    kakaoAccount != null ? (String) kakaoAccount.get("email") : null,
                    (String) profile.getOrDefault("nickname", null),
                    (String) profile.getOrDefault("profile_image_url", null)
                );
            }
            case NAVER -> {
                // 네이버는 response 객체 안에 데이터
                Map<String, Object> response = (Map<String, Object>) attrs.get("response");
                yield new OAuthUserInfo(
                    (String) response.get("id"),
                    (String) response.get("email"),
                    (String) response.get("nickname"),
                    (String) response.get("profile_image")
                );
            }
            case GOOGLE -> new OAuthUserInfo(
                (String) attrs.get("sub"),      // 구글 고유 ID
                (String) attrs.get("email"),
                (String) attrs.get("name"),
                (String) attrs.get("picture")
            );
            default -> throw new OAuth2AuthenticationException("지원하지 않는 provider: " + provider);
        };
    }

    record OAuthUserInfo(String providerId, String email, String nickname, String profileImage) {}
}

// ── OAuth2AccountUser ─────────────────────────────────────────────────────────
/**
 * OAuth2User 구현체 — Account 객체를 담아서 SuccessHandler로 전달
 */
class OAuth2AccountUser implements OAuth2User {
    private final Account account;
    private final Map<String, Object> attributes;

    OAuth2AccountUser(Account account, Map<String, Object> attributes) {
        this.account = account;
        this.attributes = attributes;
    }

    public Account getAccount() { return account; }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
            "ROLE_" + account.getRole().name()));
    }
    @Override public String getName() { return String.valueOf(account.getId()); }
}

// ── OAuth2SuccessHandler ──────────────────────────────────────────────────────
/**
 * SNS 로그인 성공 후 JWT 발급 → 프론트로 리다이렉트
 * 프론트: /oauth2/callback?token=JWT_TOKEN
 */
@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AccountUser oAuth2User = (OAuth2AccountUser) authentication.getPrincipal();
        Account account = oAuth2User.getAccount();
        String jwt = jwtProvider.generate(account);

        log.info("OAuth2 로그인 성공: accountId={} provider={}", account.getId(), account.getAuthProvider());

        // 프론트엔드의 OAuth2 콜백 페이지로 토큰을 쿼리 파라미터로 전달
        // 프론트: /oauth2/callback 페이지에서 토큰을 localStorage에 저장 후 /dashboard로 이동
        String redirectUrl = "https://your-domain.com/oauth2/callback?token=" + jwt
            + "&provider=" + account.getAuthProvider().name().toLowerCase();

        response.sendRedirect(redirectUrl);
    }
}
