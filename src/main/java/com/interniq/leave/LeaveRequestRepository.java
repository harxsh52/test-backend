package com.interniq.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByIntern_IdOrderByCreatedAtDesc(Long internId);

    List<LeaveRequest> findByManager_IdOrderByCreatedAtDesc(Long managerId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.intern.id = :internId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlappingLeave(
            @Param("internId") Long internId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses
    );

    long countByIntern_IdAndStatus(Long internId, LeaveStatus status);

    List<LeaveRequest> findByIntern_IdAndStatus(Long internId, LeaveStatus status);
}
