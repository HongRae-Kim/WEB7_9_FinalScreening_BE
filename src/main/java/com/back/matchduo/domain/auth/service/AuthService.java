package com.back.matchduo.domain.auth.service;

import com.back.matchduo.domain.auth.dto.request.LoginRequest;
import com.back.matchduo.domain.auth.dto.response.AuthUserSummary;
import com.back.matchduo.domain.auth.dto.response.LoginResponse;
import com.back.matchduo.domain.auth.dto.response.RefreshResponse;
import com.back.matchduo.domain.auth.refresh.service.RefreshTokenStore;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.config.JwtProperties;
import com.back.matchduo.global.exception.CustomErrorCode;
import com.back.matchduo.global.exception.CustomException;
import com.back.matchduo.global.security.cookie.AuthCookieProvider;
import com.back.matchduo.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final AuthCookieProvider cookieProvider;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    public AuthService(
            UserRepository userRepository,
            RefreshTokenStore refreshTokenStore,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            AuthCookieProvider cookieProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.cookieProvider = cookieProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest req, HttpServletResponse res) {
        long start = System.nanoTime();

        long t1 = System.nanoTime();
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_EMAIL));
        long t2 = System.nanoTime();

        // 비밀번호 검증 (BCrypt 해시 + 레거시 평문 호환)
        if (!validatePassword(req.password(), user)) {
            throw new CustomException(CustomErrorCode.WRONG_PASSWORD);
        }
        long t3 = System.nanoTime();

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        long t4 = System.nanoTime();

        upsertRefreshToken(user.getId(), refreshToken);
        long t5 = System.nanoTime();

        addSetCookie(res, cookieProvider.createAccessTokenCookie(accessToken, jwtProperties.accessExpireSeconds()));
        addSetCookie(res, cookieProvider.createRefreshTokenCookie(refreshToken, jwtProperties.refreshExpireSeconds()));
        long t6 = System.nanoTime();

        log.info(
                "auth_login_timing email={} findByEmail={}ms validatePassword={}ms createTokens={}ms upsertRefreshToken={}ms setCookie={}ms total={}ms",
                req.email(),
                (t2 - t1) / 1_000_000.0,
                (t3 - t2) / 1_000_000.0,
                (t4 - t3) / 1_000_000.0,
                (t5 - t4) / 1_000_000.0,
                (t6 - t5) / 1_000_000.0,
                (t6 - start) / 1_000_000.0
        );

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

        String savedToken = refreshTokenStore.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.UNAUTHORIZED_USER));

        if (!savedToken.equals(refreshToken)) {
            throw new CustomException(CustomErrorCode.UNAUTHORIZED_USER);
        }

        String newAccessToken = jwtProvider.createAccessToken(userId);
        addSetCookie(res, cookieProvider.createAccessTokenCookie(newAccessToken, jwtProperties.accessExpireSeconds()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_USER));

        return new RefreshResponse(
                new AuthUserSummary(user.getId(), user.getEmail(), user.getNickname()),
                newAccessToken
        );
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String refreshToken = extractCookie(req, AuthCookieProvider.REFRESH_TOKEN_COOKIE);

        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                jwtProvider.validate(refreshToken);
                Long userId = jwtProvider.getUserId(refreshToken);
                refreshTokenStore.deleteByUserId(userId);
            } catch (Exception e) {
                // 로그아웃은 조용히 처리 (토큰이 깨져도 쿠키 만료는 진행)
            }
        }

        addSetCookie(res, cookieProvider.expireAccessTokenCookie());
        addSetCookie(res, cookieProvider.expireRefreshTokenCookie());
    }

    private void upsertRefreshToken(Long userId, String refreshToken) {
        Duration ttl = Duration.ofSeconds(jwtProperties.refreshExpireSeconds());
        refreshTokenStore.save(userId, refreshToken, ttl);
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
