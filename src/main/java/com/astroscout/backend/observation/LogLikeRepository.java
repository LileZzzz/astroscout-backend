package com.astroscout.backend.observation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogLikeRepository extends JpaRepository<LogLike, Long> {

    Optional<LogLike> findByLog_IdAndUser_Id(Long logId, Long userId);

    long countByLog_Id(Long logId);

    void deleteByLog_IdIn(List<Long> logIds);
}

