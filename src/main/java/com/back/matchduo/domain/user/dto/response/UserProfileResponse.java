package com.back.matchduo.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String profileImage;
    private String nickname;
    private String comment;

    public static UserProfileResponse from(
            Long id,
            String email,
            String profileImagePath,
            String nickname,
            String comment,
            String baseUrl
    ) {
        return UserProfileResponse.builder()
                .id(id)
                .email(email)
                .profileImage(
                        profileImagePath != null
                                ? baseUrl + profileImagePath
                                : null
                )
                .nickname(nickname)
                .comment(comment)
                .build();
    }
}