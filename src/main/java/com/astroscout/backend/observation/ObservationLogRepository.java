package com.astroscout.backend.observation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObservationLogRepository extends JpaRepository<ObservationLog, Long> {

    List<ObservationLog> findByUser_Id(Long userId);

    List<ObservationLog> findByIsPublicTrueOrderByCreatedAtDesc();

    Page<ObservationLog> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

        @Query("""
                        select l from ObservationLog l
                        where l.isPublic = true
                            and l.coverImageUrl is not null
                            and l.coverImageUrl <> ''
                        order by l.createdAt desc
                        """)
        Page<ObservationLog> findPublicFeedWithImage(Pageable pageable);

        @Query("""
                        select l from ObservationLog l
                        where l.user.id = :userId
                            and (l.coverImageUrl is null or l.coverImageUrl = '')
                        """)
        List<ObservationLog> findNoImageLogsByUserId(@Param("userId") Long userId);

        @Query("""
                select l from ObservationLog l
                where l.coverImageUrl is null or l.coverImageUrl = ''
                """)
            List<ObservationLog> findAllNoImageLogs();

    boolean existsById(Long id);
}


