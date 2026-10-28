package com.swyp.picke.domain.funding.repository;

import com.swyp.picke.domain.funding.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
