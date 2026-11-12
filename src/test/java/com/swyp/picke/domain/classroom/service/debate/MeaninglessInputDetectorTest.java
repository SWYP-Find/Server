package com.swyp.picke.domain.classroom.service.debate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeaninglessInputDetectorTest {

    @Test
    @DisplayName("반복 문자·자음으로 50자를 채운 입력은 무의미로 본다")
    void detectsMeaningless() {
        assertThat(MeaninglessInputDetector.isMeaningless("ㅋ".repeat(60))).isTrue();
        assertThat(MeaninglessInputDetector.isMeaningless("ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ ".repeat(4) + "몰라")).isTrue();
        assertThat(MeaninglessInputDetector.isMeaningless("가나다 ".repeat(20))).isTrue();
        assertThat(MeaninglessInputDetector.isMeaningless("아무말 아무말 아무말 대잔치 ".repeat(4))).isTrue();
        assertThat(MeaninglessInputDetector.isMeaningless("!!!!!!????..... ".repeat(5))).isTrue();
    }

    @Test
    @DisplayName("정상 문장·ㅋㅋ 가 조금 섞인 문장·짧은 반론은 막지 않는다")
    void allowsNormal() {
        assertThat(MeaninglessInputDetector.isMeaningless(
                "속으로 슬퍼해도 밖으로 표현하지 않을 수 있어요. 표현 방식은 사람마다 달라서 겉모습만으로 판단하면 안 돼요.")).isFalse();
        assertThat(MeaninglessInputDetector.isMeaningless(
                "ㅋㅋ 그건 아니죠. 혼자 일기에 슬픔을 적는 사람도 스스로 감정을 확인할 수 있잖아요 ㅠㅠ 그렇지 않나요?")).isFalse();
        assertThat(MeaninglessInputDetector.isMeaningless(
                "I think sadness exists even if you do not show it to others, because feelings are private.")).isFalse();
    }
}
