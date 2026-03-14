# Workflow Logic

This document briefly explains the approval workflow and notification behaviour (including email simulation).

---

## Status model

Each **Task** has a status with three possible values:

- **PENDING** – Task has been created and is waiting for a decision.
- **APPROVED** – A Manager has approved the task.
- **REJECTED** – A Manager has rejected the task.

## Transitions

- **Creation**: New tasks are always created with status **PENDING**.
- **Allowed transitions**: Only **PENDING** → **APPROVED** and **PENDING** → **REJECTED** are allowed.
- **No further changes**: Once a task is **APPROVED** or **REJECTED**, its status is not changed again by the approval endpoint (any such request is rejected with an error).

## Who can approve or reject

- Only **Manager** role can call the approve/reject API (`PUT /api/tasks/{id}/approve`).
- **Admin** and **User** cannot; the API returns **403 Forbidden** if a non-Manager tries to approve or reject.

## Implementation details

1. **Backend (TaskService)**  
   - Validates that the task exists and is **PENDING**.  
   - Validates that the current user has role **MANAGER**.  
   - Sets the task status to **APPROVED** or **REJECTED**, stores the approver’s user id and timestamp.  
   - Triggers a notification: console log + email simulation (see below).

2. **Frontend**  
   - Approve/Reject buttons are shown only for **PENDING** tasks and only when the current user is Manager.  
   - Clicking Approve or Reject sends `PUT /api/tasks/{id}/approve` with body `{"status":"APPROVED"}` or `{"status":"REJECTED"}`.

3. **Notifications (console log + email simulation)**  
   - **NotificationService** logs approval/rejection to the console: `[NOTIFICATION] Task '...' was APPROVED/REJECTED by ...`  
   - It then looks up the task’s **assigned user**, builds a simulated recipient address (`username@example.com`), and calls **EmailService.sendTaskStatusNotification(...)**.  
   - **EmailService** does not send real email; it **simulates** by logging the would-be email: `[EMAIL SIMULATION] To: ... | Subject: Task APPROVED/REJECTED: ...` and a short body. This satisfies the requirement for “simple notification (console log or email simulation)”. To send real email later, add Spring Mail and call a mail sender from `EmailService`.

This keeps the workflow simple and predictable: one decision per task, only from PENDING, and only by Manager.

---

## Verifying notifications

1. Log in as **user**, create a task and assign it to a user.  
2. Log in as **manager**, open **Tasks**, click **Approve** or **Reject** on that task.  
3. In the **terminal** where the app is running, you should see `[NOTIFICATION]` and `[EMAIL SIMULATION]` with To, Subject, and body. The UI does not display the email; it appears only in the server console.
