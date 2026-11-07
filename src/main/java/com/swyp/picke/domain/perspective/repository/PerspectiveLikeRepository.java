package com.swyp.picke.domain.perspective.repository;

import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerspectiveLikeRepository extends JpaRepository<PerspectiveLike, Long> {

    boolean existsByPerspectiveAndUserId(Perspective perspective, Long userId);

    Optional<PerspectiveLike> findByPerspectiveAndUserId(Perspective perspective, Long userId);

    long countByPerspective(Perspective perspective);

    @Query("SELECT l FROM PerspectiveLike l JOIN FETCH l.perspective WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<PerspectiveLike> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("""
            select count(l) from PerspectiveLike l
            where l.perspective.classroom.id = :classId
              and l.perspective.user.id = :userId
              and l.perspective.status = com.swyp.picke.domain.perspective.enums.PerspectiveStatus.PUBLISHED
            """)
    long countReceivedByUserInClass(@org.springframework.data.repository.query.Param("classId") Long classId,
                                    @org.springframework.data.repository.query.Param("userId") Long userId);

    void deleteAllByPerspective(Perspective perspective);
}
