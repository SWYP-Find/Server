# 애드핏 리워드 크레딧 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AdMob을 완전히 제거하고, 애드핏 전면 광고 시청 후 티켓 방식으로 크레딧 20을 지급하는 API를 구현한다.

**Architecture:** 클라이언트가 티켓을 발급받고(`POST /ticket`) 애드핏 광고를 노출한 뒤 티켓으로 청구한다(`POST /claim`). 애드핏은 S2S 콜백을 제공하지 않으므로 서버는 광고 시청을 증명할 수 없고, 대신 JWT 인증 + 티켓 1회성 + 최소 경과시간 + 일일 한도로 남용을 억제한다. 기존 `AdRewardHistory` / `CreditService` 파이프라인을 그대로 재사용한다.

**Tech Stack:** Spring Boot, Spring Data JPA, PostgreSQL, JUnit5 + Mockito + AssertJ, Gradle

**설계 문서:** `docs/superpowers/specs/2026-07-16-adfit-reward-design.md`

## Global Constraints

- 대상 브랜치: `dev`에서 `feat/adfit-reward` 분기. 작업 완료 후 `dev`로 PR (레포 Git Flow 컨벤션).
- **커밋 메시지는 한국어로 작성한다.** `Co-Authored-By` 라인을 절대 추가하지 않는다.
- 유저 조회는 `userService.findCurrentUser()`를 사용한다. 컨트롤러에서 `@AuthenticationPrincipal`을 쓰지 않는다 (`AttendanceController` 컨벤션).
- 크레딧 금액은 항상 `CreditType.FREE_CHARGE.getDefaultAmount()`(20) 고정. 클라이언트 값을 신뢰하지 않는다.
- 시간대는 `ZoneId.of("Asia/Seoul")`. `AttendanceService`의 `SEOUL_ZONE` 패턴을 따른다.
- 설정값은 `AdFitConfig`(`@Configuration` + `@Value` + `@Getter`)로 외부화한다 — 삭제될 `AdMobConfig`와 동일한 패턴.
- 테스트는 `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` / `@Mock` + BDDMockito(`given`/`verify`) + AssertJ. `@Value` 필드는 `ReflectionTestUtils.setField`로 주입한다 (기존 `AdMobRewardServiceTest` 컨벤션).
- 스키마 마이그레이션 스크립트를 작성하지 않는다. `ddl-auto: update`가 `ad_reward_ticket`을 생성한다. `ad_reward_history`는 변경하지 않는다.
- 테스트 실행: `./gradlew test`, 단일 클래스는 `./gradlew test --tests "<FQCN>"`

## File Structure

**삭제 (Task 1)**
- `src/main/java/com/swyp/picke/global/config/AdMobConfig.java`
- `src/main/java/com/swyp/picke/domain/reward/controller/AdMobRewardController.java`
- `src/main/java/com/swyp/picke/domain/reward/service/AdMobRewardService.java`
- `src/main/java/com/swyp/picke/domain/reward/service/AdMobRewardServiceImpl.java`
- `src/main/java/com/swyp/picke/domain/reward/dto/request/AdMobRewardRequest.java`
- `src/main/java/com/swyp/picke/domain/reward/dto/response/AdMobRewardResponse.java`
- `src/test/java/com/swyp/picke/domain/reward/service/AdMobRewardServiceTest.java`

**신규**
- `domain/reward/entity/AdRewardTicket.java` — 티켓 엔티티 (Task 2)
- `domain/reward/repository/AdRewardTicketRepository.java` — 티켓 조회 (Task 2)
- `global/config/AdFitConfig.java` — 한도/시간 설정 (Task 3)
- `domain/reward/dto/response/AdFitTicketResponse.java` (Task 3)
- `domain/reward/dto/request/AdFitClaimRequest.java` (Task 4)
- `domain/reward/dto/response/AdFitClaimResponse.java` (Task 4)
- `domain/reward/service/AdFitRewardService.java` — 인터페이스 (Task 3)
- `domain/reward/service/AdFitRewardServiceImpl.java` — 발급(Task 3) + 청구(Task 4)
- `domain/reward/controller/AdFitRewardController.java` (Task 5)
- `src/test/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceTest.java` (Task 3~4)

**수정**
- `build.gradle:51-53` (Task 1), `application.yml:76-81` (Task 1, 3)
- `SecurityConfig.java:49` (Task 1, 5), `JwtFilter.java:30` (Task 1)
- `SwaggerConfig.java:88,97` (Task 1)
- `ErrorCode.java:110` (Task 1, 2)
- `docs/api-specs/reward-api.md` (Task 6)

**유지 (변경 없음)**
`AdRewardHistory`, `RewardItem`, `CreditService`, `CreditType`, `UserService`, `StaticTextFileController`

---

## Task 0: 브랜치 생성

- [ ] **Step 1: dev 최신화 후 브랜치 분기**

```bash
cd /Users/suhwonji/Desktop/SideProject/Server
git checkout dev
git pull origin dev
git checkout -b feat/adfit-reward
git status --short --branch
```

기대: `## feat/adfit-reward` 출력, 변경사항 없음

---

## Task 1: AdMob 완전 제거

