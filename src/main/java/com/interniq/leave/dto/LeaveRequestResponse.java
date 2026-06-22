package com.interniq.leave.dto;

import com.interniq.leave.LeaveStatus;
import com.interniq.leave.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {

    private Long id;
    private Long internId;
    private Long internUserId;
    private String internName;
    private String internEmail;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private LeaveType leaveType;
    private LeaveStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private String managerComment;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
