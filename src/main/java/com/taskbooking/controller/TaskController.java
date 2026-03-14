package com.taskbooking.controller;

import com.taskbooking.dto.ApproveRequest;
import com.taskbooking.dto.TaskRequest;
import com.taskbooking.dto.TaskResponse;
import com.taskbooking.entity.TaskStatus;
import com.taskbooking.entity.User;
import com.taskbooking.repository.UserRepository;
import com.taskbooking.service.TaskService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return taskService.createTask(request);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> listTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false, defaultValue = "date") String sortBy) {
        String sort = (sortBy != null) ? sortBy.trim() : "date";
        List<TaskResponse> list = taskService.listTasks(status, sort);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(list);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PutMapping("/{id}/approve")
    public TaskResponse approveOrReject(@PathVariable Long id,
                                       @Valid @RequestBody ApproveRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow();
        return taskService.approveOrReject(id, request.getStatus(), user.getId());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void exportCsv(
            @RequestParam(required = false) TaskStatus status,
            HttpServletResponse response,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        List<TaskResponse> tasks = taskService.listTasks(status, "date");
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.csv");
        try (PrintWriter w = response.getWriter()) {
            w.println("Id,Title,Description,Status,Priority,DateTime,AssignedUserId,AssignedUserName,CreatedDate,ApprovedBy,ApprovedAt");
            for (TaskResponse t : tasks) {
                w.printf("%d,\"%s\",\"%s\",%s,%s,%s,%d,\"%s\",%s,\"%s\",%s%n",
                    t.getId(),
                    escapeCsv(t.getTitle()),
                    escapeCsv(t.getDescription()),
                    t.getStatus(),
                    t.getPriority(),
                    t.getDateTime(),
                    t.getAssignedUserId(),
                    escapeCsv(t.getAssignedUserName() != null ? t.getAssignedUserName() : ""),
                    t.getCreatedDate(),
                    t.getApprovedByUserName() != null ? t.getApprovedByUserName() : "",
                    t.getApprovedAt() != null ? t.getApprovedAt().toString() : ""
                );
            }
        }
    }

    @GetMapping("/calendar")
    public List<TaskResponse> calendar(@RequestParam int year, @RequestParam int month) {
        return taskService.getTasksForCalendar(year, month);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
