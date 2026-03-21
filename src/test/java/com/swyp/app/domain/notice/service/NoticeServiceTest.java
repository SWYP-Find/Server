package com.swyp.app.domain.notice.service;

import com.swyp.app.domain.notice.entity.Notice;
import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.entity.NoticeType;
import com.swyp.app.domain.notice.repository.NoticeRepository;
import com.swyp.app.global.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    @DisplayName("활성공지 목록을 개수와 함께 반환한다")
    void getNoticeList_returns_active_notices_with_count() {
        Notice notice = Notice.builder()
                .title("공지")
                .body("내용")
                .type(NoticeType.ANNOUNCEMENT)
                .placement(NoticePlacement.HOME_TOP)
                .pinned(true)
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(LocalDateTime.now().plusDays(1))
                .build();

        when(noticeRepository.findActiveNotices(any(LocalDateTime.class), eq(NoticeType.ANNOUNCEMENT),
                eq(NoticePlacement.HOME_TOP), any(Pageable.class))).thenReturn(List.of(notice));

        var response = noticeService.getNoticeList(NoticeType.ANNOUNCEMENT, NoticePlacement.HOME_TOP, 5);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().title()).isEqualTo("공지");
    }

    @Test
    @DisplayName("활성공지가 없으면 예외를 던진다")
    void getNoticeDetail_throws_when_no_active_notice() {
        UUID noticeId = UUID.randomUUID();
        when(noticeRepository.findActiveById(eq(noticeId), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNoticeDetail(noticeId))
                .isInstanceOf(CustomException.class);
    }
}
