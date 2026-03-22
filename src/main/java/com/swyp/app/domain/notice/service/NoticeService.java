package com.swyp.app.domain.notice.service;

import com.swyp.app.domain.notice.dto.response.NoticeDetailResponse;
import com.swyp.app.domain.notice.dto.response.NoticeListResponse;
import com.swyp.app.domain.notice.dto.response.NoticeSummaryResponse;
import com.swyp.app.domain.notice.entity.Notice;
import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.entity.NoticeType;
import com.swyp.app.domain.notice.repository.NoticeRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final int DEFAULT_LIMIT = 20;

    private final NoticeRepository noticeRepository;

    public List<NoticeSummaryResponse> getActiveNotices(NoticePlacement placement, NoticeType type, Integer limit) {
        int pageSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        return noticeRepository.findActiveNotices(LocalDateTime.now(), type, placement, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public NoticeListResponse getNoticeList(NoticeType type, NoticePlacement placement, Integer limit) {
        List<NoticeSummaryResponse> items = getActiveNotices(placement, type, limit);
        return new NoticeListResponse(items, items.size());
    }

    public NoticeDetailResponse getNoticeDetail(UUID noticeId) {
        Notice notice = noticeRepository.findActiveById(noticeId, LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getType(),
                notice.getPlacement(),
                notice.isPinned(),
                notice.getStartsAt(),
                notice.getEndsAt(),
                notice.getCreatedAt()
        );
    }

    private NoticeSummaryResponse toSummaryResponse(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getType(),
                notice.getPlacement(),
                notice.isPinned(),
                notice.getStartsAt(),
                notice.getEndsAt()
        );
    }
}
