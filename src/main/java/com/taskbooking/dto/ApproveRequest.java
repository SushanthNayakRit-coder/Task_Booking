package com.taskbooking.dto;

import com.taskbooking.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class ApproveRequest {
    @NotNull(message = "Status (APPROVED or REJECTED) is required")
    private TaskStatus status;

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
}
