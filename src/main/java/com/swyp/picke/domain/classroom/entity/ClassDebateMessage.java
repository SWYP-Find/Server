package com.swyp.picke.domain.classroom.entity;

import com.swyp.picke.domain.classroom.enums.ClassDebateFocus;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토론 메시지. (세션, 라운드, 화자)당 1개만 저장한다 (PRD §8 "라운드·역할별 메시지 1개").
 * 저장 후 수정하지 않는다. 검증 전 AI 텍스트는 저장하지 않는다 (PRD §11).
 */
@Getter
@Entity
@Table(
        name = "class_debate_messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_debate_messages_turn", columnNames = {"session_id", "round_index", "speaker"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassDebateMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassDebateSession session;

    @Column(name = "round_index", nullable = false)
    private int roundIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClassDebateSpeaker speaker;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** AI 주장의 초점. 참가자 반박은 null */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClassDebateFocus focus;

    /** AI 주장이 응답한 직전 참가자 반박. 1라운드 AI 주장과 참가자 반박은 null */
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    /** 참가자 반박의 중복 전송 방지 키 (PRD §8). AI 주장은 null */
    @Column(name = "request_key", length = 64)
    private String requestKey;

    @Builder
    private ClassDebateMessage(ClassDebateSession session, int roundIndex, ClassDebateSpeaker speaker, String content,
                               ClassDebateFocus focus, Long replyToMessageId, String requestKey) {
        this.session = session;
        this.roundIndex = roundIndex;
        this.speaker = speaker;
        this.content = content;
        this.focus = focus;
        this.replyToMessageId = replyToMessageId;
        this.requestKey = requestKey;
    }
}
