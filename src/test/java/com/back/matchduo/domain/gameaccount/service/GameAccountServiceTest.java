package com.back.matchduo.domain.gameaccount.service;

import com.back.matchduo.domain.gameaccount.client.RiotApiClient;
import com.back.matchduo.domain.gameaccount.dto.RiotApiDto;
import com.back.matchduo.domain.gameaccount.dto.request.GameAccountCreateRequest;
import com.back.matchduo.domain.gameaccount.dto.request.GameAccountUpdateRequest;
import com.back.matchduo.domain.gameaccount.dto.response.GameAccountResponse;
import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.gameaccount.repository.RankRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.exeption.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameAccountService 테스트")
class GameAccountServiceTest {

    @Mock
    private GameAccountRepository gameAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RiotApiClient riotApiClient;

    @Mock
    private RankRepository rankRepository;

    @Mock
    private DataDragonService dataDragonService;

    @Mock
    private RankService rankService;

    @Mock
    private MatchService matchService;

    private GameAccountService gameAccountService;

    private User testUser;
    private GameAccount testGameAccount;

    private static final Long USER_ID = 1L;
    private static final Long GAME_ACCOUNT_ID = 100L;
    private static final String GAME_TYPE = "LEAGUE_OF_LEGENDS";
    private static final String GAME_NICKNAME = "TestPlayer";
    private static final String GAME_TAG = "KR1";
    private static final String PUUID = "test-puuid-12345";

    @BeforeEach
    void setUp() {
        gameAccountService = new GameAccountService(
                gameAccountRepository,
                userRepository,
                riotApiClient,
                rankRepository,
                dataDragonService,
                rankService,
                matchService
        );

        testUser = User.builder()
                .id(USER_ID)
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .verificationCode("VERIFIED")
                .build();

        testGameAccount = GameAccount.builder()
                .gameNickname(GAME_NICKNAME)
                .gameTag(GAME_TAG)
                .gameType(GAME_TYPE)
                .puuid(PUUID)
                .profileIconId(1234)
                .user(testUser)
                .build();
    }

    @Nested
    @DisplayName("게임 계정 생성")
    class CreateGameAccountTest {

        @Test
        @DisplayName("성공: 게임 계정 생성")
        void createGameAccount_success() {
            // given
            GameAccountCreateRequest request = GameAccountCreateRequest.builder()
                    .gameType(GAME_TYPE)
                    .gameNickname(GAME_NICKNAME)
                    .gameTag(GAME_TAG)
                    .build();

            RiotApiDto.AccountResponse accountResponse = RiotApiDto.AccountResponse.builder()
                    .puuid(PUUID)
                    .gameName(GAME_NICKNAME)
                    .tagLine(GAME_TAG)
                    .build();

            RiotApiDto.SummonerResponse summonerResponse = RiotApiDto.SummonerResponse.builder()
                    .profileIconId(1234)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(testUser));
            given(gameAccountRepository.findByUser_IdAndGameType(USER_ID, GAME_TYPE)).willReturn(Optional.empty());
            given(riotApiClient.getAccountByRiotId(GAME_NICKNAME, GAME_TAG)).willReturn(accountResponse);
            given(riotApiClient.getSummonerByPuuid(PUUID)).willReturn(summonerResponse);
            given(dataDragonService.getLatestVersion()).willReturn("14.1.1");
            given(gameAccountRepository.save(any(GameAccount.class))).willAnswer(invocation -> {
                GameAccount ga = invocation.getArgument(0);
                return GameAccount.builder()
                        .gameNickname(ga.getGameNickname())
                        .gameTag(ga.getGameTag())
                        .gameType(ga.getGameType())
                        .puuid(ga.getPuuid())
                        .profileIconId(ga.getProfileIconId())
                        .user(ga.getUser())
                        .build();
            });

            // when
            GameAccountResponse response = gameAccountService.createGameAccount(request, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getGameNickname()).isEqualTo(GAME_NICKNAME);
            assertThat(response.getGameTag()).isEqualTo(GAME_TAG);
            verify(gameAccountRepository).save(any(GameAccount.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void createGameAccount_fail_userNotFound() {
            // given
            GameAccountCreateRequest request = GameAccountCreateRequest.builder()
                    .gameType(GAME_TYPE)
                    .gameNickname(GAME_NICKNAME)
                    .gameTag(GAME_TAG)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameAccountService.createGameAccount(request, USER_ID))
                    .isInstanceOf(CustomException.class);

            verify(gameAccountRepository, never()).save(any(GameAccount.class));
        }

        @Test
        @DisplayName("실패: 중복된 게임 계정")
        void createGameAccount_fail_duplicateAccount() {
            // given
            GameAccountCreateRequest request = GameAccountCreateRequest.builder()
                    .gameType(GAME_TYPE)
                    .gameNickname(GAME_NICKNAME)
                    .gameTag(GAME_TAG)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(testUser));
            given(gameAccountRepository.findByUser_IdAndGameType(USER_ID, GAME_TYPE))
                    .willReturn(Optional.of(testGameAccount));

            // when & then
            assertThatThrownBy(() -> gameAccountService.createGameAccount(request, USER_ID))
                    .isInstanceOf(CustomException.class);

            verify(gameAccountRepository, never()).save(any(GameAccount.class));
        }

