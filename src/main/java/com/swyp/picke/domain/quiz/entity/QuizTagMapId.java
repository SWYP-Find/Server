package com.swyp.picke.domain.quiz.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class QuizTagMapId implements Serializable {
    private Long quiz;
    private Long categoryTag;
}

