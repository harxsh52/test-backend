package com.interniq.task.dto;

import com.interniq.task.Priority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 180, message = "Task title must be at most 180 characters")
    private String title;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Assigned intern is required")
    private Long assignedToInternId;

    private Priority priority;

    @FutureOrPresent(message = "Due date cannot be in the past")
    private LocalDate dueDate;
}
