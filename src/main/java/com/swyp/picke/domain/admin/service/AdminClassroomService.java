package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.classroom.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDashboardSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDetailResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassListItemResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.report.ClassResultStatsCalculator;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 클래스 목록과 요약 카드 조회. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClassroomService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CLOSING_SOON_DAYS = 30;

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassResultStatsCalculator statsCalculator;

    public AdminClassDashboardSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now(KST);
        return new AdminClassDashboardSummaryResponse(
                classroomRepository.countByStatus(ClassStatus.ACTIVE),
                classroomRepository.countOpenClasses(now),
                classMemberRepository.countSeatsHeldInActiveClasses(),
                classroomRepository.countClosingWithin(now, now.plusDays(CLOSING_SOON_DAYS)));
    }

    public PageResponse<AdminClassListItemResponse> getClasses(String keyword, AdminClassState state,
                                                                 int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        LocalDateTime now = LocalDateTime.now(KST);
        boolean includeOpen = state == null || state == AdminClassState.OPEN;
        boolean includeClosed = state == null || state == AdminClassState.CLOSED;
        Page<Classroom> result = classroomRepository.searchAdminClasses(normalizedKeyword, now,
                includeOpen, includeClosed, PageRequest.of(page, size));
        List<Long> classIds = result.getContent().stream().map(Classroom::getId).toList();
        Map<Long, Long> participantCounts = seatCounts(classIds);
        return PageResponse.of(result.map(classroom -> toListItem(classroom,
                participantCounts.getOrDefault(classroom.getId(), 0L), now)));
    }

    public AdminClassDetailResponse getClassDetail(Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = classroomRepository.findActiveForAdmin(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        List<ClassMember> members = classMemberRepository.findActiveMembers(classId);
        long participantCount = classMemberRepository.countByClassroomIdAndSeatHeldTrue(classId);
        ClassResultStatsCalculator.Result participation = statsCalculator.calculate(classroom);
        Set<Long> completedMemberIds = participation.completedMembers().stream()
                .map(ClassMember::getId).collect(Collectors.toSet());
        var owner = classroom.getOwner();
        var battle = classroom.getBattle();
        var ticket = classroom.getTicket();
        var order = ticket.getFundingOrder();
        return new AdminClassDetailResponse(classroom.getId(), classroom.getName(),
                new AdminClassDetailResponse.Owner(owner.getNickname()),
                new AdminClassDetailResponse.Ticket(ticket.getId(), order.getPlatform(), order.getCampaignId(),
                        order.getOrderId(), order.getSupporterName(), order.getRewardTier(), order.getQuantity(),
                        ticket.getCapacity(), ticket.getCodeSuffix(), ticket.getCodeStatus(), ticket.getStatus(),
                        ticket.getDeliveredAt(), ticket.getRegisterBy(), ticket.getActivatedAt(), ticket.getExpiresAt()),
                new AdminClassDetailResponse.Content(battle.getId(), battle.getTitle(), battle.getSummary(),
                        battle.getThumbnailUrl()),
                classroom.getBattleType(),
                classroom.getTopicVersion() == null ? null : classroom.getTopicVersion().getId(),
                participantCount, classroom.getCapacity(),
                new AdminClassDetailResponse.Progress(participation.stats().activeMemberCount(),
                        participation.stats().completedCount(), participation.stats().participationRate()),
                classroom.getDeadline(), classroom.getEffectiveCloseAt(), stateOf(classroom, now),
                classroom.isCommentRequired(), classroom.isOwnerParticipates(), classroom.getCreatedAt(),
                members.stream().map(member -> new AdminClassDetailResponse.Member(member.getDisplayName(),
                        member.getRole(), member.getUser().getCharacterUrl(), member.getJoinedAt(),
                        completedMemberIds.contains(member.getId()))).toList());
    }

    public byte[] exportClasses(String keyword, AdminClassState state) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        LocalDateTime now = LocalDateTime.now(KST);
        boolean includeOpen = state == null || state == AdminClassState.OPEN;
        boolean includeClosed = state == null || state == AdminClassState.CLOSED;
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("classId,name,ownerNickname,selectedContent,battleType,participantCount,capacity,state,closeAt,createdAt\r\n");
        int pageNumber = 0;
        Page<Classroom> page;
        do {
            page = classroomRepository.searchAdminClasses(normalizedKeyword, now, includeOpen, includeClosed,
                    PageRequest.of(pageNumber++, 500));
            Map<Long, Long> counts = seatCounts(page.getContent().stream().map(Classroom::getId).toList());
            for (Classroom classroom : page.getContent()) {
                AdminClassListItemResponse item = toListItem(classroom,
                        counts.getOrDefault(classroom.getId(), 0L), now);
                csv.append(item.classId()).append(',')
                        .append(csv(item.name())).append(',')
                        .append(csv(item.ownerNickname())).append(',')
                        .append(csv(item.selectedContent())).append(',')
                        .append(item.battleType()).append(',')
                        .append(item.participantCount()).append(',')
                        .append(item.capacity()).append(',')
                        .append(item.state()).append(',')
                        .append(item.closeAt() == null ? "" : item.closeAt()).append(',')
                        .append(item.createdAt() == null ? "" : item.createdAt()).append("\r\n");
            }
        } while (page.hasNext());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<Long, Long> seatCounts(List<Long> classIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (classIds.isEmpty()) return counts;
        for (Object[] row : classMemberRepository.countSeatsHeldByClassroomIds(classIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private AdminClassListItemResponse toListItem(Classroom classroom, long participantCount, LocalDateTime now) {
        var owner = classroom.getOwner();
        var battle = classroom.getBattle();
        return new AdminClassListItemResponse(classroom.getId(), classroom.getName(), owner.getNickname(),
                battle.getId(), battle.getTitle(), battle.getThumbnailUrl(),
                classroom.getBattleType(), participantCount, classroom.getCapacity(),
                classroom.getEffectiveCloseAt(), stateOf(classroom, now), classroom.getCreatedAt());
    }

    private AdminClassState stateOf(Classroom classroom, LocalDateTime now) {
        return classroom.isClosed(now) ? AdminClassState.CLOSED : AdminClassState.OPEN;
    }

    private String csv(String value) {
        return value == null ? "\"\"" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
