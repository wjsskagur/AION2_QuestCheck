package com.aion2.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Account 엔티티 — SNS 로그인 필드 추가
 *
 * auth_provider 구분:
 *   LOCAL  = 아이디/비밀번호 직접 가입
 *   KAKAO  = 카카오 OAuth2 로그인
 *   NAVER  = 네이버 OAuth2 로그인
 *   GOOGLE = 구글 OAuth2 로그인
 *   ADMIN  = 관리자가 생성한 계정
 */
@Entity
@Table(name = "account")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LOCAL·ADMIN은 필수, SNS는 NULL 허용
    @Column(unique = true, length = 50)
    private String username;

    // LOCAL·ADMIN만 존재, SNS는 NULL
    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // SNS 플랫폼에서 발급한 고유 사용자 ID (LOCAL·ADMIN은 NULL)
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // SNS에서 가져온 이메일 (네이버는 미제공 가능)
    @Column(length = 255)
    private String email;

    // SNS 닉네임 (프로필 표시용)
    @Column(length = 100)
    private String nickname;

    // SNS 프로필 이미지 URL
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── 표시용 이름 반환 (닉네임 > username 순) ──────────────
    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) return nickname;
        if (username != null && !username.isBlank()) return username;
        return "회원#" + id;
    }

    public enum Role { ADMIN, USER }

    public enum AuthProvider {
        LOCAL,   // 아이디/비밀번호 직접 가입
        KAKAO,   // 카카오 OAuth2
        NAVER,   // 네이버 OAuth2
        GOOGLE,  // 구글 OAuth2
        ADMIN    // 관리자 직접 생성
    }
}
