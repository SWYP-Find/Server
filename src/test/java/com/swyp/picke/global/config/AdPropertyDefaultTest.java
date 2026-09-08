package com.swyp.picke.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 광고 환경변수에 기본값이 없으면 값이 빠진 서버에서 앱 전체가 뜨지 않는다.
 * 광고와 무관한 API까지 같이 죽는다. 실제로 COUPANG_PARTNERS_ID 때문에 dev가 502로 내려간 적이 있다.
 *
 * <p>테스트 설정(application-test.yml)이 값을 박아 두기 때문에 컨텍스트를 띄우는 것만으로는
 * 이 경우를 재현할 수 없다. 그래서 설정 파일의 플레이스홀더 자체를 검사한다.
 */
class AdPropertyDefaultTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(COUPANG_[A-Z0-9_]+|ADPICK_[A-Z0-9_]+)(:[^}]*)?}");

    @Test
    @DisplayName("광고 환경변수는 모두 기본값을 갖는다. 값이 없어도 앱은 떠야 한다")
    void adPlaceholders_haveDefaults() throws IOException {
        String yml = new String(new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                                StandardCharsets.UTF_8);

        Matcher matcher = PLACEHOLDER.matcher(yml);
        List<String> missingDefaults = new ArrayList<>();
        int found = 0;
        while (matcher.find()) {
            found++;
            if (matcher.group(2) == null) {
                missingDefaults.add(matcher.group(1));
            }
        }

        assertThat(found).isPositive();
        assertThat(missingDefaults).isEmpty();
    }
}
