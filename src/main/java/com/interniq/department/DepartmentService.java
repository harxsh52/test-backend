package com.interniq.department;

import com.interniq.department.dto.DepartmentRequest;
import com.interniq.department.dto.DepartmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = clean(request.getName());

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Department already exists");
        }

        Department department = Department.builder()
                .name(name)
                .description(clean(request.getDescription()))
                .active(request.getActive() == null || request.getActive())
                .build();

        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = getDepartmentOrThrow(id);
        String newName = clean(request.getName());

        departmentRepository.findByNameIgnoreCase(newName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Department already exists");
                });

        department.setName(newName);
        department.setDescription(clean(request.getDescription()));

        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse deleteDepartment(Long id) {
        Department department = getDepartmentOrThrow(id);
        department.setActive(false);
        return toResponse(department);
    }

    private Department getDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .active(department.isActive())
                .build();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
