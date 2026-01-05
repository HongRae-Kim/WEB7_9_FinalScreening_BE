package com.back.matchduo.domain.auth.service;

import com.back.matchduo.domain.auth.dto.request.LoginRequest;
import com.back.matchduo.domain.auth.dto.response.LoginResponse;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthService 통합 테스트")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private HttpServletResponse response;

    @MockitoBean
    private com.back.matchduo.global.security.cookie.AuthCookieProvider cookieProvider;

    @Test
    @Transactional
    @DisplayName("레거시 평문 비밀번호 로그인 시 BCrypt로 마이그레이션되어 DB에 저장된다")
    void login_legacy_password_migration_to_db() {
        // given
        String plainPassword = "testPassword123";

        User legacyUser = User.builder()
                .email("legacy-test@test.com")
                .password(plainPassword)  // 평문 비밀번호로 저장
                .nickname("레거시테스트")
                .verificationCode("VERIFIED")
                .build();

        userRepository.save(legacyUser);

        System.out.println("========== 레거시 비밀번호 마이그레이션 테스트 ==========");
        System.out.println("[마이그레이션 전] 저장된 비밀번호: " + legacyUser.getPassword());

        // Mock 설정 (쿠키 관련)
        given(cookieProvider.createAccessTokenCookie(anyString(), anyLong()))
                .willReturn(ResponseCookie.from("accessToken", "token").build());
        given(cookieProvider.createRefreshTokenCookie(anyString(), anyLong()))
                .willReturn(ResponseCookie.from("refreshToken", "token").build());

        LoginRequest request = new LoginRequest("legacy-test@test.com", plainPassword);

        // when
        LoginResponse result = authService.login(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.user().email()).isEqualTo("legacy-test@test.com");

        // DB에서 다시 조회하여 비밀번호가 BCrypt로 변경되었는지 확인
        User updatedUser = userRepository.findByEmail("legacy-test@test.com").orElseThrow();

        System.out.println("[마이그레이션 후] 저장된 비밀번호: " + updatedUser.getPassword());
        System.out.println("[검증] BCrypt 형식 여부: " + updatedUser.getPassword().startsWith("$2"));
        System.out.println("[검증] 비밀번호 매칭: " + passwordEncoder.matches(plainPassword, updatedUser.getPassword()));
        System.out.println("=====================================================");

        // BCrypt 해시는 $2로 시작
        assertThat(updatedUser.getPassword()).startsWith("$2");

        // 변경된 비밀번호로 검증 가능한지 확인
        assertThat(passwordEncoder.matches(plainPassword, updatedUser.getPassword())).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("BCrypt 비밀번호 유저는 마이그레이션 없이 로그인 성공")
    void login_bcrypt_password_no_migration() {
        // given
        String plainPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(plainPassword);

        User bcryptUser = User.builder()
                .email("bcrypt-test@test.com")
                .password(encodedPassword)  // BCrypt로 저장
                .nickname("BCrypt테스트")
                .verificationCode("VERIFIED")
                .build();

        userRepository.save(bcryptUser);

        // Mock 설정
        given(cookieProvider.createAccessTokenCookie(anyString(), anyLong()))
                .willReturn(ResponseCookie.from("accessToken", "token").build());
        given(cookieProvider.createRefreshTokenCookie(anyString(), anyLong()))
                .willReturn(ResponseCookie.from("refreshToken", "token").build());

        LoginRequest request = new LoginRequest("bcrypt-test@test.com", plainPassword);

        // when
        LoginResponse result = authService.login(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.user().email()).isEqualTo("bcrypt-test@test.com");

        // 비밀번호가 변경되지 않았는지 확인
        User unchangedUser = userRepository.findByEmail("bcrypt-test@test.com").orElseThrow();
        assertThat(unchangedUser.getPassword()).isEqualTo(encodedPassword);
    }
}