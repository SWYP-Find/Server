package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 검증된 주제 버전으로 클래스 전용 배틀을 만든다 (#547). 투표·의견·결과 집계는 기존 배틀 구조를 그대로 재사용한다.
 * <ul>
 *   <li>공개(PUBLISHED)하지 않고 creator_type = USER 로 둬서 앱 목록·추천·검색과 어드민 배틀 목록에 나오지 않는다</li>
 *   <li>선택지 A/B 는 display_order 1/2. 대표 철학자 = 주제 버전에서 선택지별로 매핑한 철학자 (AI 토론에서 사용)</li>
 *   <li>썸네일은 고정 1종 (기획 Q&A). 설정이 비어 있으면 null</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ClassCustomBattleFactory {

    private final BattleRepository battleRepository;
    private final BattleOptionRepository battleOptionRepository;

    @Value("${picke.class.custom-battle-thumbnail-url:}")
    private String thumbnailUrl;

    public Battle create(ClassTopicVersion version, User owner) {
        Battle battle = battleRepository.save(Battle.builder()
                .title(version.getQuestion())
                .summary(version.getDescription())
                .description(version.getDescription())
                .thumbnailUrl(thumbnailUrl == null || thumbnailUrl.isBlank() ? null : thumbnailUrl)
                .status(BattleStatus.PENDING)
                .creatorType(BattleCreatorType.USER)
                .creator(owner)
                .build());
        battle.updateClassSettings(version.getDifficulty(), version.getCategory(), false, false);
        battle.getOptions().add(option(battle, 1, version.getOptionALabel(), version.getOptionARationale(),
                version.getOptionAPhilosopher()));
        battle.getOptions().add(option(battle, 2, version.getOptionBLabel(), version.getOptionBRationale(),
                version.getOptionBPhilosopher()));
        return battle;
    }

    private BattleOption option(Battle battle, int order, String label, String rationale, PhilosopherType philosopher) {
        return battleOptionRepository.save(BattleOption.builder()
                .battle(battle)
                .title(label)
                .stance(rationale)
                .representative(philosopher.getLabel())
                .imageUrl(philosopher.getImageKey())
                .displayOrder(order)
                .build());
    }
}
