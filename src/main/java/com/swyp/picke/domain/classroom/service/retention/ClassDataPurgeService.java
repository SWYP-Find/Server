package com.swyp.picke.domain.classroom.service.retention;

import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 읽기 유예(이용권 종료 + 30일)가 끝난 클래스의 참여 원문·AI 대화·리포트를 파기한다 (PRD §18 보관과 삭제).
 * <ul>
 *   <li>파기: 클래스 의견·댓글과 그 좋아요·신고, AI 토론 세션·메시지, 클래스 리포트(스냅샷·개인 피드백·인사이트)</li>
 *   <li>유지: 원문이 없는 투표·진행 단계·멤버·클래스 행, ai_jobs(토큰·오류만 기록)</li>
 *   <li>클래스마다 한 트랜잭션. 끝나면 purged_at 을 찍어 다시 처리하지 않는다</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassDataPurgeService {

    private final ClassroomRepository classroomRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Long> findTargets(LocalDateTime now, int limit) {
        LocalDateTime cutoff = now.minusDays(ClassTicket.READ_GRACE_DAYS);
        return classroomRepository.findPurgeTargetIds(cutoff, PageRequest.of(0, limit));
    }

    /** 자식 행부터 지운다. 이미 파기됐거나 아직 유예 중이면 아무것도 하지 않는다 */
    @Transactional
    public boolean purge(Long classId, LocalDateTime now) {
        Classroom classroom = classroomRepository.findById(classId).orElse(null);
        if (classroom == null || classroom.getPurgedAt() != null) {
            return false;
        }
        LocalDateTime readUntil = classroom.getTicket().getReadGraceEndsAt();
        if (readUntil == null || now.isBefore(readUntil)) {
            return false;
        }

        String classComments = "select c.id from PerspectiveComment c where c.classroom.id = :classId or c.perspective.classroom.id = :classId";
        String classPerspectives = "select p.id from Perspective p where p.classroom.id = :classId";
        int commentLikes = delete("delete from CommentLike l where l.comment.id in (" + classComments + ")", classId);
        int commentReports = delete("delete from CommentReport r where r.comment.id in (" + classComments + ")", classId);
        int comments = delete("delete from PerspectiveComment c where c.id in (" + classComments + ")", classId);
        int perspectiveLikes = delete("delete from PerspectiveLike l where l.perspective.id in (" + classPerspectives + ")", classId);
        int perspectiveReports = delete("delete from PerspectiveReport r where r.perspective.id in (" + classPerspectives + ")", classId);
        int perspectives = delete("delete from Perspective p where p.classroom.id = :classId", classId);
        int messages = delete("delete from ClassDebateMessage m where m.session.id in "
                + "(select s.id from ClassDebateSession s where s.classroom.id = :classId)", classId);
        int sessions = delete("delete from ClassDebateSession s where s.classroom.id = :classId", classId);
        int reports = delete("delete from ClassReport r where r.classroom.id = :classId", classId);

        classroom.markPurged(now);
        log.info("[ClassDataPurge] classId={} perspectives={} comments={} likes={} reports={} debateSessions={} debateMessages={} classReports={}",
                classId, perspectives, comments, perspectiveLikes + commentLikes, perspectiveReports + commentReports,
                sessions, messages, reports);
        return true;
    }

    private int delete(String jpql, Long classId) {
        return entityManager.createQuery(jpql).setParameter("classId", classId).executeUpdate();
    }
}
