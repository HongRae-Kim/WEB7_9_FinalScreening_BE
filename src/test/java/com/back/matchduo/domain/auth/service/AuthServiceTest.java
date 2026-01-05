package com.back.matchduo.domain.auth.service;

import com.back.matchduo.domain.auth.dto.request.LoginRequest;
import com.back.matchduo.domain.auth.dto.response.LoginResponse;
import com.back.matchduo.domain.auth.refresh.repository.RefreshTokenRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.config.JwtProperties;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import com.back.matchduo.global.security.cookie.AuthCookieProvider;
import com.back.matchduo.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthCookieProvider cookieProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock(lenient = true)
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .password("password123")
                .nickname("테스터")
                .verificationCode("VERIFIED")
                .build();

        // Reflection으로 ID 설정 (테스트용)
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("성공: 올바른 이메일과 비밀번호로 로그인")
        void login_success() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "password123");

            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));
            given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
            given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");
            given(jwtProperties.accessExpireSeconds()).willReturn(3600L);
            given(jwtProperties.refreshExpireSeconds()).willReturn(604800L);
            given(cookieProvider.createAccessTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("accessToken", "access-token").build());
            given(cookieProvider.createRefreshTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("refreshToken", "refresh-token").build());
            given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());
            given(refreshTokenRepository.save(any())).willReturn(null);

            // when
            LoginResponse result = authService.login(request, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.user().userId()).isEqualTo(1L);
            assertThat(result.user().email()).isEqualTo("test@test.com");
            assertThat(result.user().nickname()).isEqualTo("테스터");
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");

            verify(userRepository).findByEmail("test@test.com");
            verify(jwtProvider).createAccessToken(1L);
            verify(jwtProvider).createRefreshToken(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일")
        void login_fail_email_not_found() {
            // given
            LoginRequest request = new LoginRequest("notexist@test.com", "password123");
            given(userRepository.findByEmail("notexist@test.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.NOT_FOUND_EMAIL);
                    });

            verify(userRepository).findByEmail("notexist@test.com");
            verify(jwtProvider, never()).createAccessToken(anyLong());
        }

        @Test
        @DisplayName("실패: 잘못된 비밀번호")
        void login_fail_wrong_password() {
            // given
            LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.WRONG_PASSWORD);
                    });

            verify(userRepository).findByEmail("test@test.com");
            verify(jwtProvider, never()).createAccessToken(anyLong());
        }

        @Test
        @DisplayName("성공: BCrypt 해시 비밀번호로 로그인")
        void login_success_with_bcrypt_password() {
            // given
            String bcryptPassword = "$2a$10$abcdefghijklmnopqrstuvwxyz123456";
            User bcryptUser = User.builder()
                    .email("bcrypt@test.com")
                    .password(bcryptPassword)
                    .nickname("BCrypt유저")
                    .verificationCode("VERIFIED")
                    .build();

            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(bcryptUser, 2L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            LoginRequest request = new LoginRequest("bcrypt@test.com", "password123");

            given(userRepository.findByEmail("bcrypt@test.com")).willReturn(Optional.of(bcryptUser));
            given(passwordEncoder.matches("password123", bcryptPassword)).willReturn(true);
            given(jwtProvider.createAccessToken(2L)).willReturn("access-token");
            given(jwtProvider.createRefreshToken(2L)).willReturn("refresh-token");
            given(jwtProperties.accessExpireSeconds()).willReturn(3600L);
            given(jwtProperties.refreshExpireSeconds()).willReturn(604800L);
            given(cookieProvider.createAccessTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("accessToken", "access-token").build());
            given(cookieProvider.createRefreshTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("refreshToken", "refresh-token").build());
            given(refreshTokenRepository.findByUserId(2L)).willReturn(Optional.empty());
            given(refreshTokenRepository.save(any())).willReturn(null);

            // when
            LoginResponse result = authService.login(request, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.user().userId()).isEqualTo(2L);
            assertThat(result.user().email()).isEqualTo("bcrypt@test.com");

            verify(passwordEncoder).matches("password123", bcryptPassword);
            verify(jwtProvider).createAccessToken(2L);
        }

        @Test
        @DisplayName("실패: BCrypt 해시 비밀번호 불일치")
        void login_fail_bcrypt_password_mismatch() {
            // given
            String bcryptPassword = "$2a$10$abcdefghijklmnopqrstuvwxyz123456";
            User bcryptUser = User.builder()
                    .email("bcrypt@test.com")
                    .password(bcryptPassword)
                    .nickname("BCrypt유저")
                    .verificationCode("VERIFIED")
                    .build();

            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(bcryptUser, 2L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            LoginRequest request = new LoginRequest("bcrypt@test.com", "wrongPassword");

            given(userRepository.findByEmail("bcrypt@test.com")).willReturn(Optional.of(bcryptUser));
            given(passwordEncoder.matches("wrongPassword", bcryptPassword)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.WRONG_PASSWORD);
                    });

            verify(passwordEncoder).matches("wrongPassword", bcryptPassword);
            verify(jwtProvider, never()).createAccessToken(anyLong());
        }

        @Test
        @DisplayName("성공: 레거시 평문 비밀번호 로그인 시 BCrypt로 마이그레이션")
        void login_success_with_legacy_password_migration() {
            // given
            String plainPassword = "password123";
            String encodedPassword = "$2a$10$newEncodedPassword";

            User legacyUser = User.builder()
                    .email("legacy@test.com")
                    .password(plainPassword)
                    .nickname("레거시유저")
                    .verificationCode("VERIFIED")
                    .build();

            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(legacyUser, 3L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            LoginRequest request = new LoginRequest("legacy@test.com", plainPassword);

            given(userRepository.findByEmail("legacy@test.com")).willReturn(Optional.of(legacyUser));
            given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);
            given(jwtProvider.createAccessToken(3L)).willReturn("access-token");
            given(jwtProvider.createRefreshToken(3L)).willReturn("refresh-token");
            given(jwtProperties.accessExpireSeconds()).willReturn(3600L);
            given(jwtProperties.refreshExpireSeconds()).willReturn(604800L);
            given(cookieProvider.createAccessTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("accessToken", "access-token").build());
            given(cookieProvider.createRefreshTokenCookie(any(), anyLong()))
                    .willReturn(ResponseCookie.from("refreshToken", "refresh-token").build());
            given(refreshTokenRepository.findByUserId(3L)).willReturn(Optional.empty());
            given(refreshTokenRepository.save(any())).willReturn(null);

            // when
            LoginResponse result = authService.login(request, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.user().userId()).isEqualTo(3L);

            // 평문 비밀번호가 BCrypt로 마이그레이션되었는지 확인
            verify(passwordEncoder).encode(plainPassword);
            assertThat(legacyUser.getPassword()).isEqualTo(encodedPassword);
        }
    }
}