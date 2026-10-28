package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.CommentLike;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentAndUserId(PerspectiveComment comment, Long userId);

    Optional<CommentLike> findByCommentAndUserId(PerspectiveComment comment, Long userId);

    void deleteAllByCommentIn(List<PerspectiveComment> comments);

    void deleteAllByComment(PerspectiveComment comment);

    @Query("""
            select count(l) from CommentLike l
            where l.comment.classroom.id = :classId
              and l.comment.user.id = :userId
              and l.comment.hidden = false
              and l.comment.perspective.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
            """)
    long countReceivedByUserInClass(@Param("classId") Long classId, @Param("userId") Long userId);
}
