package com.back.matchduo.domain.party.controller;

import com.back.matchduo.domain.chat.entity.ChatRoom;
import com.back.matchduo.domain.chat.repository.ChatRoomRepository;
import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.party.dto.request.PartyMemberAddRequest;
import com.back.matchduo.domain.party.entity.Party;
import com.back.matchduo.domain.party.entity.PartyMember;
import com.back.matchduo.domain.party.entity.PartyMemberRole;
import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.party.repository.PartyRepository;
import com.back.matchduo.domain.post.entity.GameMode;
import com.back.matchduo.domain.post.entity.Position;
import com.back.matchduo.domain.post.entity.Post;
import com.back.matchduo.domain.post.entity.QueueType;
import com.back.matchduo.domain.post.repository.PostRepository;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private GameAccountRepository gameAccountRepository;

    private Long testPostId;
    private User leaderUser;
    private User memberUser;
    private Party testParty;
    private User targetUser1;
    private User targetUser2;
    private PartyMember leaderMember;
    private PartyMember normalMember;
    private GameAccount leaderGameAccount;
    private GameAccount targetUser1GameAccount;

    @BeforeAll
    void setUp() {
        // 1. 유저 생성 (파티장) - 프로필 이미지 추가
        leaderUser = User.builder()
                .email("leader@test.com")
                .password("1234")
                .nickname("파티장")
                .verificationCode("1234")
                .build();
        userRepository.save(leaderUser);

        // 2. 유저 생성 (일반 멤버) - 프로필 이미지 추가
        memberUser = User.builder()
                .email("member@test.com")
                .password("1234")
                .nickname("파티원")
                .verificationCode("5678")
                .build();
        userRepository.save(memberUser);

        // 3. 게임 계정 생성 (파티장용)
        leaderGameAccount = GameAccount.builder()
                .gameNickname("LeaderNickname")
                .gameTag("KR1")
                .gameType("LOL")
                .puuid("test-puuid-leader")
                .profileIconId(1)
                .user(leaderUser)
                .build();
        gameAccountRepository.save(leaderGameAccount);

        // 4. 모집글(Post) 생성을 위한 GameMode 저장
        GameMode gameMode = GameMode.SUMMONERS_RIFT;

        // 5. 모집글(Post) 생성 및 저장
        // memo를 제목으로 사용하므로 "테스트 모집글"이 제목이 됨
        Post post = Post.builder()
                .user(leaderUser)
                .gameAccount(leaderGameAccount)
                .gameMode(gameMode)
                .queueType(QueueType.DUO)
                .myPosition(Position.TOP)
                .lookingPositions("[\"JUNGLE\"]")
                .mic(true)
                .recruitCount(4)
                .memo("테스트 모집글") // ★ 제목 역할
                .build();
        postRepository.save(post);
        testPostId = post.getId();

        // 6. 파티 생성 (초기 상태: RECRUIT)
        testParty = new Party(testPostId, leaderUser.getId(), post.getRecruitCount());
        partyRepository.save(testParty);

        // 7. 멤버 추가
        leaderMember = new PartyMember(testParty, leaderUser, PartyMemberRole.LEADER);
        partyMemberRepository.save(leaderMember);

        normalMember = new PartyMember(testParty, memberUser, PartyMemberRole.MEMBER);
        partyMemberRepository.save(normalMember);
        testParty.increaseJoinedMemberCount(1);

        // 8. 초대 대상 유저 생성 (이미지 없는 경우 테스트)
        targetUser1 = User.builder()
                .email("target1@test.com").password("1234").nickname("초대대상1").verificationCode("0000").build();
        userRepository.save(targetUser1);

        // targetUser1용 게임 계정 생성
        targetUser1GameAccount = GameAccount.builder()
                .gameNickname("Target1Nickname")
                .gameTag("KR1")
                .gameType("LOL")
                .puuid("test-puuid-target1")
                .profileIconId(1)
                .user(targetUser1)
                .build();
        gameAccountRepository.save(targetUser1GameAccount);

        targetUser2 = User.builder()
                .email("target2@test.com").password("1234").nickname("초대대상2").verificationCode("0000").build();
        userRepository.save(targetUser2);
    }

    @Nested
    @DisplayName("모집글 기준 파티 조회 API")
    class GetPartyByPost {

        @Test
        @DisplayName("성공: 파티 정보와 멤버 목록 조회 (로그인 상태)")
        void success() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/party", testPostId)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(testPostId))
                    // 초기 상태 RECRUIT 확인 (생성자 로직 변경 반영)
                    .andExpect(jsonPath("$.status").value("RECRUIT"))
                    .andExpect(jsonPath("$.currentCount").value(2))
                    .andExpect(jsonPath("$.members[0].profileImage").isEmpty())
                    .andExpect(jsonPath("$.members[1].profileImage").isEmpty())
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 비로그인 상태로 조회 (isJoined = false 확인)")
        void success_guest() throws Exception {
            // given
            // 로그인 정보 없이 요청

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/party", testPostId)
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(testPostId))
                    .andExpect(jsonPath("$.isJoined").value(false))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파티 조회")
        void fail_party_not_found() throws Exception {
            // given
            Long invalidPostId = 9999L;

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/party", invalidPostId)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isNotFound()) // 404 Not Found
                    .andExpect(jsonPath("$.code").value("PARTY_NOT_FOUND"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("파티원 초대/추가 API")
    class AddPartyMember {

        @Test
        @DisplayName("성공: 파티장이 유저 2명을 초대하면 멤버 목록에 추가된다")
        void success_invite_multiple_users() throws Exception {
            // given
            List<Long> targetIds = List.of(targetUser1.getId(), targetUser2.getId());
            PartyMemberAddRequest request = new PartyMemberAddRequest(targetIds);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/parties/{partyId}/members", testParty.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            // ★ 핵심: 파티장(leaderUser) 권한으로 요청
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(2))
                    .andExpect(jsonPath("$[0].userId").value(targetUser1.getId()))
                    .andExpect(jsonPath("$[0].role").value("MEMBER"))
                    .andExpect(jsonPath("$[1].userId").value(targetUser2.getId()))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 초대를 시도하면 403 Forbidden")
        void fail_not_leader() throws Exception {
            // given
            List<Long> targetIds = List.of(targetUser1.getId());
            PartyMemberAddRequest request = new PartyMemberAddRequest(targetIds);

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/parties/{partyId}/members", testParty.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(new CustomUserDetails(memberUser)))
            );

            // then
            resultActions
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_PARTY_LEADER")) // CustomErrorCode 확인
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 초대할 유저를 선택하지 않음 (빈 리스트)")
        void fail_empty_list() throws Exception {
            // given
            PartyMemberAddRequest request = new PartyMemberAddRequest(List.of());

            // when
            ResultActions resultActions = mockMvc.perform(
                    post("/api/v1/parties/{partyId}/members", testParty.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("파티원 제외(강퇴) API")
    class RemovePartyMember {

        @Test
        @DisplayName("성공: 파티장이 멤버를 강퇴하면 상태가 LEFT로 변경된다")
        void success_kick_member() throws Exception {
            // given

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/parties/{partyId}/members/{memberId}", testParty.getId(), normalMember.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OK"))
                    .andExpect(jsonPath("$.message").value("파티원이 제외되었습니다."))
                    .andExpect(jsonPath("$.data.partyMemberId").value(normalMember.getId()))
                    .andExpect(jsonPath("$.data.state").value("LEFT"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 강퇴를 시도하면 403 Forbidden")
        void fail_not_leader() throws Exception {
            // given

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/parties/{partyId}/members/{memberId}", testParty.getId(), normalMember.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(memberUser))) // 일반 유저 권한
            );

            // then
            resultActions
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_PARTY_LEADER"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 다른 파티의 멤버를 강퇴하려 하면 400 Bad Request")
        void fail_member_mismatch() throws Exception {
            // given
            Party otherParty = new Party(200L, targetUser1.getId(), 2);
            partyRepository.save(otherParty);

            // [수정] PartyMember 생성 시 User 객체 전달 (targetUser2)
            PartyMember otherPartyMember = new PartyMember(otherParty, targetUser2, PartyMemberRole.MEMBER);
            partyMemberRepository.save(otherPartyMember);

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/parties/{partyId}/members/{memberId}", testParty.getId(), otherPartyMember.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARTY_MEMBER_NOT_MATCH"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 파티장이 자기 자신을 강퇴하려 하면 400 Bad Request")
        void fail_kick_self() throws Exception {
            // given

            // when
            ResultActions resultActions = mockMvc.perform(
                    delete("/api/v1/parties/{partyId}/members/{memberId}", testParty.getId(), leaderMember.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CANNOT_KICK_LEADER"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("파티원 목록 조회 API")
    class GetPartyMemberList {

        @Test
        @DisplayName("성공: 로그인 없이 파티원 목록과 정원 정보를 조회한다")
        void success() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/parties/{partyId}/members", testParty.getId())
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.members[0].profileImage").isEmpty())
                    .andExpect(jsonPath("$.data.members[1].profileImage").isEmpty())
                    .andDo(print());
        }
    }

    @Test
    @DisplayName("실패: 존재하지 않는 파티 ID로 조회 시 404 에러")
    void fail_party_not_found() throws Exception {
        // given
        Long invalidPartyId = 99999L;

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/v1/parties/{partyId}/members", invalidPartyId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PARTY_NOT_FOUND"))
                .andDo(print());
    }

    @Nested
    @DisplayName("내가 참여한 파티 목록 조회 API")
    class GetMyPartyList {

        @Test
        @DisplayName("성공: 내가 참여한 파티 목록(리더 1개 + 멤버 1개)을 최신순으로 조회한다")
        void success() throws Exception {
            // given
            // 1. [추가 데이터 생성] leaderUser가 MEMBER로 참여할 '두 번째 파티' 생성
            GameMode flexMode = GameMode.SUMMONERS_RIFT;

            // 2. 두 번째 모집글 생성 (memo를 제목으로 사용)
            Post secondPost = Post.builder()
                    .user(targetUser1) // targetUser1이 쓴 글
                    .gameAccount(targetUser1GameAccount)
                    .gameMode(flexMode)
                    .queueType(QueueType.FLEX)
                    .myPosition(Position.MID)
                    .lookingPositions("[\"JUNGLE\"]")
                    .mic(true)
                    .recruitCount(5)
                    .memo("자유랭크 달리실 분") // ★ 제목 역할
                    .build();
            postRepository.save(secondPost);

            // 3. 두 번째 파티 생성
            Party secondParty = new Party(secondPost.getId(), targetUser1.getId(), secondPost.getRecruitCount());
            partyRepository.save(secondParty);

            // 4. leaderUser를 멤버로 가입시킴
            PartyMember secondMemberShip = new PartyMember(secondParty, leaderUser, PartyMemberRole.MEMBER);
            partyMemberRepository.save(secondMemberShip);
            secondParty.increaseJoinedMemberCount(1);

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/users/me/parties")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.parties[0].postTitle").value("자유랭크 달리실 분"))
                    .andExpect(jsonPath("$.data.parties[0].gameMode").value("소환사의 협곡"))
                    .andExpect(jsonPath("$.data.parties[0].queueType").value("FLEX"))
                    .andExpect(jsonPath("$.data.parties[1].postTitle").value("테스트 모집글"))
                    .andExpect(jsonPath("$.data.parties[1].gameMode").value("소환사의 협곡"))
                    .andExpect(jsonPath("$.data.parties[1].queueType").value("DUO"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 비로그인 상태로 요청 시 401 Unauthorized")
        void fail_unauthorized() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/parties/my")
                            .accept(MediaType.APPLICATION_JSON)
            );

            // then
            resultActions
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_USER"))
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 참여한 파티가 하나도 없을 때 빈 리스트 반환")
        void success_empty_list() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/users/me/parties")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(targetUser2))) // 가입한 적 없는 유저
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.parties").isArray())
                    .andExpect(jsonPath("$.data.parties").isEmpty())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("파티 수동 종료 API")
    class CloseParty {

        @Test
        @DisplayName("성공: 파티장이 파티를 종료하면 상태가 CLOSED로 변경된다")
        void success() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    patch("/api/v1/parties/{partyId}/close", testParty.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("파티가 종료되었습니다."))
                    .andExpect(jsonPath("$.data.partyId").value(testParty.getId()))
                    .andExpect(jsonPath("$.data.status").value("CLOSED"))
                    .andExpect(jsonPath("$.data.closedAt").exists())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 종료를 시도하면 403 Forbidden")
        void fail_not_leader() throws Exception {
            // when
            ResultActions resultActions = mockMvc.perform(
                    patch("/api/v1/parties/{partyId}/close", testParty.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(memberUser)))
            );

            // then
            resultActions
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_PARTY_LEADER"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 이미 종료된 파티를 다시 종료하려 하면 400 Bad Request")
        void fail_already_closed() throws Exception {
            // given
            // 이 테스트만을 위한 '이미 종료된 파티'를 새로 만듭니다.
            Party closedParty = new Party(testPostId, leaderUser.getId(), testParty.getCapacity());
            closedParty.closeParty(); // 강제 종료 설정
            partyRepository.save(closedParty);

            // when
            ResultActions resultActions = mockMvc.perform(
                    patch("/api/v1/parties/{partyId}/close", closedParty.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARTY_ALREADY_CLOSED"))
                    .andDo(print());
        }
    }


    @Nested
    @DisplayName("파티 영입 후보 조회 API")
    class GetChatCandidates {

        @Test
        @DisplayName("성공: 채팅을 걸었지만 아직 파티원이 아닌 유저만 조회된다")
        void success() throws Exception {
            // given
            // 1. 테스트를 위해 Post 엔티티를 다시 조회 (setUp에서 만든 모집글)
            Post savedPost = postRepository.findById(testPostId).orElseThrow();

            // 2. [상황 설정]
            // - targetUser1: 채팅을 걸었고, 파티원이 아님 -> (O) 조회 되어야 함
            // - memberUser: 채팅을 걸었지만, 이미 파티원임 -> (X) 조회 되면 안 됨
            // - targetUser2: 채팅을 건 적이 없음 -> (X) 조회 되면 안 됨

            // ChatRoom 데이터 생성 (Entity의 create 메서드 활용 가정)
            // ChatRoom.create(post, receiver(방장), sender(지원자))
            ChatRoom chat1 = ChatRoom.create(savedPost, leaderUser, targetUser1);
            chatRoomRepository.save(chat1);

            ChatRoom chat2 = ChatRoom.create(savedPost, leaderUser, memberUser);
            chatRoomRepository.save(chat2);

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/candidates", testPostId)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser))) // 파티장 권한
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("영입 후보 목록을 조회했습니다."))
                    .andExpect(jsonPath("$.data.size()").value(1)) // targetUser1 한 명만 나와야 함
                    .andExpect(jsonPath("$.data[0].userId").value(targetUser1.getId()))
                    .andExpect(jsonPath("$.data[0].nickname").value("초대대상1"))
                    .andDo(print());
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 API를 호출하면 '권한 없음(NOT_PARTY_LEADER)' 에러가 발생한다")
        void fail_not_leader() throws Exception {
            // given
            Post savedPost = postRepository.findById(testPostId).orElseThrow();


            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/candidates", savedPost.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(memberUser))) // 👈 일반 유저 로그인
            );

            // then
            resultActions
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_PARTY_LEADER"))
                    .andExpect(jsonPath("$.message").value("파티장만 접근할 수 있는 권한입니다."))
                    .andDo(print());
        }

        @Test
        @DisplayName("성공: 채팅을 건 사람이 아무도 없으면 빈 리스트 반환")
        void success_empty() throws Exception {
            // given
            // 새로운 모집글 생성 (채팅 기록 없음)
            Post newPost = Post.builder()
                    .user(leaderUser)
                    .gameAccount(leaderGameAccount)
                    .gameMode(GameMode.SUMMONERS_RIFT) // [변경] Enum 상수 직접 사용
                    .queueType(QueueType.DUO)
                    .myPosition(Position.ADC)
                    .lookingPositions("[\"SUPPORT\"]")
                    .mic(true)
                    .recruitCount(2)
                    .memo("새로운 글")
                    .build();
            postRepository.save(newPost);

            // when
            ResultActions resultActions = mockMvc.perform(
                    get("/api/v1/posts/{postId}/candidates", newPost.getId())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(user(new CustomUserDetails(leaderUser)))
            );

            // then
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty()) // 빈 배열 확인
                    .andDo(print());
        }
    }
}
