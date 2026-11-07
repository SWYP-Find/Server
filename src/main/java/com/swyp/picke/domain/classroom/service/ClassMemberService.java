package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.dto.request.ClassJoinRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassMemberNameUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassJoinPreviewResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import java.util.List;

public interface ClassMemberService {

    ClassDetailResponse join(Long userId, ClassJoinRequest request);

    ClassJoinPreviewResponse previewJoin(Long userId, String inviteCode);

    List<ClassMemberResponse> getMembers(Long userId, Long classId);

    ClassMemberResponse changeMyDisplayName(Long userId, Long classId, ClassMemberNameUpdateRequest request);

    void leave(Long userId, Long classId);

    void removeMember(Long userId, Long classId, Long memberId);
}