        @Test
        @DisplayName("성공: Riot API 실패 시에도 계정 생성 (puuid null)")
        void createGameAccount_success_riotApiFail() {
            // given
            GameAccountCreateRequest request = GameAccountCreateRequest.builder()
                    .gameType(GAME_TYPE)
                    .gameNickname(GAME_NICKNAME)
                    .gameTag(GAME_TAG)
                    .build();

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(testUser));
            given(gameAccountRepository.findByUser_IdAndGameType(USER_ID, GAME_TYPE)).willReturn(Optional.empty());
            given(riotApiClient.getAccountByRiotId(GAME_NICKNAME, GAME_TAG))
                    .willThrow(new RuntimeException("Network error"));
            given(gameAccountRepository.save(any(GameAccount.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            GameAccountResponse response = gameAccountService.createGameAccount(request, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getPuuid()).isNull();
            verify(gameAccountRepository).save(any(GameAccount.class));
        }
    }

    @Nested
    @DisplayName("게임 계정 조회")
    class GetGameAccountTest {

        @BeforeEach
        void setUp() {
            lenient().when(dataDragonService.getLatestVersion()).thenReturn("14.1.1");
        }

        @Test
        @DisplayName("성공: 게임 계정 조회")
        void getGameAccount_success() {
            // given
            given(gameAccountRepository.findById(GAME_ACCOUNT_ID)).willReturn(Optional.of(testGameAccount));

            // when
            GameAccountResponse response = gameAccountService.getGameAccount(GAME_ACCOUNT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getGameNickname()).isEqualTo(GAME_NICKNAME);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게임 계정")
        void getGameAccount_fail_notFound() {
            // given
            given(gameAccountRepository.findById(GAME_ACCOUNT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameAccountService.getGameAccount(GAME_ACCOUNT_ID, USER_ID))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("사용자 게임 계정 목록 조회")
    class GetUserGameAccountsTest {

        @BeforeEach
        void setUp() {
            lenient().when(dataDragonService.getLatestVersion()).thenReturn("14.1.1");
        }

        @Test
        @DisplayName("성공: 사용자 게임 계정 조회")
        void getUserGameAccounts_success() {
            // given
            given(gameAccountRepository.findByUser_Id(USER_ID)).willReturn(Optional.of(testGameAccount));

            // when
            List<GameAccountResponse> responses = gameAccountService.getUserGameAccounts(USER_ID);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getGameNickname()).isEqualTo(GAME_NICKNAME);
        }

        @Test
        @DisplayName("성공: 게임 계정이 없는 경우 빈 리스트 반환")
        void getUserGameAccounts_success_emptyList() {
            // given
            given(gameAccountRepository.findByUser_Id(USER_ID)).willReturn(Optional.empty());

            // when
            List<GameAccountResponse> responses = gameAccountService.getUserGameAccounts(USER_ID);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("게임 계정 삭제")
    class DeleteGameAccountTest {

        @Test
        @DisplayName("성공: 게임 계정 삭제")
        void deleteGameAccount_success() {
            // given
            given(gameAccountRepository.findById(GAME_ACCOUNT_ID)).willReturn(Optional.of(testGameAccount));
            given(rankRepository.findByGameAccount_GameAccountId(GAME_ACCOUNT_ID)).willReturn(List.of());

            // when
            gameAccountService.deleteGameAccount(GAME_ACCOUNT_ID, USER_ID);

            // then
            verify(matchService).deleteMatchesByGameAccountId(GAME_ACCOUNT_ID);
            verify(gameAccountRepository).delete(testGameAccount);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게임 계정")
        void deleteGameAccount_fail_notFound() {
            // given
            given(gameAccountRepository.findById(GAME_ACCOUNT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameAccountService.deleteGameAccount(GAME_ACCOUNT_ID, USER_ID))
                    .isInstanceOf(CustomException.class);

            verify(gameAccountRepository, never()).delete(any(GameAccount.class));
        }

        @Test
        @DisplayName("실패: 권한 없는 사용자")
        void deleteGameAccount_fail_forbidden() {
            // given
            Long otherUserId = 999L;
            given(gameAccountRepository.findById(GAME_ACCOUNT_ID)).willReturn(Optional.of(testGameAccount));

            // when & then
            assertThatThrownBy(() -> gameAccountService.deleteGameAccount(GAME_ACCOUNT_ID, otherUserId))
                    .isInstanceOf(CustomException.class);

            verify(gameAccountRepository, never()).delete(any(GameAccount.class));
        }
    }
}