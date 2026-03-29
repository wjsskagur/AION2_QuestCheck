package com.aion2.service;

import com.aion2.dto.AuthDto;
import com.aion2.entity.Account;
import com.aion2.repository.AccountRepository;
import com.aion2.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ── 일반 로그인 (기존 유지) ───────────────────────────────────────────────
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Account account = accountRepo.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다"));

        // SNS 가입 계정으로 일반 로그인 시도 방지
        if (account.getAuthProvider() != Account.AuthProvider.LOCAL
            && account.getAuthProvider() != Account.AuthProvider.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                account.getAuthProvider().name() + " 로그인을 이용해주세요");
        }

        if (!passwordEncoder.matches(req.getPassword(), account.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다");
        }

        return buildLoginResponse(account);
    }

    // ── 일반 회원가입 (신규 추가) ──────────────────────────────────────────────
    public AuthDto.LoginResponse signup(AuthDto.SignupRequest req) {
        if (accountRepo.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다");
        }

        Account account = Account.builder()
            .username(req.getUsername())
            .password(passwordEncoder.encode(req.getPassword()))
            .authProvider(Account.AuthProvider.LOCAL)
            .nickname(req.getNickname())
            .build();

        return buildLoginResponse(accountRepo.save(account));
    }

    // ── 공통: LoginResponse 생성 ──────────────────────────────────────────────
    private AuthDto.LoginResponse buildLoginResponse(Account account) {
        return AuthDto.LoginResponse.builder()
            .token(jwtProvider.generate(account))
            .id(account.getId())
            .username(account.getUsername())
            .nickname(account.getDisplayName())
            .role(account.getRole().name())
            .authProvider(account.getAuthProvider().name())
            .profileImage(account.getProfileImage())
            .build();
    }
}
