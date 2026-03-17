package com.back.matchduo.domain.post.service;

import com.back.matchduo.domain.party.entity.Party;
import com.back.matchduo.domain.party.repository.PartyRepository;
import com.back.matchduo.domain.post.dto.request.PostCreateRequest;
import com.back.matchduo.domain.post.dto.request.PostStatusUpdateRequest;
import com.back.matchduo.domain.post.dto.request.PostUpdateRequest;
import com.back.matchduo.domain.post.dto.response.PostCreateResponse;
import com.back.matchduo.domain.post.dto.response.PostDeleteResponse;
import com.back.matchduo.domain.post.dto.response.PostListResponse;
import com.back.matchduo.domain.post.dto.response.PostStatusUpdateResponse;
import com.back.matchduo.domain.post.dto.response.PostUpdateResponse;
import com.back.matchduo.domain.post.entity.GameMode;
import com.back.matchduo.domain.post.entity.Post;
import com.back.matchduo.domain.post.entity.PostStatus;
import com.back.matchduo.domain.post.entity.QueueType;
import com.back.matchduo.domain.post.repository.PostRepository;
import com.back.matchduo.global.exception.CustomErrorCode;
import com.back.matchduo.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PartyRepository partyRepository; // 👈 [추가] 파티 저장소 주입
    private final PostValidator postValidator;
    private final PostListFacade postListFacade;

    // 모집글 생성
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, Long userId) {
        return postListFacade.createPostWithPartyView(request, userId);
    }

    // 모집글 목록 조회
    public PostListResponse getPostList(
            Long cursor,
            Integer size,
            PostStatus status,
            QueueType queueType,
            GameMode gameMode,
            String myPositions,
            String tier,
            Long currentUserId
    ) {
        return postListFacade.getPostList(cursor, size, status, queueType, gameMode, myPositions, tier, currentUserId);
    }

    // 모집글 수정
    @Transactional
    public PostUpdateResponse updatePost(Long postId, PostUpdateRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));

        if (!post.getIsActive()) {
            throw new CustomException(CustomErrorCode.POST_ALREADY_DELETED); // 혹은 POST_NOT_FOUND
        }

        postValidator.validatePostOwner(post, userId);
        return postListFacade.updatePostWithPartyView(post, request);
    }

    // 상태 변경
    @Transactional
    public PostStatusUpdateResponse updatePostStatus(Long postId, PostStatusUpdateRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));

        postValidator.validatePostOwner(post, userId);
        postValidator.validateStatusUpdateAllowed(request.status());

        post.updateStatus(request.status());
        return PostStatusUpdateResponse.of(post);
    }

    // 모집글 단건 조회 (작성자 검증)
    public PostUpdateResponse getPostDetail(Long postId,Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));

        if (!post.getIsActive()) { // getter 이름은 Entity에 따라 isActive() 또는 getIsActive()
            throw new CustomException(CustomErrorCode.POST_ALREADY_DELETED);
        }

        // postValidator.validatePostOwner(post, userId);

        return postListFacade.buildPostDetailForEdit(post);
    }

    // 삭제
    @Transactional
    public PostDeleteResponse deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.POST_NOT_FOUND));

        postValidator.validatePostOwner(post, userId);

        if (!post.getIsActive()) {
            throw new CustomException(CustomErrorCode.POST_ALREADY_DELETED); // 혹은 POST_NOT_FOUND
        }

        // [수정] 삭제 시 상태도 CLOSED로 변경하여 데이터 정합성 유지
        post.updateStatus(PostStatus.CLOSED);
        post.deactivate(); // Soft Delete
        postRepository.save(post); // 👈 [핵심] 변경 사항 강제 저장

        // Party도 종료 처리
        Party party = partyRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(CustomErrorCode.PARTY_NOT_FOUND));
        party.closeParty();
        partyRepository.save(party); // 👈 [핵심] Party도 강제 저장

        return PostDeleteResponse.of(postId);
    }
}
