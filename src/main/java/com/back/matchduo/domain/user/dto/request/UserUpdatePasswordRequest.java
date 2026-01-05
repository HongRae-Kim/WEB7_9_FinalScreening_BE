package com.back.matchduo.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record UserUpdatePasswordRequest(
        @Schema(description = "현재 비밀번호", defaultValue = "string", example = "string")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,100}$",
                message = "비밀번호는 8글자 이상, 영어 대소문자와 숫자, 특수문자(!, @, #, $, %, ^, &, *)를 포함해야 합니다."
        )
        String password,

        @Schema(description = "새 비밀번호", defaultValue = "string", example = "string")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,100}$",
                message = "비밀번호는 8글자 이상, 영어 대소문자와 숫자, 특수문자(!, @, #, $, %, ^, &, *)를 포함해야 합니다."
        )
        String newPassword,

        @Schema(description = "새 비밀번호 확인", defaultValue = "string", example = "string")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,100}$",
                message = "비밀번호는 8글자 이상, 영어 대소문자와 숫자, 특수문자(!, @, #, $, %, ^, &, *)를 포함해야 합니다."
        )
        String newPasswordConfirm
) {
}