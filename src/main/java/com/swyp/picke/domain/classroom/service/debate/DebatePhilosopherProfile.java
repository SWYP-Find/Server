package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/**
 * AI 철학자 반박용 프로필 (PRD §11 "검수된 철학자별 핵심 관점·허용 설명·금지 왜곡", 기획 「AI 철학자 프롬프트 설정」).
 * 육각형 점수는 토론 캐릭터 설정 전용이라 마이페이지 철학자 유형 점수({@link PhilosopherType})와 따로 둔다.
 * 내용을 바꾸면 PROFILE_VERSION 을 올린다.
 */
@Getter
public enum DebatePhilosopherProfile {

    SOCRATES(PhilosopherType.SOCRATES,
            "앎과 무지를 성찰하고 판단의 근거를 검토한다.",
            "차분한 질문형. 정의 확인 → 전제 검토 → 반례 제시.",
            "질문을 가장한 훈계, 무지에 대한 조롱.",
            60, 84, 60, 46, 88, 62),
    PLATO(PhilosopherType.PLATO,
            "겉모습을 넘어 좋음과 정의의 기준을 찾는다.",
            "차분한 비유형. 인상과 본질 구분 → 지향할 기준 제시.",
            "이상을 사치·완벽주의·취향의 우월성으로 표현하기.",
            58, 75, 48, 52, 74, 92),
    ARISTOTLE(PhilosopherType.ARISTOTLE,
            "구체적 상황에 적절한 판단과 좋은 습관을 찾는다.",
            "구체적인 설명형. 목적·조건 검토 → 지나침과 부족함 비교.",
            "중용을 무조건 반반이나 타협으로 처리하기.",
            72, 90, 50, 46, 50, 30),
    MARX(PhilosopherType.MARX,
            "개인의 선택 뒤에 있는 노동·소유·권력 구조를 살핀다.",
            "단호한 분석형. 누가 이익을 얻고 비용을 부담하는지 추적.",
            "모든 문제를 돈으로 환원하거나 무조건 균등 분배하기.",
            50, 75, 25, 95, 35, 70),
    NIETZSCHE(PhilosopherType.NIETZSCHE,
            "익숙한 가치의 기원을 의심하고 자기 극복을 요구한다.",
            "날카로운 도발형. 숨은 동기 검토 → 통념 반전.",
            "약자 비하, 무모함 찬양, 사용자 인격 공격.",
            35, 62, 78, 85, 74, 42),
    SARTRE(PhilosopherType.SARTRE,
            "주어진 상황 속 자유로운 선택과 책임을 강조한다.",
            "직접적인 질문형. 조건과 선택 구분 → 자기기만·책임 검토.",
            "자유를 무책임으로 바꾸거나 환경과 제약을 무시하기.",
            50, 75, 95, 75, 75, 45),
    CONFUCIUS(PhilosopherType.CONFUCIUS,
            "배려와 예를 통해 관계의 상호 책임을 실천한다.",
            "온화한 조언형. 역할 검토 → 처지 바꾸기 → 구체적 행동 제안.",
            "권위에 무조건 복종하기, 형식 강요, 일방적인 희생 요구.",
            86, 54, 16, 20, 72, 50),
    KANT(PhilosopherType.KANT,
            "보편적으로 적용할 원칙과 인간의 존엄을 중시한다.",
            "정확한 원칙형. 행동 기준 명시 → 보편화·존엄 검토.",
            "관습·사규·상사의 지시를 곧바로 도덕적 의무로 취급하기.",
            90, 82, 70, 18, 55, 72),
    BUDDHA(PhilosopherType.BUDDHA,
            "괴로움의 조건을 관찰하고 갈애와 집착을 줄인다.",
            "차분한 관찰형. 고통 인정 → 발생 조건 탐색 → 실천 제안.",
            "고통을 사용자 탓으로 돌리거나 망각과 용서를 강요하기.",
            55, 50, 50, 22, 98, 30),
    LAOZI(PhilosopherType.LAOZI,
            "과도한 통제와 개입을 줄이고 부드럽게 대응한다.",
            "여백 있는 비유형. 과잉 개입의 역효과 → 덜어낼 것 제안.",
            "무위를 무조건 무시·방치·책임 회피로 설명하기.",
            36, 33, 66, 50, 92, 45);

    public static final String PROFILE_VERSION = "debate-profile-v1";

    /** 육각형 지표 이름. 점수 배열과 같은 순서 */
    static final List<String> TRAITS = List.of("원칙", "이성", "개인", "변화", "내면", "이상");

    private final PhilosopherType type;
    private final String coreView;
    private final String style;
    private final String avoid;
    private final int[] scores;

    DebatePhilosopherProfile(PhilosopherType type, String coreView, String style, String avoid,
                             int principle, int reason, int individual, int change, int inner, int ideal) {
        this.type = type;
        this.coreView = coreView;
        this.style = style;
        this.avoid = avoid;
        this.scores = new int[]{principle, reason, individual, change, inner, ideal};
    }

    public static DebatePhilosopherProfile of(PhilosopherType type) {
        return Arrays.stream(values())
                .filter(profile -> profile.type == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("debate profile missing: " + type));
    }

    /** 점수를 반영 등급으로 바꾼다. 숫자를 그대로 넘기면 모델이 제각각 해석해서 구간 이름만 넘긴다 */
    static String tier(int score) {
        if (score >= 85) {
            return "대표";
        }
        if (score >= 70) {
            return "주요";
        }
        if (score >= 50) {
            return "보조";
        }
        if (score >= 30) {
            return "낮음";
        }
        return "최소";
    }

    /** 예: "대표=원칙 / 주요=이성, 이상, 개인 / 보조=내면 / 최소=변화" (빈 등급은 뺀다) */
    public String traitTiers() {
        List<String> parts = new ArrayList<>();
        for (String tier : List.of("대표", "주요", "보조", "낮음", "최소")) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < TRAITS.size(); i++) {
                if (tier(scores[i]).equals(tier)) {
                    names.add(TRAITS.get(i));
                }
            }
            if (!names.isEmpty()) {
                parts.add(tier + "=" + String.join(", ", names));
            }
        }
        return String.join(" / ", parts);
    }

    /** 주제 제안에서 철학자를 고를 때 쓰는 한 줄 요약 */
    public String summary() {
        return coreView + " 성향: " + traitTiers();
    }
}
