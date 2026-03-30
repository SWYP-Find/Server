package com.swyp.picke.domain.tag.repository;

import com.swyp.picke.domain.tag.entity.Tag;
import com.swyp.picke.domain.tag.enums.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAllByTypeAndDeletedAtIsNull(TagType type);
    List<Tag> findAllByDeletedAtIsNull();
    Boolean existsByNameAndTypeAndDeletedAtIsNull(String name, TagType type);
    Optional<Tag> findByIdAndDeletedAtIsNull(Long id);
}