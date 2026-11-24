package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassReportView;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassReportViewRepository extends JpaRepository<ClassReportView, Long> {

    Optional<ClassReportView> findByReportIdAndUserId(Long reportId, Long userId);

    long countByClassIdAndReportId(Long classId, Long reportId);
}
