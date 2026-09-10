package com.swyp.picke.domain.admin.adfit;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "adfit_daily_reports", uniqueConstraints = @UniqueConstraint(
        name = "uk_adfit_daily_date_unit", columnNames = {"report_date", "ad_unit"}))
public class AdfitDaily extends BaseEntity {
    @Column(name = "report_date", nullable = false)
    private LocalDate date;
    @Enumerated(EnumType.STRING)
    @Column(name = "ad_unit", nullable = false, length = 30)
    private AdfitUnit unit;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal revenue;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal cost;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdfitCostBasis costBasis;

    public AdfitDaily(LocalDate date, AdfitUnit unit) {
        this.date = date;
        this.unit = unit;
    }

    public void update(BigDecimal revenue, BigDecimal cost, AdfitCostBasis costBasis) {
        this.revenue = revenue;
        this.cost = cost;
        this.costBasis = costBasis;
    }
}
