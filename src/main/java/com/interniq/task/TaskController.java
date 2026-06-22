package com.interniq.task;

import com.interniq.common.ApiResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.task.dto.TaskRequest;
import com.interniq.task.dto.TaskResponse;
import com.interniq.task.dto.TaskReviewRequest;
import com.interniq.task.dto.TaskStatusRequest;
import com.interniq.task.dto.TaskSubmitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            Authentication authentication
    ) {
        TaskResponse response = taskService.createTask(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task assigned successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Tasks loaded successfully",
                    taskService.searchMyTasks(status, priority, internId, managerId, fromDate, toDate, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Tasks loaded successfully",
                taskService.getMyTasks(status, priority, internId, managerId, fromDate, toDate, authentication)
        ));
    }

    @GetMapping("/assigned-by-me")
    public ResponseEntity<ApiResponse<?>> getTasksAssignedByMe(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        if (PageRequestFactory.isPaged(page, size)) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Assigned tasks loaded successfully",
                    taskService.searchTasksAssignedByMe(status, priority, internId, fromDate, toDate, page, size, sortBy, sortDirection, authentication)
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Assigned tasks loaded successfully",
                taskService.getTasksAssignedByMe(status, priority, internId, fromDate, toDate, authentication)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task loaded successfully", taskService.getTask(id, authentication)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", taskService.updateTaskStatus(id, request, authentication)));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<TaskResponse>> submitTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskSubmitRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task submitted successfully", taskService.submitTask(id, request, authentication)));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TaskResponse>> reviewTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success("Task reviewed successfully", taskService.reviewTask(id, request, authentication)));
    }
}
