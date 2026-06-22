package com.interniq.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignedCompanyRepository extends JpaRepository<AssignedCompany, Long> {

    boolean existsByDepartment_IdAndNameIgnoreCase(Long departmentId, String name);

    Optional<AssignedCompany> findByDepartment_IdAndNameIgnoreCase(Long departmentId, String name);

    List<AssignedCompany> findAllByOrderByDepartment_NameAscNameAsc();
}
