package com.taskbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Email notification service. Simulates sending by logging the email.
 * To enable real sending: add spring-boot-starter-mail, configure spring.mail.*,
 * inject JavaMailSender and call it here when configured.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /**
     * Simulates sending an email by logging it. In production you would
     * use JavaMailSender or another provider to send the email.
     */
    public void sendSimulation(String to, String subject, String body) {
        String simulated = String.format(
            "[EMAIL SIMULATION] To: %s | Subject: %s%nBody:%n%s",
            to, subject, body
        );
        log.info(simulated);
        // To send for real: mailSender.send(mimeMessage);
    }

    /**
     * Convenience: send task status notification (approved/rejected).
     */
    public void sendTaskStatusNotification(String toEmail, String taskTitle, boolean approved, String approvedBy) {
        String status = approved ? "APPROVED" : "REJECTED";
        String subject = "Task " + status + ": " + taskTitle;
        String body = String.format(
            "Your task \"%s\" has been %s by %s.%n%nThis is an automated notification.",
            taskTitle, status.toLowerCase(), approvedBy
        );
        sendSimulation(toEmail, subject, body);
    }
}
