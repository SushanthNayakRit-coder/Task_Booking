package com.taskbooking.service;

import com.taskbooking.dto.TaskRequest;
import com.taskbooking.dto.TaskResponse;
import com.taskbooking.entity.*;
import com.taskbooking.repository.TaskRepository;
import com.taskbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        userRepository.findById(request.getAssignedUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assigned user not found: " + request.getAssignedUserId()));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDateTime(request.getDateTime());
        task.setPriority(request.getPriority());
        task.setAssignedUserId(request.getAssignedUserId());
        task.setStatus(TaskStatus.PENDING);
        task = taskRepository.save(task);
        return toResponse(task);
    }

    public List<TaskResponse> listTasks(TaskStatus status, String sortBy) {
        List<Task> tasks = status == null
            ? taskRepository.findAll()
            : taskRepository.findByStatus(status);

        String sort = (sortBy != null) ? sortBy.trim() : "date";
        boolean byPriority = "priority".equalsIgnoreCase(sort) || (sort != null && sort.toLowerCase().contains("priority"));
        if (byPriority) {
            // Highest priority first (URGENT > HIGH > MEDIUM > LOW), then newest date first
            tasks = tasks.stream()
                .sorted(Comparator
                    .comparingInt((Task t) -> t.getPriority().ordinal()).reversed()
                    .thenComparing(Task::getDateTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        } else {
            tasks = tasks.stream()
                .sorted(Comparator.comparing(Task::getDateTime).reversed())
                .collect(Collectors.toList());
        }

        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TaskResponse getTask(Long id) {
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        return toResponse(task);
    }

    @Transactional
    public TaskResponse approveOrReject(Long taskId, TaskStatus newStatus, Long approvedByUserId) {
        if (newStatus != TaskStatus.APPROVED && newStatus != TaskStatus.REJECTED) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tasks can be approved or rejected");
        }

        User approver = userRepository.findById(approvedByUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (approver.getRole() != UserRole.MANAGER) {
            throw new IllegalStateException("Only Manager can approve/reject tasks");
        }

        task.setStatus(newStatus);
        task.setApprovedByUserId(approvedByUserId);
        task.setApprovedAt(java.time.LocalDateTime.now());
        task = taskRepository.save(task);

        notificationService.notifyStatusChange(task, newStatus, approver.getName());
        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse r = new TaskResponse();
        r.setId(task.getId());
        r.setTitle(task.getTitle());
        r.setDescription(task.getDescription());
        r.setStatus(task.getStatus());
        r.setPriority(task.getPriority());
        r.setDateTime(task.getDateTime());
        r.setAssignedUserId(task.getAssignedUserId());
        r.setCreatedDate(task.getCreatedDate());
        r.setApprovedByUserId(task.getApprovedByUserId());
        r.setApprovedAt(task.getApprovedAt());
        userRepository.findById(task.getAssignedUserId())
            .ifPresent(u -> r.setAssignedUserName(u.getName()));
        if (task.getApprovedByUserId() != null) {
            userRepository.findById(task.getApprovedByUserId())
                .ifPresent(u -> r.setApprovedByUserName(u.getName()));
        }
        return r;
    }

    public List<TaskResponse> getTasksForCalendar(int year, int month) {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
            .filter(t -> t.getDateTime().getYear() == year && t.getDateTime().getMonthValue() == month)
            .sorted(Comparator.comparing(Task::getDateTime))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}
