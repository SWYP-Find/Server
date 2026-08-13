package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.ValueAxis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTendencyScoreTest {

    @Test
    @DisplayName("기준 가치관 6개는 각 축에 +1로 반영된다")
    void appliesPositiveValueTags() {
        UserTendencyScore score = emptyScore();

        for (String tag : new String[]{"원칙", "이성", "개인", "변화", "내면", "이상"}) {
            score.applyValueTag(tag);
        }

        assertThat(score.getPrinciple()).isEqualTo(1);
        assertThat(score.getReason()).isEqualTo(1);
        assertThat(score.getIndividual()).isEqualTo(1);
        assertThat(score.getChange()).isEqualTo(1);
        assertThat(score.getInner()).isEqualTo(1);
        assertThat(score.getIdeal()).isEqualTo(1);
    }

    @Test
    @DisplayName("반대 가치관 6개는 대응하는 기존 축에 -1로 반영된다")
    void appliesNegativeValueTags() {
        UserTendencyScore score = emptyScore();

        for (String tag : new String[]{"결과", "감성", "관계", "전통", "구조", "현실"}) {
            score.applyValueTag(tag);
        }

        assertThat(score.getPrinciple()).isEqualTo(-1);
        assertThat(score.getReason()).isEqualTo(-1);
        assertThat(score.getIndividual()).isEqualTo(-1);
        assertThat(score.getChange()).isEqualTo(-1);
        assertThat(score.getInner()).isEqualTo(-1);
        assertThat(score.getIdeal()).isEqualTo(-1);
    }

    @Test
    @DisplayName("육각형 축은 6개만 유지한다")
    void keepsSixAxes() {
        assertThat(ValueAxis.values()).hasSize(6);
    }

    private UserTendencyScore emptyScore() {
        return UserTendencyScore.builder().build();
    }
}
