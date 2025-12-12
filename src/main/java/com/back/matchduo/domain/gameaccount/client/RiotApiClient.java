package com.back.matchduo.domain.gameaccount.client;

import com.back.matchduo.domain.gameaccount.dto.RiotApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Riot API 호출을 담당하는 Client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private final RestTemplate restTemplate;

    @Value("${riot.api.base-url:https://asia.api.riotgames.com}")
    private String riotApiBaseUrl;

    @Value("${riot.api.key:}")
    private String riotApiKey;

    /**
     * Riot ID로 계정 정보 조회
     * @param gameName 게임 닉네임
     * @param tagLine 게임 태그
     * @return Riot 계정 정보
     */
    public RiotApiDto.AccountResponse getAccountByRiotId(String gameName, String tagLine) {
        String url = UriComponentsBuilder
                .fromHttpUrl(riotApiBaseUrl)
                .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                .buildAndExpand(gameName, tagLine)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotApiDto.AccountResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotApiDto.AccountResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Riot API 호출 실패: gameName={}, tagLine={}, error={}", gameName, tagLine, e.getMessage());
            throw new RuntimeException("Riot API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }
}

