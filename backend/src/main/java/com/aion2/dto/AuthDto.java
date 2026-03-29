package com.aion2.dto;

import lombok.*;

// ── AuthDto 변경사항 ─────────────────────────────────────────────────────────
public class AuthDto {

    // 기존 로그인 요청 (변경 없음)
    @Getter @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    // 신규: 일반 회원가입 요청
    @Getter @Setter
    public static class SignupRequest {
        private String username;   // 아이디 (영문+숫자 4~20자)
        private String password;   // 비밀번호 (8자 이상)
        private String nickname;   // 표시 이름 (선택)
    }

    // LoginResponse — authProvider, nickname, profileImage 필드 추가
    @Getter @Builder
    public static class LoginResponse {
        private String token;
        private Long id;
        private String username;
        private String nickname;      // 추가: 표시 이름
        private String role;
        private String authProvider;  // 추가: LOCAL | KAKAO | NAVER | GOOGLE | ADMIN
        private String profileImage;  // 추가: SNS 프로필 이미지
    }
}

// ── AccountRepository 추가 메서드 ────────────────────────────────────────────
/*
// repository/AccountRepository.java 에 아래 메서드 추가

import com.aion2.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);

    // SNS 로그인용 — 플랫폼 + 플랫폼 고유 ID로 조회
    Optional<Account> findByAuthProviderAndProviderId(
        Account.AuthProvider authProvider, String providerId);
}
*/
