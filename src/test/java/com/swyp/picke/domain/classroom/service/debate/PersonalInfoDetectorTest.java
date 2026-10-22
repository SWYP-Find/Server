package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersonalInfoDetectorTest {

    @ParameterizedTest
    @DisplayName("연락처·식별번호가 있으면 잡는다")
    @ValueSource(strings = {
            "연락은 010-1234-5678 로 주세요",
            "01012345678 번호로",
            "010 1234 5678",
            "사무실 02-123-4567 입니다",
            "031-1234-5678",
            "메일 test.user@example.com 으로",
            "제 주민번호는 900101-1234567",
            "9001011234567",
            "카드 1234-5678-9012-3456"
    })
    void detects(String text) {
        assertThat(PersonalInfoDetector.contains(text)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("연도·통계·일반 숫자가 든 정상 논증은 잡지 않는다")
    @ValueSource(strings = {
            "2024년 조사에서 1,000명 중 30%가 그렇게 답했어요",
            "1789년 프랑스 혁명 이후 인권 개념이 넓어졌어요",
            "10대 청소년 2명 중 1명은 처벌보다 교육이 낫다고 봤어요",
            "버전 1.2.3 처럼 숫자가 섞인 문장",
            "인구 51,000,000명의 나라에서",
            "123456 같은 여섯 자리 숫자"
    })
    void ignoresNormalNumbers(String text) {
        assertThat(PersonalInfoDetector.contains(text)).isFalse();
    }
}
