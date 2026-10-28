package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.dto.request.ClassMidVoteRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassMidVoteInfo;
import com.swyp.picke.domain.classroom.entity.ClassMidVote;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.repository.ClassMidVoteRepository;
import com.swyp.picke.domain.scenario.entity.InteractiveOption;
import com.swyp.picke.domain.scenario.entity.ScenarioNode;
import com.swyp.picke.domain.scenario.repository.ScenarioRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 중간투표 (PRD §7 "기존 콘텐츠에 해당 단계가 있으면 수집").
 * <ul>
 *   <li>대상: 기존 콘텐츠(TTS) 중 battles.mid_vote_enabled 이고 시나리오에 선택 지점이 있는 배틀. 없는 콘텐츠에 임의로 만들지 않는다</li>
 *   <li>선택 지점(checkpoint) = 선택지가 있는 시나리오 노드. 지점마다 1건 저장하고 사후투표 전까지 다시 고를 수 있다</li>
 *   <li>필수: 시작 노드부터 내가 고른 선택지를 따라가며 만나는 선택 지점은 모두 답해야 사후투표할 수 있다 (PRD §7 "필수 중간투표")</li>
 *   <li>처음 기록하면 콘텐츠를 시작한 것으로 보고 사전투표를 잠근다 (PRD §7 "사전투표는 배틀 시작 후 잠근다")</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassMidVoteService {

    static final int LABEL_MAX = 255;

    private final ScenarioRepository scenarioRepository;
    private final ClassMidVoteRepository classMidVoteRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final UserBattleService userBattleService;

    public record Checkpoint(Long checkpointId, List<Choice> options) {
    }

    public record Choice(Long optionId, String label, Long nextNodeId) {
    }

    /** 이 클래스에서 중간투표를 받는지 */
    public boolean enabled(Classroom classroom) {
        return midVoteBattle(classroom) && !checkpoints(classroom.getBattle()).isEmpty();
    }

    /** 콘텐츠의 선택 지점 목록 (노드 ID 순) */
    public List<Checkpoint> checkpoints(Battle battle) {
        List<Checkpoint> checkpoints = new ArrayList<>();
        for (ScenarioNode node : nodesOf(battle)) {
            if (node.getOptions().isEmpty()) {
                continue;
            }
            List<Choice> choices = new ArrayList<>();
            for (InteractiveOption option : node.getOptions()) {
                choices.add(new Choice(option.getId(), labelOf(option), option.getNextNodeId()));
            }
            checkpoints.add(new Checkpoint(node.getId(), choices));
        }
        checkpoints.sort(Comparator.comparing(Checkpoint::checkpointId));
        return checkpoints;
    }

    @Transactional
    public VoteResultResponse record(Classroom classroom, User user, ClassMidVoteRequest request) {
        if (!midVoteBattle(classroom)) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_NOT_SUPPORTED);
        }
        Battle battle = classroom.getBattle();
        List<Checkpoint> checkpoints = checkpoints(battle);
        if (checkpoints.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_NOT_SUPPORTED);
        }
        Choice choice = findChoice(checkpoints, request);

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        // 연속 탭으로 같은 지점이 두 번 들어와도 1건만 남도록 투표 행을 잠근다
        classMidVoteRepository.lockVote(vote.getId());
        UserBattleStep step = userBattleService.getUserBattleStatus(user, battle, classroom).step();
        if (step == UserBattleStep.COMPLETED || vote.getPostVoteOption() != null) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_LOCKED);
        }

        Optional<ClassMidVote> saved = classMidVoteRepository.findByVoteAndCheckpointId(vote, request.checkpointId());
        if (saved.isPresent()) {
            saved.get().change(choice.optionId(), choice.label());
        } else {
            classMidVoteRepository.save(ClassMidVote.builder()
                    .vote(vote)
                    .checkpointId(request.checkpointId())
                    .optionId(choice.optionId())
                    .optionLabel(choice.label())
                    .build());
        }
        if (step == UserBattleStep.NONE || step == UserBattleStep.PRE_VOTE) {
            userBattleService.upsertStep(user, battle, classroom, UserBattleStep.TTS_LISTENING);
            step = UserBattleStep.TTS_LISTENING;
        }
        return new VoteResultResponse(vote.getId(), step);
    }

    /** 사후투표 전 검사. 고른 경로에 답하지 않은 선택 지점이 있으면 409 */
    public void requireCompleted(Classroom classroom, BattleVote vote) {
        if (vote == null || !midVoteBattle(classroom)) {
            return;
        }
        Map<Long, Long> chosen = new HashMap<>();
        for (ClassMidVote midVote : classMidVoteRepository.findByVoteOrderByIdAsc(vote)) {
            chosen.put(midVote.getCheckpointId(), midVote.getOptionId());
        }
        if (firstMissingCheckpoint(nodesOf(classroom.getBattle()), chosen).isPresent()) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_REQUIRED);
        }
    }

    /** 내 중간투표 (선택 지점 순). 중간투표 대상이 아니면 빈 목록 */
    public List<ClassMidVoteInfo> choicesOf(Classroom classroom, BattleVote vote) {
        if (vote == null || !midVoteBattle(classroom)) {
            return List.of();
        }
        List<ClassMidVoteInfo> infos = new ArrayList<>();
        for (ClassMidVote midVote : classMidVoteRepository.findByVoteOrderByIdAsc(vote)) {
            infos.add(new ClassMidVoteInfo(midVote.getCheckpointId(), midVote.getOptionId(), midVote.getOptionLabel()));
        }
        infos.sort(Comparator.comparing(ClassMidVoteInfo::checkpointId));
        return infos;
    }

    /**
     * 시작 노드부터 고른 선택지(선택 지점) 또는 자동 다음 노드를 따라가며 답이 없는 첫 선택 지점을 찾는다.
     * 고른 선택지가 시나리오 수정으로 사라졌으면 그 지점도 답이 없는 것으로 본다.
     */
    static Optional<Long> firstMissingCheckpoint(List<ScenarioNode> nodes, Map<Long, Long> chosen) {
        Map<Long, ScenarioNode> byId = new HashMap<>();
        ScenarioNode current = null;
        for (ScenarioNode node : nodes) {
            byId.put(node.getId(), node);
            if (current == null && Boolean.TRUE.equals(node.getIsStartNode())) {
                current = node;
            }
        }
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current.getId())) {
            if (current.getOptions().isEmpty()) {
                current = current.getAutoNextNodeId() == null ? null : byId.get(current.getAutoNextNodeId());
                continue;
            }
            Long optionId = chosen.get(current.getId());
            InteractiveOption option = optionId == null ? null : current.getOptions().stream()
                    .filter(o -> optionId.equals(o.getId()))
                    .findFirst()
                    .orElse(null);
            if (option == null) {
                return Optional.of(current.getId());
            }
            current = option.getNextNodeId() == null ? null : byId.get(option.getNextNodeId());
        }
        return Optional.empty();
    }

    private static Choice findChoice(List<Checkpoint> checkpoints, ClassMidVoteRequest request) {
        if (request == null || request.checkpointId() == null || request.optionId() == null) {
            throw new CustomException(ErrorCode.CLASS_MID_VOTE_OPTION_NOT_FOUND);
        }
        return checkpoints.stream()
                .filter(checkpoint -> checkpoint.checkpointId().equals(request.checkpointId()))
                .flatMap(checkpoint -> checkpoint.options().stream())
                .filter(choice -> choice.optionId().equals(request.optionId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_MID_VOTE_OPTION_NOT_FOUND));
    }

    private List<ScenarioNode> nodesOf(Battle battle) {
        if (battle == null) {
            return List.of();
        }
        return scenarioRepository.findByBattleId(battle.getId())
                .<List<ScenarioNode>>map(scenario -> scenario.getNodes())
                .orElse(List.of());
    }

    private static boolean midVoteBattle(Classroom classroom) {
        return classroom.getBattleType() == ClassBattleType.TTS
                && classroom.getBattle() != null
                && classroom.getBattle().isMidVoteEnabled();
    }

    private static String labelOf(InteractiveOption option) {
        String label = option.getLabel() != null && !option.getLabel().isBlank() ? option.getLabel() : option.getTitle();
        if (label == null) {
            return "";
        }
        return label.length() > LABEL_MAX ? label.substring(0, LABEL_MAX) : label;
    }
}
