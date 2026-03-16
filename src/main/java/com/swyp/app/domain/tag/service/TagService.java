package com.swyp.app.domain.tag.service;

import com.swyp.app.domain.tag.dto.request.TagRequest;
import com.swyp.app.domain.tag.dto.response.TagDeleteResponse;
import com.swyp.app.domain.tag.dto.response.TagListResponse;
import com.swyp.app.domain.tag.dto.response.TagResponse;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;

import java.util.List;
import java.util.UUID;

public interface TagService {
    List<Tag> findByBattleId(UUID battleId);

    TagListResponse getTags(TagType type);
    TagResponse createTag(TagRequest request);
    TagResponse updateTag(UUID tagId, TagRequest request);
    TagDeleteResponse deleteTag(UUID tagId);
}