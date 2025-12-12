package com.back.matchduo.domain.gameaccount.service;

import com.back.matchduo.domain.gameaccount.dto.GameAccountDto;
import com.back.matchduo.domain.gameaccount.entity.GameAccount;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GameAccountService {

    private final GameAccountRepository gameAccountRepository;
    private final UserRepository userRepository;

    /**
     * 게임 계정 생성 (닉네임과 태그 저장)
     * 한 게임에 한 가지 계정만 등록 가능
     * @param request 게임 타입, 닉네임, 태그, 유저 ID를 포함한 요청 DTO
     * @return 생성된 게임 계정 정보
     */
    public GameAccountDto.Response createGameAccount(GameAccountDto.CreateRequest request) {
        // 임시: User 조회 (나중에 인증 정보로 대체)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. userId: " + request.getUserId()));

        // 중복 체크: 같은 유저가 같은 게임 타입의 계정을 이미 가지고 있는지 확인
        gameAccountRepository.findByUser_IdAndGameType(request.getUserId(), request.getGameType())
                .ifPresent(existingAccount -> {
                    throw new IllegalArgumentException(
                        String.format("이미 %s 게임 계정이 등록되어 있습니다. (기존 계정 ID: %d)", 
                            request.getGameType(), existingAccount.getGameAccountId())
                    );
                });

        // 게임 계정 생성
        GameAccount gameAccount = GameAccount.builder()
                .gameNickname(request.getGameNickname())
                .gameTag(request.getGameTag())
                .gameType(request.getGameType())
                .user(user)
                .build();

        GameAccount savedGameAccount = gameAccountRepository.save(gameAccount);

        return GameAccountDto.Response.builder()
                .gameAccountId(savedGameAccount.getGameAccountId())
                .gameNickname(savedGameAccount.getGameNickname())
                .gameTag(savedGameAccount.getGameTag())
                .gameType(savedGameAccount.getGameType())
                .userId(savedGameAccount.getUser().getId())
                .build();
    }

    /**
     * 게임 계정 조회
     * @param gameAccountId 게임 계정 ID
     * @return 게임 계정 정보
     */
    @Transactional(readOnly = true)
    public GameAccountDto.Response getGameAccount(Long gameAccountId) {
        GameAccount gameAccount = gameAccountRepository.findById(gameAccountId)
                .orElseThrow(() -> new IllegalArgumentException("게임 계정을 찾을 수 없습니다. gameAccountId: " + gameAccountId));

        return GameAccountDto.Response.builder()
                .gameAccountId(gameAccount.getGameAccountId())
                .gameNickname(gameAccount.getGameNickname())
                .gameTag(gameAccount.getGameTag())
                .gameType(gameAccount.getGameType())
                .userId(gameAccount.getUser().getId())
                .build();
    }
}
