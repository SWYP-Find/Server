package com.swyp.app.domain.tag.service;

import com.swyp.app.domain.tag.entity.Tag;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TagServiceImpl implements TagService {

    @Override
    public List<Tag> findByBattleId(UUID battleId) {
        throw new UnsupportedOperationException("Not yet implemented - pending Tag domain merge");
    }
}
