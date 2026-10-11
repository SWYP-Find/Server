package com.swyp.picke.domain.battle.thumbnail;

/**
 * 예시 이미지가 어떤 배틀 주제를 어떤 장면으로 옮긴 것인지에 대한 설명.
 * 예시 이미지와 같은 이름의 txt 파일에 "주제: ...", "장면: ..." 을 한 줄씩 적는다. 없으면 화풍 예시로만 쓰인다.
 */
public record ReferenceCaption(String topic, String scene) {

    public static final ReferenceCaption NONE = new ReferenceCaption("", "");

    private static final String TOPIC_PREFIX = "주제:";
    private static final String SCENE_PREFIX = "장면:";

    public static ReferenceCaption parse(String text) {
        String topic = "";
        String scene = "";
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(TOPIC_PREFIX)) {
                topic = trimmed.substring(TOPIC_PREFIX.length()).trim();
            } else if (trimmed.startsWith(SCENE_PREFIX)) {
                scene = trimmed.substring(SCENE_PREFIX.length()).trim();
            }
        }
        return new ReferenceCaption(topic, scene);
    }

    public boolean hasTopic() {
        return !topic.isEmpty();
    }

    /** 주제와 장면이 모두 있어야 "주제 → 장면" 예시로 쓸 수 있다. */
    public boolean hasScene() {
        return hasTopic() && !scene.isEmpty();
    }
}