AdMob 코드를 먼저 지워야 `AdRewardHistoryRepository` 등 재사용 대상이 깨끗한 상태에서 신규 코드를 얹을 수 있다. 이 태스크의 산출물은 **AdMob 흔적이 0이면서 빌드가 통과하는 상태**다.

**Files:**
- Delete: 위 "삭제" 목록 7개 파일
- Modify: `build.gradle`, `src/main/resources/application.yml`, `SecurityConfig.java`, `JwtFilter.java`, `SwaggerConfig.java`, `ErrorCode.java`

**Interfaces:**
- Consumes: 없음
- Produces: 없음 (제거 전용). `AdRewardHistory`, `AdRewardHistoryRepository.existsByTransactionId(String)`, `RewardItem`은 그대로 남는다.

- [ ] **Step 1: 파일 7개 삭제**

```bash
cd /Users/suhwonji/Desktop/SideProject/Server
git rm src/main/java/com/swyp/picke/global/config/AdMobConfig.java \
       src/main/java/com/swyp/picke/domain/reward/controller/AdMobRewardController.java \
       src/main/java/com/swyp/picke/domain/reward/service/AdMobRewardService.java \
       src/main/java/com/swyp/picke/domain/reward/service/AdMobRewardServiceImpl.java \
       src/main/java/com/swyp/picke/domain/reward/dto/request/AdMobRewardRequest.java \
       src/main/java/com/swyp/picke/domain/reward/dto/response/AdMobRewardResponse.java \
       src/test/java/com/swyp/picke/domain/reward/service/AdMobRewardServiceTest.java
```

- [ ] **Step 2: Tink 의존성 제거**

`build.gradle`에서 51~53행 3줄(주석 포함)을 삭제한다:

```gradle
    // AdMob SSV 검증을 위한 Tink 라이브러리
    implementation 'com.google.crypto.tink:apps-rewardedads:1.9.1'
    testImplementation 'com.google.crypto.tink:apps-rewardedads:1.9.1'
```

- [ ] **Step 3: application.yml에서 admob 블록 제거**

76~81행을 삭제한다:

```yaml
admob:
  app-id: ${ADMOB_APP_ID}
  reward:
    unit-id:
      ios: ${ADMOB_REWARD_UNIT_ID_IOS}
      android: ${ADMOB_REWARD_UNIT_ID_ANDROID}
```

- [ ] **Step 4: SecurityConfig에서 permitAll 항목 제거**

`SecurityConfig.java:49`의 아래 한 줄을 삭제한다. **애드핏 엔드포인트를 여기 추가하지 않는다** — 인증이 필요하다.

```java
                                "/api/v1/admob/reward/**",
```

- [ ] **Step 5: JwtFilter에서 제외 경로 제거**

`JwtFilter.java:30`의 아래 한 줄을 삭제한다:

```java
            "/api/v1/admob/reward",
```

- [ ] **Step 6: SwaggerConfig에서 admob 경로 제거**

88행과 97행에서 `, "/api/v1/admob/**"` 부분만 각각 제거한다.

88행 (사용자 API 그룹):
```java
                .pathsToExclude("/api/v1/admin/**", "/api/v1/files/**", "/api/v1/resources/**", "/api/test/**")
```

97행 (관리자 API 그룹):
```java
                .pathsToMatch("/api/v1/admin/**", "/api/v1/files/**", "/api/v1/resources/**", "/api/test/**")
```

- [ ] **Step 7: ErrorCode에서 AdMob 전용 코드 제거**

`ErrorCode.java:110`의 아래 한 줄을 삭제한다 (애드핏에는 서명 검증이 없다):

```java
    REWARD_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "REWARD_401", "AdMob 서명 검증에 실패했습니다."),
```

- [ ] **Step 8: 잔존 참조 확인**

```bash
grep -rn -i "admob\|tink" --include="*.java" --include="*.yml" --include="*.gradle" . | grep -v "^./.git" | grep -v "^./docs"
```

기대: 출력 없음. (`docs/`는 Task 6에서 정리하므로 제외)

- [ ] **Step 9: 빌드 및 전체 테스트**

```bash
./gradlew clean build
```

기대: `BUILD SUCCESSFUL`. 컴파일 에러가 나면 8단계에서 놓친 참조가 있는 것이다.

- [ ] **Step 10: 커밋**

```bash
git add -A
git commit -m "chore: AdMob 리워드 연동 및 Tink 의존성 제거

계정 승인 거부로 AdMob 광고를 받을 수 없어 애드핏으로 교체한다.
지급 파이프라인(AdRewardHistory, CreditService)은 재사용하므로 남긴다."
```

---

## Task 2: 티켓 엔티티와 에러 코드

**Files:**
- Create: `src/main/java/com/swyp/picke/domain/reward/entity/AdRewardTicket.java`
- Create: `src/main/java/com/swyp/picke/domain/reward/repository/AdRewardTicketRepository.java`
- Modify: `src/main/java/com/swyp/picke/global/common/exception/ErrorCode.java`
- Modify: `src/main/java/com/swyp/picke/domain/reward/repository/AdRewardHistoryRepository.java`

