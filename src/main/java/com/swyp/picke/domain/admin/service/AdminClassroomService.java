package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.classroom.request.AdminClassUpdateRequest;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassDetailResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassListItemResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassState;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminClassSummaryResponse;
import com.swyp.picke.domain.admin.dto.classroom.response.AdminTicketSummaryResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.ClassImageUrlResolver;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.entity.UserBattle;
import com.swyp.picke.domain.user.entity.UserProfile;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import com.swyp.picke.domain.user.repository.UserProfileRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.common.response.PageResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminClassroomService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassTicketRepository classTicketRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserBattleRepository userBattleRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final ClassImageUrlResolver imageUrls;

    public AdminClassSummaryResponse getClassSummary() {
        LocalDateTime now = LocalDateTime.now(KST);
        return new AdminClassSummaryResponse(
                classroomRepository.countByStatus(ClassStatus.ACTIVE),
                classroomRepository.countAdminOpenClasses(now),
                classMemberRepository.countAdminSeatsHeld(),
                classroomRepository.countAdminClosingWithin(now, now.plusDays(30)));
    }

    public PageResponse<AdminClassListItemResponse> searchClasses(
            String keyword, AdminClassState state, Integer capacity, ClassBattleType battleType, int page, int size) {
        validatePage(page, size);
        if (capacity != null && capacity != 5 && capacity != 15 && capacity != 30) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        LocalDateTime now = LocalDateTime.now(KST);
        Page<Classroom> result = classroomRepository.searchAdmin(
                keyword == null || keyword.isBlank() ? null : keyword.strip(),
                state == null ? null : state == AdminClassState.CLOSED,
                battleType, capacity, now, PageRequest.of(page, size));
        Map<Long, Long> counts = heldSeatCounts(result.getContent());
        List<AdminClassListItemResponse> content = result.getContent().stream()
                .map(classroom -> toListItem(classroom, now, counts)).toList();
        return PageResponse.of(new PageImpl<>(content, result.getPageable(), result.getTotalElements()));
    }

    public AdminClassDetailResponse getClassDetail(Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = classroomRepository.findByIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        List<ClassMember> members = classMemberRepository.findActiveMembers(classroom.getId());
        List<Long> userIds = members.stream().map(member -> member.getUser().getId()).toList();
        Map<Long, UserProfile> profiles = userIds.isEmpty() ? Map.of() : userProfileRepository.findAllByUserIdIn(userIds)
                .stream().collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));
        Map<Long, UserBattleStep> steps = userBattleRepository.findByClassroomId(classroom.getId()).stream()
                .collect(Collectors.toMap(battle -> battle.getUser().getId(), UserBattle::getStep, (left, right) -> right));
        Set<Long> usersWithPublishedPerspective = classroom.isCommentRequired()
                ? perspectiveRepository.findUserIdsByClassroomIdAndStatus(classroom.getId(), PerspectiveStatus.PUBLISHED)
                : Set.of();

        List<AdminClassDetailResponse.Member> adminMembers = members.stream().map(member -> {
            Long userId = member.getUser().getId();
            UserProfile profile = profiles.get(userId);
            CharacterType characterType = profile == null ? null : profile.getCharacterType();
            UserBattleStep step = steps.get(userId);
            boolean completed = step == UserBattleStep.COMPLETED
                    && (!classroom.isCommentRequired() || usersWithPublishedPerspective.contains(userId));
            return new AdminClassDetailResponse.Member(member.getDisplayName(), member.getRole().name(),
                    characterType == null ? null : imageUrls.avatar(characterType), member.getJoinedAt(), completed);
        }).toList();

        long activeCount = members.size();
        long completedCount = adminMembers.stream().filter(AdminClassDetailResponse.Member::completed).count();
        double completionRate = activeCount == 0 ? 0 : completedCount * 100.0 / activeCount;
        ClassTicket ticket = classroom.getTicket();
        FundingOrder order = ticket.getFundingOrder();
        String nickname = classroom.getOwner().getNickname();
        return new AdminClassDetailResponse(classroom.getId(), classroom.getName(),
                new AdminClassDetailResponse.Owner(nickname == null || nickname.isBlank() ? null : nickname.strip()),
                new AdminClassDetailResponse.Ticket(ticket.getId(), order.getPlatform().name(), order.getCampaignId(),
                        order.getOrderId(), order.getSupporterName(), order.getRewardTier().name(), order.getQuantity(),
                        ticket.getCapacity(), ticket.getCodeSuffix(), ticket.getCodeStatus().name(), ticket.getStatus().name(),
                        ticket.getDeliveredAt(), ticket.getRegisterBy(), ticket.getActivatedAt(), ticket.getExpiresAt()),
                new AdminClassDetailResponse.Content(classroom.getBattle().getId(), classroom.getBattle().getTitle(),
                        classroom.getBattle().getSummary(), imageUrls.battleThumbnail(classroom.getBattle())),
                classroom.getBattleType(), classroom.getTopicVersion() == null ? null : classroom.getTopicVersion().getId(),
                classMemberRepository.countByClassroomIdAndSeatHeldTrue(classroom.getId()), classroom.getCapacity(),
                new AdminClassDetailResponse.Progress(activeCount, completedCount, completionRate),
                classroom.getDeadline(), classroom.getEffectiveCloseAt(), stateOf(classroom, now),
                classroom.isCommentRequired(), classroom.isOwnerParticipates(), classroom.getCreatedAt(), adminMembers);
    }

    @Transactional
    public AdminClassDetailResponse updateClass(Long classId, AdminClassUpdateRequest request) {
        Classroom classroom = classroomRepository.findByIdAndStatusForUpdate(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(KST);
        if (classroom.isClosed(now)) throw new CustomException(ErrorCode.CLASS_CLOSED);
        if (request.name() == null && request.deadline() == null) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }

        LocalDateTime oldDeadline = classroom.getDeadline();
        if (request.name() != null) {
            String name = request.name().strip();
            if (name.isEmpty()) throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
            classroom.rename(name);
        }
        if (request.deadline() != null) {
            LocalDateTime deadline = request.deadline();
            LocalDateTime ticketExpiry = classroom.getTicket().getExpiresAt();
            if (!deadline.isAfter(now) || ticketExpiry == null || deadline.isAfter(ticketExpiry)) {
                throw new CustomException(ErrorCode.CLASS_INVALID_DEADLINE);
            }
            if (oldDeadline == null || !deadline.isAfter(oldDeadline)) {
                throw new CustomException(ErrorCode.CLASS_DEADLINE_NOT_EXTENDED);
            }
            classroom.extendDeadline(deadline);
        }

        return getClassDetail(classId);
    }

    public AdminTicketSummaryResponse getTicketSummary() {
        LocalDateTime now = LocalDateTime.now(KST);
        return new AdminTicketSummaryResponse(
                classTicketRepository.countByStatusNot(TicketStatus.REVOKED),
                classTicketRepository.countAdminUsableTickets(now),
                classTicketRepository.countAdminUnregisteredTickets(now),
                classTicketRepository.countAdminExpiringTickets(now, now.plusDays(30)));
    }

    private AdminClassListItemResponse toListItem(Classroom classroom, LocalDateTime now, Map<Long, Long> counts) {
        return new AdminClassListItemResponse(classroom.getId(), classroom.getName(), classroom.getOwner().getNickname(),
                classroom.getBattle().getId(), classroom.getBattle().getTitle(), imageUrls.battleThumbnail(classroom.getBattle()),
                classroom.getBattleType(), counts.getOrDefault(classroom.getId(), 0L), classroom.getCapacity(),
                classroom.getEffectiveCloseAt(), stateOf(classroom, now), classroom.getCreatedAt());
    }

    private Map<Long, Long> heldSeatCounts(List<Classroom> classrooms) {
        if (classrooms.isEmpty()) return Map.of();
        List<Long> ids = classrooms.stream().map(Classroom::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : classMemberRepository.countSeatsHeldByClassroomIds(ids)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private AdminClassState stateOf(Classroom classroom, LocalDateTime now) {
        return classroom.isClosed(now) ? AdminClassState.CLOSED : AdminClassState.OPEN;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
    }

}
