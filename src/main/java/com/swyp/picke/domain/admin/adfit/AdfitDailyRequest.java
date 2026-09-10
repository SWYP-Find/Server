package com.swyp.picke.domain.admin.adfit;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AdfitDailyRequest(
        @NotNull LocalDate date,
        @NotNull AdfitUnit unit,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal revenue,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 2) BigDecimal cost,
        @NotNull AdfitCostBasis costBasis
) {}
