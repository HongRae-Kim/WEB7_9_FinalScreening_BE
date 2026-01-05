package com.back.matchduo.domain.user.service;

import com.back.matchduo.domain.user.dto.request.UserUpdatePasswordRequest;
import com.back.matchduo.domain.user.dto.response.UserProfileResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService 테스트")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .nickname("테스터")
                .verificationCode("VERIFIED")
                .build();
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetProfileTest {

        @Test
        @DisplayName("성공: 프로필 조회")
        void getProfile_success() {
            // given
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));

            // when
            UserProfileResponse response = userProfileService.getProfile(testUser);

            // then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(testUser.getEmail());
            assertThat(response.nickname()).isEqualTo(testUser.getNickname());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void getProfile_fail_userNotFound() {
            // given
            given(userRepository.findById(testUser.getId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userProfileService.getProfile(testUser))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("닉네임 수정")
    class UpdateNicknameTest {

        @Test
        @DisplayName("성공: 정상적인 닉네임 수정")
        void updateNickname_success() {
            // given
            String newNickname = "새닉네임";
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(userRepository.existsByNickname(newNickname)).willReturn(false);

            // when
            userProfileService.updateNickname(testUser, newNickname);

            // then
            assertThat(testUser.getNickname()).isEqualTo(newNickname);
            assertThat(testUser.getNicknameUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패: null 닉네임")
        void updateNickname_fail_null() {
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, null))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 빈 닉네임")
        void updateNickname_fail_empty() {
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, "  "))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 형식에 맞지 않는 닉네임 (1글자)")
        void updateNickname_fail_tooShort() {
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, "가"))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 형식에 맞지 않는 닉네임 (9글자 이상)")
        void updateNickname_fail_tooLong() {
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, "닉네임이너무길어요"))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 비속어 포함")
        void updateNickname_fail_bannedWord() {
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, "병신테스트"))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 중복된 닉네임")
        void updateNickname_fail_duplicate() {
            // given
            String duplicateNickname = "중복닉네임";
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(userRepository.existsByNickname(duplicateNickname)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userProfileService.updateNickname(testUser, duplicateNickname))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("자기소개 수정")
    class UpdateCommentTest {

        @BeforeEach
        void setUp() {
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        }

        @Test
        @DisplayName("성공: 자기소개 수정")
        void updateComment_success() {
            // given
            String newComment = "새로운 자기소개입니다.";

            // when
            userProfileService.updateComment(testUser, newComment);

            // then
            assertThat(testUser.getComment()).isEqualTo(newComment);
        }

        @Test
        @DisplayName("성공: 자기소개 삭제 (null)")
        void updateComment_success_null() {
            // when
            userProfileService.updateComment(testUser, null);

            // then
            assertThat(testUser.getComment()).isNull();
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePasswordTest {

        @Test
        @DisplayName("성공: 비밀번호 변경")
        void updatePassword_success() {
            // given
            String currentPassword = "currentPass123!";
            String newPassword = "newPassword123!";
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                    currentPassword, newPassword, newPassword
            );

            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(passwordEncoder.encode(newPassword)).willReturn("$2a$10$newEncodedPassword");

            // when
            userProfileService.updatePassword(testUser, request);

            // then
            verify(passwordEncoder).encode(newPassword);
        }

        @Test
        @DisplayName("실패: 새 비밀번호 불일치")
        void updatePassword_fail_mismatch() {
            // given
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                    "currentPass123!", "newPassword123!", "differentPassword123!"
            );

            // when & then
            assertThatThrownBy(() -> userProfileService.updatePassword(testUser, request))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 현재 비밀번호 틀림")
        void updatePassword_fail_wrongCurrentPassword() {
            // given
            String wrongPassword = "wrongPassword123!";
            String newPassword = "newPassword123!";
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                    wrongPassword, newPassword, newPassword
            );

            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(wrongPassword, testUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userProfileService.updatePassword(testUser, request))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패: 비밀번호 형식 오류")
        void updatePassword_fail_invalidFormat() {
            // given
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                    "currentPass123!", "short", "short"
            );

            // when & then
            assertThatThrownBy(() -> userProfileService.updatePassword(testUser, request))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("프로필 이미지 수정")
    class UpdateProfileImageTest {

        @Mock
        private MultipartFile mockFile;

        @BeforeEach
        void setUp() {
            given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        }

        @Test
        @DisplayName("성공: 프로필 이미지 업로드")
        void updateProfileImage_success() {
            // given
            String newImageUrl = "/uploads/profile/new-image.png";
            given(fileStorageService.upload(mockFile)).willReturn(newImageUrl);

            // when
            userProfileService.updateProfileImage(testUser, mockFile);

            // then
            assertThat(testUser.getProfileImage()).isEqualTo(newImageUrl);
            verify(fileStorageService).upload(mockFile);
        }

        @Test
        @DisplayName("성공: 기존 이미지 삭제 후 새 이미지 업로드")
        void updateProfileImage_success_deleteOldImage() {
            // given
            String oldImageUrl = "/uploads/profile/old-image.png";
            testUser.updateProfileImage(oldImageUrl);

            String newImageUrl = "/uploads/profile/new-image.png";
            given(fileStorageService.upload(mockFile)).willReturn(newImageUrl);

            // when
            userProfileService.updateProfileImage(testUser, mockFile);

            // then
            verify(fileStorageService).delete(oldImageUrl);
            verify(fileStorageService).upload(mockFile);
            assertThat(testUser.getProfileImage()).isEqualTo(newImageUrl);
        }
    }
}