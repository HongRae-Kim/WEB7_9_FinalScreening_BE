package com.back.matchduo.domain.post.repository;

import com.back.matchduo.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 게시글 ID 목록으로 조회 + GameMode 함께 조회 (N+1 방지)
    // 내가 참여한 파티 리스트를 보여줄 때 사용
    @Query("SELECT p FROM Post p JOIN FETCH p.gameMode WHERE p.id IN :ids")
    List<Post> findAllByIdInWithGameMode(@Param("ids") List<Long> ids);

    void deleteAllByUser_Id(Long userId);

    // 특정 게임 계정을 참조하는 활성 Post 조회 (isActive = true, status != CLOSED)
    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.gameAccount.gameAccountId = :gameAccountId AND p.isActive = true AND p.status != 'CLOSED'")
    boolean existsActivePostByGameAccountId(@Param("gameAccountId") Long gameAccountId);
}
