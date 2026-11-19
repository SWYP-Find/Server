package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.admin.dto.classroom.AdminClassState;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 클래스 상세. 멤버에는 클래스 표시 이름만 포함하고 계정 ID/연락처는 포함하지 않습니다. */
public record AdminClassDetailResponse(
        Long classId,
        String name,
        Owner owner,
        Ticket ticket,
        Content content,
        ClassBattleType battleType,
        Long topicVersionId,
        long participantCount,
        int capacity,
        Progress progress,
        LocalDateTime deadline,
        LocalDateTime closeAt,
        AdminClassState state,
        boolean commentRequired,
        boolean ownerParticipates,
        LocalDateTime createdAt,
        List<Member> members
) {
    public record Owner(String nickname) { }

    @Schema(description = "연결된 이용권과 구매 주문 요약. 이용권 원문 코드는 포함하지 않습니다.")
    public record Ticket(Long ticketId, FundingPlatform platform, String campaignId, String orderId,
                         String supporterName, RewardTier rewardTier, int purchaseQuantity, int capacity,
                         String codeSuffix, TicketCodeStatus codeStatus, TicketStatus status,
                         LocalDateTime deliveredAt, LocalDateTime registerBy, LocalDateTime activatedAt,
                         LocalDateTime expiresAt) { }

    public record Content(Long battleId, String title, String summary, String thumbnailUrl) { }

    @Schema(description = "활성 멤버 기준의 참여 진행 현황")
    public record Progress(int activeParticipants, int completedParticipants, double completionRate) { }

    @Schema(description = "클래스 내 표시 이름 기준의 활성 멤버")
    public record Member(String displayName, ClassMemberRole role, String characterUrl,
                         LocalDateTime joinedAt, boolean completed) { }
}
