package com.interniq.leave.dto;

import com.interniq.leave.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeaveStatusRequest {

    @NotNull(message = "Status is required")
    private LeaveStatus status;

    @Size(max = 500, message = "Manager comment cannot exceed 500 characters")
    private String managerComment;
}
