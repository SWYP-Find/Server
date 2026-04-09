package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.CommentLike;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentAndUserId(PerspectiveComment comment, Long userId);

    Optional<CommentLike> findByCommentAndUserId(PerspectiveComment comment, Long userId);
}
