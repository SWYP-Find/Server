package com.swyp.picke.domain.classroom.service.report;

import java.util.List;
import java.util.Objects;

/**
 * 클래스 인사이트 AI 입력 (PRD §14). 공개 의견·댓글만, 작성자는 익명 번호로 넘긴다. 개인 리포트·비공개 내용은 넣지 않는다.
 */
public record ClassInsightInput(
        String battleTitle,
        String battleSummary,
        List<MemberFeedbackInput.OptionInfo> options,
        ClassResultStats stats,
        List<PublicPost> posts,
        int distinctAuthorCount,
        int excludedCount
) {

    /** 공개 글 1건. sourceId 는 "p:{id}" / "c:{id}". 댓글의 stance 는 부모 의견의 입장 */
    public record PublicPost(String sourceId, String sourceType, Long id, int authorNo, Long optionId,
                             String stance, String content, String parentSourceId) {
    }

    public boolean isPartial() {
        return excludedCount > 0;
    }

    public String coverage() {
        return isPartial() ? "partial" : "full";
    }

    public PublicPost findPost(String sourceId) {
        for (PublicPost post : posts) {
            if (Objects.equals(post.sourceId(), sourceId)) {
                return post;
            }
        }
        return null;
    }
}
