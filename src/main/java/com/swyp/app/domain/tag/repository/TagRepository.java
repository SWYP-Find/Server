package com.swyp.app.domain.tag.repository;

import com.swyp.app.domain.tag.entity.Tag;
import com.swyp.app.domain.tag.enums.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByType(TagType type);

    Boolean existsByNameAndType(String name, TagType type);
}