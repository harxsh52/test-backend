package com.interniq.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubDepartmentRepository extends JpaRepository<SubDepartment, Long> {

    boolean existsByDepartment_IdAndNameIgnoreCase(Long departmentId, String name);

    Optional<SubDepartment> findByDepartment_IdAndNameIgnoreCase(Long departmentId, String name);

    List<SubDepartment> findAllByOrderByDepartment_NameAscNameAsc();
}
