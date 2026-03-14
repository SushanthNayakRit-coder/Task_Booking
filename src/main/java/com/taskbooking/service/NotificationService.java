package com.taskbooking.service;

import com.taskbooking.entity.Task;
import com.taskbooking.entity.TaskStatus;
import com.taskbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Notifications: console log + email simulation when a task is approved or rejected.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailService emailService;
    private final UserRepository userRepository;

    public NotificationService(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    public void notifyTaskApproved(Task task, String approvedByUserName) {
        String msg = String.format(
            "[NOTIFICATION] Task '%s' (id=%d) was APPROVED by %s at %s. Assigned to user id %d.",
            task.getTitle(), task.getId(), approvedByUserName, task.getApprovedAt(), task.getAssignedUserId()
        );
        log.info(msg);
        sendEmailSimulation(task, true, approvedByUserName);
    }

    public void notifyTaskRejected(Task task, String rejectedByUserName) {
        String msg = String.format(
            "[NOTIFICATION] Task '%s' (id=%d) was REJECTED by %s at %s. Assigned to user id %d.",
            task.getTitle(), task.getId(), rejectedByUserName, task.getApprovedAt(), task.getAssignedUserId()
        );
        log.info(msg);
        sendEmailSimulation(task, false, rejectedByUserName);
    }

    public void notifyStatusChange(Task task, TaskStatus newStatus, String byUserName) {
        if (newStatus == TaskStatus.APPROVED) {
            notifyTaskApproved(task, byUserName);
        } else if (newStatus == TaskStatus.REJECTED) {
            notifyTaskRejected(task, byUserName);
        }
    }

    private void sendEmailSimulation(Task task, boolean approved, String byUserName) {
        String toEmail = userRepository.findById(task.getAssignedUserId())
            .map(u -> u.getUsername() + "@example.com")
            .orElse("assigned-user@" + task.getAssignedUserId() + ".example.com");
        emailService.sendTaskStatusNotification(toEmail, task.getTitle(), approved, byUserName);
    }
}
