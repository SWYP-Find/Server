package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.user.enums.CharacterType;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.global.infra.s3.enums.FileCategory;
import com.swyp.picke.global.infra.s3.service.S3PresignedUrlService;
import com.swyp.picke.global.infra.s3.util.ResourceUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 클래스 응답에 나가는 이미지(배틀 썸네일·철학자·아바타)를 저장 키가 아닌 URL로 바꾼다.
 * 일반 배틀·의견 API와 같은 방식을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ClassImageUrlResolver {

    private final ResourceUrlProvider urlProvider;
    private final S3PresignedUrlService s3PresignedUrlService;

    public String battleThumbnail(Battle battle) {
        return battle == null ? null : urlProvider.getImageUrl(FileCategory.BATTLE, battle.getThumbnailUrl());
    }

    public String philosopher(PhilosopherType philosopher) {
        return philosopher == null ? null : urlProvider.getImageUrl(FileCategory.PHILOSOPHER, philosopher.getImageKey());
    }

    public String avatar(CharacterType characterType) {
        String imageKey = CharacterType.resolveImageKey(characterType);
        return imageKey == null ? null : s3PresignedUrlService.generatePresignedUrl(imageKey);
    }
}
