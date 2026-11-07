package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.dto.request.ClassCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassSummaryResponse;
import java.util.List;

public interface ClassroomService {

    ClassDetailResponse create(Long userId, ClassCreateRequest request);

    List<ClassSummaryResponse> getMyClasses(Long userId);

    ClassDetailResponse getDetail(Long userId, Long classId);

    ClassDetailResponse update(Long userId, Long classId, ClassUpdateRequest request);

    void delete(Long userId, Long classId);
}
