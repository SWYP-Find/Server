package com.swyp.picke.domain.user.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterTypeTest {

    @Test
    @DisplayName("다운로드 폴더 기준 모든 캐릭터 enum이 등록되어 있다")
    void characterTypes_include_all_downloaded_animals() {
        Set<String> characterNames = Arrays.stream(CharacterType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(characterNames).containsExactlyInAnyOrder(
                "OWL", "FOX", "WOLF", "LION", "PENGUIN", "BEAR", "RABBIT", "CAT",
                "ALPACA", "CAPYBARA", "DEER", "DOG", "DUCK", "EAGLE", "HAMSTER", "HEDGEHOG",
                "HONEYBEE", "KOALA", "OTTER", "PANDA", "POODLE", "RACCOON", "RAGDOLL",
                "RETRIEVER", "SLOTH", "SQUIRREL", "TIGER", "WHALE"
        );
    }

    @Test
    @DisplayName("캐릭터는 enum 이름과 한글 라벨 모두로 조회할 수 있다")
    void from_supports_enum_name_and_label() {
        assertThat(CharacterType.from("owl")).isEqualTo(CharacterType.OWL);
        assertThat(CharacterType.from("부엉이")).isEqualTo(CharacterType.OWL);
        assertThat(CharacterType.from("카피바라")).isEqualTo(CharacterType.CAPYBARA);
    }

    @Test
    @DisplayName("캐릭터 이미지 키를 단일 API로 해석한다")
    void resolveImageKey_returns_registered_key() {
        assertThat(CharacterType.resolveImageKey("RAGDOLL")).isEqualTo("images/characters/ragdoll.png");
        assertThat(CharacterType.resolveImageKey(CharacterType.WHALE)).isEqualTo("images/characters/whale.png");
    }
}
