package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicDraftResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicValidationResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicVersionResponse;
import com.swyp.picke.domain.classroom.entity.ClassTopicDraft;
import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import com.swyp.picke.domain.classroom.service.topic.TopicContent;
import com.swyp.picke.domain.classroom.service.topic.TopicIssue;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.List;
import java.util.Map;

public final class ClassTopicConverter {

    private ClassTopicConverter() {
    }

    public static ClassTopicDraftResponse toDraft(ClassTopicDraft draft, ClassTopicVersion latestVersion) {
        List<Map<String, Object>> candidates = draft.getCandidates() == null ? List.of() : draft.getCandidates();
        return new ClassTopicDraftResponse(
                draft.getId(),
                draft.getTopicSeed(),
                draft.getDifficulty(),
                draft.getCategories().stream().map(BattleCategory::valueOf).toList(),
                draft.getSuggestStatus(),
                draft.getClarificationMessage(),
                draft.getBatchNo(),
                draft.remainingSuggestions(),
                draft.remainingValidations(),
                candidates.stream().map(ClassTopicConverter::toCandidate).toList(),
                latestVersion == null ? null : toVersion(latestVersion));
    }

    public static ClassTopicVersionResponse toVersion(ClassTopicVersion version) {
        return new ClassTopicVersionResponse(
                version.getId(),
                version.getDraft().getId(),
                version.getVersionNo(),
                version.getSource(),
                version.getQuestion(),
                version.getDescription(),
                version.getCategory(),
                version.getDifficulty(),
                List.of(
                        option("A", version.getOptionALabel(), version.getOptionARationale(), version.getOptionAPhilosopher()),
                        option("B", version.getOptionBLabel(), version.getOptionBRationale(), version.getOptionBPhilosopher())),
                version.getCreatedAt());
    }

    public static ClassTopicValidationResponse toValidation(List<TopicIssue> issues, ClassTopicVersion version,
                                                            int remainingValidations) {
        return new ClassTopicValidationResponse(
                version != null,
                issues.stream().map(i -> new ClassTopicValidationResponse.Issue(i.field(), i.code(), i.message())).toList(),
                version == null ? null : version.getId(),
                remainingValidations,
                version == null ? null : toVersion(version));
    }

    private static ClassTopicDraftResponse.Candidate toCandidate(Map<String, Object> map) {
        TopicContent content = TopicContent.fromMap(map);
        return new ClassTopicDraftResponse.Candidate(
                TopicContent.candidateIdOf(map),
                content.question(),
                content.description(),
                content.category(),
                content.difficulty(),
                List.of(
                        option("A", content.optionA().label(), content.optionA().rationale(), content.optionA().philosopher()),
                        option("B", content.optionB().label(), content.optionB().rationale(), content.optionB().philosopher())));
    }

    private static ClassTopicVersionResponse.Option option(String position, String label, String rationale,
                                                          PhilosopherType philosopher) {
        return new ClassTopicVersionResponse.Option(
                position, label, rationale, philosopher, philosopher.getLabel(), philosopher.getImageKey());
    }
}
