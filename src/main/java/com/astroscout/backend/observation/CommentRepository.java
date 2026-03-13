package com.astroscout.backend.observation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByLog_IdOrderByCreatedAtAsc(Long logId);

    long countByLog_Id(Long logId);

    void deleteByLog_IdIn(List<Long> logIds);
}