**Interfaces:**
- Consumes: `BaseEntity`(`getId()`, `getCreatedAt()`), `User`
- Produces:
  - `AdRewardTicket.builder().user(User).ticketId(String).build()`
  - `AdRewardTicket#getTicketId(): String`, `#getUser(): User`, `#getUsedAt(): LocalDateTime`, `#isUsed(): boolean`, `#markUsed(LocalDateTime): void`
  - `AdRewardTicketRepository#findByTicketId(String): Optional<AdRewardTicket>`
  - `AdRewardHistoryRepository#countByUserIdAndCreatedAtBetween(Long, LocalDateTime, LocalDateTime): long`
  - `ErrorCode.REWARD_TICKET_NOT_FOUND`, `REWARD_TICKET_ALREADY_USED`, `REWARD_TICKET_EXPIRED`, `REWARD_TICKET_TOO_SOON`, `REWARD_DAILY_LIMIT_EXCEEDED`

- [ ] **Step 1: 티켓 엔티티 작성**

`src/main/java/com/swyp/picke/domain/reward/entity/AdRewardTicket.java`:

```java
package com.swyp.picke.domain.reward.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 애드핏 광고 시청 보상 청구용 1회성 티켓.
 * 애드핏은 S2S 콜백을 제공하지 않으므로 서버가 광고 시청을 증명할 수 없다.
 * 티켓은 증명이 아니라 남용 억제 수단이다 (1회성 + 최소 경과시간 + 일일 한도).
 */
@Entity
@Getter
@Table(name = "ad_reward_ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdRewardTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ticket_id", unique = true, nullable = false)
    private String ticketId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    public AdRewardTicket(User user, String ticketId) {
        this.user = user;
        this.ticketId = ticketId;
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public void markUsed(LocalDateTime at) {
        this.usedAt = at;
    }
}
```

- [ ] **Step 2: 티켓 리포지토리 작성**

`src/main/java/com/swyp/picke/domain/reward/repository/AdRewardTicketRepository.java`:

```java
package com.swyp.picke.domain.reward.repository;

import com.swyp.picke.domain.reward.entity.AdRewardTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdRewardTicketRepository extends JpaRepository<AdRewardTicket, Long> {

    Optional<AdRewardTicket> findByTicketId(String ticketId);
}
```

- [ ] **Step 3: 일일 한도 집계 메서드 추가**

`AdRewardHistoryRepository`에 아래 메서드를 추가한다 (기존 `existsByTransactionId`는 유지):

```java
    /**
     * 당일 보상 지급 건수. 일일 한도 강제에 사용한다.
     * 기준은 실제 지급 이력이며, 티켓 발급 건수가 아니다.
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
```

`import java.time.LocalDateTime;`를 추가한다.

- [ ] **Step 4: ErrorCode 추가**

`ErrorCode.java`의 `// Reward` 섹션(107~109행 근처, `REWARD_INVALID_TYPE` 아래)에 추가한다:

```java
    REWARD_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "REWARD_404_2", "유효하지 않은 티켓입니다."),
    REWARD_TICKET_ALREADY_USED(HttpStatus.CONFLICT, "REWARD_409", "이미 사용된 티켓입니다."),
    REWARD_TICKET_EXPIRED(HttpStatus.GONE, "REWARD_410", "만료된 티켓입니다."),
    REWARD_TICKET_TOO_SOON(HttpStatus.BAD_REQUEST, "REWARD_400_2", "광고 시청이 완료되지 않았습니다."),
    REWARD_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "REWARD_429", "오늘 받을 수 있는 광고 보상을 모두 받았습니다."),
```

- [ ] **Step 5: 컴파일 확인**

```bash
./gradlew compileJava
```

기대: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat: 애드핏 보상 티켓 엔티티와 에러 코드 추가

ad_reward_ticket 테이블은 ddl-auto: update가 생성하므로
마이그레이션 스크립트를 두지 않는다."
```

---

## Task 3: 티켓 발급

**Files:**
- Create: `src/main/java/com/swyp/picke/global/config/AdFitConfig.java`
- Create: `src/main/java/com/swyp/picke/domain/reward/dto/response/AdFitTicketResponse.java`
- Create: `src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardService.java`
- Create: `src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceImpl.java`
- Create: `src/test/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceTest.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Consumes: Task 2의 `AdRewardTicket`, `AdRewardTicketRepository`, `AdRewardHistoryRepository#countByUserIdAndCreatedAtBetween`, `ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED`
- Produces:
  - `AdFitConfig#getDailyLimit(): int`, `#getMinWatchSeconds(): long`, `#getTicketTtlSeconds(): long`
  - `AdFitRewardService#issueTicket(): AdFitTicketResponse`
  - `AdFitTicketResponse(String ticketId, long expiresInSeconds)` — record, 정적 팩토리 `of(String, long)`

- [ ] **Step 1: 설정값 추가**

`application.yml` 끝에 추가한다 (제거된 `admob` 블록 자리):

```yaml
adfit:
  reward:
    daily-limit: 10          # 하루 최대 지급 횟수 (20크레딧 × 10 = 200/일)
    min-watch-seconds: 5     # 티켓 발급~청구 최소 간격. 실제 광고 길이 측정 후 조정 필요
    ticket-ttl-seconds: 300  # 티켓 만료 (5분)
```

- [ ] **Step 2: AdFitConfig 작성**

