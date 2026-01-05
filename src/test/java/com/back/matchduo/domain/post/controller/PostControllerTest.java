package com.back.matchduo.domain.post.controller;

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
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.security.CustomUserDetails;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Post API 통합 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameAccountRepository gameAccountRepository;

    private User testUser;
    private User testUser2;
    private GameAccount testGameAccount;
    private Post recruitPost;
    private Post activePost;
    private Post closedPost;

    @BeforeAll
    void setUp() {
        // 테스트 유저 생성
        testUser = User.builder()
                .email("posttest@test.com")
                .password("password123")
                .nickname("포스트테스터")
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(testUser);

        testUser2 = User.builder()
                .email("posttest2@test.com")
                .password("password123")
                .nickname("포스트테스터2")
                .verificationCode("VERIFIED")
                .build();
        userRepository.save(testUser2);

        // 테스트 게임 계정 생성
        testGameAccount = GameAccount.builder()
                .gameNickname("테스트게이머")
                .gameTag("KR1")
                .gameType("LOL")
                .puuid("test-puuid-12345")
                .profileIconId(1234)
                .user(testUser)
                .build();
        gameAccountRepository.save(testGameAccount);

        // RECRUIT 상태 모집글
        recruitPost = Post.builder()
                .user(testUser)
                .gameAccount(testGameAccount)
                .gameMode(GameMode.SUMMONERS_RIFT)
                .queueType(QueueType.DUO)
                .myPosition(Position.TOP)
                .lookingPositions("[\"JUNGLE\"]")
                .mic(true)
                .recruitCount(2)
                .memo("모집중 테스트")
                .build();
        postRepository.save(recruitPost);

        // RECRUIT 파티 생성
        Party recruitParty = new Party(recruitPost.getId(), testUser.getId());
        partyRepository.save(recruitParty);
        partyMemberRepository.save(new PartyMember(recruitParty, testUser, PartyMemberRole.LEADER));

        // ACTIVE 상태 모집글 (모집 완료)
        activePost = Post.builder()
                .user(testUser)
                .gameAccount(testGameAccount)
                .gameMode(GameMode.SUMMONERS_RIFT)
                .queueType(QueueType.FLEX)
                .myPosition(Position.MID)
                .lookingPositions("[\"SUPPORT\"]")
                .mic(false)
                .recruitCount(2)
                .memo("모집완료 테스트")
                .build();
        postRepository.save(activePost);
        activePost.updateStatus(PostStatus.ACTIVE);
        postRepository.save(activePost);

        // ACTIVE 파티 생성
        Party activeParty = new Party(activePost.getId(), testUser.getId());
        activeParty.activateParty(java.time.LocalDateTime.now().plusHours(6));
        partyRepository.save(activeParty);
        partyMemberRepository.save(new PartyMember(activeParty, testUser, PartyMemberRole.LEADER));
        partyMemberRepository.save(new PartyMember(activeParty, testUser2, PartyMemberRole.MEMBER));

        // CLOSED 상태 모집글
        closedPost = Post.builder()
                .user(testUser)
                .gameAccount(testGameAccount)
                .gameMode(GameMode.HOWLING_ABYSS)
                .queueType(QueueType.NORMAL)
                .myPosition(Position.ANY)
                .lookingPositions("[\"ANY\"]")
                .mic(false)
                .recruitCount(5)
                .memo("종료된 테스트")
                .build();
        postRepository.save(closedPost);
        closedPost.updateStatus(PostStatus.CLOSED);
        postRepository.save(closedPost);

        // CLOSED 파티 생성
        Party closedParty = new Party(closedPost.getId(), testUser.getId());
        closedParty.closeParty();
        partyRepository.save(closedParty);
    }

    @Nested
    @DisplayName("모집글 상태별 조회 테스트")
    class GetPostsByStatus {

        @Test
        @DisplayName("성공: status=RECRUIT로 조회하면 모집중인 글만 반환된다")
        void success_filter_recruit() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .param("status", "RECRUIT")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.posts[?(@.status == 'RECRUIT')]").exists())
                    .andExpect(jsonPath("$.posts[?(@.status == 'ACTIVE')]").doesNotExist())
                    .andExpect(jsonPath("$.posts[?(@.status == 'CLOSED')]").doesNotExist())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: status=ACTIVE로 조회하면 모집완료된 글만 반환된다")
        void success_filter_active() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .param("status", "ACTIVE")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.posts[?(@.status == 'ACTIVE')]").exists())
                    .andExpect(jsonPath("$.posts[?(@.status == 'RECRUIT')]").doesNotExist())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: status 파라미터 없이 조회하면 CLOSED 제외한 모든 글 반환")
        void success_filter_all_except_closed() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.posts[?(@.status == 'CLOSED')]").doesNotExist())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: status=RECRUIT + queueType=FLEX 복합 필터링")
        void success_filter_combined() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .param("status", "RECRUIT")
                            .param("queueType", "FLEX")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 비로그인 상태에서도 목록 조회 가능")
        void success_guest_access() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 로그인 상태에서 목록 조회")
        void success_authenticated_access() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(testUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("모집글 단건 조회 테스트")
    class GetPostDetail {

        @Test
        @DisplayName("성공: 모집글 상세 조회")
        void success_get_detail() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}", recruitPost.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(testUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(recruitPost.getId()))
                    .andExpect(jsonPath("$.memo").value("모집중 테스트"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 모집글 조회")
        void fail_post_not_found() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}", 99999L)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(testUser)))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound())
                    .andDo(print());
        }
    }
}