package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassTopicSuggestStatus;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 커스텀 배틀 주제 초안 (PRD §10 AI-01). 운영자의 사용 중 이용권 아래에 만들고, AI 제안 후보와 쿼터를 초안 단위로 관리한다.
 * <ul>
 *   <li>categories : 카테고리 enum 이름 목록 (1~3개)</li>
 *   <li>candidates : 마지막 성공 배치의 후보 3개. candidate_id(1~3)·question·description·category·difficulty·options[2]</li>
 *   <li>batch_no : 성공한 제안 배치 수 = 현재 후보의 배치 번호. 최대 3 (최초 + 재추천)</li>
 *   <li>suggest_request_count : 제안 요청 수(실패 포함). ai_jobs 멱등키에 쓴다</li>
 *   <li>validation_count : AI 의미 검증을 호출한 수정 검증 수. 최대 3</li>
 * </ul>
 */
@Getter
@Entity
@Table(name = "class_topic_drafts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassTopicDraft extends BaseEntity {

    public static final int TOPIC_SEED_MIN_LENGTH = 2;
    public static final int TOPIC_SEED_MAX_LENGTH = 200;
    public static final int MAX_CATEGORIES = 3;
    public static final int MAX_SUGGEST_BATCHES = 3;
    public static final int MAX_VALIDATIONS = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ClassTicket ticket;

    @Column(name = "topic_seed", nullable = false, length = TOPIC_SEED_MAX_LENGTH)
    private String topicSeed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleDifficulty difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", nullable = false)
    private List<String> categories;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggest_status", nullable = false, length = 30)
    private ClassTopicSuggestStatus suggestStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidates")
    private List<Map<String, Object>> candidates;

    @Column(name = "batch_no", nullable = false)
    private int batchNo;

    @Column(name = "suggest_request_count", nullable = false)
    private int suggestRequestCount;

    @Column(name = "suggest_started_at")
    private LocalDateTime suggestStartedAt;

    @Column(name = "clarification_message", length = 200)
    private String clarificationMessage;

    @Column(name = "validation_count", nullable = false)
    private int validationCount;

    @Builder
    private ClassTopicDraft(User owner, ClassTicket ticket, String topicSeed, BattleDifficulty difficulty, List<String> categories) {
        this.owner = owner;
        this.ticket = ticket;
        this.topicSeed = topicSeed;
        this.difficulty = difficulty;
        this.categories = categories;
        this.suggestStatus = ClassTopicSuggestStatus.NONE;
        this.batchNo = 0;
        this.suggestRequestCount = 0;
        this.validationCount = 0;
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    /** 다른 요청이 제안을 만들고 있는지. 오래 멈춘 GENERATING 은 진행 중으로 보지 않는다 */
    public boolean isGenerating(LocalDateTime staleBefore) {
        return suggestStatus == ClassTopicSuggestStatus.GENERATING
                && suggestStartedAt != null && suggestStartedAt.isAfter(staleBefore);
    }

    public int remainingSuggestions() {
        return Math.max(0, MAX_SUGGEST_BATCHES - batchNo);
    }

    public int remainingValidations() {
        return Math.max(0, MAX_VALIDATIONS - validationCount);
    }

    public void startSuggest(LocalDateTime now) {
        this.suggestStatus = ClassTopicSuggestStatus.GENERATING;
        this.suggestRequestCount++;
        this.suggestStartedAt = now;
    }

    /** 성공 배치 반영. 조건 변경도 같은 초안의 새 배치로 세므로 이때 조건을 함께 바꾼다 (PRD §10) */
    public void completeSuggest(String topicSeed, BattleDifficulty difficulty, List<String> categories,
                                List<Map<String, Object>> candidates) {
        this.topicSeed = topicSeed;
        this.difficulty = difficulty;
        this.categories = categories;
        this.candidates = candidates;
        this.batchNo++;
        this.suggestStatus = ClassTopicSuggestStatus.SUCCEEDED;
        this.clarificationMessage = null;
    }

    /** 맥락 부족. 후보를 게시하지 않고 기존 후보는 유지, 횟수도 차감하지 않는다 */
    public void needClarification(String message) {
        this.suggestStatus = ClassTopicSuggestStatus.NEEDS_CLARIFICATION;
        this.clarificationMessage = message;
    }

    public void failSuggest() {
        this.suggestStatus = ClassTopicSuggestStatus.FAILED;
    }

    public void consumeValidation() {
        this.validationCount++;
    }

    /** 제공자 장애로 검증 결과를 얻지 못하면 횟수를 돌려준다 */
    public void refundValidation() {
        this.validationCount = Math.max(0, validationCount - 1);
    }
}
