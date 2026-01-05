package com.back.matchduo.domain.usersearch.service;

import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.domain.usersearch.dto.response.UserSearchListResponse;
import com.back.matchduo.global.exeption.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("UserSearchService 테스트")
class UserSearchServiceTest {

    @Autowired
    private UserSearchService userSearchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameAccountRepository gameAccountRepository;

    private User userWithGameAccount;
    private User userWithoutGameAccount;

    @BeforeEach
    void setUp() {
        userWithGameAccount = userRepository.save(User.builder()
                .email("gamer@test.com")
                .password("password123")
                .nickname("프로게이머")
                .verificationCode("1234")
                .comment("게임 좋아합니다")
                .build());

        gameAccountRepository.save(GameAccount.builder()
                .gameNickname("TestPlayer")
                .gameTag("KR1")
                .gameType("LEAGUE_OF_LEGENDS")
                .puuid("test-puuid-12345")
                .profileIconId(1234)
                .user(userWithGameAccount)
                .build());

        userWithoutGameAccount = userRepository.save(User.builder()
                .email("normal@test.com")
                .password("password123")
                .nickname("일반유저")
                .verificationCode("5678")
                .build());
    }

    @Nested
    @DisplayName("검색 유효성 검사")
    class ValidationTest {

        @Test
        @DisplayName("null 검색어 시 예외 발생")
        void search_null_keyword_throws_exception() {
            assertThatThrownBy(() -> userSearchService.search(null, null, 10))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("빈 검색어 시 예외 발생")
        void search_blank_keyword_throws_exception() {
            assertThatThrownBy(() -> userSearchService.search("   ", null, 10))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("검색 결과")
    class SearchResultTest {

        @Test
        @DisplayName("닉네임으로 검색 성공")
        void search_by_nickname_success() {
            // when
            UserSearchListResponse result = userSearchService.search("게이머", null, 10);

            // then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.users()).hasSize(1);
            assertThat(result.users().get(0).nickname()).isEqualTo("프로게이머");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void search_no_result() {
            // when
            UserSearchListResponse result = userSearchService.search("존재하지않는닉네임", null, 10);

            // then
            assertThat(result.totalCount()).isZero();
            assertThat(result.users()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("게임 계정 연동된 유저 검색")
        void search_user_with_game_account() {
            // when
            UserSearchListResponse result = userSearchService.search("프로게이머", null, 10);

            // then
            assertThat(result.users()).hasSize(1);
            UserSearchListResponse.GameAccountDto gameAccount = result.users().get(0).gameAccount();
            assertThat(gameAccount.linked()).isTrue();
            assertThat(gameAccount.gameName()).isEqualTo("TestPlayer");
            assertThat(gameAccount.tagLine()).isEqualTo("KR1");
        }

        @Test
        @DisplayName("게임 계정 미연동 유저 검색")
        void search_user_without_game_account() {
            // when
            UserSearchListResponse result = userSearchService.search("일반유저", null, 10);

            // then
            assertThat(result.users()).hasSize(1);
            UserSearchListResponse.GameAccountDto gameAccount = result.users().get(0).gameAccount();
            assertThat(gameAccount.linked()).isFalse();
            assertThat(gameAccount.gameName()).isNull();
        }
    }

    @Nested
    @DisplayName("페이징")
    class PagingTest {

        @BeforeEach
        void setUpMultipleUsers() {
            for (int i = 1; i <= 15; i++) {
                userRepository.save(User.builder()
                        .email("paging" + i + "@test.com")
                        .password("password123")
                        .nickname("페이징테스트" + i)
                        .verificationCode("code" + i)
                        .build());
            }
        }

        @Test
        @DisplayName("페이지 크기 제한 동작")
        void search_with_page_size() {
            // when
            UserSearchListResponse result = userSearchService.search("페이징테스트", null, 5);

            // then
            assertThat(result.users()).hasSize(5);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
        }

        @Test
        @DisplayName("커서 기반 페이징")
        void search_with_cursor() {
            // given
            UserSearchListResponse firstPage = userSearchService.search("페이징테스트", null, 5);
            Long cursor = firstPage.nextCursor();

            // when
            UserSearchListResponse secondPage = userSearchService.search("페이징테스트", cursor, 5);

            // then
            assertThat(secondPage.users()).hasSize(5);
            assertThat(secondPage.users().get(0).userId()).isLessThan(cursor);
        }

        @Test
        @DisplayName("마지막 페이지에서 hasNext는 false")
        void search_last_page_has_no_next() {
            // when
            UserSearchListResponse result = userSearchService.search("페이징테스트", null, 50);

            // then
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("size가 null이면 기본값 10 적용")
        void search_default_size() {
            // when
            UserSearchListResponse result = userSearchService.search("페이징테스트", null, null);

            // then
            assertThat(result.users()).hasSize(10);
        }

        @Test
        @DisplayName("size가 50 초과시 최대 50으로 제한")
        void search_max_size_limit() {
            // when
            UserSearchListResponse result = userSearchService.search("페이징테스트", null, 100);

            // then
            assertThat(result.users().size()).isLessThanOrEqualTo(50);
        }
    }
}