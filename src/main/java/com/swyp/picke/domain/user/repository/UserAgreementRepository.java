package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {
}
