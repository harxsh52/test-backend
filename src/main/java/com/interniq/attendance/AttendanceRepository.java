package com.interniq.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByIntern_IdAndDate(Long internId, LocalDate date);

    List<Attendance> findByIntern_IdOrderByDateDesc(Long internId);

    List<Attendance> findByIntern_IdAndIntern_Manager_IdOrderByDateDesc(Long internId, Long managerId);

    List<Attendance> findByIntern_Manager_IdOrderByDateDesc(Long managerId);

    List<Attendance> findAllByOrderByDateDesc();
}