`src/main/java/com/swyp/picke/global/config/AdFitConfig.java`:

```java
package com.swyp.picke.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AdFitConfig {

    /** 하루 최대 보상 지급 횟수 */
    @Value("${adfit.reward.daily-limit}")
    private int dailyLimit;

    /** 티켓 발급 후 청구까지 최소 경과시간(초). 즉시 청구 자동화를 차단한다. */
    @Value("${adfit.reward.min-watch-seconds}")
    private long minWatchSeconds;

    /** 티켓 만료 시간(초) */
    @Value("${adfit.reward.ticket-ttl-seconds}")
    private long ticketTtlSeconds;
}
```

- [ ] **Step 3: 응답 DTO 작성**

`src/main/java/com/swyp/picke/domain/reward/dto/response/AdFitTicketResponse.java`:

```java
package com.swyp.picke.domain.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "애드핏 광고 보상 티켓 발급 응답")
public record AdFitTicketResponse(

        @Schema(description = "보상 청구에 사용할 1회성 티켓 ID", example = "9f1c8e2a-4b7d-4c1e-9a3f-2b8c6d5e4f10")
        String ticketId,

        @Schema(description = "티켓 만료까지 남은 시간(초)", example = "300")
        long expiresInSeconds
) {
    public static AdFitTicketResponse of(String ticketId, long expiresInSeconds) {
        return new AdFitTicketResponse(ticketId, expiresInSeconds);
    }
}
```

- [ ] **Step 4: 서비스 인터페이스 작성**

`src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardService.java`:

```java
package com.swyp.picke.domain.reward.service;

import com.swyp.picke.domain.reward.dto.response.AdFitTicketResponse;

public interface AdFitRewardService {

    /** 광고 노출 전 1회성 티켓을 발급한다. 일일 한도 초과 시 거부한다. */
    AdFitTicketResponse issueTicket();
}
```

- [ ] **Step 5: 실패하는 테스트 작성**

`src/test/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceTest.java`:

```java
package com.swyp.picke.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.swyp.picke.domain.reward.dto.response.AdFitTicketResponse;
import com.swyp.picke.domain.reward.entity.AdRewardTicket;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.reward.repository.AdRewardTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.config.AdFitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class AdFitRewardServiceTest {

    @InjectMocks
    private AdFitRewardServiceImpl rewardService;

    @Mock
    private AdRewardTicketRepository ticketRepository;

    @Mock
    private AdRewardHistoryRepository adRewardHistoryRepository;

    @Mock
    private UserService userService;

    @Mock
    private CreditService creditService;

    @Mock
    private AdFitConfig adFitConfig;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("일일 한도 미달이면 티켓이 발급된다")
    void issueTicket_success() {
        given(userService.findCurrentUser()).willReturn(user);
        given(adFitConfig.getDailyLimit()).willReturn(10);
        given(adFitConfig.getTicketTtlSeconds()).willReturn(300L);
        given(adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                .willReturn(3L);

        AdFitTicketResponse response = rewardService.issueTicket();

        assertThat(response.ticketId()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(300L);
        verify(ticketRepository).save(any(AdRewardTicket.class));
    }

    @Test
    @DisplayName("일일 한도에 도달하면 티켓 발급이 거부된다")
    void issueTicket_dailyLimitExceeded() {
        given(userService.findCurrentUser()).willReturn(user);
        given(adFitConfig.getDailyLimit()).willReturn(10);
        given(adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                .willReturn(10L);

        assertThatThrownBy(() -> rewardService.issueTicket())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED);

        verify(ticketRepository, never()).save(any(AdRewardTicket.class));
    }
}
```

주의: `User.builder().build()`와 `CustomException`의 필드명(`errorCode`)이 실제 구현과 다르면 컴파일/단언이 실패한다. `src/main/java/com/swyp/picke/domain/user/entity/User.java`와 `global/common/exception/CustomException.java`를 열어 실제 빌더 필수값과 필드명을 확인하고 맞춘다. 기존 `AdMobRewardServiceTest`가 삭제되었으므로, 필요하면 `git show HEAD~2 -- src/test/java/com/swyp/picke/domain/reward/service/AdMobRewardServiceTest.java`로 이전 테스트의 `User` 생성 방식을 참고한다.

- [ ] **Step 6: 테스트 실패 확인**

```bash
./gradlew test --tests "com.swyp.picke.domain.reward.service.AdFitRewardServiceTest"
```

기대: 컴파일 실패 — `AdFitRewardServiceImpl` 클래스 없음

- [ ] **Step 7: 최소 구현 작성**

`src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceImpl.java`:

