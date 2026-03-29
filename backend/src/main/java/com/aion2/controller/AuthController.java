package com.aion2.controller;

import com.aion2.dto.AuthDto;
import com.aion2.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 기존: 일반 로그인
    @PostMapping("/login")
    public AuthDto.LoginResponse login(@RequestBody AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    // 신규: 일반 회원가입
    @PostMapping("/signup")
    public AuthDto.LoginResponse signup(@RequestBody AuthDto.SignupRequest req) {
        return authService.signup(req);
    }

    // SNS 로그인 시작 URL (Spring Security가 자동 처리)
    // GET /api/oauth2/authorization/kakao  → 카카오 인증 페이지로 이동
    // GET /api/oauth2/authorization/naver  → 네이버 인증 페이지로 이동
    // GET /api/oauth2/authorization/google → 구글 인증 페이지로 이동
    //
    // SNS 인증 성공 후 콜백:
    // GET /api/auth/oauth2/callback/{provider}?code=... (Spring Security 처리)
    // → OAuth2SuccessHandler → 프론트 /oauth2/callback?token=JWT로 리다이렉트
}
