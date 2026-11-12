package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.enums.ClassDebateResponseType;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.List;

/**
 * AI 주장 1건의 입력 (PRD §11 입력 계약). 이 참가자의 세션 대화만 담고 다른 참가자의 대화·이름은 넣지 않는다.
 * responseHint 는 직전 반박의 입력 분류로 정한 응답 방향 (엣지 케이스 PRD 4.1 response_type). 1라운드·분류 없음이면 null
 */
public record DebatePromptInput(
        String question,
        String description,
        Side participant,
        Side opponent,
        PhilosopherType philosopher,
        BattleDifficulty difficulty,
        int round,
        List<Turn> history,
        ClassDebateResponseType responseHint
) {

    /** position = A / B */
    public record Side(String position, String label, String rationale) {
    }

    public record Turn(int round, ClassDebateSpeaker speaker, String content) {
    }
}
