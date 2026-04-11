package com.swyp.picke.domain.tag.service;

import com.swyp.picke.domain.tag.dto.request.TagRequest;
import com.swyp.picke.domain.tag.dto.response.TagDeleteResponse;
import com.swyp.picke.domain.tag.dto.response.TagListResponse;
import com.swyp.picke.domain.tag.dto.response.TagResponse;
import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;

import java.util.List;

public interface TagService {
    List<Tag> findByBattleId(Long battleId);

    TagListResponse getTags(TagType type);
    TagResponse createTag(TagRequest request);
    TagResponse updateTag(Long tagId, TagRequest request);
    TagDeleteResponse deleteTag(Long tagId);
}