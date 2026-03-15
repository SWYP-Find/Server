package com.swyp.app.domain.tag.service;

import com.swyp.app.domain.tag.entity.Tag;

import java.util.List;
import java.util.UUID;

public interface TagService {

    List<Tag> findByBattleId(UUID battleId);
}
