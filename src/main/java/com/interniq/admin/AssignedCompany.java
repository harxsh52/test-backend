package com.interniq.admin;

import com.interniq.department.Department;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "assigned_companies",
        uniqueConstraints = @UniqueConstraint(name = "uk_assigned_companies_department_name", columnNames = {"department_id", "name"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