```java
package com.swyp.picke.domain.reward.service;

import com.swyp.picke.domain.reward.dto.response.AdFitTicketResponse;
import com.swyp.picke.domain.reward.entity.AdRewardTicket;
import com.swyp.picke.domain.reward.repository.AdRewardHistoryRepository;
import com.swyp.picke.domain.reward.repository.AdRewardTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.service.CreditService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import com.swyp.picke.global.config.AdFitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdFitRewardServiceImpl implements AdFitRewardService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final AdRewardTicketRepository ticketRepository;
    private final AdRewardHistoryRepository adRewardHistoryRepository;
    private final UserService userService;
    private final CreditService creditService;
    private final AdFitConfig adFitConfig;

    @Override
    @Transactional
    public AdFitTicketResponse issueTicket() {
        User user = userService.findCurrentUser();

        // 광고를 보여주기 전에 미리 차단한다. 실제 한도 강제는 claim에서 한다.
        if (countTodayRewards(user.getId()) >= adFitConfig.getDailyLimit()) {
            log.info("[AdFit] 일일 한도 초과로 티켓 발급 거부: userId={}", user.getId());
            throw new CustomException(ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED);
        }

        String ticketId = UUID.randomUUID().toString();
        ticketRepository.save(AdRewardTicket.builder()
                .user(user)
                .ticketId(ticketId)
                .build());

        log.info("[AdFit] 티켓 발급: userId={}, ticketId={}", user.getId(), ticketId);
        return AdFitTicketResponse.of(ticketId, adFitConfig.getTicketTtlSeconds());
    }

    /** 당일 실제 지급 건수. 티켓 발급 수가 아니라 이력 기준이다. */
    private long countTodayRewards(Long userId) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        return adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(
                userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }
}
```

- [ ] **Step 8: 테스트 통과 확인**

```bash
./gradlew test --tests "com.swyp.picke.domain.reward.service.AdFitRewardServiceTest"
```

기대: PASS (2개 테스트)

- [ ] **Step 9: 커밋**

```bash
git add -A
git commit -m "feat: 애드핏 보상 티켓 발급 구현

광고 노출 전 1회성 티켓을 발급하고, 일일 한도 초과 시 이 단계에서 차단한다.
발급 시 차단은 UX 목적이며 실제 한도 강제는 청구 시점에서 한다."
```

---

## Task 4: 크레딧 청구

**Files:**
- Create: `src/main/java/com/swyp/picke/domain/reward/dto/request/AdFitClaimRequest.java`
- Create: `src/main/java/com/swyp/picke/domain/reward/dto/response/AdFitClaimResponse.java`
- Modify: `src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardService.java`
- Modify: `src/main/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceImpl.java`
- Modify: `src/test/java/com/swyp/picke/domain/reward/service/AdFitRewardServiceTest.java`

**Interfaces:**
- Consumes: Task 3의 `AdFitRewardServiceImpl`, `AdFitConfig`; Task 2의 `AdRewardTicket#markUsed`, `#isUsed`; 기존 `AdRewardHistory.builder()`, `AdRewardHistoryRepository#existsByTransactionId`, `CreditService#addCredit(Long, CreditType, int, Long)`, `#getTotalPoints(Long)`
- Produces:
  - `AdFitRewardService#claim(AdFitClaimRequest): AdFitClaimResponse`
  - `AdFitClaimRequest(String ticketId)` — record
  - `AdFitClaimResponse(int rewardedAmount, int totalCredit)` — record, 정적 팩토리 `of(int, int)`

- [ ] **Step 1: 요청/응답 DTO 작성**

`src/main/java/com/swyp/picke/domain/reward/dto/request/AdFitClaimRequest.java`:

```java
package com.swyp.picke.domain.reward.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "애드핏 광고 보상 청구 요청")
public record AdFitClaimRequest(

        @Schema(description = "발급받은 티켓 ID", example = "9f1c8e2a-4b7d-4c1e-9a3f-2b8c6d5e4f10")
        @NotBlank(message = "티켓 ID는 필수입니다.")
        String ticketId
) {
}
```

`src/main/java/com/swyp/picke/domain/reward/dto/response/AdFitClaimResponse.java`:

```java
package com.swyp.picke.domain.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "애드핏 광고 보상 청구 결과")
public record AdFitClaimResponse(

        @Schema(description = "이번 청구로 지급된 크레딧. 이미 처리된 티켓이면 0", example = "20")
        int rewardedAmount,

        @Schema(description = "지급 후 유저의 총 크레딧", example = "145")
        int totalCredit
) {
    public static AdFitClaimResponse of(int rewardedAmount, int totalCredit) {
        return new AdFitClaimResponse(rewardedAmount, totalCredit);
    }
}
```

- [ ] **Step 2: 인터페이스에 claim 추가**

`AdFitRewardService.java`에 추가한다:

```java
    /**
     * 티켓을 검증하고 크레딧을 지급한다.
     * 애드핏은 S2S 콜백이 없어 광고 시청을 증명할 수 없다.
     * 티켓 1회성 + 최소 경과시간 + 일일 한도로 남용을 억제한다.
     */
    AdFitClaimResponse claim(AdFitClaimRequest request);
```

import 2개를 추가한다:
```java
import com.swyp.picke.domain.reward.dto.request.AdFitClaimRequest;
import com.swyp.picke.domain.reward.dto.response.AdFitClaimResponse;
```

- [ ] **Step 3: 실패하는 테스트 작성**

`AdFitRewardServiceTest.java`에 아래 테스트들을 추가한다. 기존 import에 더해 `AdRewardHistory`, `CreditType`, `RewardItem`, `Duration`, `Optional`, `eq`, `times`, `willAnswer` 등이 필요하다.

