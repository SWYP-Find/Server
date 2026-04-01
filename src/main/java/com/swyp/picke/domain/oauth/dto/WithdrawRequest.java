package com.swyp.picke.domain.oauth.dto;

import com.swyp.picke.domain.user.enums.WithdrawalReason;
import jakarta.validation.constraints.NotNull;

public record WithdrawRequest(
        @NotNull
        WithdrawalReason reason
) {}
