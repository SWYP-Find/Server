package com.swyp.picke.domain.classroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "클래스 리포트 화면용 내 활동. 받은 좋아요는 공개된 의견·댓글에 받은 수의 합계")
public record ClassMyActivityResponse(
        Long classId,
        long receivedLikeCount,
        long commentCount,
        List<Comment> comments,
        String nextCursor,
        boolean hasNext
) {
    public record Comment(
            Long commentId,
            Long perspectiveId,
            String perspectiveTitle,
            String stance,
            String content,
            int likeCount,
            LocalDateTime createdAt
    ) {
    }
}