```java
    private AdRewardTicket ticketIssuedSecondsAgo(User owner, long secondsAgo) {
        AdRewardTicket ticket = AdRewardTicket.builder()
                .user(owner)
                .ticketId("ticket-uuid")
                .build();
        ReflectionTestUtils.setField(ticket, "createdAt",
                LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusSeconds(secondsAgo));
        return ticket;
    }

    private void givenClaimConfig() {
        given(adFitConfig.getDailyLimit()).willReturn(10);
        given(adFitConfig.getMinWatchSeconds()).willReturn(5L);
        given(adFitConfig.getTicketTtlSeconds()).willReturn(300L);
    }

    @Test
    @DisplayName("정상 티켓으로 청구하면 크레딧 20이 적립되고 티켓이 사용 처리된다")
    void claim_success() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 10);
        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));
        givenClaimConfig();
        given(adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                .willReturn(0L);
        given(adRewardHistoryRepository.existsByTransactionId("ticket-uuid")).willReturn(false);
        given(adRewardHistoryRepository.saveAndFlush(any(AdRewardHistory.class)))
                .willAnswer(invocation -> {
                    AdRewardHistory saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 99L);
                    return saved;
                });
        given(creditService.getTotalPoints(1L)).willReturn(145);

        AdFitClaimResponse response = rewardService.claim(new AdFitClaimRequest("ticket-uuid"));

        assertThat(response.rewardedAmount()).isEqualTo(CreditType.FREE_CHARGE.getDefaultAmount());
        assertThat(response.totalCredit()).isEqualTo(145);
        assertThat(ticket.isUsed()).isTrue();
        verify(creditService).addCredit(
                eq(1L), eq(CreditType.FREE_CHARGE),
                eq(CreditType.FREE_CHARGE.getDefaultAmount()), eq(99L));
    }

    @Test
    @DisplayName("존재하지 않는 티켓이면 거부된다")
    void claim_ticketNotFound() {
        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("nope")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_TICKET_NOT_FOUND);
    }

    @Test
    @DisplayName("타인의 티켓이면 존재하지 않는 것과 동일하게 거부된다")
    void claim_otherUsersTicket() {
        User other = User.builder().build();
        ReflectionTestUtils.setField(other, "id", 2L);
        AdRewardTicket ticket = ticketIssuedSecondsAgo(other, 10);

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("ticket-uuid")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_TICKET_NOT_FOUND);

        verify(creditService, never()).addCredit(anyLong(), any(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("이미 사용된 티켓이면 거부된다")
    void claim_alreadyUsed() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 10);
        ticket.markUsed(LocalDateTime.now(ZoneId.of("Asia/Seoul")));

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("ticket-uuid")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_TICKET_ALREADY_USED);
    }

    @Test
    @DisplayName("만료된 티켓이면 거부된다")
    void claim_expired() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 301);

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));
        given(adFitConfig.getTicketTtlSeconds()).willReturn(300L);

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("ticket-uuid")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_TICKET_EXPIRED);
    }

    @Test
    @DisplayName("최소 경과시간 전에 청구하면 거부된다")
    void claim_tooSoon() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 1);

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));
        given(adFitConfig.getTicketTtlSeconds()).willReturn(300L);
        given(adFitConfig.getMinWatchSeconds()).willReturn(5L);

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("ticket-uuid")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_TICKET_TOO_SOON);
    }

    @Test
    @DisplayName("한도 초과분 티켓을 미리 발급받아 청구해도 한도에서 막힌다")
    void claim_dailyLimitEnforcedAtClaim() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 10);

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));
        givenClaimConfig();
        given(adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                .willReturn(10L);

        assertThatThrownBy(() -> rewardService.claim(new AdFitClaimRequest("ticket-uuid")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED);

        verify(creditService, never()).addCredit(anyLong(), any(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("이미 지급 이력이 있는 티켓이면 재지급 없이 멱등 응답한다")
    void claim_idempotent() {
        AdRewardTicket ticket = ticketIssuedSecondsAgo(user, 10);

        given(userService.findCurrentUser()).willReturn(user);
        given(ticketRepository.findByTicketId("ticket-uuid")).willReturn(Optional.of(ticket));
        givenClaimConfig();
        given(adRewardHistoryRepository.countByUserIdAndCreatedAtBetween(anyLong(), any(), any()))
                .willReturn(0L);
        given(adRewardHistoryRepository.existsByTransactionId("ticket-uuid")).willReturn(true);
        given(creditService.getTotalPoints(1L)).willReturn(145);

        AdFitClaimResponse response = rewardService.claim(new AdFitClaimRequest("ticket-uuid"));

        assertThat(response.rewardedAmount()).isZero();
        assertThat(response.totalCredit()).isEqualTo(145);
        verify(creditService, never()).addCredit(anyLong(), any(), anyInt(), anyLong());
    }
```

- [ ] **Step 4: 테스트 실패 확인**

```bash
./gradlew test --tests "com.swyp.picke.domain.reward.service.AdFitRewardServiceTest"
```

기대: 컴파일 실패 — `claim` 메서드 없음

- [ ] **Step 5: claim 구현**

`AdFitRewardServiceImpl.java`에 추가한다:

