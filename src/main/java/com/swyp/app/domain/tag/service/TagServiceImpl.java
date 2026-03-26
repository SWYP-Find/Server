package com.swyp.app.domain.tag.service;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.repository.BattleRepository;
import com.swyp.app.domain.battle.repository.BattleTagRepository;
import com.swyp.app.domain.tag.converter.TagConverter;
import com.swyp.app.domain.tag.dto.request.TagRequest;
import com.swyp.app.domain.tag.dto.response.*;
import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
import com.swyp.app.domain.tag.repository.TagRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final BattleTagRepository battleTagRepository;
    private final BattleRepository battleRepository;

    @Override
    public List<Tag> findByBattleId(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        return battleTagRepository.findByBattle(battle).stream()
                .map(bt -> bt.getTag())
                .toList();
    }

    @Override
    public TagListResponse getTags(TagType type) {
        List<Tag> tags = (type != null)
                ? tagRepository.findAllByTypeAndDeletedAtIsNull(type)
                : tagRepository.findAllByDeletedAtIsNull();
        return TagConverter.toListResponse(tags);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse createTag(TagRequest request) {
        validateDuplicateTag(request.name(), request.type());

        Tag newTag = TagConverter.toEntity(request);
        Tag savedTag = tagRepository.save(newTag);

        return TagConverter.toDetailResponse(savedTag);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse updateTag(Long tagId, TagRequest request) {
        Tag tag = findTagById(tagId);

        if (!tag.getName().equals(request.name()) || tag.getType() != request.type()) {
            validateDuplicateTag(request.name(), request.type());
        }

        tag.updateTag(request.name(), request.type());
        return TagConverter.toDetailResponse(tag);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TagDeleteResponse deleteTag(Long tagId) {
        Tag tag = findTagById(tagId);

        if (battleTagRepository.existsByTag(tag)) {
            throw new CustomException(ErrorCode.TAG_IN_USE);
        }

        tag.delete();
        return TagConverter.toDeleteResponse();
    }

    private Tag findTagById(Long tagId) {
        return tagRepository.findByIdAndDeletedAtIsNull(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
    }

    private void validateDuplicateTag(String name, TagType type) {
        if (tagRepository.existsByNameAndTypeAndDeletedAtIsNull(name, type)) {
            throw new CustomException(ErrorCode.TAG_DUPLICATED);
        }
    }
}