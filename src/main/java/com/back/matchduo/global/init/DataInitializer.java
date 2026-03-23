package com.back.matchduo.global.init;

import com.back.matchduo.domain.chat.entity.ChatMessage;
import com.back.matchduo.domain.chat.entity.ChatRoom;
import com.back.matchduo.domain.chat.entity.MessageType;
import com.back.matchduo.domain.chat.repository.ChatMessageRepository;
import com.back.matchduo.domain.chat.repository.ChatRoomRepository;
import com.back.matchduo.domain.gameaccount.entity.FavoriteChampion;
import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.entity.Rank;
import com.back.matchduo.domain.gameaccount.repository.FavoriteChampionRepository;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.gameaccount.repository.RankRepository;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile({"local", "dev", "prod"})
@ConditionalOnProperty(name = "app.seed.test-enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String TEST_PASSWORD = "test1234!";
    private static final String GAME_TYPE = "LOL";

    private final UserRepository userRepository;
    private final GameAccountRepository gameAccountRepository;
    private final RankRepository rankRepository;
    private final FavoriteChampionRepository favoriteChampionRepository;
    private final PostRepository postRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("leader@test.com")) {
            log.info("Test data already exists, skipping seed");
            return;
        }

        log.info("Seeding test data...");

        String encoded = passwordEncoder.encode(TEST_PASSWORD);

        // ── 1. Users (5명) ──
        User leader = User.createUser("leader@test.com", encoded, "리더짱");
        User member1 = User.createUser("member1@test.com", encoded, "듀오원해");
        User member2 = User.createUser("member2@test.com", encoded, "서폿장인");
        User member3 = User.createUser("member3@test.com", encoded, "정글러킹");
        User member4 = User.createUser("member4@test.com", encoded, "탑솔러");
        userRepository.saveAll(List.of(leader, member1, member2, member3, member4));

        // ── 2. GameAccounts (유저당 1개) ──
        GameAccount ga1 = GameAccount.builder()
                .gameNickname("Hide on bush").gameTag("KR1")
                .gameType(GAME_TYPE).puuid("puuid-test-1").profileIconId(29)
                .user(leader).build();
        GameAccount ga2 = GameAccount.builder()
                .gameNickname("T1 Keria").gameTag("KR1")
                .gameType(GAME_TYPE).puuid("puuid-test-2").profileIconId(10)
                .user(member1).build();
        GameAccount ga3 = GameAccount.builder()
                .gameNickname("서폿갓").gameTag("KR2")
                .gameType(GAME_TYPE).puuid("puuid-test-3").profileIconId(15)
                .user(member2).build();
        GameAccount ga4 = GameAccount.builder()
                .gameNickname("정글의왕").gameTag("KR3")
                .gameType(GAME_TYPE).puuid("puuid-test-4").profileIconId(22)
                .user(member3).build();
        GameAccount ga5 = GameAccount.builder()
                .gameNickname("탑라인킹").gameTag("KR4")
                .gameType(GAME_TYPE).puuid("puuid-test-5").profileIconId(8)
                .user(member4).build();
        gameAccountRepository.saveAll(List.of(ga1, ga2, ga3, ga4, ga5));

        // ── 3. Ranks (솔로랭크 / 자유랭크) ──
        rankRepository.saveAll(List.of(
                Rank.builder().queueType("RANKED_SOLO_5x5").tier("DIAMOND").rank("IV")
                        .wins(200).losses(80).winRate(71.43).gameAccount(ga1).build(),
                Rank.builder().queueType("RANKED_FLEX_SR").tier("PLATINUM").rank("II")
                        .wins(80).losses(60).winRate(57.14).gameAccount(ga1).build(),
                Rank.builder().queueType("RANKED_SOLO_5x5").tier("PLATINUM").rank("I")
                        .wins(120).losses(80).winRate(60.0).gameAccount(ga2).build(),
                Rank.builder().queueType("RANKED_SOLO_5x5").tier("DIAMOND").rank("III")
                        .wins(130).losses(90).winRate(59.09).gameAccount(ga3).build(),
                Rank.builder().queueType("RANKED_SOLO_5x5").tier("GOLD").rank("I")
                        .wins(100).losses(110).winRate(47.62).gameAccount(ga4).build(),
                Rank.builder().queueType("RANKED_SOLO_5x5").tier("EMERALD").rank("II")
                        .wins(170).losses(140).winRate(54.84).gameAccount(ga5).build()
        ));

        // ── 4. FavoriteChampions (주요 유저만) ──
        favoriteChampionRepository.saveAll(List.of(
                // leader - 미드 챔피언
                FavoriteChampion.builder().gameAccount(ga1).rank(1)
                        .championId(238).championName("Zed")
                        .totalGames(15).wins(10).losses(5).winRate(66.67).build(),
                FavoriteChampion.builder().gameAccount(ga1).rank(2)
                        .championId(7).championName("LeBlanc")
                        .totalGames(12).wins(7).losses(5).winRate(58.33).build(),
                FavoriteChampion.builder().gameAccount(ga1).rank(3)
                        .championId(103).championName("Ahri")
                        .totalGames(8).wins(5).losses(3).winRate(62.5).build(),
                // member1 - 서폿 챔피언
                FavoriteChampion.builder().gameAccount(ga2).rank(1)
                        .championId(412).championName("Thresh")
                        .totalGames(18).wins(11).losses(7).winRate(61.11).build(),
                FavoriteChampion.builder().gameAccount(ga2).rank(2)
                        .championId(497).championName("Rakan")
                        .totalGames(10).wins(6).losses(4).winRate(60.0).build(),
                FavoriteChampion.builder().gameAccount(ga2).rank(3)
                        .championId(117).championName("Lulu")
                        .totalGames(7).wins(4).losses(3).winRate(57.14).build(),
                // member2 - 서폿 챔피언
                FavoriteChampion.builder().gameAccount(ga3).rank(1)
                        .championId(12).championName("Alistar")
                        .totalGames(14).wins(9).losses(5).winRate(64.29).build(),
                FavoriteChampion.builder().gameAccount(ga3).rank(2)
                        .championId(89).championName("Leona")
                        .totalGames(11).wins(7).losses(4).winRate(63.64).build(),
                FavoriteChampion.builder().gameAccount(ga3).rank(3)
                        .championId(111).championName("Nautilus")
                        .totalGames(9).wins(5).losses(4).winRate(55.56).build()
        ));

        // ── 5. Posts (모집글 3개) ──
        Post post1 = Post.builder()
                .user(leader).gameAccount(ga1)
                .gameMode(GameMode.SUMMONERS_RIFT).queueType(QueueType.DUO)
                .myPosition(Position.MID).lookingPositions("[\"SUPPORT\"]")
                .mic(true).recruitCount(1)
                .memo("다이아 이상 서폿 구합니다! 마이크 필수")
                .build();
        Post post2 = Post.builder()
                .user(member3).gameAccount(ga4)
                .gameMode(GameMode.SUMMONERS_RIFT).queueType(QueueType.FLEX)
                .myPosition(Position.JUNGLE).lookingPositions("[\"TOP\",\"MID\",\"ADC\",\"SUPPORT\"]")
                .mic(true).recruitCount(4)
                .memo("자랭 5인큐 모집합니다 골드 이상")
                .build();
        Post post3 = Post.builder()
                .user(member4).gameAccount(ga5)
                .gameMode(GameMode.HOWLING_ABYSS).queueType(QueueType.NORMAL)
                .myPosition(Position.ANY).lookingPositions("[\"ANY\"]")
                .mic(false).recruitCount(1)
                .memo("칼바람 같이 하실 분~")
                .build();
        postRepository.saveAll(List.of(post1, post2, post3));

        // ── 6. Party + PartyMembers ──
        // post1 기반 파티 (리더 + 멤버1 참여)
        Party party1 = new Party(post1.getId(), leader.getId());
        partyRepository.save(party1);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party1).user(leader).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party1).user(member1).role(PartyMemberRole.MEMBER).build()
        ));

        // post2 기반 파티 (모집 중 - 리더만)
        Party party2 = new Party(post2.getId(), member3.getId());
        partyRepository.save(party2);
        partyMemberRepository.save(
                PartyMember.builder().party(party2).user(member3).role(PartyMemberRole.LEADER).build()
        );

        // ── 7. ChatRooms + ChatMessages ──
        // member1 → post1(leader 글)에 지원
        ChatRoom room1 = ChatRoom.create(post1, leader, member1);
        chatRoomRepository.save(room1);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room1, member1, MessageType.TEXT, "안녕하세요! 서폿 지원합니다. 쓰레쉬 장인이에요"),
                ChatMessage.create(room1, leader, MessageType.TEXT, "반갑습니다! 티어 어떻게 되세요?"),
                ChatMessage.create(room1, member1, MessageType.TEXT, "플레1이고 시즌 최고 다이아였어요"),
                ChatMessage.create(room1, leader, MessageType.TEXT, "좋아요 같이 해봐요!")
        ));

        // member2 → post1(leader 글)에 지원
        ChatRoom room2 = ChatRoom.create(post1, leader, member2);
        chatRoomRepository.save(room2);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room2, member2, MessageType.TEXT, "서폿 지원합니다! 다이아3 알리스타 장인"),
                ChatMessage.create(room2, leader, MessageType.TEXT, "감사합니다 근데 이미 서폿 구했어요 ㅠ")
        ));

        // member4 → post2(member3 글)에 지원
        ChatRoom room3 = ChatRoom.create(post2, member3, member4);
        chatRoomRepository.save(room3);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room3, member4, MessageType.TEXT, "탑 지원합니다~"),
                ChatMessage.create(room3, member3, MessageType.TEXT, "환영합니다! 에메2면 딱 좋네요")
        ));

        log.info("Test data seeded: users={}, posts={}, parties={}, chatRooms={}",
                5, 3, 2, 3);
    }
}
