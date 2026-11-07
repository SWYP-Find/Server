package com.swyp.picke.domain.classroom.service.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * class_reports.content(JSONB) 를 Map 으로 다룰 때 쓰는 읽기 헬퍼. JSON 숫자는 Integer/Long/Double 어느 쪽으로도 올 수 있다.
 */
public final class ReportContent {

    private ReportContent() {
    }

    public static String str(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    public static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static int asInt(Map<String, Object> map, String key) {
        Long value = map == null ? null : asLong(map.get(key));
        return value == null ? 0 : value.intValue();
    }

    public static double asDouble(Map<String, Object> map, String key) {
        if (map == null) {
            return 0d;
        }
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mapList(Map<String, Object> map, String key) {
        if (map == null) {
            return Collections.emptyList();
        }
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add((Map<String, Object>) m);
            }
        }
        return result;
    }

    public static List<String> stringList(Map<String, Object> map, String key) {
        if (map == null) {
            return Collections.emptyList();
        }
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }
}
