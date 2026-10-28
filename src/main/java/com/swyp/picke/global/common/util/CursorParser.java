package com.swyp.picke.global.common.util;

import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** Parses cursors emitted by APIs as ISO-8601 local timestamps. */
public final class CursorParser {

    private CursorParser() {
    }

    public static LocalDateTime parseDateTime(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
    }
}
