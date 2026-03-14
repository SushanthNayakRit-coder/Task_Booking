package com.taskbooking.service;

import com.taskbooking.dto.TaskRequest;
import com.taskbooking.dto.TaskResponse;
import com.taskbooking.entity.*;
import com.taskbooking.repository.TaskRepository;
import com.taskbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private TaskService taskService;

    private User manager;
    private User user;
    private Task pendingTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository, notificationService);
        manager = new User("manager", "hash", "Manager", UserRole.MANAGER);
        manager.setId(1L);
        user = new User("user", "hash", "User", UserRole.USER);
        user.setId(2L);
        pendingTask = new Task();
        pendingTask.setId(10L);
        pendingTask.setTitle("Test Task");
        pendingTask.setStatus(TaskStatus.PENDING);
        pendingTask.setAssignedUserId(2L);
        pendingTask.setDateTime(LocalDateTime.now());
        pendingTask.setPriority(Priority.MEDIUM);
    }

    @Test
    void createTask_createsAndReturnsTask() {
        TaskRequest request = new TaskRequest();
        request.setTitle("New Task");
        request.setDescription("Desc");
        request.setDateTime(LocalDateTime.now().plusDays(1));
        request.setPriority(Priority.HIGH);
        request.setAssignedUserId(2L);

        Task saved = new Task();
        saved.setId(1L);
        saved.setTitle(request.getTitle());
        saved.setStatus(TaskStatus.PENDING);
        saved.setAssignedUserId(request.getAssignedUserId());
        saved.setDateTime(request.getDateTime());
        saved.setPriority(request.getPriority());
        saved.setCreatedDate(LocalDateTime.now());

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedDate(LocalDateTime.now());
            return t;
        });

        TaskResponse response = taskService.createTask(request);

        assertThat(response.getTitle()).isEqualTo("New Task");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.getAssignedUserId()).isEqualTo(2L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void approveOrReject_managerCanApprove() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        TaskResponse response = taskService.approveOrReject(10L, TaskStatus.APPROVED, 1L);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.APPROVED);
        verify(notificationService).notifyStatusChange(any(Task.class), eq(TaskStatus.APPROVED), eq("Manager"));
    }

    @Test
    void approveOrReject_adminCannotApprove() {
        User admin = new User("admin", "hash", "Admin", UserRole.ADMIN);
        admin.setId(3L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> taskService.approveOrReject(10L, TaskStatus.APPROVED, 3L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only Manager");
    }

    @Test
    void approveOrReject_throwsWhenNotPending() {
        pendingTask.setStatus(TaskStatus.APPROVED);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));

        assertThatThrownBy(() -> taskService.approveOrReject(10L, TaskStatus.REJECTED, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only PENDING");
    }

    @Test
    void listTasks_returnsFilteredByStatus() {
        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(pendingTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<TaskResponse> list = taskService.listTasks(TaskStatus.PENDING, "date");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void listTasks_sortByPriority_returnsHigherPriorityFirst() {
        Task mediumTask = new Task();
        mediumTask.setId(1L);
        mediumTask.setTitle("Medium Task");
        mediumTask.setPriority(Priority.MEDIUM);
        mediumTask.setDateTime(LocalDateTime.now().plusDays(1));
        mediumTask.setAssignedUserId(2L);
        mediumTask.setStatus(TaskStatus.PENDING);
        Task highTask = new Task();
        highTask.setId(2L);
        highTask.setTitle("High Task");
        highTask.setPriority(Priority.HIGH);
        highTask.setDateTime(LocalDateTime.now());
        highTask.setAssignedUserId(2L);
        highTask.setStatus(TaskStatus.PENDING);
        when(taskRepository.findAll()).thenReturn(List.of(mediumTask, highTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<TaskResponse> list = taskService.listTasks(null, "priority");

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(list.get(0).getTitle()).isEqualTo("High Task");
        assertThat(list.get(1).getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(list.get(1).getTitle()).isEqualTo("Medium Task");
    }

    @Test
    void createTask_throwsWhenAssignedUserNotFound() {
        TaskRequest request = new TaskRequest();
        request.setTitle("New Task");
        request.setDescription("Desc");
        request.setDateTime(LocalDateTime.now().plusDays(1));
        request.setPriority(Priority.HIGH);
        request.setAssignedUserId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assigned user not found");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void approveOrReject_managerCanReject() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        TaskResponse response = taskService.approveOrReject(10L, TaskStatus.REJECTED, 1L);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(response.getApprovedByUserId()).isEqualTo(1L);
        assertThat(response.getApprovedAt()).isNotNull();
        verify(notificationService).notifyStatusChange(any(Task.class), eq(TaskStatus.REJECTED), eq("Manager"));
    }

    @Test
    void approveOrReject_throwsWhenTaskNotFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.approveOrReject(10L, TaskStatus.APPROVED, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void approveOrReject_throwsWhenApproverNotFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.approveOrReject(10L, TaskStatus.APPROVED, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void approveOrReject_throwsWhenStatusIsPending() {
        assertThatThrownBy(() -> taskService.approveOrReject(10L, TaskStatus.PENDING, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must be APPROVED or REJECTED");

        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void listTasks_withoutStatus_returnsAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(pendingTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<TaskResponse> list = taskService.listTasks(null, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Test Task");
    }

    @Test
    void listTasks_sortByDate_returnsNewestFirst() {
        Task olderTask = new Task();
        olderTask.setId(1L);
        olderTask.setTitle("Older Task");
        olderTask.setPriority(Priority.MEDIUM);
        olderTask.setDateTime(LocalDateTime.now().plusDays(1));
        olderTask.setAssignedUserId(2L);
        olderTask.setStatus(TaskStatus.PENDING);

        Task newerTask = new Task();
        newerTask.setId(2L);
        newerTask.setTitle("Newer Task");
        newerTask.setPriority(Priority.HIGH);
        newerTask.setDateTime(LocalDateTime.now().plusDays(3));
        newerTask.setAssignedUserId(2L);
        newerTask.setStatus(TaskStatus.PENDING);

        when(taskRepository.findAll()).thenReturn(List.of(olderTask, newerTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<TaskResponse> list = taskService.listTasks(null, "date");

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getTitle()).isEqualTo("Newer Task");
        assertThat(list.get(1).getTitle()).isEqualTo("Older Task");
    }

    @Test
    void getTask_returnsTaskWhenFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(pendingTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        TaskResponse response = taskService.getTask(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void getTask_throwsWhenNotFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void getTasksForCalendar_returnsOnlyTasksForGivenMonth() {
        Task marchTask = new Task();
        marchTask.setId(1L);
        marchTask.setTitle("March Task");
        marchTask.setPriority(Priority.MEDIUM);
        marchTask.setDateTime(LocalDateTime.of(2026, 3, 20, 10, 0));
        marchTask.setAssignedUserId(2L);
        marchTask.setStatus(TaskStatus.PENDING);

        Task aprilTask = new Task();
        aprilTask.setId(2L);
        aprilTask.setTitle("April Task");
        aprilTask.setPriority(Priority.HIGH);
        aprilTask.setDateTime(LocalDateTime.of(2026, 4, 5, 11, 0));
        aprilTask.setAssignedUserId(2L);
        aprilTask.setStatus(TaskStatus.PENDING);

        when(taskRepository.findAll()).thenReturn(List.of(marchTask, aprilTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        List<TaskResponse> list = taskService.getTasksForCalendar(2026, 3);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("March Task");
    }
}
