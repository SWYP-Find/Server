package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.repository.ClassReportRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 리포트 생성 오케스트레이터. 트랜잭션을 열지 않고 (1) 준비 → (2) AI 호출 → (3) 반영 을 잇는다.
 * 스케줄러가 주기적으로 {@link #runOnce(LocalDateTime)} 를 부르고, 한 리포트의 실패는 다른 리포트에 영향을 주지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassReportGenerationService {

    /** 한 번의 스케줄 틱에서 처리할 최대 리포트 수 (호출당 최대 45초 × 10 = 틱 사이 간격 안에 끝나도록) */
    static final int BATCH_SIZE = 10;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassroomRepository classroomRepository;
    private final ClassReportRepository classReportRepository;
    private final ClassReportSnapshotService snapshotService;
    private final ClassReportAiClient aiClient;
    private final ClassReportContentValidator validator;

    public void runOnce() {
        runOnce(LocalDateTime.now(KST));
    }

    /** 마감된 클래스 스냅샷 생성 → 멈춘 작업 복구 → 대기 리포트 생성. 처리한 리포트 수를 돌려준다 */
    public int runOnce(LocalDateTime now) {
        for (Classroom classroom : classroomRepository.findClosedWithoutReports(now)) {
            try {
                snapshotService.createSnapshots(classroom.getId(), now);
            } catch (Exception e) {
                log.error("[ClassReport] snapshot failed classId={}", classroom.getId(), e);
            }
        }
        try {
            snapshotService.recoverStale(now);
        } catch (Exception e) {
            log.error("[ClassReport] stale recovery failed", e);
        }
        List<ClassReport> pending = classReportRepository.findByStatusOrderByIdAsc(ClassReportStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        int processed = 0;
        for (ClassReport report : pending) {
            try {
                generate(report.getId(), LocalDateTime.now(KST));
                processed++;
            } catch (Exception e) {
                log.error("[ClassReport] generate failed reportId={}", report.getId(), e);
            }
        }
        return processed;
    }

    /** 리포트 1건 생성. PENDING 이 아니면 아무것도 하지 않는다 */
    public void generate(Long reportId, LocalDateTime now) {
        ClassReportSnapshotService.Prepared prepared = snapshotService.prepare(reportId, now);
        if (prepared == null || !prepared.needsAi()) {
            return;
        }
        ClassReportAiClient.AiChatResult result;
        Map<String, Object> validated;
        try {
            if (prepared.type() == ClassReportType.MEMBER_FEEDBACK) {
                result = aiClient.chatJson(ClassReportPrompts.MEMBER_FEEDBACK_SYSTEM,
                        ClassReportPrompts.memberFeedbackUser(prepared.memberInput()),
                        ClassReportPrompts.MEMBER_FEEDBACK_MAX_OUTPUT_TOKENS);
                validated = validator.validateMemberFeedback(result.json(), prepared.memberInput());
            } else {
                result = aiClient.chatJson(ClassReportPrompts.CLASS_INSIGHT_SYSTEM,
                        ClassReportPrompts.classInsightUser(prepared.insightInput()),
                        ClassReportPrompts.CLASS_INSIGHT_MAX_OUTPUT_TOKENS);
                validated = validator.validateClassInsight(result.json(), prepared.insightInput());
            }
        } catch (ReportValidationException e) {
            snapshotService.fail(reportId, prepared.jobId(), e.getCode(), true, LocalDateTime.now(KST));
            return;
        } catch (Exception e) {
            log.warn("[ClassReport] provider call failed reportId={} : {}", reportId, e.toString());
            snapshotService.fail(reportId, prepared.jobId(), errorCodeOf(e), false, LocalDateTime.now(KST));
            return;
        }
        snapshotService.complete(reportId, prepared.jobId(), validated, result, LocalDateTime.now(KST));
    }

    private static String errorCodeOf(Exception e) {
        String name = e.getClass().getSimpleName();
        if (name.contains("Timeout") || (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("Timeout"))) {
            return "TIMEOUT";
        }
        return name.length() > 50 ? name.substring(0, 50) : name;
    }
}
