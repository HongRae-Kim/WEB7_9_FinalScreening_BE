package com.back.matchduo.domain.review.service;

import com.back.matchduo.domain.party.repository.PartyMemberRepository;
import com.back.matchduo.domain.review.dto.request.ReviewCreateRequest;
import com.back.matchduo.domain.review.dto.request.ReviewUpdateRequest;
import com.back.matchduo.domain.review.dto.response.*;
import com.back.matchduo.domain.review.entity.Review;
import com.back.matchduo.domain.review.entity.ReviewRequest;
import com.back.matchduo.domain.review.enums.ReviewEmoji;
import com.back.matchduo.domain.review.enums.ReviewRequestStatus;
import com.back.matchduo.domain.review.repository.ReviewRepository;
import com.back.matchduo.domain.review.repository.ReviewRequestRepository;
import com.back.matchduo.domain.user.entity.User;
import com.back.matchduo.domain.user.repository.UserRepository;
import com.back.matchduo.global.exeption.CustomErrorCode;
import com.back.matchduo.global.exeption.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;

    // 리뷰 작성
    public ReviewCreateResponse createReview(Long currentUserId, ReviewCreateRequest reqDto) {
        Long partyId = reqDto.partyId();
        Long revieweeId = reqDto.revieweeId();

        // 요청서 조회
        ReviewRequest reviewRequest = reviewRequestRepository.findByPartyIdAndRequestUserId(partyId, currentUserId).
                orElseThrow(() -> new CustomException(CustomErrorCode.PARTY_MEMBER_NOT_MATCH));

        // 리뷰요청관리 상태 검증 : COMPLETED인가
        if(reviewRequest.getStatus() != ReviewRequestStatus.COMPLETED) {
            throw new CustomException(CustomErrorCode.MATCH_NOT_END);
        }

        // 중복 작성 검증
        if(reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(partyId,currentUserId,revieweeId)) {
            throw new CustomException(CustomErrorCode.REVIEW_ALREADY_WRITTEN);
        }

        // 파티원 여부 검증
        if(!partyMemberRepository.existsByPartyIdAndUserId(partyId,revieweeId)) {
            throw new CustomException(CustomErrorCode.PARTY_MEMBER_NOT_MATCH);
        }

        User reviewer = reviewRequest.getRequestUser();
        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_USER));

        Review review = Review.builder()
                .party(reviewRequest.getParty())
                .reviewer(reviewer)
                .reviewee(reviewee)
                .reviewRequest(reviewRequest)
                .emoji(reqDto.emoji())
                .content(reqDto.content())
                .build();

        Review savedReview = reviewRepository.save(review);

        long totalTeamMembers = partyMemberRepository.countByPartyId(partyId) - 1;
        long myReviewCount = reviewRepository.countByPartyIdAndReviewerId(partyId, currentUserId);

        if (myReviewCount == totalTeamMembers) reviewRequest.deactivate();

        return ReviewCreateResponse.from(savedReview);
    }

    // 리뷰 수정
    public ReviewUpdateResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest reqDto) {
        Review review = reviewRepository.findByIdAndReviewerId(reviewId, userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.FORBIDDEN_REVIEW_MODIFY));

        review.update(reqDto.emoji(), reqDto.content());

        return ReviewUpdateResponse.from(review);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdAndReviewerId(reviewId, userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.FORBIDDEN_REVIEW_DELETE));

        review.deactivate();
    }

    // 특정 유저가 받은 리뷰목록 조회
    public List<ReviewListResponse> getReviewsReceivedByUser(Long userId) {
        List<Review> reviews = reviewRepository.findAllByRevieweeId(userId);

        return reviews.stream()
                .map(ReviewListResponse::from)
                .toList();
    }

    // 내가 작성한 리뷰 목록 조회
    public List<MyReviewListResponse> getMyWrittenReviews(Long userId) {
        List<Review> reviews = reviewRepository.findAllByReviewerId(userId);

        return reviews.stream()
                .map(MyReviewListResponse::from)
                .toList();
    }

    // 특정 유저 리뷰 분포 조회(비율)
    public ReviewDistributionResponse getReviewDistribution(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.NOT_FOUND_USER));
        List<Object[]> results = reviewRepository.countReviewEmojisByRevieweeId(userId);

        long goodCount = 0;
        long normalCount = 0;
        long badCount = 0;

        for (Object[] result : results) {
            ReviewEmoji emoji = (ReviewEmoji) result[0];
            Long count = (Long) result[1];

            switch (emoji) {
                case GOOD -> goodCount = count;
                case NORMAL -> normalCount = count;
                case BAD -> badCount = count;
            }
        }

        return ReviewDistributionResponse.of(userId,user.getNickname(),goodCount, normalCount, badCount);
    }

    public List<ReviewListResponse> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();

        return reviews.stream()
                .map(ReviewListResponse::from)
                .toList();
    }
}
