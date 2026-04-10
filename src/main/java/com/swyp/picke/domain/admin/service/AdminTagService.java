package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.tag.request.TagRequest;
import com.swyp.picke.domain.admin.dto.tag.response.TagDeleteResponse;
import com.swyp.picke.domain.admin.dto.tag.response.TagResponse;
import com.swyp.picke.domain.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagService tagService;

    public TagResponse createTag(TagRequest request) {
        return tagService.createTag(request);
    }

    public TagResponse updateTag(Long tagId, TagRequest request) {
        return tagService.updateTag(tagId, request);
    }

    public TagDeleteResponse deleteTag(Long tagId) {
        return tagService.deleteTag(tagId);
    }
}
