package com.interniq.report.dto;

import com.interniq.task.Priority;
import com.interniq.task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReportResponse {

    private Long totalTasks;
    private Long completedTasks;
    private Long pendingTasks;
    private Long submittedTasks;
    private BigDecimal completionPercentage;
    private BigDecimal averageRating;
    private List<Row> records;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Long id;
        private String title;
        private Long internId;
        private String internName;
        private Long managerId;
        private String managerName;
        private Priority priority;
        private TaskStatus status;
        private LocalDate dueDate;
        private Integer rating;
        private String managerFeedback;
        private LocalDateTime createdAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
    }
}
