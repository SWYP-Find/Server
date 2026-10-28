package com.swyp.picke.domain.classroom.service.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 클래스 정량 통계 (PRD §14·§15). 서버가 계산하고 AI 는 숫자를 만들지 않는다.
 * <ul>
 *   <li>참여율 = 참여 완료 / 활성 멤버, 댓글 작성률 = 의견 작성자 / 활성 멤버</li>
 *   <li>입장 분포 = 사후투표 완료자 기준. 필수 댓글을 안 쓴 사람도 분포에는 포함된다</li>
 *   <li>분모 0 이면 0% (PRD S14 "분모 0은 참여 없음")</li>
 *   <li>중간투표는 중간투표가 있는 기존 콘텐츠(midVoteEnabled)만 선택 지점별로 집계한다. 아니면 빈 목록이고 화면·AI 입력에서 숨긴다</li>
 * </ul>
 * 마감 후에는 CLASS_SUMMARY 리포트 content.stats 에 스냅샷으로 저장되어 {@link #toMap()} / {@link #fromMap(Map)} 으로 오간다.
 */
public record ClassResultStats(
        int activeMemberCount,
        int completedCount,
        double participationRate,
        int commentWriterCount,
        double commentRate,
        int postVoteCount,
        boolean midVoteEnabled,
        int midVoteCount,
        List<OptionStat> options,
        List<CheckpointStat> midVoteCheckpoints
) {

    public record OptionStat(Long optionId, String title, String stance, int preVoteCount, int postVoteCount, double postVoteRatio) {
    }

    /** 선택 지점별 중간투표. voteCount = 이 지점에 답한 인원, 비율은 그 인원 기준 */
    public record CheckpointStat(Long checkpointId, int voteCount, List<ChoiceStat> options) {
    }

    public record ChoiceStat(Long optionId, String label, int voteCount, double ratio) {
    }

    public static double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0d;
        }
        return Math.round(numerator * 1000d / denominator) / 10d;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("active_member_count", activeMemberCount);
        map.put("completed_count", completedCount);
        map.put("participation_rate", participationRate);
        map.put("comment_writer_count", commentWriterCount);
        map.put("comment_rate", commentRate);
        map.put("post_vote_count", postVoteCount);
        map.put("mid_vote_enabled", midVoteEnabled);
        map.put("mid_vote_count", midVoteCount);
        List<Map<String, Object>> optionMaps = new ArrayList<>();
        for (OptionStat option : options) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("option_id", option.optionId());
            o.put("title", option.title());
            o.put("stance", option.stance());
            o.put("pre_vote_count", option.preVoteCount());
            o.put("post_vote_count", option.postVoteCount());
            o.put("post_vote_ratio", option.postVoteRatio());
            optionMaps.add(o);
        }
        map.put("options", optionMaps);
        List<Map<String, Object>> checkpointMaps = new ArrayList<>();
        for (CheckpointStat checkpoint : midVoteCheckpoints) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("checkpoint_id", checkpoint.checkpointId());
            c.put("vote_count", checkpoint.voteCount());
            List<Map<String, Object>> choiceMaps = new ArrayList<>();
            for (ChoiceStat choice : checkpoint.options()) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("option_id", choice.optionId());
                o.put("label", choice.label());
                o.put("vote_count", choice.voteCount());
                o.put("ratio", choice.ratio());
                choiceMaps.add(o);
            }
            c.put("options", choiceMaps);
            checkpointMaps.add(c);
        }
        map.put("mid_vote_checkpoints", checkpointMaps);
        return map;
    }

    public static ClassResultStats fromMap(Map<String, Object> map) {
        List<OptionStat> options = new ArrayList<>();
        for (Map<String, Object> o : ReportContent.mapList(map, "options")) {
            options.add(new OptionStat(
                    ReportContent.asLong(o.get("option_id")),
                    ReportContent.str(o, "title"),
                    ReportContent.str(o, "stance"),
                    ReportContent.asInt(o, "pre_vote_count"),
                    ReportContent.asInt(o, "post_vote_count"),
                    ReportContent.asDouble(o, "post_vote_ratio")
            ));
        }
        // 중간투표 도입 전 스냅샷은 키가 없어 빈 목록
        List<CheckpointStat> checkpoints = new ArrayList<>();
        for (Map<String, Object> c : ReportContent.mapList(map, "mid_vote_checkpoints")) {
            List<ChoiceStat> choices = new ArrayList<>();
            for (Map<String, Object> o : ReportContent.mapList(c, "options")) {
                choices.add(new ChoiceStat(
                        ReportContent.asLong(o.get("option_id")),
                        ReportContent.str(o, "label"),
                        ReportContent.asInt(o, "vote_count"),
                        ReportContent.asDouble(o, "ratio")
                ));
            }
            checkpoints.add(new CheckpointStat(ReportContent.asLong(c.get("checkpoint_id")), ReportContent.asInt(c, "vote_count"), choices));
        }
        return new ClassResultStats(
                ReportContent.asInt(map, "active_member_count"),
                ReportContent.asInt(map, "completed_count"),
                ReportContent.asDouble(map, "participation_rate"),
                ReportContent.asInt(map, "comment_writer_count"),
                ReportContent.asDouble(map, "comment_rate"),
                ReportContent.asInt(map, "post_vote_count"),
                Boolean.TRUE.equals(map.get("mid_vote_enabled")), // 중간투표 도입 전 스냅샷은 키가 없어 false
                ReportContent.asInt(map, "mid_vote_count"),
                options,
                checkpoints
        );
    }
}
