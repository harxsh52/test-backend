package com.interniq.task.dto;

import com.interniq.task.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusRequest {

    @NotNull(message = "Task status is required")
    private TaskStatus status;
}
