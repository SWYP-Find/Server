package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import java.time.LocalDateTime;
import java.util.List;

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
    public record Owner(String nickname) {}

    public record Ticket(
            Long ticketId,
            String platform,
            String campaignId,
            String orderId,
            String supporterName,
            String rewardTier,
            int purchaseQuantity,
            int capacity,
            String codeSuffix,
            String codeStatus,
            String status,
            LocalDateTime deliveredAt,
            LocalDateTime registerBy,
            LocalDateTime activatedAt,
            LocalDateTime expiresAt
    ) {}

    public record Content(Long battleId, String title, String summary, String thumbnailUrl) {}

    public record Progress(long activeParticipants, long completedParticipants, double completionRate) {}

    public record Member(String displayName, String role, String characterUrl, LocalDateTime joinedAt, boolean completed) {}
}
