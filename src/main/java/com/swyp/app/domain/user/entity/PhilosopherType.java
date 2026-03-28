package com.swyp.app.domain.user.entity;

import lombok.Getter;

@Getter
public enum PhilosopherType {
    SOCRATES("소크라테스", "images/philosophers/socrates.png"),
    PLATO("플라톤", "images/philosophers/plato.png"),
    ARISTOTLE("아리스토텔레스", "images/philosophers/aristotle.png"),
    KANT("칸트", "images/philosophers/kant.png"),
    NIETZSCHE("니체", "images/philosophers/nietzsche.png"),
    MARX("마르크스", "images/philosophers/marx.png"),
    SARTRE("사르트르", "images/philosophers/sartre.png"),
    CONFUCIUS("공자", "images/philosophers/confucius.png"),
    LAOZI("노자", "images/philosophers/laozi.png"),
    BUDDHA("붓다", "images/philosophers/buddha.png");

    private final String label;
    private final String imageKey;

    PhilosopherType(String label, String imageKey) {
        this.label = label;
        this.imageKey = imageKey;
    }
}
