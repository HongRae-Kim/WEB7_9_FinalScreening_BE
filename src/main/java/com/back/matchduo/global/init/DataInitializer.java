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
    private static final String GAME_TYPE = "LEAGUE_OF_LEGENDS";

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

        // ── 1. Users (15명) ──
        User u1 = User.createUser("leader@test.com", encoded, "리더짱");
        User u2 = User.createUser("member1@test.com", encoded, "듀오원해");
        User u3 = User.createUser("member2@test.com", encoded, "서폿장인");
        User u4 = User.createUser("member3@test.com", encoded, "정글러킹");
        User u5 = User.createUser("member4@test.com", encoded, "탑솔러");
        User u6 = User.createUser("midking@test.com", encoded, "미드황제");
        User u7 = User.createUser("adcmaster@test.com", encoded, "원딜장인");
        User u8 = User.createUser("junglediff@test.com", encoded, "정글디프");
        User u9 = User.createUser("wardmaster@test.com", encoded, "와드장인");
        User u10 = User.createUser("solocarry@test.com", encoded, "솔로캐리");
        User u11 = User.createUser("teamfight@test.com", encoded, "한타머신");
        User u12 = User.createUser("ganker@test.com", encoded, "갱킹왕");
        User u13 = User.createUser("farmer@test.com", encoded, "CS장인");
        User u14 = User.createUser("roamer@test.com", encoded, "로밍신");
        User u15 = User.createUser("shotcaller@test.com", encoded, "샷콜러");
        userRepository.saveAll(List.of(u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15));

        // ── 2. GameAccounts (15명) ──
        GameAccount ga1 = buildGameAccount("Hide on bush", "KR1", "puuid-test-1", 29, u1);
        GameAccount ga2 = buildGameAccount("T1 Keria", "KR1", "puuid-test-2", 10, u2);
        GameAccount ga3 = buildGameAccount("서폿갓", "KR2", "puuid-test-3", 15, u3);
        GameAccount ga4 = buildGameAccount("정글의왕", "KR3", "puuid-test-4", 22, u4);
        GameAccount ga5 = buildGameAccount("탑라인킹", "KR4", "puuid-test-5", 8, u5);
        GameAccount ga6 = buildGameAccount("MidOrFeed", "KR1", "puuid-test-6", 31, u6);
        GameAccount ga7 = buildGameAccount("ADCarry99", "KR2", "puuid-test-7", 45, u7);
        GameAccount ga8 = buildGameAccount("JungleMaster", "KR1", "puuid-test-8", 12, u8);
        GameAccount ga9 = buildGameAccount("WardBot", "KR3", "puuid-test-9", 18, u9);
        GameAccount ga10 = buildGameAccount("SoloQ King", "KR1", "puuid-test-10", 25, u10);
        GameAccount ga11 = buildGameAccount("TeamFighter", "KR2", "puuid-test-11", 33, u11);
        GameAccount ga12 = buildGameAccount("GankMachine", "KR1", "puuid-test-12", 40, u12);
        GameAccount ga13 = buildGameAccount("CSPerfect", "KR4", "puuid-test-13", 7, u13);
        GameAccount ga14 = buildGameAccount("RoamKing", "KR2", "puuid-test-14", 19, u14);
        GameAccount ga15 = buildGameAccount("ShotCaller1", "KR1", "puuid-test-15", 36, u15);
        gameAccountRepository.saveAll(List.of(ga1, ga2, ga3, ga4, ga5, ga6, ga7, ga8, ga9, ga10, ga11, ga12, ga13, ga14, ga15));

        // ── 3. Ranks ──
        rankRepository.saveAll(List.of(
                buildRank("RANKED_SOLO_5x5", "DIAMOND", "IV", 200, 80, 71.43, ga1),
                buildRank("RANKED_FLEX_SR", "PLATINUM", "II", 80, 60, 57.14, ga1),
                buildRank("RANKED_SOLO_5x5", "PLATINUM", "I", 120, 80, 60.0, ga2),
                buildRank("RANKED_SOLO_5x5", "DIAMOND", "III", 130, 90, 59.09, ga3),
                buildRank("RANKED_SOLO_5x5", "GOLD", "I", 100, 110, 47.62, ga4),
                buildRank("RANKED_SOLO_5x5", "EMERALD", "II", 170, 140, 54.84, ga5),
                buildRank("RANKED_SOLO_5x5", "CHALLENGER", "I", 350, 120, 74.47, ga6),
                buildRank("RANKED_SOLO_5x5", "GRANDMASTER", "I", 280, 150, 65.12, ga7),
                buildRank("RANKED_SOLO_5x5", "MASTER", "I", 220, 130, 62.86, ga8),
                buildRank("RANKED_SOLO_5x5", "DIAMOND", "I", 190, 100, 65.52, ga9),
                buildRank("RANKED_SOLO_5x5", "EMERALD", "IV", 140, 130, 51.85, ga10),
                buildRank("RANKED_SOLO_5x5", "PLATINUM", "III", 110, 100, 52.38, ga11),
                buildRank("RANKED_SOLO_5x5", "DIAMOND", "II", 160, 90, 64.0, ga12),
                buildRank("RANKED_SOLO_5x5", "GOLD", "II", 90, 95, 48.65, ga13),
                buildRank("RANKED_SOLO_5x5", "EMERALD", "I", 180, 120, 60.0, ga14),
                buildRank("RANKED_SOLO_5x5", "PLATINUM", "I", 150, 110, 57.69, ga15),
                buildRank("RANKED_FLEX_SR", "DIAMOND", "II", 100, 50, 66.67, ga6),
                buildRank("RANKED_FLEX_SR", "EMERALD", "I", 70, 40, 63.64, ga9)
        ));

        // ── 4. FavoriteChampions ──
        favoriteChampionRepository.saveAll(List.of(
                // u1 (리더짱) - 미드
                buildChampion(ga1, 1, 238, "Zed", 15, 10, 5, 66.67),
                buildChampion(ga1, 2, 7, "LeBlanc", 12, 7, 5, 58.33),
                buildChampion(ga1, 3, 103, "Ahri", 8, 5, 3, 62.5),
                // u2 (듀오원해) - 서폿
                buildChampion(ga2, 1, 412, "Thresh", 18, 11, 7, 61.11),
                buildChampion(ga2, 2, 497, "Rakan", 10, 6, 4, 60.0),
                buildChampion(ga2, 3, 117, "Lulu", 7, 4, 3, 57.14),
                // u3 (서폿장인) - 서폿
                buildChampion(ga3, 1, 12, "Alistar", 14, 9, 5, 64.29),
                buildChampion(ga3, 2, 89, "Leona", 11, 7, 4, 63.64),
                buildChampion(ga3, 3, 111, "Nautilus", 9, 5, 4, 55.56),
                // u6 (미드황제) - 미드
                buildChampion(ga6, 1, 61, "Orianna", 25, 18, 7, 72.0),
                buildChampion(ga6, 2, 112, "Viktor", 20, 14, 6, 70.0),
                buildChampion(ga6, 3, 4, "TwistedFate", 18, 12, 6, 66.67),
                // u7 (원딜장인) - 원딜
                buildChampion(ga7, 1, 51, "Jinx", 22, 15, 7, 68.18),
                buildChampion(ga7, 2, 236, "Lucian", 19, 12, 7, 63.16),
                buildChampion(ga7, 3, 81, "Ezreal", 16, 10, 6, 62.5),
                // u8 (정글디프) - 정글
                buildChampion(ga8, 1, 64, "LeeSin", 20, 13, 7, 65.0),
                buildChampion(ga8, 2, 76, "Nidalee", 15, 10, 5, 66.67),
                buildChampion(ga8, 3, 121, "Khazix", 14, 9, 5, 64.29),
                // u10 (솔로캐리) - 탑
                buildChampion(ga10, 1, 92, "Riven", 17, 10, 7, 58.82),
                buildChampion(ga10, 2, 157, "Yasuo", 14, 8, 6, 57.14),
                buildChampion(ga10, 3, 39, "Irelia", 12, 7, 5, 58.33),
                // u12 (갱킹왕) - 정글
                buildChampion(ga12, 1, 254, "Vi", 16, 11, 5, 68.75),
                buildChampion(ga12, 2, 28, "Evelynn", 13, 9, 4, 69.23),
                buildChampion(ga12, 3, 887, "Gwen", 11, 7, 4, 63.64),
                // u14 (로밍신) - 서폿
                buildChampion(ga14, 1, 432, "Bard", 18, 12, 6, 66.67),
                buildChampion(ga14, 2, 555, "Pyke", 14, 9, 5, 64.29),
                buildChampion(ga14, 3, 350, "Yuumi", 10, 6, 4, 60.0)
        ));

        // ── 5. Posts (12개) ──
        Post p1 = buildPost(u1, ga1, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.MID,
                "[\"SUPPORT\"]", true, 2, "다이아 이상 서폿 구합니다! 마이크 필수");
        Post p2 = buildPost(u4, ga4, GameMode.SUMMONERS_RIFT, QueueType.FLEX, Position.JUNGLE,
                "[\"TOP\",\"MID\",\"ADC\",\"SUPPORT\"]", true, 5, "자랭 5인큐 모집합니다 골드 이상");
        Post p3 = buildPost(u5, ga5, GameMode.HOWLING_ABYSS, QueueType.NORMAL, Position.ANY,
                "[\"ANY\"]", false, 2, "칼바람 같이 하실 분~");
        Post p4 = buildPost(u6, ga6, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.MID,
                "[\"JUNGLE\"]", true, 2, "챌린저 미드 정글 듀오 구합니다");
        Post p5 = buildPost(u7, ga7, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.ADC,
                "[\"SUPPORT\"]", true, 2, "그마 원딜 서폿 듀오 찾습니다 마이크 필수");
        Post p6 = buildPost(u8, ga8, GameMode.SUMMONERS_RIFT, QueueType.FLEX, Position.JUNGLE,
                "[\"TOP\",\"MID\"]", true, 3, "마스터 정글러 자랭 같이 할 분");
        Post p7 = buildPost(u10, ga10, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.TOP,
                "[\"JUNGLE\"]", false, 2, "에메 탑 정글 듀오 구해요 편하게 ㄱㄱ");
        Post p8 = buildPost(u11, ga11, GameMode.HOWLING_ABYSS, QueueType.NORMAL, Position.ANY,
                "[\"ANY\"]", false, 5, "칼바람 5인큐 ㄱ ㄱ 아무나 환영");
        Post p9 = buildPost(u12, ga12, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.JUNGLE,
                "[\"MID\"]", true, 2, "다이아 정글 미드 듀오 구합니다 캐리 가능");
        Post p10 = buildPost(u14, ga14, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.SUPPORT,
                "[\"ADC\"]", true, 2, "에메 서폿 원딜 듀오 찾아요 로밍 잘합니다");
        Post p11 = buildPost(u15, ga15, GameMode.SUMMONERS_RIFT, QueueType.FLEX, Position.SUPPORT,
                "[\"TOP\",\"JUNGLE\",\"MID\",\"ADC\"]", true, 5, "플레 서폿 자랭 5인큐 모집 마이크 필수");
        Post p12 = buildPost(u9, ga9, GameMode.SUMMONERS_RIFT, QueueType.DUO, Position.SUPPORT,
                "[\"ADC\"]", true, 2, "다이아 서폿 원딜 듀오 찾습니다 와딩 장인");
        postRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12));

        // ── 6. Party + PartyMembers ──
        // p1: RECRUIT (리더 + 멤버1)
        Party party1 = new Party(p1.getId(), u1.getId());
        partyRepository.save(party1);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party1).user(u1).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party1).user(u2).role(PartyMemberRole.MEMBER).build()
        ));

        // p2: RECRUIT (리더만)
        Party party2 = new Party(p2.getId(), u4.getId());
        partyRepository.save(party2);
        partyMemberRepository.save(
                PartyMember.builder().party(party2).user(u4).role(PartyMemberRole.LEADER).build()
        );

        // p3: RECRUIT (리더만)
        Party party3 = new Party(p3.getId(), u5.getId());
        partyRepository.save(party3);
        partyMemberRepository.save(
                PartyMember.builder().party(party3).user(u5).role(PartyMemberRole.LEADER).build()
        );

        // p4: ACTIVE (리더 + 멤버 → 풀파티)
        Party party4 = new Party(p4.getId(), u6.getId());
        partyRepository.save(party4);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party4).user(u6).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party4).user(u8).role(PartyMemberRole.MEMBER).build()
        ));

        // p5: RECRUIT (리더만)
        Party party5 = new Party(p5.getId(), u7.getId());
        partyRepository.save(party5);
        partyMemberRepository.save(
                PartyMember.builder().party(party5).user(u7).role(PartyMemberRole.LEADER).build()
        );

        // p6: RECRUIT (리더 + 1명)
        Party party6 = new Party(p6.getId(), u8.getId());
        partyRepository.save(party6);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party6).user(u8).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party6).user(u10).role(PartyMemberRole.MEMBER).build()
        ));

        // p7: RECRUIT (리더만)
        Party party7 = new Party(p7.getId(), u10.getId());
        partyRepository.save(party7);
        partyMemberRepository.save(
                PartyMember.builder().party(party7).user(u10).role(PartyMemberRole.LEADER).build()
        );

        // p8: RECRUIT (리더 + 2명)
        Party party8 = new Party(p8.getId(), u11.getId());
        partyRepository.save(party8);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party8).user(u11).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party8).user(u13).role(PartyMemberRole.MEMBER).build(),
                PartyMember.builder().party(party8).user(u15).role(PartyMemberRole.MEMBER).build()
        ));

        // p9: RECRUIT (리더만)
        Party party9 = new Party(p9.getId(), u12.getId());
        partyRepository.save(party9);
        partyMemberRepository.save(
                PartyMember.builder().party(party9).user(u12).role(PartyMemberRole.LEADER).build()
        );

        // p10: ACTIVE (리더 + 멤버 → 풀파티)
        Party party10 = new Party(p10.getId(), u14.getId());
        partyRepository.save(party10);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party10).user(u14).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party10).user(u7).role(PartyMemberRole.MEMBER).build()
        ));

        // p11: RECRUIT (리더 + 1명)
        Party party11 = new Party(p11.getId(), u15.getId());
        partyRepository.save(party11);
        partyMemberRepository.saveAll(List.of(
                PartyMember.builder().party(party11).user(u15).role(PartyMemberRole.LEADER).build(),
                PartyMember.builder().party(party11).user(u9).role(PartyMemberRole.MEMBER).build()
        ));

        // p12: RECRUIT (리더만)
        Party party12 = new Party(p12.getId(), u9.getId());
        partyRepository.save(party12);
        partyMemberRepository.save(
                PartyMember.builder().party(party12).user(u9).role(PartyMemberRole.LEADER).build()
        );

        // ── 7. ChatRooms + ChatMessages (6개) ──
        // u2 → p1(u1 글)
        ChatRoom room1 = ChatRoom.create(p1, u1, u2);
        chatRoomRepository.save(room1);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room1, u2, MessageType.TEXT, "안녕하세요! 서폿 지원합니다. 쓰레쉬 장인이에요"),
                ChatMessage.create(room1, u1, MessageType.TEXT, "반갑습니다! 티어 어떻게 되세요?"),
                ChatMessage.create(room1, u2, MessageType.TEXT, "플레1이고 시즌 최고 다이아였어요"),
                ChatMessage.create(room1, u1, MessageType.TEXT, "좋아요 같이 해봐요!")
        ));

        // u3 → p1(u1 글)
        ChatRoom room2 = ChatRoom.create(p1, u1, u3);
        chatRoomRepository.save(room2);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room2, u3, MessageType.TEXT, "서폿 지원합니다! 다이아3 알리스타 장인"),
                ChatMessage.create(room2, u1, MessageType.TEXT, "감사합니다 근데 이미 서폿 구했어요 ㅠ")
        ));

        // u5 → p2(u4 글)
        ChatRoom room3 = ChatRoom.create(p2, u4, u5);
        chatRoomRepository.save(room3);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room3, u5, MessageType.TEXT, "탑 지원합니다~"),
                ChatMessage.create(room3, u4, MessageType.TEXT, "환영합니다! 에메2면 딱 좋네요")
        ));

        // u8 → p4(u6 글)
        ChatRoom room4 = ChatRoom.create(p4, u6, u8);
        chatRoomRepository.save(room4);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room4, u8, MessageType.TEXT, "정글 지원합니다 마스터 리신 장인"),
                ChatMessage.create(room4, u6, MessageType.TEXT, "오 좋아요 듀오 합시다"),
                ChatMessage.create(room4, u8, MessageType.TEXT, "ㄱㄱ 바로 들어갈게요")
        ));

        // u7 → p10(u14 글)
        ChatRoom room5 = ChatRoom.create(p10, u14, u7);
        chatRoomRepository.save(room5);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room5, u7, MessageType.TEXT, "원딜 지원합니다! 그마 진스 장인이에요"),
                ChatMessage.create(room5, u14, MessageType.TEXT, "오 딱 좋아요! 바드 로밍 많이 다닐게요"),
                ChatMessage.create(room5, u7, MessageType.TEXT, "좋습니다 라인전 잘 버틸게요"),
                ChatMessage.create(room5, u14, MessageType.TEXT, "ㄱㄱ 파티 초대할게요")
        ));

        // u13 → p8(u11 글)
        ChatRoom room6 = ChatRoom.create(p8, u11, u13);
        chatRoomRepository.save(room6);
        chatMessageRepository.saveAll(List.of(
                ChatMessage.create(room6, u13, MessageType.TEXT, "칼바람 참여하고 싶어요~"),
                ChatMessage.create(room6, u11, MessageType.TEXT, "ㅎㅎ 환영합니다 바로 초대할게요")
        ));

        log.info("Test data seeded: users={}, posts={}, parties={}, chatRooms={}",
                15, 12, 12, 6);
    }

    private GameAccount buildGameAccount(String nickname, String tag, String puuid, int iconId, User user) {
        return GameAccount.builder()
                .gameNickname(nickname).gameTag(tag)
                .gameType(GAME_TYPE).puuid(puuid).profileIconId(iconId)
                .user(user).build();
    }

    private Rank buildRank(String queueType, String tier, String rank, int wins, int losses, double winRate, GameAccount ga) {
        return Rank.builder()
                .queueType(queueType).tier(tier).rank(rank)
                .wins(wins).losses(losses).winRate(winRate)
                .gameAccount(ga).build();
    }

    private FavoriteChampion buildChampion(GameAccount ga, int rank, int champId, String champName,
                                           int total, int wins, int losses, double winRate) {
        return FavoriteChampion.builder()
                .gameAccount(ga).rank(rank)
                .championId(champId).championName(champName)
                .totalGames(total).wins(wins).losses(losses).winRate(winRate)
                .build();
    }

    private Post buildPost(User user, GameAccount ga, GameMode mode, QueueType queue,
                           Position myPos, String lookingPos, boolean mic, int recruit, String memo) {
        return Post.builder()
                .user(user).gameAccount(ga)
                .gameMode(mode).queueType(queue)
                .myPosition(myPos).lookingPositions(lookingPos)
                .mic(mic).recruitCount(recruit)
                .memo(memo).build();
    }
}