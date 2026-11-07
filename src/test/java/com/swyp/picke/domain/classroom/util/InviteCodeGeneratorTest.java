package com.swyp.picke.domain.classroom.util;

import com.swyp.picke.domain.classroom.entity.Classroom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class InviteCodeGeneratorTest {

    @RepeatedTest(50)
    @DisplayName("초대코드는 6자이고 헷갈리는 글자(0, O, 1, I, L)를 쓰지 않는다")
    void generate_lengthAndAlphabet() {
        String code = InviteCodeGenerator.generate();

        assertThat(code).hasSize(Classroom.INVITE_CODE_LENGTH);
        assertThat(code).doesNotContain("0", "O", "1", "I", "L");
        assertThat(code.chars().allMatch(ch -> InviteCodeGenerator.ALPHABET.indexOf(ch) >= 0)).isTrue();
    }
}
