package com.swyp.app.domain.tag.repository;

import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
}
