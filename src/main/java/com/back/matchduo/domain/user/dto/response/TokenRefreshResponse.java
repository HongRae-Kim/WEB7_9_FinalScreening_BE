package com.back.matchduo.domain.user.dto.response;

public record TokenRefreshResponse(String accessToken, String refreshToken) {
}
