package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTopicVersionRepository extends JpaRepository<ClassTopicVersion, Long> {

    Optional<ClassTopicVersion> findTopByDraftIdOrderByVersionNoDesc(Long draftId);

    Optional<ClassTopicVersion> findByIdAndDraftId(Long id, Long draftId);

    /** 같은 후보를 다시 선택하면 새 버전을 만들지 않고 기존 버전을 돌려준다 */
    Optional<ClassTopicVersion> findFirstByDraftIdAndSourceRefOrderByVersionNoDesc(Long draftId, String sourceRef);
}
