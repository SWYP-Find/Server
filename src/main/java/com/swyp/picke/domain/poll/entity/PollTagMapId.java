package com.swyp.picke.domain.poll.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class PollTagMapId implements Serializable {
    private Long poll;
    private Long categoryTag;
}

