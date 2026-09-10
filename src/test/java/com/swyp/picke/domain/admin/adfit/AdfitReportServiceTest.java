package com.swyp.picke.domain.admin.adfit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdfitReportServiceTest {
    @Mock AdfitDailyRepository repository;
    @InjectMocks AdfitReportService service;
    private final LocalDate date = LocalDate.of(2026, 9, 1);

    private AdfitDaily day(LocalDate date, String revenue, String cost, AdfitCostBasis basis) {
        AdfitDaily day = new AdfitDaily(date, AdfitUnit.NATIVE_WIDE);
        day.update(new BigDecimal(revenue), new BigDecimal(cost), basis);
        return day;
    }

    @Test void calculatesProfitBasedRoiOnceForSharedUnit() {
        when(repository.findAllByDateBetweenOrderByDateDesc(date, date.plusDays(1))).thenReturn(List.of(
                day(date, "200", "100", AdfitCostBasis.AD_OPERATIONS),
                day(date.plusDays(1), "100", "50", AdfitCostBasis.AD_OPERATIONS)));
        var result = service.report(date, date.plusDays(1));
        assertThat(result.units()).hasSize(3);
        var unit = result.units().getFirst();
        assertThat(unit.placements()).hasSize(3);
        assertThat(unit.revenue()).isEqualByComparingTo("300");
        assertThat(unit.cost()).isEqualByComparingTo("150");
        assertThat(unit.roi()).isEqualByComparingTo("100");
    }

    @Test void missingRevenueIsNotZero() {
        when(repository.findAllByDateBetweenOrderByDateDesc(date, date)).thenReturn(List.of());
        var unit = service.report(date, date).units().getFirst();
        assertThat(unit.revenue()).isNull();
        assertThat(unit.cost()).isNull();
        assertThat(unit.roi()).isNull();
        assertThat(unit.reportedDays()).isZero();
    }

    @Test void partialPeriodDoesNotReportRoi() {
        when(repository.findAllByDateBetweenOrderByDateDesc(date, date.plusDays(1)))
                .thenReturn(List.of(day(date, "200", "100", AdfitCostBasis.AD_OPERATIONS)));
        var unit = service.report(date, date.plusDays(1)).units().getFirst();
        assertThat(unit.revenue()).isEqualByComparingTo("200");
        assertThat(unit.roi()).isNull();
        assertThat(unit.expectedDays()).isEqualTo(2);
    }

    @Test void zeroCostHasNoRoiButLossIsNegative() {
        when(repository.findAllByDateBetweenOrderByDateDesc(date, date))
                .thenReturn(List.of(day(date, "100", "0", AdfitCostBasis.AD_OPERATIONS)))
                .thenReturn(List.of(day(date, "50", "100", AdfitCostBasis.AD_OPERATIONS)));
        assertThat(service.report(date, date).units().getFirst().roi()).isNull();
        assertThat(service.report(date, date).units().getFirst().roi()).isEqualByComparingTo("-50");
    }

    @Test void mixedCostBasesHaveNoRoi() {
        when(repository.findAllByDateBetweenOrderByDateDesc(date, date.plusDays(1))).thenReturn(List.of(
                day(date, "200", "100", AdfitCostBasis.AD_OPERATIONS),
                day(date.plusDays(1), "100", "50", AdfitCostBasis.ACQUISITION)));
        assertThat(service.report(date, date.plusDays(1)).units().getFirst().roi()).isNull();
    }

    @Test void repeatedSaveUpdatesExistingDay() {
        var existing = day(date, "100", "10", AdfitCostBasis.AD_OPERATIONS);
        when(repository.findByDateAndUnit(date, AdfitUnit.NATIVE_WIDE)).thenReturn(Optional.of(existing));
        service.save(new AdfitDailyRequest(date, AdfitUnit.NATIVE_WIDE,
                new BigDecimal("200"), new BigDecimal("20"), AdfitCostBasis.ACQUISITION));
        verify(repository).save(existing);
        assertThat(existing.getRevenue()).isEqualByComparingTo("200");
        assertThat(existing.getCostBasis()).isEqualTo(AdfitCostBasis.ACQUISITION);
    }

    @Test void rejectsInvalidPeriodsAndFutureEntries() {
        assertThatThrownBy(() -> service.report(date.plusDays(1), date)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.report(date, date.plusDays(366))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save(new AdfitDailyRequest(LocalDate.now().plusDays(2),
                AdfitUnit.BANNER, BigDecimal.ONE, BigDecimal.ONE, AdfitCostBasis.AD_OPERATIONS)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
}
