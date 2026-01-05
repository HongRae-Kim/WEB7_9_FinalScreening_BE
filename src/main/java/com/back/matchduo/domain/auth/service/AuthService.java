package com.back.matchduo.domain.auth.service;

import com.back.matchduo.domain.auth.dto.request.LoginRequest;
import com.back.matchduo.domain.auth.dto.response.AuthUserSummary;
import com.back.matchduo.domain.auth.dto.response.LoginResponse;
import com.back.matchduo.domain.auth.dto.response.RefreshResponse;
import com.back.matchduo.domain.auth.refresh.entity.RefreshToken;
import com.back.matchduo.domain.auth.refresh.repository.RefreshTokenRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.config.JwtProperties;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import com.back.matchduo.global.security.cookie.AuthCookieProvider;
import com.back.matchduo.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final AuthCookieProvider cookieProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            AuthCookieProvider cookieProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.cookieProvider = cookieProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest req, HttpServletResponse res) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_EMAIL));

        // 비밀번호 검증 (BCrypt 해시 + 레거시 평문 호환)
        if (!validatePassword(req.password(), user)) {
            throw new CustomException(CustomErrorCode.WRONG_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        upsertRefreshToken(user.getId(), refreshToken);

        addSetCookie(res, cookieProvider.createAccessTokenCookie(accessToken, jwtProperties.accessExpireSeconds()));
        addSetCookie(res, cookieProvider.createRefreshTokenCookie(refreshToken, jwtProperties.refreshExpireSeconds()));

        return new LoginResponse(
                new AuthUserSummary(user.getId(), user.getEmail(), user.getNickname()),
                accessToken,
                refreshToken
        );
    }

    public RefreshResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String refreshToken = extractCookie(req, AuthCookieProvider.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(CustomErrorCode.UNAUTHORIZED_USER);
        }

        // 서명/만료 검증
        try {
            jwtProvider.validate(refreshToken);
        } catch (Exception e) {
            throw new CustomException(CustomErrorCode.UNAUTHORIZED_USER);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        // 서버 저장 RT 존재 + 일치 검증
        RefreshToken saved = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.UNAUTHORIZED_USER));

        if (!saved.getToken().equals(refreshToken)) {
            throw new CustomException(CustomErrorCode.UNAUTHORIZED_USER);
        }

        // AccessToken 재발급
        String newAccessToken = jwtProvider.createAccessToken(userId);
        addSetCookie(res, cookieProvider.createAccessTokenCookie(newAccessToken, jwtProperties.accessExpireSeconds()));

        // 응답 정책: user 요약 포함 + accessToken만 내려줌
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_USER));

        return new RefreshResponse(
                new AuthUserSummary(user.getId(), user.getEmail(), user.getNickname()),
                newAccessToken
        );

        // refreshToken은 쿠키로만 유지하고, 응답 Body에는 포함하지 않는다
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String refreshToken = extractCookie(req, AuthCookieProvider.REFRESH_TOKEN_COOKIE);

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                jwtProvider.validate(refreshToken);
                Long userId = jwtProvider.getUserId(refreshToken);
                refreshTokenRepository.deleteByUserId(userId);
            } catch (Exception e) {
                // 로그아웃은 조용히 처리 (토큰이 깨져도 쿠키 만료는 진행)
            }
        }

        addSetCookie(res, cookieProvider.expireAccessTokenCookie());
        addSetCookie(res, cookieProvider.expireRefreshTokenCookie());
    }

    private void upsertRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshExpireSeconds());

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.update(refreshToken, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.create(userId, refreshToken, expiresAt))
                );
    }

    private void addSetCookie(HttpServletResponse res, ResponseCookie cookie) {
        // ResponseCookie는 toString()이 Set-Cookie 헤더 형식으로 나옴
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 비밀번호 검증 (BCrypt 해시 + 레거시 평문 호환)
     * - BCrypt 해시인 경우: passwordEncoder.matches() 사용
     * - 레거시 평문인 경우: 직접 비교 후 BCrypt로 마이그레이션
     */
    private boolean validatePassword(String rawPassword, User user) {
        String storedPassword = user.getPassword();

        // BCrypt 해시 여부 확인 ($2a$, $2b$, $2y$로 시작)
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        // 레거시 평문 비밀번호 처리
        if (storedPassword.equals(rawPassword)) {
            // 로그인 성공 시 BCrypt로 마이그레이션
            user.setPassword(passwordEncoder.encode(rawPassword));
            return true;
        }

        return false;
    }
}
