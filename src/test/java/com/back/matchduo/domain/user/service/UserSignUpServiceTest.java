package com.back.matchduo.domain.user.service;

import com.back.matchduo.domain.user.dto.request.UserSignUpRequest;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.exeption.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSignUpService 테스트")
class UserSignUpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSignUpService userSignUpService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";

    @Nested
    @DisplayName("회원가입")
    class SignUpTest {

        @Test
        @DisplayName("성공: 정상적인 회원가입")
        void signUp_success() {
            // given
            UserSignUpRequest request = new UserSignUpRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD, "123456"
            );

            given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
            given(emailService.isVerified(TEST_EMAIL)).willReturn(true);
            given(passwordEncoder.encode(TEST_PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            userSignUpService.signUp(request);

            // then
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 중복된 이메일")
        void signUp_fail_duplicateEmail() {
            // given
            UserSignUpRequest request = new UserSignUpRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD, "123456"
            );

            given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userSignUpService.signUp(request))
                    .isInstanceOf(CustomException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 이메일 미인증")
        void signUp_fail_emailNotVerified() {
            // given
            UserSignUpRequest request = new UserSignUpRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD, "123456"
            );

            given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
            given(emailService.isVerified(TEST_EMAIL)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userSignUpService.signUp(request))
                    .isInstanceOf(CustomException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치")
        void signUp_fail_passwordMismatch() {
            // given
            UserSignUpRequest request = new UserSignUpRequest(
                    TEST_EMAIL, TEST_PASSWORD, "differentPassword!", "123456"
            );

            given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
            given(emailService.isVerified(TEST_EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userSignUpService.signUp(request))
                    .isInstanceOf(CustomException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("성공: 닉네임 중복 시 숫자 추가")
        void signUp_success_duplicateNickname() {
            // given
            UserSignUpRequest request = new UserSignUpRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD, "123456"
            );

            given(userRepository.existsByEmail(TEST_EMAIL)).willReturn(false);
            given(emailService.isVerified(TEST_EMAIL)).willReturn(true);
            given(passwordEncoder.encode(TEST_PASSWORD)).willReturn(ENCODED_PASSWORD);
            // 첫 번째 닉네임은 중복, 두 번째는 사용 가능
            given(userRepository.existsByNickname("test")).willReturn(true);
            given(userRepository.existsByNickname("test_1")).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            userSignUpService.signUp(request);

            // then
            verify(userRepository).save(any(User.class));
        }
    }
}
