package com.interniq.intern;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface InternProfileRepository extends JpaRepository<InternProfile, Long>, JpaSpecificationExecutor<InternProfile> {

    boolean existsByUserId(Long userId);

    Optional<InternProfile> findByUserId(Long userId);

    Optional<InternProfile> findByEmpId(String empId);

    List<InternProfile> findByManager_Id(Long managerId);
}
