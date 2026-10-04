package com.swyp.picke.domain.battle.thumbnail;

import org.springframework.http.MediaType;

/** 썸네일 스타일 레퍼런스로 함께 보내는 예시 이미지. */
public record ReferenceImage(String fileName, byte[] bytes, MediaType mediaType) {}
