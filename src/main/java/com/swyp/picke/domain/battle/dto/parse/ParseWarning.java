package com.swyp.picke.domain.battle.dto.parse;

/**
 * 붙여넣기 파싱 중 사람이 미리보기에서 확인/보정해야 하는 지점.
 * blocking=true 면 그 상태로는 발행할 수 없다(보이스 미지정 등).
 */
public record ParseWarning(
        String code,
        String message,
        String context,
        boolean blocking
) {
    public static ParseWarning of(String code, String message, String context) {
        return new ParseWarning(code, message, context, false);
    }

    public static ParseWarning blocking(String code, String message, String context) {
        return new ParseWarning(code, message, context, true);
    }
}
