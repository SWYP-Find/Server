package com.swyp.app.domain.user.entity;

public enum TierCode {
    WANDERER("방랑자");

    private final String label;

    TierCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
