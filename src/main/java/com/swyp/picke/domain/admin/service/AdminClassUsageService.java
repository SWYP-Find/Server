package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.classroom.response.AdminAiErrorRow;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassAnalyticsResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassPerformanceResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminTicketUsageResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 이용권 계정 관리·성과 분석 집계 (Figma '어드민 추가 페이지').
 * 화면 단위 집계라 엔티티 저장소 대신 JPQL 묶음 조회로 센다. 한 번에 다루는 클래스 수가 작아 날짜 묶기는 자바에서 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClassUsageService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<ClassReportStatus> DONE = List.of(ClassReportStatus.SUCCEEDED, ClassReportStatus.LIMITED);
    private static final List<AiJobType> REPORT_JOBS = List.of(AiJobType.MEMBER_REPORT, AiJobType.CLASS_INSIGHT);
    private static final int MAX_RANGE_DAYS = 366;

    private final EntityManager em;
    private final ClassTicketRepository classTicketRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;

    /** 이용권 목록 한 페이지의 클래스 수·최근 활동. */
    public record TicketListStats(long classCount, LocalDateTime lastActiveAt) {}

    public Map<Long, TicketListStats> ticketListStats(Collection<Long> ticketIds) {
        if (ticketIds.isEmpty()) return Map.of();
        Map<Long, List<Long>> classesByTicket = classIdsByTicket(ticketIds);
        List<Long> classIds = classesByTicket.values().stream().flatMap(List::stream).toList();
        Map<Long, LocalDateTime> activity = lastActivity(classIds);
        Map<Long, TicketListStats> stats = new HashMap<>();
        for (Long ticketId : ticketIds) {
            List<Long> ids = classesByTicket.getOrDefault(ticketId, List.of());
            stats.put(ticketId, new TicketListStats(ids.size(), latest(ids, activity)));
        }
        return stats;
    }

    /** 등록 계정 이메일 (소셜 계정 이메일 중 하나). */
    public Map<Long, String> accountEmails(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, String> emails = new HashMap<>();
        for (Object[] row : em.createQuery("""
                        select s.user.id, s.providerEmail from UserSocialAccount s
                        where s.user.id in :ids and s.providerEmail is not null
                        order by s.id asc
                        """, Object[].class)
                .setParameter("ids", userIds).getResultList()) {
            emails.putIfAbsent((Long) row[0], (String) row[1]);
        }
        return emails;
    }

    public AdminTicketUsageResponse ticketUsage(Long ticketId) {
        if (!classTicketRepository.existsById(ticketId)) throw new CustomException(ErrorCode.TICKET_NOT_FOUND);
        LocalDateTime now = LocalDateTime.now(KST);
        List<Classroom> classes = classroomRepository.findAdminByTicketId(ticketId);
        List<Long> classIds = classes.stream().map(Classroom::getId).toList();
        Map<Long, Long> seats = countByClass("select m.classroom.id, count(m) from ClassMember m "
                + "where m.classroom.id in :ids and m.seatHeld = true group by m.classroom.id", classIds);
        Map<Long, Long> debates = countByClass("select d.classroom.id, count(d) from ClassDebateSession d "
                + "where d.classroom.id in :ids group by d.classroom.id", classIds);
        Map<Long, Long> completed = classIds.isEmpty() ? Map.of() : toCountMap(em.createQuery(
                        "select ub.classroom.id, count(ub) from UserBattle ub where ub.classroom.id in :ids "
                                + "and ub.step = :step group by ub.classroom.id", Object[].class)
                .setParameter("ids", classIds).setParameter("step", UserBattleStep.COMPLETED).getResultList());
        Map<Long, Long> reports = classIds.isEmpty() ? Map.of() : toCountMap(em.createQuery(
                        "select r.classroom.id, count(r) from ClassReport r where r.classroom.id in :ids "
                                + "and r.type = :type and r.status in :done group by r.classroom.id", Object[].class)
                .setParameter("ids", classIds).setParameter("type", ClassReportType.MEMBER_FEEDBACK)
                .setParameter("done", DONE).getResultList());
        long recentReports = classIds.isEmpty() ? 0 : em.createQuery(
                        "select count(r) from ClassReport r where r.classroom.id in :ids and r.status in :done "
                                + "and r.generatedAt >= :since", Long.class)
                .setParameter("ids", classIds).setParameter("done", DONE)
                .setParameter("since", now.minusDays(30)).getSingleResult();
        Map<Long, LocalDateTime> activity = lastActivity(classIds);

        List<AdminTicketUsageResponse.ClassUsage> rows = classes.stream()
                .map(c -> new AdminTicketUsageResponse.ClassUsage(c.getId(), c.getName(),
                        c.isClosed(now) ? AdminClassState.CLOSED : AdminClassState.OPEN,
                        seats.getOrDefault(c.getId(), 0L).intValue(), c.getCapacity(),
                        debates.getOrDefault(c.getId(), 0L), completed.getOrDefault(c.getId(), 0L),
                        reports.getOrDefault(c.getId(), 0L), activity.get(c.getId()), c.getCreatedAt()))
                .toList();
        return new AdminTicketUsageResponse(classes.size(),
                rows.stream().mapToLong(AdminTicketUsageResponse.ClassUsage::participantCount).sum(),
                rows.stream().mapToLong(AdminTicketUsageResponse.ClassUsage::battleCompletedCount).sum(),
                recentReports, latest(classIds, activity), rows);
    }

    public AdminClassPerformanceResponse classPerformance(Long classId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        Classroom classroom = classroomRepository.findByIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        List<ClassMember> targets = classMemberRepository.findActiveMembers(classId).stream()
                .filter(m -> m.getRole() != ClassMemberRole.OWNER || classroom.isOwnerParticipates())
                .toList();
        List<Long> userIds = targets.stream().map(m -> m.getUser().getId()).toList();

        Map<Long, UserBattleStep> steps = new HashMap<>();
        for (Object[] row : em.createQuery("select ub.user.id, ub.step from UserBattle ub where ub.classroom.id = :id",
                Object[].class).setParameter("id", classId).getResultList()) {
            steps.put((Long) row[0], (UserBattleStep) row[1]);
        }
        Map<Long, Long> comments = new HashMap<>();
        mergeCounts(comments, em.createQuery("select p.user.id, count(p) from Perspective p where p.classroom.id = :id "
                + "and p.status = :status group by p.user.id", Object[].class)
                .setParameter("id", classId).setParameter("status", PerspectiveStatus.PUBLISHED).getResultList());
        mergeCounts(comments, em.createQuery("select c.user.id, count(c) from PerspectiveComment c where c.classroom.id = :id "
                + "and c.hidden = false group by c.user.id", Object[].class)
                .setParameter("id", classId).getResultList());
        Map<Long, Object[]> reports = new HashMap<>();
        for (Object[] row : em.createQuery("select r.member.id, r.status, r.generatedAt from ClassReport r "
                        + "where r.classroom.id = :id and r.type = :type", Object[].class)
                .setParameter("id", classId).setParameter("type", ClassReportType.MEMBER_FEEDBACK).getResultList()) {
            reports.put((Long) row[0], row);
        }

        List<AdminClassPerformanceResponse.Participant> participants = targets.stream().map(m -> {
            Object[] report = reports.get(m.getId());
            return new AdminClassPerformanceResponse.Participant(m.getId(), m.getUser().getUserTag(), m.getDisplayName(),
                    m.getRole() == ClassMemberRole.OWNER, comments.getOrDefault(m.getUser().getId(), 0L),
                    steps.getOrDefault(m.getUser().getId(), UserBattleStep.NONE),
                    report == null ? null : (ClassReportStatus) report[1],
                    report == null ? null : (LocalDateTime) report[2]);
        }).toList();
        long started = userIds.stream().filter(id -> steps.getOrDefault(id, UserBattleStep.NONE) != UserBattleStep.NONE).count();
        long reportDone = participants.stream().filter(p -> p.reportStatus() != null && DONE.contains(p.reportStatus())).count();
        List<AdminAiErrorRow> errors = aiErrors(classId, null, null);

        int from = Math.min(page * size, participants.size());
        int to = Math.min(from + size, participants.size());
        var pageResult = new PageImpl<>(participants.subList(from, to), PageRequest.of(page, size), participants.size());
        String ownerEmail = accountEmails(List.of(classroom.getOwner().getId())).get(classroom.getOwner().getId());
        return new AdminClassPerformanceResponse(classId, ownerEmail,
                (int) classMemberRepository.countByClassroomIdAndSeatHeldTrue(classId), classroom.getCapacity(),
                started, participants.size(), reportDone, rate(reportDone, participants.size()),
                errors.stream().mapToLong(AdminAiErrorRow::count).sum(), errors, PageResponse.of(pageResult));
    }

    public AdminClassAnalyticsResponse analytics(LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate end = to == null ? now.toLocalDate() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();

        long issued = classTicketRepository.count();
        long revoked = em.createQuery("select count(t) from ClassTicket t where t.status = :revoked", Long.class)
                .setParameter("revoked", TicketStatus.REVOKED).getSingleResult();
        long registered = em.createQuery("select count(t) from ClassTicket t where t.activatedAt is not null "
                        + "and t.status <> :revoked", Long.class)
                .setParameter("revoked", TicketStatus.REVOKED).getSingleResult();
        long activeAccounts = em.createQuery("select count(distinct t.registeredUser.id) from ClassTicket t "
                        + "where t.status = :active and t.expiresAt > :now", Long.class)
                .setParameter("active", TicketStatus.ACTIVE).setParameter("now", now).getSingleResult();
        var operation = new AdminClassAnalyticsResponse.Operation(issued, revoked, issued - revoked, registered,
                rate(registered, issued - revoked), activeAccounts);

        long operators = em.createQuery("select count(distinct r.classroom.owner.id) from ClassReport r "
                        + "where r.status in :done and r.generatedAt >= :from and r.generatedAt < :to", Long.class)
                .setParameter("done", DONE).setParameter("from", rangeStart).setParameter("to", rangeEnd).getSingleResult();
        long aiReports = em.createQuery("select count(j) from AiJob j where j.type in :types and j.status = :ok "
                        + "and j.finishedAt >= :from and j.finishedAt < :to", Long.class)
                .setParameter("types", REPORT_JOBS).setParameter("ok", AiJobStatus.SUCCEEDED)
                .setParameter("from", rangeStart).setParameter("to", rangeEnd).getSingleResult();
        long battles = em.createQuery("select count(ub) from UserBattle ub where ub.classroom is not null "
                        + "and ub.step <> :none and ub.createdAt >= :from and ub.createdAt < :to", Long.class)
                .setParameter("none", UserBattleStep.NONE).setParameter("from", rangeStart).setParameter("to", rangeEnd)
                .getSingleResult();
        long battlesWithReport = em.createQuery("select count(ub) from UserBattle ub where ub.classroom is not null "
                        + "and ub.step <> :none and ub.createdAt >= :from and ub.createdAt < :to and exists ("
                        + "select r.id from ClassReport r where r.classroom = ub.classroom and r.member.user = ub.user "
                        + "and r.type = :type and r.status in :done)", Long.class)
                .setParameter("none", UserBattleStep.NONE).setParameter("from", rangeStart).setParameter("to", rangeEnd)
                .setParameter("type", ClassReportType.MEMBER_FEEDBACK).setParameter("done", DONE).getSingleResult();
        List<LocalDateTime> completedAt = em.createQuery("select r.generatedAt from ClassReport r where r.status in :done "
                        + "and r.generatedAt >= :from and r.generatedAt < :to", LocalDateTime.class)
                .setParameter("done", DONE).setParameter("from", rangeStart).setParameter("to", rangeEnd).getResultList();
        long viewed = em.createQuery("select count(r) from ClassReport r where r.status in :done "
                        + "and r.generatedAt >= :from and r.generatedAt < :to and exists ("
                        + "select v.id from ClassReportView v where v.reportId = r.id)", Long.class)
                .setParameter("done", DONE).setParameter("from", rangeStart).setParameter("to", rangeEnd).getSingleResult();
        var flow = new AdminClassAnalyticsResponse.Flow(operators, aiReports, battles, battlesWithReport,
                rate(battlesWithReport, battles), completedAt.size(), viewed, rate(viewed, completedAt.size()));

        Map<LocalDate, Long> daily = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) daily.put(d, 0L);
        completedAt.forEach(at -> daily.merge(at.toLocalDate(), 1L, Long::sum));
        List<AdminClassAnalyticsResponse.DailyCount> dailyReports = daily.entrySet().stream()
                .map(e -> new AdminClassAnalyticsResponse.DailyCount(e.getKey(), e.getValue())).toList();

        List<AdminAiErrorRow> errors = aiErrors(null, rangeStart, rangeEnd);
        return new AdminClassAnalyticsResponse(now, start, end, operation, flow, dailyReports,
                errors.stream().mapToLong(AdminAiErrorRow::count).sum(), errors);
    }

    /** 실패한 AI 작업을 종류·상태·오류 코드별로 센다. 반박 입력 분류의 REJECTED 는 입력 반려라 오류가 아니다. */
    private List<AdminAiErrorRow> aiErrors(Long classId, LocalDateTime from, LocalDateTime to) {
        StringBuilder jpql = new StringBuilder("select j.type, j.status, j.errorCode, count(j) from AiJob j where "
                + "(j.status = :failed or (j.status = :rejected and j.type <> :classify))");
        if (classId != null) jpql.append(" and j.classroom.id = :classId");
        if (from != null) jpql.append(" and j.updatedAt >= :from and j.updatedAt < :to");
        jpql.append(" group by j.type, j.status, j.errorCode");
        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("failed", AiJobStatus.FAILED).setParameter("rejected", AiJobStatus.REJECTED)
                .setParameter("classify", AiJobType.DEBATE_INPUT_CLASSIFY);
        if (classId != null) query.setParameter("classId", classId);
        if (from != null) query.setParameter("from", from).setParameter("to", to);
        return query.getResultList().stream()
                .map(row -> new AdminAiErrorRow((AiJobType) row[0], (AiJobStatus) row[1], (String) row[2], (Long) row[3]))
                .sorted(Comparator.comparingLong(AdminAiErrorRow::count).reversed()
                        .thenComparing(r -> r.type().name()))
                .toList();
    }

    private Map<Long, List<Long>> classIdsByTicket(Collection<Long> ticketIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        for (Object[] row : em.createQuery("select c.ticket.id, c.id from Classroom c where c.ticket.id in :ids "
                        + "and c.status = :active order by c.id asc", Object[].class)
                .setParameter("ids", ticketIds).setParameter("active", ClassStatus.ACTIVE).getResultList()) {
            result.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((Long) row[1]);
        }
        return result;
    }

    /** 클래스별 마지막 활동: 클래스 생성·참가·의견·댓글·배틀 진행·AI 토론 중 가장 늦은 시각. */
    private Map<Long, LocalDateTime> lastActivity(Collection<Long> classIds) {
        Map<Long, LocalDateTime> result = new HashMap<>();
        if (classIds.isEmpty()) return result;
        List<String> queries = List.of(
                "select c.id, c.createdAt from Classroom c where c.id in :ids",
                "select m.classroom.id, max(m.joinedAt) from ClassMember m where m.classroom.id in :ids group by m.classroom.id",
                "select p.classroom.id, max(p.createdAt) from Perspective p where p.classroom.id in :ids group by p.classroom.id",
                "select c.classroom.id, max(c.createdAt) from PerspectiveComment c where c.classroom.id in :ids group by c.classroom.id",
                "select ub.classroom.id, max(ub.updatedAt) from UserBattle ub where ub.classroom.id in :ids group by ub.classroom.id",
                "select d.classroom.id, max(d.updatedAt) from ClassDebateSession d where d.classroom.id in :ids group by d.classroom.id");
        for (String jpql : queries) {
            for (Object[] row : em.createQuery(jpql, Object[].class).setParameter("ids", classIds).getResultList()) {
                if (row[1] == null) continue;
                result.merge((Long) row[0], (LocalDateTime) row[1], (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return result;
    }

    private Map<Long, Long> countByClass(String jpql, Collection<Long> classIds) {
        if (classIds.isEmpty()) return Map.of();
        return toCountMap(em.createQuery(jpql, Object[].class).setParameter("ids", classIds).getResultList());
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        rows.forEach(row -> result.put((Long) row[0], (Long) row[1]));
        return result;
    }

    private static void mergeCounts(Map<Long, Long> target, List<Object[]> rows) {
        rows.forEach(row -> target.merge((Long) row[0], (Long) row[1], Long::sum));
    }

    private static LocalDateTime latest(List<Long> classIds, Map<Long, LocalDateTime> activity) {
        return classIds.stream().map(activity::get).filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    private static Double rate(long numerator, long denominator) {
        return denominator == 0 ? null : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }

    /** 목록 이메일 마스킹: 첫 글자 + *** + 도메인. */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.charAt(0) + "***" + email.substring(at) : "***";
    }

}
