package com.back.matchduo.domain.auth.controller;

import com.back.matchduo.domain.auth.dto.request.LoginRequest;
import com.back.matchduo.domain.auth.refresh.entity.RefreshToken;
import com.back.matchduo.domain.auth.refresh.repository.RefreshTokenRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.security.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Auth API 통합 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User bcryptUser;

    private static final String TEST_EMAIL = "auth-test@test.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NICKNAME = "인증테스터";

    private static final String BCRYPT_EMAIL = "bcrypt-test@test.com";
    private static final String BCRYPT_PASSWORD = "securePassword123";
    private static final String BCRYPT_NICKNAME = "BCrypt테스터";

    @BeforeAll
    void setUp() {
        testUser = User.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .nickname(TEST_NICKNAME)
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(testUser);
    }

    @Nested
    @DisplayName("로그인 API (POST /api/v1/auth/login)")
    class Login {

        @Test
        @DisplayName("성공: 올바른 이메일과 비밀번호로 로그인")
        void success_login() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.userId").value(testUser.getId()))
                    .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.user.nickname").value(TEST_NICKNAME))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 로그인 시 쿠키에 토큰이 설정된다")
        void success_login_sets_cookies() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            // when
            MvcResult result = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            ).andReturn();

            // then
            // Set-Cookie 헤더가 여러 개일 수 있으므로 getHeaders 사용
            var setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
            assertThat(setCookieHeaders).isNotEmpty();
            assertThat(setCookieHeaders.stream().anyMatch(h -> h.contains("accessToken"))).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일로 로그인 시 404 Not Found")
        void fail_email_not_found() throws Exception {
            // given
            LoginRequest request = new LoginRequest("notexist@test.com", TEST_PASSWORD);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND_EMAIL"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 잘못된 비밀번호로 로그인 시 401 Unauthorized")
        void fail_wrong_password() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 API (POST /api/v1/auth/refresh)")
    class Refresh {

        @Test
        @DisplayName("성공: 유효한 RefreshToken으로 AccessToken 갱신")
        void success_refresh() throws Exception {
            // given
            String refreshToken = jwtProvider.createRefreshToken(testUser.getId());

            // DB에 RefreshToken 저장
            RefreshToken savedToken = RefreshToken.create(
                    testUser.getId(),
                    refreshToken,
                    LocalDateTime.now().plusDays(7)
            );
            refreshTokenRepository.save(savedToken);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/refresh")
                            .cookie(refreshCookie)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.userId").value(testUser.getId()))
                    .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(header().exists("Set-Cookie"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: RefreshToken 쿠키 없이 요청 시 401 Unauthorized")
        void fail_no_refresh_token() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/refresh")
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_USER"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 유효하지 않은 RefreshToken으로 요청 시 401 Unauthorized")
        void fail_invalid_refresh_token() throws Exception {
            // given
            Cookie invalidCookie = new Cookie("refreshToken", "invalid.token.here");

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/refresh")
                            .cookie(invalidCookie)
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_USER"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: DB에 저장되지 않은 RefreshToken으로 요청 시 401 Unauthorized")
        void fail_refresh_token_not_in_db() throws Exception {
            // given
            // 유효한 토큰이지만 DB에 저장하지 않음
            String refreshToken = jwtProvider.createRefreshToken(testUser.getId());
            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/refresh")
                            .cookie(refreshCookie)
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_USER"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("로그아웃 API (POST /api/v1/auth/logout)")
    class Logout {

        @Test
        @DisplayName("성공: 로그아웃 시 쿠키가 만료된다")
        void success_logout() throws Exception {
            // given
            String refreshToken = jwtProvider.createRefreshToken(testUser.getId());

            // DB에 RefreshToken 저장
            RefreshToken savedToken = RefreshToken.create(
                    testUser.getId(),
                    refreshToken,
                    LocalDateTime.now().plusDays(7)
            );
            refreshTokenRepository.save(savedToken);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/logout")
                            .cookie(refreshCookie)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andDo(print());

            // DB에서 RefreshToken 삭제 확인
            assertThat(refreshTokenRepository.findByUserId(testUser.getId())).isEmpty();
        }

        @Test
        @DisplayName("성공: RefreshToken 없이 로그아웃해도 정상 처리된다")
        void success_logout_without_token() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/logout")
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("비밀번호 암호화 통합 테스트")
    class PasswordEncryption {

        @Test
        @DisplayName("성공: BCrypt 해시 비밀번호로 로그인")
        void success_login_with_bcrypt_password() throws Exception {
            // given
            String encodedPassword = passwordEncoder.encode(BCRYPT_PASSWORD);
            bcryptUser = User.builder()
                    .email(BCRYPT_EMAIL)
                    .password(encodedPassword)
                    .nickname(BCRYPT_NICKNAME)
                    .verificationCode("VERIFIED")
                    .build();
            userRepository.save(bcryptUser);

            LoginRequest request = new LoginRequest(BCRYPT_EMAIL, BCRYPT_PASSWORD);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.userId").value(bcryptUser.getId()))
                    .andExpect(jsonPath("$.user.email").value(BCRYPT_EMAIL))
                    .andExpect(jsonPath("$.user.nickname").value(BCRYPT_NICKNAME))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: BCrypt 해시 비밀번호 불일치")
        void fail_login_with_wrong_bcrypt_password() throws Exception {
            // given
            String encodedPassword = passwordEncoder.encode(BCRYPT_PASSWORD);
            User user = User.builder()
                    .email("bcrypt-wrong@test.com")
                    .password(encodedPassword)
                    .nickname("BCrypt오류테스터")
                    .verificationCode("VERIFIED")
                    .build();
            userRepository.save(user);

            LoginRequest request = new LoginRequest("bcrypt-wrong@test.com", "wrongPassword");

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"))
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 레거시 평문 비밀번호 로그인 후 BCrypt로 마이그레이션")
        void success_login_with_legacy_password_and_migration() throws Exception {
            // given
            String plainPassword = "legacyPassword123";
            User legacyUser = User.builder()
                    .email("legacy@test.com")
                    .password(plainPassword)  // 평문 비밀번호
                    .nickname("레거시유저")
                    .verificationCode("VERIFIED")
                    .build();
            userRepository.save(legacyUser);

            LoginRequest request = new LoginRequest("legacy@test.com", plainPassword);

            // when - 첫 번째 로그인 (마이그레이션 발생)
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value("legacy@test.com"))
                    .andDo(print());

            // 비밀번호가 BCrypt로 마이그레이션되었는지 확인
            User updatedUser = userRepository.findByEmail("legacy@test.com").orElseThrow();
            assertThat(updatedUser.getPassword()).startsWith("$2");
            assertThat(passwordEncoder.matches(plainPassword, updatedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("성공: 마이그레이션 후 BCrypt 비밀번호로 재로그인")
        void success_relogin_after_migration() throws Exception {
            // given
            String plainPassword = "migrationTest123";
            User legacyUser = User.builder()
                    .email("migration-relogin@test.com")
                    .password(plainPassword)  // 평문 비밀번호
                    .nickname("마이그레이션재로그인")
                    .verificationCode("VERIFIED")
                    .build();
            userRepository.save(legacyUser);

            LoginRequest request = new LoginRequest("migration-relogin@test.com", plainPassword);

            // 첫 번째 로그인 (마이그레이션 발생)
            mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isOk());

            // when - 두 번째 로그인 (BCrypt 비밀번호로)
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value("migration-relogin@test.com"))
                    .andDo(print());
        }
    }
}