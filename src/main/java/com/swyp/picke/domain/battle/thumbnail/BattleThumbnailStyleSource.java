package com.swyp.picke.domain.battle.thumbnail;

import java.util.List;

/**
 * 썸네일 생성에 쓰는 공통 스타일(프롬프트 템플릿 + 예시 이미지) 제공처.
 * 지금은 리소스 파일 구현만 있고, 어드민에서 수정하게 되면 DB 구현으로 교체한다.
 */
public interface BattleThumbnailStyleSource {

    /**
     * 프롬프트 템플릿. {scene} 자리에 배틀 내용으로 만든 장면 묘사가 들어간다.
     */
    String promptTemplate();

    List<ReferenceImage> referenceImages();
}
