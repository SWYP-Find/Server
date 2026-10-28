package com.swyp.picke.domain.funding.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 펀딩 운영 중 관리자가 수행한 변경 이력을 저장합니다.
@Entity
@Table(name = "funding_admin_audit_logs",
        indexes = @Index(name = "idx_funding_audit_target_created", columnList = "target_type, target_id, created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog extends BaseEntity {

    // 작업을 수행한 관리자 ID
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    // 관리자 작업 종류
    @Column(nullable = false, length = 50)
    private String action;

    // 작업 대상 유형
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    // 작업 대상 ID
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 작업 사유
    @Column(length = 500)
    private String reason;

    // 작업 상세 정보
    @Column(columnDefinition = "TEXT")
    private String metadata;

    public AdminAuditLog(Long adminUserId, String action, String targetType, Long targetId,
                         String reason, String metadata) {
        this.adminUserId = adminUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.metadata = metadata;
    }
}