```java
    @Override
    @Transactional
    public AdFitClaimResponse claim(AdFitClaimRequest request) {
        User user = userService.findCurrentUser();

        AdRewardTicket ticket = ticketRepository.findByTicketId(request.ticketId())
                .orElseThrow(() -> new CustomException(ErrorCode.REWARD_TICKET_NOT_FOUND));

        // 타인의 티켓은 존재 여부를 노출하지 않고 NOT_FOUND로 응답한다
        if (!ticket.getUser().getId().equals(user.getId())) {
            log.warn("[AdFit] 타인 티켓 청구 시도: userId={}, ticketId={}", user.getId(), request.ticketId());
            throw new CustomException(ErrorCode.REWARD_TICKET_NOT_FOUND);
        }

        if (ticket.isUsed()) {
            throw new CustomException(ErrorCode.REWARD_TICKET_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        long elapsedSeconds = Duration.between(ticket.getCreatedAt(), now).getSeconds();

        if (elapsedSeconds > adFitConfig.getTicketTtlSeconds()) {
            throw new CustomException(ErrorCode.REWARD_TICKET_EXPIRED);
        }
        if (elapsedSeconds < adFitConfig.getMinWatchSeconds()) {
            log.info("[AdFit] 최소 경과시간 미달 청구: userId={}, elapsed={}s", user.getId(), elapsedSeconds);
            throw new CustomException(ErrorCode.REWARD_TICKET_TOO_SOON);
        }

        // 한도를 실제로 강제하는 지점. 발급 시 검사만으로는 티켓 선발급으로 우회된다.
        if (countTodayRewards(user.getId()) >= adFitConfig.getDailyLimit()) {
            throw new CustomException(ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED);
        }

        int amount = CreditType.FREE_CHARGE.getDefaultAmount();

        if (adRewardHistoryRepository.existsByTransactionId(ticket.getTicketId())) {
            log.info("[AdFit] 이미 처리된 티켓: ticketId={}", ticket.getTicketId());
            return AdFitClaimResponse.of(0, creditService.getTotalPoints(user.getId()));
        }

        ticket.markUsed(now);

        AdRewardHistory history = AdRewardHistory.builder()
                .transactionId(ticket.getTicketId())
                .user(user)
                .rewardAmount(amount)
                .rewardItem(RewardItem.POINT)
                .build();
        adRewardHistoryRepository.saveAndFlush(history);

        // history.getId()를 referenceId로 써서 CreditHistory unique 충돌을 피한다 (기존 AdMob 구현과 동일)
        creditService.addCredit(user.getId(), CreditType.FREE_CHARGE, amount, history.getId());
        log.info("[AdFit] 보상 지급 완료: userId={}, amount={}, historyId={}",
                user.getId(), amount, history.getId());

        return AdFitClaimResponse.of(amount, creditService.getTotalPoints(user.getId()));
    }
```

import를 추가한다:
```java
import com.swyp.picke.domain.reward.dto.request.AdFitClaimRequest;
import com.swyp.picke.domain.reward.dto.response.AdFitClaimResponse;
import com.swyp.picke.domain.reward.entity.AdRewardHistory;
import com.swyp.picke.domain.reward.enums.RewardItem;
import com.swyp.picke.domain.user.enums.CreditType;
import java.time.Duration;
import java.time.LocalDateTime;
```

- [ ] **Step 6: 테스트 통과 확인**

```bash
./gradlew test --tests "com.swyp.picke.domain.reward.service.AdFitRewardServiceTest"
```

기대: PASS (10개 테스트)

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "feat: 애드핏 광고 보상 크레딧 청구 구현

티켓 소유자/사용여부/만료/최소 경과시간/일일 한도를 순서대로 검증한다.
크레딧은 클라이언트 값을 믿지 않고 FREE_CHARGE 고정값만 지급한다."
```

---

## Task 5: 컨트롤러와 보안 설정

**Files:**
- Create: `src/main/java/com/swyp/picke/domain/reward/controller/AdFitRewardController.java`
- Verify: `SecurityConfig.java` (애드핏 경로가 permitAll에 없어야 함)

**Interfaces:**
- Consumes: Task 3~4의 `AdFitRewardService#issueTicket()`, `#claim(AdFitClaimRequest)`
- Produces: `POST /api/v1/reward/adfit/ticket`, `POST /api/v1/reward/adfit/claim`

- [ ] **Step 1: 컨트롤러 작성**

`src/main/java/com/swyp/picke/domain/reward/controller/AdFitRewardController.java`:

```java
package com.swyp.picke.domain.reward.controller;

import com.swyp.picke.domain.reward.dto.request.AdFitClaimRequest;
import com.swyp.picke.domain.reward.dto.response.AdFitClaimResponse;
import com.swyp.picke.domain.reward.dto.response.AdFitTicketResponse;
import com.swyp.picke.domain.reward.service.AdFitRewardService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reward/adfit")
@Tag(name = "광고 보상 API", description = "애드핏 광고 시청 보상 티켓 발급 및 크레딧 청구")
public class AdFitRewardController {

    private final AdFitRewardService adFitRewardService;

    @Operation(summary = "광고 보상 티켓 발급",
            description = "애드핏 광고를 노출하기 전에 호출한다. 일일 한도 초과 시 429로 거부된다.")
    @PostMapping("/ticket")
    public ApiResponse<AdFitTicketResponse> issueTicket() {
        return ApiResponse.onSuccess(adFitRewardService.issueTicket());
    }

    @Operation(summary = "광고 보상 크레딧 청구",
            description = "광고 시청 완료 후 발급받은 티켓으로 크레딧을 청구한다. 티켓은 1회만 사용 가능하다.")
    @PostMapping("/claim")
    public ApiResponse<AdFitClaimResponse> claim(@Valid @RequestBody AdFitClaimRequest request) {
        return ApiResponse.onSuccess(adFitRewardService.claim(request));
    }
}
```

