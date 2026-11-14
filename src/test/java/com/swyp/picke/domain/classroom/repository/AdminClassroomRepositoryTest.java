package com.swyp.picke.domain.classroom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.ticket.repository.FundingOrderRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AdminClassroomRepositoryTest {
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private ClassMemberRepository classMemberRepository;
    @Autowired private ClassTicketRepository classTicketRepository;
    @Autowired private FundingOrderRepository fundingOrderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private com.swyp.picke.domain.battle.repository.BattleRepository battleRepository;

    @Test
    void adminQueriesCountSearchAndLoadOpenClasses() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        User owner = userRepository.saveAndFlush(User.builder()
                .userTag("admin-class-" + UUID.randomUUID().toString().substring(0, 8))
                .nickname("관리자 화면 생성자").role(UserRole.USER).status(UserStatus.ACTIVE).build());
        FundingOrder order = fundingOrderRepository.saveAndFlush(FundingOrder.builder()
                .platform(FundingPlatform.WADIZ).campaignId("admin-class-test")
                .orderId("admin-class-" + UUID.randomUUID().toString().substring(0, 8))
                .rewardTier(RewardTier.SEAT_5).quantity(1).supporterName("구매자")
                .paymentStatus(PaymentStatus.PAID_CONFIRMED).paidAt(now.minusDays(1)).build());
        ClassTicket ticket = classTicketRepository.saveAndFlush(ClassTicket.issue(order, UUID.randomUUID().toString().replace("-", "")));
        Battle battle = battleRepository.saveAndFlush(Battle.builder()
                .title("관리자 목록 콘텐츠").summary("요약").description("설명")
                .status(BattleStatus.PUBLISHED).creatorType(BattleCreatorType.ADMIN).build());
        Classroom classroom = classroomRepository.saveAndFlush(Classroom.builder()
                .name("관리자 클래스").inviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .owner(owner).ticket(ticket).battle(battle).battleType(ClassBattleType.TTS).capacity(5)
                .deadline(now.plusDays(10)).commentRequired(false).ownerParticipates(true).build());
        classMemberRepository.saveAndFlush(ClassMember.builder().classroom(classroom).user(owner)
                .displayName("운영자").role(ClassMemberRole.OWNER).joinedAt(now).build());

        var page = classroomRepository.searchAdminClasses("관리자", now, true, false, PageRequest.of(0, 20));

        assertThat(classroomRepository.countByStatus(ClassStatus.ACTIVE)).isEqualTo(1);
        assertThat(classroomRepository.countOpenClasses(now)).isEqualTo(1);
        assertThat(classroomRepository.countClosingWithin(now, now.plusDays(30))).isEqualTo(1);
        assertThat(classMemberRepository.countSeatsHeldInActiveClasses()).isEqualTo(1);
        assertThat(classTicketRepository.countByStatusNot(TicketStatus.REVOKED)).isEqualTo(1);
        assertThat(classTicketRepository.countCurrentlyInUse(TicketStatus.ACTIVE, now)).isZero();
        assertThat(classTicketRepository.countUnregistered(List.of(TicketStatus.UNREGISTERED,
                TicketStatus.REGISTER_EXPIRED))).isEqualTo(1);
        assertThat(classTicketRepository.countExpiringBetween(TicketStatus.ACTIVE, now,
                now.plusDays(30))).isZero();
        assertThat(classTicketRepository.countLiveTumblbugTickets("admin-class-test")).isZero();
        assertThat(classTicketRepository.countDeliveredTumblbugTickets("admin-class-test")).isZero();
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(classroom.getId());
        assertThat(classroomRepository.findActiveForAdmin(classroom.getId())).isPresent();
    }
}
