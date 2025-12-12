package com.back.matchduo.domain.gameaccount.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Riot API 응답 DTO
 */
public class RiotApiDto {

    /**
     * Riot 계정 정보 응답
     * /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine} 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountResponse {
        @JsonProperty("puuid")
        private String puuid;

        @JsonProperty("gameName")
        private String gameName;

        @JsonProperty("tagLine")
        private String tagLine;
    }
}

