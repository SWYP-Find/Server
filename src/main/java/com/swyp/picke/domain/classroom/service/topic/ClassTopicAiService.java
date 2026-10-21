package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.classroom.dto.request.ClassTopicSuggestRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicValidationRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicDraftResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicValidationResponse;
import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;
import com.swyp.picke.domain.classroom.service.report.ReportValidationException;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * 주제 제안·수정 검증 오케스트레이터. 트랜잭션을 열지 않고 (1) 준비 → (2) AI 호출 → (3) 반영 을 잇는다.
 * <ul>
 *   <li>업무 작업 1건당 제공자 호출은 최초 포함 최대 3회 (PRD §17). 연결 오류·429·5xx 만 지연 후 재시도하고,
 *       크레딧 소진 429(insufficient_quota)는 바로 실패시킨다</li>
 *   <li>규칙을 어긴 응답은 문제 목록을 넘겨 1회 보정하고, 그래도 어기면 게시하지 않는다 (PRD §10 "1회 보정")</li>
 *   <li>요청 스레드에서 끝까지 처리한다. 중간에 연결이 끊겨도 서버는 결과를 저장하므로 초안 조회로 이어 볼 수 있다</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassTopicAiService {

    static final int MAX_PROVIDER_CALLS = 3;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassTopicDraftService draftService;
    private final ClassReportAiClient aiClient;
    private final ClassTopicContentValidator validator;

    /** 재시도 지연의 기준값. 1회차 뒤 base, 2회차 뒤 base×2 에 jitter 를 더한다 */
    long retryBackoffMillis = 500;

    public ClassTopicDraftResponse suggest(Long userId, Long draftId, ClassTopicSuggestRequest request) {
        ClassTopicDraftService.PreparedSuggest prepared =
                draftService.prepareSuggest(userId, draftId, request, aiClient.getModel(), now());
        String userPrompt = ClassTopicPrompts.suggestUser(prepared.conditions());
        Attempts attempts = new Attempts(userPrompt);
        AiUsage usage = new AiUsage();

        while (usage.calls() < MAX_PROVIDER_CALLS) {
            ClassReportAiClient.AiChatResult result = attempts.call(ClassTopicPrompts.SUGGEST_SYSTEM,
                    ClassTopicPrompts.SUGGEST_MAX_OUTPUT_TOKENS, usage);
            if (result == null) {
                if (attempts.stop) {
                    break;
                }
                continue;
            }
            try {
                ClassTopicContentValidator.SuggestResult parsed = validator.parseSuggestion(result.json(), prepared.conditions());
                if (parsed.needsClarification()) {
                    draftService.clarifySuggest(prepared, parsed.clarificationMessage(), usage, now());
                } else {
                    draftService.completeSuggest(prepared, parsed.candidates(), usage, now());
                }
                return draftService.get(userId, draftId);
            } catch (TopicValidationException e) {
                log.info("[ClassTopic] suggestion rejected draftId={} problems={}", draftId, e.getProblems());
                if (!attempts.correct(e.getCode(), result.rawContent(), e.getProblems())) {
                    break;
                }
            }
        }
        draftService.failSuggest(prepared, attempts.lastError, attempts.rejected, usage, now());
        return draftService.get(userId, draftId);
    }

    public ClassTopicValidationResponse validate(Long userId, Long draftId, ClassTopicValidationRequest request) {
        ClassTopicDraftService.PreparedValidation prepared =
                draftService.prepareValidation(userId, draftId, request, aiClient.getModel(), now());
        if (prepared.immediate() != null) {
            return prepared.immediate();
        }
        Attempts attempts = new Attempts(ClassTopicPrompts.validateUser(prepared.edit()));
        AiUsage usage = new AiUsage();

        while (usage.calls() < MAX_PROVIDER_CALLS) {
            ClassReportAiClient.AiChatResult result = attempts.call(ClassTopicPrompts.VALIDATE_SYSTEM,
                    ClassTopicPrompts.VALIDATE_MAX_OUTPUT_TOKENS, usage);
            if (result == null) {
                if (attempts.stop) {
                    break;
                }
                continue;
            }
            try {
                List<TopicIssue> issues = validator.parseVerdict(result.json());
                return draftService.completeValidation(prepared, issues, usage, now());
            } catch (TopicValidationException e) {
                if (!attempts.correct(e.getCode(), result.rawContent(), e.getProblems())) {
                    break;
                }
            }
        }
        draftService.failValidation(prepared, attempts.lastError, attempts.rejected, usage, now());
        throw new CustomException(ErrorCode.CLASS_TOPIC_AI_UNAVAILABLE);
    }

    /** 한 업무 작업 안의 호출 상태: 다음 프롬프트, 보정 여부, 마지막 오류 */
    private final class Attempts {

        private final String originalPrompt;
        private String prompt;
        private boolean corrected;
        private boolean stop;
        private boolean rejected;
        private String lastError;

        private Attempts(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            this.prompt = originalPrompt;
        }

        /** 성공하면 응답, 실패하면 null. 더 시도할 수 없으면 stop 을 켠다 */
        private ClassReportAiClient.AiChatResult call(String system, int maxTokens, AiUsage usage) {
            try {
                ClassReportAiClient.AiChatResult result = aiClient.chatJson(system, prompt, maxTokens);
                usage.add(result);
                return result;
            } catch (ReportValidationException e) {
                // 잘린 출력·JSON 아님: 형식 위반으로 보고 보정 1회
                usage.addCall();
                if (!correct(e.getCode(), "(완전한 JSON 이 아니었음)", List.of("완전한 JSON 객체 하나만 출력하세요"))) {
                    stop = true;
                }
                return null;
            } catch (Exception e) {
                usage.addCall();
                lastError = errorCodeOf(e);
                rejected = false;
                log.warn("[ClassTopic] provider call failed : {}", e.toString());
                if (!retryable(e) || usage.calls() >= MAX_PROVIDER_CALLS) {
                    stop = true;
                } else {
                    backoff(usage.calls());
                }
                return null;
            }
        }

        /** 보정 호출을 준비한다. 이미 한 번 보정했으면 false */
        private boolean correct(String code, String previousResponse, List<String> problems) {
            lastError = code;
            rejected = true;
            if (corrected) {
                return false;
            }
            corrected = true;
            prompt = ClassTopicPrompts.correctionUser(originalPrompt, previousResponse, problems);
            return true;
        }
    }

    /** 크레딧 소진(insufficient_quota)도 429 로 오지만 다시 호출해도 소용없으므로 재시도하지 않는다 */
    private static boolean retryable(Exception e) {
        if (e instanceof HttpClientErrorException.TooManyRequests tooMany) {
            return !isQuotaExhausted(tooMany);
        }
        return e instanceof ResourceAccessException || e instanceof HttpServerErrorException;
    }

    private static boolean isQuotaExhausted(HttpClientErrorException e) {
        return e.getResponseBodyAsString().contains("insufficient_quota");
    }

    private void backoff(int calls) {
        if (retryBackoffMillis <= 0) {
            return;
        }
        long delay = retryBackoffMillis * (1L << (calls - 1)) + ThreadLocalRandom.current().nextLong(retryBackoffMillis / 2 + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String errorCodeOf(Exception e) {
        if (e instanceof HttpClientErrorException.TooManyRequests tooMany && isQuotaExhausted(tooMany)) {
            return "INSUFFICIENT_QUOTA";
        }
        String name = e.getClass().getSimpleName();
        if (name.contains("Timeout") || (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("Timeout"))) {
            return "TIMEOUT";
        }
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }
}
