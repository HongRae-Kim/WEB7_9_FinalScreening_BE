package com.back.matchduo.domain.review.controller;

import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.party.entity.Party;
import com.back.matchduo.domain.party.entity.PartyMember;
import com.back.matchduo.domain.party.entity.PartyMemberRole;
import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.party.repository.PartyRepository;
import com.back.matchduo.domain.post.entity.GameMode;
import com.back.matchduo.domain.post.entity.Position;
import com.back.matchduo.domain.post.entity.Post;
import com.back.matchduo.domain.post.entity.PostStatus;
import com.back.matchduo.domain.post.entity.QueueType;
import com.back.matchduo.domain.post.repository.PostRepository;
import com.back.matchduo.domain.review.dto.request.ReviewCreateRequest;
import com.back.matchduo.domain.review.entity.Review;
import com.back.matchduo.domain.review.entity.ReviewRequest;
import com.back.matchduo.domain.review.enums.ReviewEmoji;
import com.back.matchduo.domain.review.enums.ReviewRequestStatus;
import com.back.matchduo.domain.review.repository.ReviewRepository;
import com.back.matchduo.domain.review.repository.ReviewRequestRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Review API 통합 테스트")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private GameAccountRepository gameAccountRepository;

    private User reviewer;
    private User reviewee;
    private User thirdUser;
    private GameAccount testGameAccount;
    private Post testPost;
    private Party testParty;
    private ReviewRequest completedReviewRequest;
    private ReviewRequest pendingReviewRequest;
    private Review existingReview;

    @BeforeAll
    void setUp() {
        // 유저 생성
        reviewer = User.builder()
                .email("reviewer@test.com")
                .password("password123")
                .nickname("리뷰작성자")
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(reviewer);

        reviewee = User.builder()
                .email("reviewee@test.com")
                .password("password123")
                .nickname("리뷰대상자")
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(reviewee);

        thirdUser = User.builder()
                .email("third@test.com")
                .password("password123")
                .nickname("제3자")
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(thirdUser);

        // 테스트 게임 계정 생성
        testGameAccount = GameAccount.builder()
                .gameNickname("리뷰테스트게이머")
                .gameTag("KR1")
                .gameType("LOL")
                .puuid("review-test-puuid-12345")
                .profileIconId(1234)
                .user(reviewer)
                .build();
        gameAccountRepository.save(testGameAccount);

        // 모집글 생성 (CLOSED 상태 - 게임 종료)
        testPost = Post.builder()
                .user(reviewer)
                .gameAccount(testGameAccount)
                .gameMode(GameMode.SUMMONERS_RIFT)
                .queueType(QueueType.DUO)
                .myPosition(Position.TOP)
                .lookingPositions("[\"JUNGLE\"]")
                .mic(true)
                .recruitCount(2)
                .memo("리뷰 테스트용 모집글")
                .build();
        postRepository.save(testPost);
        testPost.updateStatus(PostStatus.CLOSED);
        postRepository.save(testPost);

        // 파티 생성 (CLOSED 상태)
        testParty = new Party(testPost.getId(), reviewer.getId());
        testParty.closeParty();
        partyRepository.save(testParty);

        // 파티 멤버 추가
        partyMemberRepository.save(new PartyMember(testParty, reviewer, PartyMemberRole.LEADER));
        partyMemberRepository.save(new PartyMember(testParty, reviewee, PartyMemberRole.MEMBER));

        // 리뷰 요청 생성 (COMPLETED - 리뷰 작성 가능)
        completedReviewRequest = ReviewRequest.builder()
                .party(testParty)
                .requestUser(reviewer)
                .build();
        completedReviewRequest.complete(); // COMPLETED 상태로 변경
        reviewRequestRepository.save(completedReviewRequest);

        // 리뷰 요청 생성 (PENDING - 리뷰 작성 불가)
        pendingReviewRequest = ReviewRequest.builder()
                .party(testParty)
                .requestUser(thirdUser)
                .build();
        // PENDING 상태 유지
        reviewRequestRepository.save(pendingReviewRequest);

        // 기존 리뷰 생성 (모든 리뷰 조회 테스트용)
        existingReview = Review.builder()
                .party(testParty)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .reviewRequest(completedReviewRequest)
                .emoji(ReviewEmoji.GOOD)
                .content("좋은 팀원이었습니다!")
                .build();
        reviewRepository.save(existingReview);
    }

    @Nested
    @DisplayName("모든 리뷰 조회 API (GET /api/v1/reviews)")
    class GetAllReviews {

        @Test
        @DisplayName("성공: 비로그인 상태에서 모든 리뷰 조회 (permitAll)")
        void success_get_all_reviews_guest() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].reviewId").exists())
                    .andExpect(jsonPath("$[0].reviewerNickname").exists())
                    .andExpect(jsonPath("$[0].emoji").exists())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 로그인 상태에서 모든 리뷰 조회")
        void success_get_all_reviews_authenticated() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(reviewer)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("특정 유저가 받은 리뷰 조회 API (GET /api/v1/reviews/users/{userId})")
    class GetReviewsByUser {

        @Test
        @DisplayName("성공: 비로그인 상태에서 특정 유저 리뷰 조회 (permitAll)")
        void success_get_user_reviews_guest() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/users/{userId}", reviewee.getId())
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 리뷰가 없는 유저 조회시 빈 배열 반환")
        void success_get_empty_reviews() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/users/{userId}", thirdUser.getId())
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("리뷰 요청 관리 목록 조회 API (GET /api/v1/reviews/requests)")
    class GetWritableReviewRequests {

        @Test
        @DisplayName("성공: COMPLETED 상태인 리뷰 요청만 조회된다")
        void success_get_completed_review_requests() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/requests")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(reviewer)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: PENDING 상태 요청만 있는 유저는 빈 배열 반환")
        void success_get_empty_when_only_pending() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/requests")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(thirdUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 비로그인 상태에서 접근시 401 Unauthorized")
        void fail_unauthorized() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/requests")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("내 리뷰 작성 목록 조회 API (GET /api/v1/reviews/me)")
    class GetMyWrittenReviews {

        @Test
        @DisplayName("성공: 내가 작성한 리뷰 목록 조회")
        void success_get_my_reviews() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/me")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(reviewer)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].revieweeNickname").value("리뷰대상자"))
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 작성한 리뷰가 없으면 빈 배열 반환")
        void success_get_empty_my_reviews() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/me")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(thirdUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 비로그인 상태에서 접근시 401 Unauthorized")
        void fail_unauthorized() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/me")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("리뷰 비율 분포 조회 API (GET /api/v1/reviews/users/{userId}/distribution)")
    class GetReviewDistribution {

        @Test
        @DisplayName("성공: 비로그인 상태에서 리뷰 분포 조회 (permitAll)")
        void success_get_distribution_guest() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/reviews/users/{userId}/distribution", reviewee.getId())
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReviews").exists())
                    .andExpect(jsonPath("$.ratios.GOOD").exists())
                    .andExpect(jsonPath("$.ratios.NORMAL").exists())
                    .andExpect(jsonPath("$.ratios.BAD").exists())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("리뷰 작성 API (POST /api/v1/reviews)")
    class CreateReview {

        @Test
        @DisplayName("실패: 비로그인 상태에서 리뷰 작성 시도시 401 Unauthorized")
        void fail_create_review_unauthorized() throws Exception {
            // given
            ReviewCreateRequest request = new ReviewCreateRequest(
                    testParty.getId(),
                    reviewee.getId(),
                    ReviewEmoji.GOOD,
                    "테스트 리뷰"
            );

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
