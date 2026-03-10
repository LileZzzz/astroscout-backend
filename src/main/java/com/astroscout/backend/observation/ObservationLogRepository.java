package com.astroscout.backend.observation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationLogRepository extends JpaRepository<ObservationLog, Long> {

    List<ObservationLog> findByUser_Id(Long userId);

    List<ObservationLog> findByIsPublicTrueOrderByCreatedAtDesc();

    boolean existsById(Long id);
}


