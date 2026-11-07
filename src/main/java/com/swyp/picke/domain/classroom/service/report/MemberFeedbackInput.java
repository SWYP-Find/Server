package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.classroom.dto.response.ClassMidVoteInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 개인 리포트 AI 입력 (PRD §12). 본인 발언만 분석 대상이고 타인의 글은 부모 맥락으로만 들어간다.
 */
public record MemberFeedbackInput(
        String battleTitle,
        String battleSummary,
        List<OptionInfo> options,
        OptionInfo preOption,
        List<ClassMidVoteInfo> midChoices,
        OptionInfo postOption,
        List<Statement> statements,
        int excludedCount
) {

    public record OptionInfo(Long optionId, String title, String stance) {
    }

    /**
     * 본인 발언 1건. sourceId 는 "p:{perspectiveId}" / "c:{commentId}" 형식으로 AI 와 주고받는다.
     * parentContext 는 대댓글일 때 부모 의견 원문(타인 글, 맥락 전용).
     */
    public record Statement(String sourceId, String sourceType, Long id, LocalDateTime createdAt,
                            String stance, String content, String parentContext) {
    }

    public static final String SOURCE_PERSPECTIVE = "PERSPECTIVE";
    public static final String SOURCE_COMMENT = "COMMENT";

    public static String sourceId(String sourceType, Long id) {
        return (SOURCE_PERSPECTIVE.equals(sourceType) ? "p:" : "c:") + id;
    }

    public boolean isPartial() {
        return excludedCount > 0;
    }

    public String coverage() {
        return isPartial() ? "partial" : "full";
    }

    public Statement findStatement(String sourceId) {
        for (Statement statement : statements) {
            if (Objects.equals(statement.sourceId(), sourceId)) {
                return statement;
            }
        }
        return null;
    }
}
