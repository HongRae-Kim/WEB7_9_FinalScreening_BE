package com.back.matchduo.domain.post.service;

import com.back.matchduo.domain.party.entity.Party;
import com.back.matchduo.domain.party.repository.PartyRepository;
import com.back.matchduo.domain.post.dto.request.PostStatusUpdateRequest;
import com.back.matchduo.domain.post.dto.request.PostUpdateRequest;
import com.back.matchduo.domain.post.dto.response.PostDeleteResponse;
import com.back.matchduo.domain.post.dto.response.PostStatusUpdateResponse;
import com.back.matchduo.domain.post.dto.response.PostUpdateResponse;
import com.back.matchduo.domain.post.entity.*;
import com.back.matchduo.domain.post.repository.PostRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 단위 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PostValidator postValidator;

    @Mock
    private PostListFacade postListFacade;

    @InjectMocks
    private PostService postService;

    private User user;
    private User otherUser;
    private Post post;
    private Party party;

    @BeforeEach
    void setUp() {
        user = createUserWithId(1L, "test@test.com", "테스트유저");
        otherUser = createUserWithId(2L, "other@test.com", "다른유저");
        post = createPostWithId(100L, user);
        party = createPartyWithId(1L, 100L, 1L);
    }

    private User createUserWithId(Long id, String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .verificationCode("VERIFIED")
                .build();
        setId(user, id, User.class);
        return user;
    }

    private Post createPostWithId(Long id, User user) {
        Post post = Post.builder()
                .user(user)
                .gameMode(GameMode.SUMMONERS_RIFT)
                .queueType(QueueType.DUO)
                .myPosition(Position.TOP)
                .lookingPositions("[\"JUNGLE\"]")
                .mic(true)
                .recruitCount(2)
                .memo("테스트 모집글")
                .build();
        setId(post, id, Post.class);
        return post;
    }

    private Party createPartyWithId(Long id, Long postId, Long leaderId) {
        Party party = new Party(postId, leaderId);
        setId(party, id, Party.class);
        return party;
    }

    private void setId(Object entity, Long id, Class<?> clazz) {
        try {
            var idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setIsActive(Post post, boolean isActive) {
        try {
            var field = post.getClass().getSuperclass().getSuperclass().getDeclaredField("isActive");
            field.setAccessible(true);
            field.set(post, isActive);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("모집글 수정 테스트")
    class UpdatePostTest {

        @Test
        @DisplayName("성공: 작성자가 모집글 수정")
        void updatePost_success() {
            // given
            PostUpdateRequest request = new PostUpdateRequest(
                    Position.MID,
                    List.of(Position.ADC),
                    QueueType.DUO,
                    false,
                    2,
                    "수정된 메모"
            );
            PostUpdateResponse expectedResponse = mock(PostUpdateResponse.class);

            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);
            given(postListFacade.updatePostWithPartyView(post, request)).willReturn(expectedResponse);

            // when
            PostUpdateResponse result = postService.updatePost(100L, request, 1L);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(postValidator).validatePostOwner(post, 1L);
            verify(postListFacade).updatePostWithPartyView(post, request);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 모집글")
        void updatePost_fail_not_found() {
            // given
            PostUpdateRequest request = new PostUpdateRequest(
                    Position.MID, List.of(Position.ADC), QueueType.DUO, false, 2, "메모"
            );
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.updatePost(999L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 이미 삭제된 모집글")
        void updatePost_fail_already_deleted() {
            // given
            setIsActive(post, false);
            PostUpdateRequest request = new PostUpdateRequest(
                    Position.MID, List.of(Position.ADC), QueueType.DUO, false, 2, "메모"
            );
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.updatePost(100L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_ALREADY_DELETED);
                    });
        }

        @Test
        @DisplayName("실패: 작성자가 아닌 유저가 수정 시도")
        void updatePost_fail_not_owner() {
            // given
            PostUpdateRequest request = new PostUpdateRequest(
                    Position.MID, List.of(Position.ADC), QueueType.DUO, false, 2, "메모"
            );
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doThrow(new CustomException(CustomErrorCode.POST_FORBIDDEN))
                    .when(postValidator).validatePostOwner(post, 2L);

            // when & then
            assertThatThrownBy(() -> postService.updatePost(100L, request, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_FORBIDDEN);
                    });
        }
    }

    @Nested
    @DisplayName("모집글 상태 변경 테스트")
    class UpdatePostStatusTest {

        @Test
        @DisplayName("성공: 작성자가 상태를 CLOSED로 변경")
        void updatePostStatus_success() {
            // given
            PostStatusUpdateRequest request = new PostStatusUpdateRequest(PostStatus.CLOSED);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);
            doNothing().when(postValidator).validateStatusUpdateAllowed(PostStatus.CLOSED);

            // when
            PostStatusUpdateResponse result = postService.updatePostStatus(100L, request, 1L);

            // then
            assertThat(result.postId()).isEqualTo(100L);
            assertThat(result.status()).isEqualTo(PostStatus.CLOSED);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 모집글")
        void updatePostStatus_fail_not_found() {
            // given
            PostStatusUpdateRequest request = new PostStatusUpdateRequest(PostStatus.CLOSED);
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.updatePostStatus(999L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: CLOSED 외 상태로 변경 시도")
        void updatePostStatus_fail_invalid_status() {
            // given
            PostStatusUpdateRequest request = new PostStatusUpdateRequest(PostStatus.RECRUIT);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);
            doThrow(new CustomException(CustomErrorCode.INVALID_POST_STATUS_UPDATE))
                    .when(postValidator).validateStatusUpdateAllowed(PostStatus.RECRUIT);

            // when & then
            assertThatThrownBy(() -> postService.updatePostStatus(100L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.INVALID_POST_STATUS_UPDATE);
                    });
        }
    }

    @Nested
    @DisplayName("모집글 단건 조회 테스트")
    class GetPostDetailTest {

        @Test
        @DisplayName("성공: 모집글 상세 조회")
        void getPostDetail_success() {
            // given
            PostUpdateResponse expectedResponse = mock(PostUpdateResponse.class);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            given(postListFacade.buildPostDetailForEdit(post)).willReturn(expectedResponse);

            // when
            PostUpdateResponse result = postService.getPostDetail(100L, 1L);

            // then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 모집글")
        void getPostDetail_fail_not_found() {
            // given
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.getPostDetail(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 이미 삭제된 모집글")
        void getPostDetail_fail_already_deleted() {
            // given
            setIsActive(post, false);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.getPostDetail(100L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_ALREADY_DELETED);
                    });
        }
    }

    @Nested
    @DisplayName("모집글 삭제 테스트")
    class DeletePostTest {

        @Test
        @DisplayName("성공: 작성자가 모집글 삭제")
        void deletePost_success() {
            // given
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);
            given(partyRepository.findByPostId(100L)).willReturn(Optional.of(party));

            // when
            PostDeleteResponse result = postService.deletePost(100L, 1L);

            // then
            assertThat(result.postId()).isEqualTo(100L);
            assertThat(result.deleted()).isTrue();
            verify(postRepository).save(post);
            verify(partyRepository).save(party);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 모집글")
        void deletePost_fail_not_found() {
            // given
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deletePost(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("실패: 작성자가 아닌 유저가 삭제 시도")
        void deletePost_fail_not_owner() {
            // given
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doThrow(new CustomException(CustomErrorCode.POST_FORBIDDEN))
                    .when(postValidator).validatePostOwner(post, 2L);

            // when & then
            assertThatThrownBy(() -> postService.deletePost(100L, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_FORBIDDEN);
                    });
        }

        @Test
        @DisplayName("실패: 이미 삭제된 모집글")
        void deletePost_fail_already_deleted() {
            // given
            setIsActive(post, false);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);

            // when & then
            assertThatThrownBy(() -> postService.deletePost(100L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.POST_ALREADY_DELETED);
                    });
        }

        @Test
        @DisplayName("실패: 연관된 파티가 없음")
        void deletePost_fail_party_not_found() {
            // given
            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            doNothing().when(postValidator).validatePostOwner(post, 1L);
            given(partyRepository.findByPostId(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deletePost(100L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.PARTY_NOT_FOUND);
                    });
        }
    }
}