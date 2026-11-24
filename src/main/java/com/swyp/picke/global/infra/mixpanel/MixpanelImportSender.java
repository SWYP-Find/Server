package com.swyp.picke.global.infra.mixpanel;

import java.util.List;
import java.util.Map;

/** Mixpanel 로 이벤트를 보내는 통로. 테스트에서 바꿔 끼운다 */
public interface MixpanelImportSender {

    /** 성공 여부. 예외를 던지지 않는다 */
    boolean send(List<Map<String, Object>> events);
}
