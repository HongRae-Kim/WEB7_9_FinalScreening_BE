package com.back.matchduo.domain.review.service;

import com.back.matchduo.domain.party.entity.Party;
import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.review.dto.request.ReviewCreateRequest;
import com.back.matchduo.domain.review.dto.response.ReviewCreateResponse;
import com.back.matchduo.domain.review.dto.response.ReviewDistributionResponse;
import com.back.matchduo.domain.review.dto.response.ReviewListResponse;
import com.back.matchduo.domain.review.entity.Review;
import com.back.matchduo.domain.review.entity.ReviewRequest;
import com.back.matchduo.domain.review.enums.ReviewEmoji;
import com.back.matchduo.domain.review.enums.ReviewRequestStatus;
import com.back.matchduo.domain.review.repository.ReviewRepository;
import com.back.matchduo.domain.review.repository.ReviewRequestRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewRequestRepository reviewRequestRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User reviewer;
    private User reviewee;
    private Party party;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        reviewer = createUserWithId(1L, "reviewer@test.com", "리뷰작성자");
        reviewee = createUserWithId(2L, "reviewee@test.com", "리뷰대상자");
        party = createPartyWithId(1L, 100L, 1L);
        reviewRequest = createReviewRequest(1L, party, reviewer, ReviewRequestStatus.COMPLETED);
    }

    private User createUserWithId(Long id, String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password("password123")
                .nickname(nickname)
                .verificationCode("VERIFIED")
                .build();
        setId(user, id);
        return user;
    }

    private Party createPartyWithId(Long id, Long postId, Long leaderId) {
        Party party = new Party(postId, leaderId);
        setId(party, id);
        return party;
    }

    private ReviewRequest createReviewRequest(Long id, Party party, User requestUser, ReviewRequestStatus status) {
        ReviewRequest request = ReviewRequest.builder()
                .party(party)
                .requestUser(requestUser)
                .build();
        if (status == ReviewRequestStatus.COMPLETED) {
            request.complete();
        }
        setId(request, id);
        return request;
    }

    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("리뷰 작성 테스트")
    class CreateReviewTest {

        @Test
        @DisplayName("성공: 유효한 리뷰 작성")
        void createReview_success() {
            // given
            ReviewCreateRequest request = new ReviewCreateRequest(1L, 2L, ReviewEmoji.GOOD, "좋은 팀원이었습니다!");

            given(reviewRequestRepository.findByPartyIdAndRequestUserId(1L, 1L))
                    .willReturn(Optional.of(reviewRequest));
            given(reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(1L, 1L, 2L))
                    .willReturn(false);
            given(partyMemberRepository.existsByPartyIdAndUserId(1L, 2L))
                    .willReturn(true);
            given(userRepository.findById(2L))
                    .willReturn(Optional.of(reviewee));
            given(reviewRepository.save(any(Review.class)))
                    .willAnswer(invocation -> {
                        Review review = invocation.getArgument(0);
                        setId(review, 1L);
                        return review;
                    });
            given(partyMemberRepository.countByPartyId(1L)).willReturn(3L);
            given(reviewRepository.countByPartyIdAndReviewerId(1L, 1L)).willReturn(1L);

            // when
            ReviewCreateResponse result = reviewService.createReview(1L, request);

            // then
            assertThat(result).isNotNull();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("실패: 리뷰 요청이 COMPLETED 상태가 아님")
        void createReview_fail_not_completed() {
            // given
            ReviewRequest pendingRequest = ReviewRequest.builder()
                    .party(party)
                    .requestUser(reviewer)
                    .build();
            setId(pendingRequest, 1L);

            ReviewCreateRequest request = new ReviewCreateRequest(1L, 2L, ReviewEmoji.GOOD, "리뷰");

            given(reviewRequestRepository.findByPartyIdAndRequestUserId(1L, 1L))
                    .willReturn(Optional.of(pendingRequest));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.MATCH_NOT_END);
                    });
        }

        @Test
        @DisplayName("실패: 이미 리뷰를 작성함")
        void createReview_fail_already_written() {
            // given
            ReviewCreateRequest request = new ReviewCreateRequest(1L, 2L, ReviewEmoji.GOOD, "리뷰");

            given(reviewRequestRepository.findByPartyIdAndRequestUserId(1L, 1L))
                    .willReturn(Optional.of(reviewRequest));
            given(reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(1L, 1L, 2L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.REVIEW_ALREADY_WRITTEN);
                    });
        }

        @Test
        @DisplayName("실패: 리뷰 대상자가 파티원이 아님")
        void createReview_fail_not_party_member() {
            // given
            ReviewCreateRequest request = new ReviewCreateRequest(1L, 2L, ReviewEmoji.GOOD, "리뷰");

            given(reviewRequestRepository.findByPartyIdAndRequestUserId(1L, 1L))
                    .willReturn(Optional.of(reviewRequest));
            given(reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(1L, 1L, 2L))
                    .willReturn(false);
            given(partyMemberRepository.existsByPartyIdAndUserId(1L, 2L))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.PARTY_MEMBER_NOT_MATCH);
                    });
        }
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class GetReviewsTest {

        @Test
        @DisplayName("성공: 특정 유저가 받은 리뷰 목록 조회")
        void getReviewsReceivedByUser_success() {
            // given
            Review review = Review.builder()
                    .party(party)
                    .reviewer(reviewer)
                    .reviewee(reviewee)
                    .reviewRequest(reviewRequest)
                    .emoji(ReviewEmoji.GOOD)
                    .content("좋은 팀원!")
                    .build();
            setId(review, 1L);

            given(reviewRepository.findAllByRevieweeId(2L))
                    .willReturn(List.of(review));

            // when
            List<ReviewListResponse> result = reviewService.getReviewsReceivedByUser(2L);

            // then
            assertThat(result).hasSize(1);
            verify(reviewRepository).findAllByRevieweeId(2L);
        }

        @Test
        @DisplayName("성공: 리뷰가 없으면 빈 리스트 반환")
        void getReviewsReceivedByUser_empty() {
            // given
            given(reviewRepository.findAllByRevieweeId(999L))
                    .willReturn(List.of());

            // when
            List<ReviewListResponse> result = reviewService.getReviewsReceivedByUser(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("리뷰 분포 조회 테스트")
    class GetDistributionTest {

        @Test
        @DisplayName("성공: 리뷰 분포 비율 계산")
        void getReviewDistribution_success() {
            // given
            given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));
            given(reviewRepository.countReviewEmojisByRevieweeId(2L))
                    .willReturn(List.of(
                            new Object[]{ReviewEmoji.GOOD, 7L},
                            new Object[]{ReviewEmoji.NORMAL, 2L},
                            new Object[]{ReviewEmoji.BAD, 1L}
                    ));

            // when
            ReviewDistributionResponse result = reviewService.getReviewDistribution(2L);

            // then
            assertThat(result.totalReviews()).isEqualTo(10);
            assertThat(result.ratios().good()).isEqualTo(70.0);
            assertThat(result.ratios().normal()).isEqualTo(20.0);
            assertThat(result.ratios().bad()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저")
        void getReviewDistribution_fail_user_not_found() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getReviewDistribution(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.NOT_FOUND_USER);
                    });
        }
    }
}