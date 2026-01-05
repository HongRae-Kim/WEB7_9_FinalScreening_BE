package com.back.matchduo.domain.party.service;

import com.back.matchduo.domain.chat.repository.ChatRoomRepository;
import com.back.matchduo.domain.party.dto.request.PartyMemberAddRequest;
import com.back.matchduo.domain.party.dto.response.PartyByPostResponse;
import com.back.matchduo.domain.party.dto.response.PartyCloseResponse;
import com.back.matchduo.domain.party.dto.response.PartyMemberAddResponse;
import com.back.matchduo.domain.party.dto.response.PartyMemberRemoveResponse;
import com.back.matchduo.domain.party.entity.*;
import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.party.repository.PartyRepository;
import com.back.matchduo.domain.post.entity.GameMode;
import com.back.matchduo.domain.post.entity.Position;
import com.back.matchduo.domain.post.entity.Post;
import com.back.matchduo.domain.post.entity.QueueType;
import com.back.matchduo.domain.post.repository.PostRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyService 단위 테스트")
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PartyService partyService;

    private User leaderUser;
    private User memberUser;
    private User targetUser;
    private Party party;
    private Post post;
    private PartyMember leaderMember;
    private PartyMember normalMember;

    @BeforeEach
    void setUp() {
        leaderUser = createUserWithId(1L, "leader@test.com", "파티장");
        memberUser = createUserWithId(2L, "member@test.com", "파티원");
        targetUser = createUserWithId(3L, "target@test.com", "초대대상");

        party = createPartyWithId(1L, 100L, 1L);
        post = createPostWithId(100L, leaderUser, 2);

        leaderMember = createPartyMember(1L, party, leaderUser, PartyMemberRole.LEADER);
        normalMember = createPartyMember(2L, party, memberUser, PartyMemberRole.MEMBER);
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

    private Party createPartyWithId(Long id, Long postId, Long leaderId) {
        Party party = new Party(postId, leaderId);
        setId(party, id, Party.class);
        return party;
    }

    private Post createPostWithId(Long id, User user, int recruitCount) {
        Post post = Post.builder()
                .user(user)
                .gameMode(GameMode.SUMMONERS_RIFT)
                .queueType(QueueType.DUO)
                .myPosition(Position.TOP)
                .lookingPositions("[\"JUNGLE\"]")
                .mic(true)
                .recruitCount(recruitCount)
                .memo("테스트 모집글")
                .build();
        setId(post, id, Post.class);
        return post;
    }

    private PartyMember createPartyMember(Long id, Party party, User user, PartyMemberRole role) {
        PartyMember member = new PartyMember(party, user, role);
        setId(member, id, PartyMember.class);
        return member;
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

    @Nested
    @DisplayName("파티 조회 테스트")
    class GetPartyTest {

        @Test
        @DisplayName("성공: 모집글 기준 파티 조회 - 로그인 유저")
        void getPartyByPostId_success_loggedIn() {
            // given
            given(partyRepository.findByPostId(100L)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyId(1L)).willReturn(List.of(leaderMember, normalMember));
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when
            PartyByPostResponse result = partyService.getPartyByPostId(100L, 1L);

            // then
            assertThat(result.postId()).isEqualTo(100L);
            assertThat(result.status()).isEqualTo(PartyStatus.RECRUIT);
            assertThat(result.isJoined()).isTrue();
            assertThat(result.members()).hasSize(2);
        }

        @Test
        @DisplayName("성공: 모집글 기준 파티 조회 - 비로그인")
        void getPartyByPostId_success_guest() {
            // given
            given(partyRepository.findByPostId(100L)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyId(1L)).willReturn(List.of(leaderMember, normalMember));
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when
            PartyByPostResponse result = partyService.getPartyByPostId(100L, null);

            // then
            assertThat(result.isJoined()).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파티")
        void getPartyByPostId_fail_not_found() {
            // given
            given(partyRepository.findByPostId(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> partyService.getPartyByPostId(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.PARTY_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("파티원 추가 테스트")
    class AddMembersTest {

        @Test
        @DisplayName("성공: 파티장이 유저를 초대")
        void addMembers_success() {
            // given
            PartyMemberAddRequest request = new PartyMemberAddRequest(List.of(3L));

            given(partyRepository.findById(1L)).willReturn(Optional.of(party));
            given(partyMemberRepository.findByPartyIdAndUserId(1L, 3L)).willReturn(Optional.empty());
            given(userRepository.findById(3L)).willReturn(Optional.of(targetUser));
            given(partyMemberRepository.save(any(PartyMember.class))).willAnswer(invocation -> {
                PartyMember member = invocation.getArgument(0);
                setId(member, 3L, PartyMember.class);
                return member;
            });
            given(partyMemberRepository.countByPartyIdAndState(1L, PartyMemberState.JOINED)).willReturn(2);
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when
            List<PartyMemberAddResponse> result = partyService.addMembers(1L, 1L, request);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userId()).isEqualTo(3L);
            verify(partyMemberRepository).save(any(PartyMember.class));
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 초대 시도")
        void addMembers_fail_not_leader() {
            // given
            PartyMemberAddRequest request = new PartyMemberAddRequest(List.of(3L));
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            // when & then
            assertThatThrownBy(() -> partyService.addMembers(1L, 2L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.NOT_PARTY_LEADER);
                    });
        }

        @Test
        @DisplayName("실패: 종료된 파티에 멤버 추가 시도")
        void addMembers_fail_party_closed() {
            // given
            party.closeParty();
            PartyMemberAddRequest request = new PartyMemberAddRequest(List.of(3L));
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            // when & then
            assertThatThrownBy(() -> partyService.addMembers(1L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.PARTY_ALREADY_CLOSED);
                    });
        }
    }

    @Nested
    @DisplayName("파티원 강퇴 테스트")
    class RemoveMemberTest {

        @Test
        @DisplayName("성공: 파티장이 멤버 강퇴")
        void removeMember_success() {
            // given
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));
            given(partyMemberRepository.findById(2L)).willReturn(Optional.of(normalMember));

            // when
            PartyMemberRemoveResponse result = partyService.removeMember(1L, 2L, 1L);

            // then
            assertThat(result.partyMemberId()).isEqualTo(2L);
            // leaveParty() 호출 후 상태가 LEFT로 변경됨
            assertThat(normalMember.getState()).isEqualTo(PartyMemberState.LEFT);
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 강퇴 시도")
        void removeMember_fail_not_leader() {
            // given
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            // when & then
            assertThatThrownBy(() -> partyService.removeMember(1L, 2L, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.NOT_PARTY_LEADER);
                    });
        }

        @Test
        @DisplayName("실패: 파티장이 자기 자신 강퇴 시도")
        void removeMember_fail_kick_self() {
            // given
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));
            given(partyMemberRepository.findById(1L)).willReturn(Optional.of(leaderMember));

            // when & then
            assertThatThrownBy(() -> partyService.removeMember(1L, 1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.CANNOT_KICK_LEADER);
                    });
        }
    }

    @Nested
    @DisplayName("파티 종료 테스트")
    class ClosePartyTest {

        @Test
        @DisplayName("성공: 파티장이 파티 종료")
        void closeParty_success() {
            // given
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));
            given(postRepository.findById(100L)).willReturn(Optional.of(post));

            // when
            PartyCloseResponse result = partyService.closeParty(1L, 1L);

            // then
            assertThat(result.partyId()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo("CLOSED");
            assertThat(result.closedAt()).isNotNull();
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 유저가 종료 시도")
        void closeParty_fail_not_leader() {
            // given
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            // when & then
            assertThatThrownBy(() -> partyService.closeParty(1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.NOT_PARTY_LEADER);
                    });
        }

        @Test
        @DisplayName("실패: 이미 종료된 파티 재종료 시도")
        void closeParty_fail_already_closed() {
            // given
            party.closeParty();
            given(partyRepository.findById(1L)).willReturn(Optional.of(party));

            // when & then
            assertThatThrownBy(() -> partyService.closeParty(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo(CustomErrorCode.PARTY_ALREADY_CLOSED);
                    });
        }
    }
}