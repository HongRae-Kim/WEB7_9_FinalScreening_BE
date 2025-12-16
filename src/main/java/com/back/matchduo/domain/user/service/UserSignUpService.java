package com.back.matchduo.domain.user.service;

import com.back.matchduo.domain.user.dto.request.UserSignUpRequest;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserSignUpService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    public void signUp(UserSignUpRequest request) {

        //이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(CustomErrorCode.DUPLICATE_EMAIL);
        }

        //이메일 인증번호 검증
        if (!emailVerificationService.verifyCode(
                request.email(),
                request.verification_code()
        )) {
            throw new CustomException(CustomErrorCode.INVALID_VERIFICATION_CODE);
        }

        //비밀번호 / 비밀번호 확인
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(CustomErrorCode.WRONG_PASSWORD);
        }

        //비밀번호 암호화
        String encodedPassword = request.password();

        //User 생성
        User user = User.createUser(
                request.email(),
                encodedPassword
        );

        //저장
        userRepository.save(user);
    }
}