package com.aion2.security;

import com.aion2.entity.Account;
import com.aion2.repository.AccountRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration @EnableWebSecurity @EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/public/**", "/login/oauth2/**", "/oauth2/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(o -> o
                .userInfoEndpoint(u -> u.userService(oauth2UserService))
                .successHandler(oauth2SuccessHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}

// ── JwtProvider ───────────────────────────────────────────────────────────────
@Component
public class JwtProvider {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expiration-ms}") private long expMs;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    public String generate(Account a) {
        return Jwts.builder()
            .subject(a.getId().toString())
            .claim("role", a.getRole().name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expMs))
            .signWith(key()).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
}

// ── JwtAuthFilter ─────────────────────────────────────────────────────────────
@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final AccountRepository accountRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtProvider.parse(header.substring(7));
                Long id = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);
                accountRepo.findById(id).ifPresent(a -> {
                    var auth = new UsernamePasswordAuthenticationToken(a, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (JwtException e) {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.getWriter().write("{\"message\":\"Invalid token\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}

// ── CustomOAuth2UserService ───────────────────────────────────────────────────
@Service @RequiredArgsConstructor @Slf4j
class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final AccountRepository accountRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(req);
        String provider = req.getClientRegistration().getRegistrationId().toUpperCase();
        Account.AuthProvider ap = Account.AuthProvider.valueOf(provider);
        OAuthInfo info = extract(ap, oAuth2User.getAttributes());
        log.info("OAuth2 login: provider={} id={}", provider, info.providerId());
        Account account = accountRepo.findByAuthProviderAndProviderId(ap, info.providerId())
            .orElseGet(() -> accountRepo.save(Account.builder()
                .authProvider(ap).providerId(info.providerId())
                .email(info.email()).nickname(info.nickname()).profileImage(info.image()).build()));
        account.setEmail(info.email());
        account.setNickname(info.nickname());
        account.setProfileImage(info.image());
        accountRepo.save(account);
        return new OAuth2AccountUser(account, oAuth2User.getAttributes());
    }

    @SuppressWarnings("unchecked")
    private OAuthInfo extract(Account.AuthProvider ap, Map<String, Object> attrs) {
        return switch (ap) {
            case KAKAO -> {
                Map<String, Object> ka = (Map<String, Object>) attrs.get("kakao_account");
                Map<String, Object> pr = ka != null ? (Map<String, Object>) ka.get("profile") : Map.of();
                yield new OAuthInfo(String.valueOf(attrs.get("id")),
                    ka != null ? (String) ka.get("email") : null,
                    (String) pr.getOrDefault("nickname", null),
                    (String) pr.getOrDefault("profile_image_url", null));
            }
            case NAVER -> {
                Map<String, Object> r = (Map<String, Object>) attrs.get("response");
                yield new OAuthInfo((String) r.get("id"), (String) r.get("email"),
                    (String) r.get("nickname"), (String) r.get("profile_image"));
            }
            case GOOGLE -> new OAuthInfo((String) attrs.get("sub"), (String) attrs.get("email"),
                (String) attrs.get("name"), (String) attrs.get("picture"));
            default -> throw new OAuth2AuthenticationException("unsupported: " + ap);
        };
    }
    record OAuthInfo(String providerId, String email, String nickname, String image) {}
}

class OAuth2AccountUser implements OAuth2User {
    private final Account account;
    private final Map<String, Object> attributes;
    OAuth2AccountUser(Account a, Map<String, Object> attrs) { account = a; attributes = attrs; }
    public Account getAccount() { return account; }
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }
    @Override public String getName() { return String.valueOf(account.getId()); }
}

// ── OAuth2SuccessHandler ──────────────────────────────────────────────────────
@Component @RequiredArgsConstructor @Slf4j
class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    @Value("${app.frontend-url}") private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
            Authentication auth) throws IOException {
        Account account = ((OAuth2AccountUser) auth.getPrincipal()).getAccount();
        String jwt = jwtProvider.generate(account);
        res.sendRedirect(frontendUrl + "/oauth2/callback?token=" + jwt
            + "&provider=" + account.getAuthProvider().name().toLowerCase());
    }
}
