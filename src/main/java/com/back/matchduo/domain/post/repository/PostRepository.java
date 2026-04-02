package com.back.matchduo.domain.post.repository;

import com.back.matchduo.domain.post.entity.Post;
import com.back.matchduo.domain.post.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 게시글 ID 목록 조회
    // gameMode는 enum/basic 필드라 fetch join 대상이 아니다.
    @Query("SELECT p FROM Post p WHERE p.id IN :ids")
    List<Post> findAllByIdInWithGameMode(@Param("ids") List<Long> ids);

    void deleteAllByUser_Id(Long userId);

    // 특정 게임 계정을 참조하는 활성 Post 조회 (isActive = true, status != CLOSED)
    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.gameAccount.gameAccountId = :gameAccountId AND p.isActive = true AND p.status != 'CLOSED'")
    boolean existsActivePostByGameAccountId(@Param("gameAccountId") Long gameAccountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.status = :status where p.id = :postId")
    int updateStatusById(@Param("postId") Long postId, @Param("status") PostStatus status);

    Optional<Post> findFirstByMemo(String memo);
}
