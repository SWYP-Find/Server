package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.classroom.entity.ClassReportView;
import com.swyp.picke.domain.classroom.repository.ClassReportViewRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리포트 열람을 기록한다 (#689). 리포트 조회는 읽기 전용 트랜잭션이라 별도 트랜잭션으로 쓴다.
 * 기록에 실패해도 조회 응답은 그대로 나간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassReportViewRecorder {

    private final ClassReportViewRepository repository;

    /** @return 이 사용자의 첫 열람이면 true */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean record(Long classId, Long reportId, Long userId, LocalDateTime now) {
        try {
            ClassReportView existing = repository.findByReportIdAndUserId(reportId, userId).orElse(null);
            if (existing != null) {
                existing.viewAgain(now);
                return false;
            }
            repository.saveAndFlush(new ClassReportView(classId, reportId, userId, now));
            return true;
        } catch (DataIntegrityViolationException e) {
            // 두 기기에서 동시에 처음 열었다: 먼저 들어간 행이 첫 열람이다
            return false;
        }
    }
}
