package com.back.matchduo.domain.user.service;

import com.back.matchduo.domain.auth.refresh.repository.RefreshTokenRepository;
import com.back.matchduo.domain.chat.repository.ChatMessageRepository;
import com.back.matchduo.domain.gameaccount.repository.GameAccountRepository;
import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.review.repository.ReviewRepository;
import com.back.matchduo.domain.user.dto.request.UserLoginRequest;
import com.back.matchduo.domain.user.dto.response.UserLoginResponse;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import com.back.matchduo.global.security.cookie.AuthCookieProvider;
import com.back.matchduo.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional
@RequiredArgsConstructor
public class UserLoginService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthCookieProvider cookieProvider;
    private final ChatMessageRepository chatMessageRepository;
    private final GameAccountRepository gameAccountRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ReviewRepository reviewRepository;

    //회원 탈퇴 기능
    public void resign(Long userId, HttpServletResponse res) {
        // 1. 채팅 메시지 삭제
        chatMessageRepository.deleteBySenderId(userId);

        // 2. 파티 멤버 기록 삭제
        partyMemberRepository.deleteAllByUser_Id(userId);

        // 3. 게임 계정 삭제
        gameAccountRepository.deleteAllByUser_Id(userId);

        // 4. 리뷰 전부 삭제
        reviewRepository.deleteByReviewerId(userId);
        reviewRepository.deleteByRevieweeId(userId);

        // 5. DB에서 해당 유저의 리프레시 토큰 삭제 (로그아웃 로직과 동일)
        refreshTokenRepository.deleteByUserId(userId);

        // 6. DB에서 유저 엔티티 삭제
        userRepository.deleteById(userId);

        // 7. 클라이언트의 쿠키 만료 처리
        res.addHeader("Set-Cookie", cookieProvider.expireAccessTokenCookie().toString());
        res.addHeader("Set-Cookie", cookieProvider.expireRefreshTokenCookie().toString());
    }
}