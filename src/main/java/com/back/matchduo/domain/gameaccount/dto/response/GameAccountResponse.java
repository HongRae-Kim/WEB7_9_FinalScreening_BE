package com.back.matchduo.domain.gameaccount.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameAccountResponse {
    private Long gameAccountId;
    private String gameNickname;
    private String gameTag;
    private String gameType;
    private String puuid;
    private Long userId;
}

