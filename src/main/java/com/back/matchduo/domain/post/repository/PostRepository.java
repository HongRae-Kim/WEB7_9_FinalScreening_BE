package com.back.matchduo.domain.post.repository;

import com.back.matchduo.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
