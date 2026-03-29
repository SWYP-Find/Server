package com.swyp.app.domain.user.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PhilosopherType {
    SOCRATES("소크라테스", "질문형", "확신보다는 끊임없는 물음표로 진리를 찾는 탐구자",
            "KANT", "ARISTOTLE",
            75, 96, 50, 58, 72, 60,
            "images/philosophers/socrates.png"),
    PLATO("플라톤", "이상형", "현실 너머 더 완벽하고 가치 있는 세상을 꿈꾸는 이상주의자",
            "MARX", "ARISTOTLE",
            82, 65, 30, 42, 68, 95,
            "images/philosophers/plato.png"),
    ARISTOTLE("아리스토텔레스", "현실형", "모호한 이론보다 명확한 증거와 논리로 판단하는 실천가",
            "SOCRATES", "PLATO",
            78, 92, 62, 40, 45, 25,
            "images/philosophers/aristotle.png"),
    KANT("칸트", "원칙형", "스스로 세운 도덕적 원칙과 보편적 가치를 지키는 원칙주의자",
            "CONFUCIUS", "NIETZSCHE",
            92, 85, 72, 38, 88, 45,
            "images/philosophers/kant.png"),
    NIETZSCHE("니체", "돌파형", "기존의 틀을 깨고 나만의 길을 개척하는 극복의 아이콘",
            "SARTRE", "KANT",
            32, 48, 95, 90, 42, 85,
            "images/philosophers/nietzsche.png"),
    MARX("마르크스", "구조형", "개인의 문제보다 사회의 구조와 시스템을 꿰뚫는 분석가",
            "PLATO", "SARTRE",
            40, 78, 18, 94, 28, 80,
            "images/philosophers/marx.png"),
    SARTRE("사르트르", "자유형", "선택의 무게를 짊어지며 행동으로 존재를 증명하는 자유인",
            "NIETZSCHE", "MARX",
            28, 52, 98, 86, 48, 72,
            "images/philosophers/sartre.png"),
    CONFUCIUS("공자", "관계형", "예의와 배려로 조화로운 인간관계를 만들어가는 평화주의자",
            "KANT", "LAOZI",
            94, 65, 15, 30, 80, 50,
            "images/philosophers/confucius.png"),
    LAOZI("노자", "자연형", "억지로 바꾸기보다 세상의 순리와 흐름에 몸을 맡기는 유연한 영혼",
            "BUDDHA", "CONFUCIUS",
            22, 38, 68, 88, 94, 70,
            "images/philosophers/laozi.png"),
    BUDDHA("붓다", "내면형", "외부의 소음에서 벗어나 마음속 깊은 평화와 고요를 찾는 수행자",
            "LAOZI", "ARISTOTLE",
            35, 55, 42, 48, 96, 62,
            "images/philosophers/buddha.png"),
    AQUINAS("토마스 아퀴나스", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/aquinas.png"),
    CAMUS("카뮈", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/camus.png"),
    CHOE_HANGI("최한기", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/choe_hangi.png"),
    DESCARTES("데카르트", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/descartes.png"),
    EPICURUS("에피쿠로스", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/epicurus.png"),
    FROMM("에리히 프롬", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/fromm.png"),
    HOBBES("홉스", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/hobbes.png"),
    HUME("흄", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/hume.png"),
    JEONG_YAKYONG("정약용", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/jeong_yakyong.png"),
    JUNG("융", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/jung.png"),
    LEIBNIZ("라이프니츠", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/leibniz.png"),
    MENCIUS("맹자", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/mencius.png"),
    MILL("존 스튜어트 밀", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/mill.png"),
    RAWLS("롤스", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/rawls.png"),
    SCHOPENHAUER("쇼펜하우어", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/schopenhauer.png"),
    XUNZI("순자", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/xunzi.png"),
    YI_HWANG("이황", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/yi_hwang.png"),
    YI_I("이이", null, null, null, null, 0, 0, 0, 0, 0, 0, "images/philosophers/yi_i.png");

    private final String label;
    private final String typeName;
    private final String description;
    private final String bestMatchName;
    private final String worstMatchName;
    private final int principle;
    private final int reason;
    private final int individual;
    private final int change;
    private final int inner;
    private final int ideal;
    private final String imageKey;

    PhilosopherType(String label, String typeName, String description,
                    String bestMatchName, String worstMatchName,
                    int principle, int reason, int individual,
                    int change, int inner, int ideal,
                    String imageKey) {
        this.label = label;
        this.typeName = typeName;
        this.description = description;
        this.bestMatchName = bestMatchName;
        this.worstMatchName = worstMatchName;
        this.principle = principle;
        this.reason = reason;
        this.individual = individual;
        this.change = change;
        this.inner = inner;
        this.ideal = ideal;
        this.imageKey = imageKey;
    }

    public PhilosopherType getBestMatch() {
        return bestMatchName != null ? valueOf(bestMatchName) : null;
    }

    public PhilosopherType getWorstMatch() {
        return worstMatchName != null ? valueOf(worstMatchName) : null;
    }

    public static PhilosopherType fromLabel(String label) {
        return Arrays.stream(values())
                .filter(type -> type.label.equals(label))
                .findFirst()
                .orElse(null);
    }
}
