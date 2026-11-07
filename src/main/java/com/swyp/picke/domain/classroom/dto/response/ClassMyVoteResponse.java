package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.vote.dto.response.MyVoteResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "클래스 안에서의 내 투표")
public record ClassMyVoteResponse(
        String battleTitle,
        @Schema(description = "사전투표 선택지") MyVoteResponse.OptionInfo preVote,
        @Schema(description = "선택 지점별 중간투표. 중간투표가 없는 배틀이거나 아직 고르지 않았으면 빈 배열") List<ClassMidVoteInfo> midVotes,
        @Schema(description = "사후투표 선택지") MyVoteResponse.OptionInfo postVote,
        UserBattleStep status,
        boolean opinionChanged
) {

    public static ClassMyVoteResponse of(MyVoteResponse vote, List<ClassMidVoteInfo> midVotes) {
        return new ClassMyVoteResponse(vote.battleTitle(), vote.preVote(), midVotes, vote.postVote(), vote.status(), vote.opinionChanged());
    }
}
