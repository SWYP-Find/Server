package com.swyp.picke.domain.vote.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.converter.VoteConverter;
import com.swyp.picke.domain.vote.dto.request.VoteRequest;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import com.swyp.picke.domain.vote.dto.response.VoteResultResponse;
import com.swyp.picke.domain.vote.dto.response.VoteStatsResponse;
import com.swyp.picke.domain.vote.entity.Vote;
import com.swyp.picke.domain.vote.repository.VoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final BattleService battleService;
    private final BattleOptionRepository battleOptionRepository;
    private final UserRepository userRepository;
    private final UserBattleService userBattleService;

    @Override
    public BattleOption findPreVoteOption(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (vote.getPreVoteOption() == null) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }
        return vote.getPreVoteOption();
    }

    @Override
    public Long findPostVoteOptionId(Long battleId, Long userId) {
        return voteRepository.findByBattleIdAndUserId(battleId, userId)
                .map(vote -> vote.getPostVoteOption() != null ? vote.getPostVoteOption().getId() : null)
                .orElse(null);
    }

    @Override
    public VoteStatsResponse getVoteStats(Long battleId) {
        Battle battle = battleService.findById(battleId);
        List<BattleOption> options = battleOptionRepository.findByBattle(battle);
        long totalCount = voteRepository.countByBattle(battle);

        List<VoteStatsResponse.OptionStat> stats = options.stream()
                .map(option -> {
                    long count = voteRepository.countByBattleAndPreVoteOption(battle, option);
                    double ratio = totalCount > 0
                            ? Math.round((double) count / totalCount * 1000.0) / 10.0
                            : 0.0;
                    return new VoteStatsResponse.OptionStat(
                            option.getId(), option.getLabel().name(), option.getTitle(), count, ratio);
                })
                .toList();

        LocalDateTime updatedAt = voteRepository.findTopByBattleOrderByUpdatedAtDesc(battle)
                .map(Vote::getUpdatedAt)
                .orElse(null);

        return VoteConverter.toVoteStatsResponse(stats, totalCount, updatedAt);
    }

    @Override
    public MyVoteResponse getMyVote(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);
        return VoteConverter.toMyVoteResponse(vote, status.step());
    }

    @Override
    @Transactional
    public VoteResultResponse preVote(Long battleId, Long userId, VoteRequest request) {
        // 1. 기본 정보 조회 (배틀, 유저, 선택한 옵션)
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        // 2. 기존 투표 여부 확인 (에러 대신 Optional로 받음)
        Optional<Vote> existingVote = voteRepository.findByBattleAndUser(battle, user);
        Vote vote;

        if (existingVote.isPresent()) {
            // 이미 투표가 있다면 기존 객체의 사전 투표 옵션을 변경
            vote = existingVote.get();
            vote.updatePreVote(option);
        } else {
            // 투표가 없다면 새로 생성하여 저장
            vote = Vote.createPreVote(user, battle, option);
            voteRepository.save(vote);
        }

        // 3. 현재 유저의 진행 단계 확인
        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);

        // 4. 단계 업데이트 (처음 참여하는 경우에만 단계를 PRE_VOTE로 변경)
        // 이미 POST_VOTE나 COMPLETED라면 단계를 강제로 낮추지 않음
        if (status.step() == UserBattleStep.NONE) {
            userBattleService.upsertStep(user, battle, UserBattleStep.PRE_VOTE);
        }

        // 5. 현재 유지 중인 단계를 반환 (수정 후에도 COMPLETED 유지 가능)
        UserBattleStep currentStep = (status.step() == UserBattleStep.NONE) ? UserBattleStep.PRE_VOTE : status.step();
        return new VoteResultResponse(vote.getId(), currentStep);
    }

    @Override
    @Transactional
    public VoteResultResponse postVote(Long battleId, Long userId, VoteRequest request) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption option = battleOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_OPTION_NOT_FOUND));

        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        // [검증] 사전 투표를 완료한 상태(혹은 오디오 청취 완료 상태)인지 확인
        UserBattleStatusResponse status = userBattleService.getUserBattleStatus(user, battle);
        if (status.step() == UserBattleStep.NONE) {
            throw new CustomException(ErrorCode.PRE_VOTE_REQUIRED);
        }

        // 1. 사후 투표 업데이트
        vote.doPostVote(option);

        // 2. 최종 완료 단계(COMPLETED)로 업데이트
        userBattleService.upsertStep(user, battle, UserBattleStep.COMPLETED);

        return new VoteResultResponse(vote.getId(), UserBattleStep.COMPLETED);
    }

    @Override
    @Transactional
    public void deleteVote(Long voteId) {
        // 1. 투표 기록 조회
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        Battle battle = vote.getBattle();
        User user = vote.getUser();

        // 2. 배틀 통계치 감소 (Battle 엔티티에 해당 메서드가 있다고 가정)
        // battle.decreaseParticipant(); // 예시: totalParticipantsCount--

        // 3. 각 옵션별 투표수 감소
        if (vote.getPreVoteOption() != null) {
            // vote.getPreVoteOption().decreaseVoteCount();
        }
        if (vote.getPostVoteOption() != null) {
            // vote.getPostVoteOption().decreaseVoteCount();
        }

        // 4. 유저의 배틀 진행 단계 초기화
        userBattleService.upsertStep(user, battle, UserBattleStep.NONE);

        // 5. 투표 레코드 물리 삭제
        voteRepository.delete(vote);
    }

    @Override
    @Transactional
    public void completeTts(Long battleId, Long userId) {
        Battle battle = battleService.findById(battleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 엔티티 상태 변경 (isTtsListened = true)
        Vote vote = voteRepository.findByBattleAndUser(battle, user)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));
        vote.completeTts();

        // 2. 단계를 POST_VOTE(사후 투표 가능 단계)로 업데이트
        userBattleService.upsertStep(user, battle, UserBattleStep.POST_VOTE);
    }
}