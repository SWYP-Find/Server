package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.Notice;
import com.swyp.app.domain.user.entity.NoticeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByTypeOrderByIsPinnedDescPublishedAtDesc(NoticeType type);

    List<Notice> findAllByOrderByIsPinnedDescPublishedAtDesc();
}