- [ ] **Step 2: 인증 필수 확인**

```bash
grep -n "reward" src/main/java/com/swyp/picke/global/config/SecurityConfig.java src/main/java/com/swyp/picke/domain/oauth/jwt/JwtFilter.java
```

기대: 출력 없음. 출력이 있으면 애드핏 경로가 인증 예외로 열려 있다는 뜻이므로 **반드시 제거**한다. 이 API는 JWT 인증이 필수다.

- [ ] **Step 3: 빌드 및 전체 테스트**

```bash
./gradlew clean build
```

기대: `BUILD SUCCESSFUL`

- [ ] **Step 4: 애플리케이션 기동 후 인증 없이 호출 시 401 확인**

```bash
./gradlew bootRun &
sleep 30
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/reward/adfit/ticket
```

기대: `401`

확인 후 `kill %1`로 종료한다. 로컬 DB/환경변수가 없어 기동이 실패하면 이 단계는 건너뛰고 Step 2의 grep 결과로 갈음한다.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat: 애드핏 광고 보상 API 엔드포인트 추가

AdMob SSV와 달리 우리 앱이 직접 호출하므로 JWT 인증을 필수로 둔다.
유저 식별을 토큰에서 하므로 custom_data 방식의 사칭 위험이 사라진다."
```

---

## Task 6: API 문서 갱신

**Files:**
- Modify: `docs/api-specs/reward-api.md`

- [ ] **Step 1: 기존 문서 확인**

```bash
cat docs/api-specs/reward-api.md
```

- [ ] **Step 2: 애드핏 API로 재작성**

AdMob SSV 콜백 명세를 삭제하고 아래 2개 엔드포인트로 교체한다. 기존 문서의 서식(헤더 구성, 표 스타일)을 그대로 따른다.

- `POST /api/v1/reward/adfit/ticket` — 인증 필요. 응답 `{ ticketId, expiresInSeconds }`
- `POST /api/v1/reward/adfit/claim` — 인증 필요. 요청 `{ ticketId }`, 응답 `{ rewardedAmount, totalCredit }`
- 에러 코드 표: `REWARD_404_2`, `REWARD_409`, `REWARD_410`, `REWARD_400_2`, `REWARD_429`
- 클라이언트 호출 순서: 티켓 발급 → 광고 노출 → 청구
- 제약 명시: 티켓 1회성, 만료 5분, 최소 경과시간 5초, 일일 10회

- [ ] **Step 3: 커밋**

```bash
git add -A
git commit -m "docs: 광고 보상 API 문서를 애드핏 기준으로 갱신"
```

- [ ] **Step 4: PR 생성**

```bash
git push -u origin feat/adfit-reward
gh pr create --base dev --title "feat: AdMob 제거 및 애드핏 광고 보상 연동" --body "$(cat <<'EOF'
## 요약
AdMob 계정 승인 거부로 광고를 받을 수 없어 애드핏으로 교체한다.

- AdMob 연동 및 Tink 의존성 완전 제거
- 애드핏 티켓 방식 리워드 크레딧 구현 (발급 → 광고 노출 → 청구)
- 지급 파이프라인(AdRewardHistory, CreditService)은 재사용, CreditType 무변경

## 설계
docs/superpowers/specs/2026-07-16-adfit-reward-design.md

## 알려진 한계
애드핏은 S2S 콜백을 제공하지 않아 서버가 광고 시청을 증명할 수 없다.
티켓 1회성 + 최소 경과시간 + 일일 한도로 남용을 억제하는 구조이며, 증명이 아니다.
크레딧은 현금화 경로가 없어 위조 시 피해는 배틀 참여 증가로 제한된다.

## 배포 전 확인
- 환경변수 ADMOB_APP_ID, ADMOB_REWARD_UNIT_ID_IOS, ADMOB_REWARD_UNIT_ID_ANDROID 제거
- app-ads.txt를 애드핏 항목으로 교체 (애드핏 승인 후)
- adfit.reward.min-watch-seconds는 실제 광고 길이 측정 후 조정 필요
EOF
)"
```

---

## 배포 전 운영 작업 (코드 외)

- [ ] 카카오 고객센터에 리워드 형태 사용 가능 여부 문의 — **구현 착수 전 권장** (설계 문서 11절)
- [ ] 애드핏 매체 등록 및 승인
- [ ] `static/app-ads.txt`를 애드핏 항목으로 교체
- [ ] 배포 환경에서 `ADMOB_*` 환경변수 제거
- [ ] iOS/Android 앱에 애드핏 SDK 연동 (별도 작업, 본 계획 범위 밖)
- [ ] 실제 광고 길이 측정 후 `adfit.reward.min-watch-seconds` 조정
