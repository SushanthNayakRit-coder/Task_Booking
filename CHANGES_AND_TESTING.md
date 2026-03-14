# Summary of Changes & How to Test

## 1. Approve/Reject buttons only for Manager (not Admin)

**What changed**
- Only users with **Manager** role see Approve/Reject and can call the approve API.
- **Admin** and **User** no longer have approve/reject.

**Where**
- `SecurityConfig.java`: `PUT /api/tasks/*/approve` restricted to `hasRole("MANAGER")`.
- `TaskService.java`: `approveOrReject()` throws if approver is not MANAGER.
- `app.js`: `canApprove` is true only when `role === 'MANAGER'`.

**How to test**
- Log in as **manager** / **manager** → open **Tasks** → PENDING tasks show **Approve** and **Reject**.
- Log in as **admin** / **admin** → open **Tasks** → PENDING tasks do **not** show Approve/Reject.
- Log in as **user** / **user** → same as admin, no buttons.
- Unit test: `approveOrReject_managerCanApprove`, `approveOrReject_adminCannotApprove`.

---

## 2. Priority sort: highest first (URGENT → HIGH → MEDIUM → LOW)

**What changed**
- Sort-by-priority now uses “highest priority first”.
- Backend: `comparingInt(t -> t.getPriority().ordinal()).reversed()` so URGENT is first, then HIGH, MEDIUM, LOW.
- Cache-busting (`&_=timestamp`) and `Cache-Control: no-store` on task list so browser doesn’t serve old order.

**Where**
- `TaskService.java`: `listTasks(..., "priority")` sorting logic.
- `TaskController.java`: list endpoint returns `ResponseEntity` with `CacheControl.noStore()`.
- `app.js`: task list URL includes `&_=Date.now()`.

**How to test**
- Create tasks with **Medium**, **High**, and **Urgent**.
- **Tasks** → Status **All** → Sort by **Priority** → Apply (or change dropdown).
- Order must be: **Urgent** first, then **High**, then **Medium**, then **Low**.
- Unit test: `listTasks_sortByPriority_returnsHigherPriorityFirst`.

---

## 3. PENDING badge and Approve/Reject same size

**What changed**
- Status badge (PENDING/APPROVED/REJECTED) and Approve/Reject buttons use the same height and padding so they look the same size.

**Where**
- `static/css/style.css`: `.task-item .status` and `.task-actions button` both use `min-height: 2.25rem`, `padding: 0 0.75rem`, `display: inline-flex`, `align-items: center`, `box-sizing: border-box`.

**How to test**
- Log in as **manager** → **Tasks** → find a PENDING task.
- Visually: the **PENDING** label and **Approve** / **Reject** buttons should have the same height and similar padding.
- Hard refresh (Cmd+Shift+R / Ctrl+Shift+R) if you don’t see it.

---

## 4. Create task: assigned user must exist

**What changed**
- Creating a task with a non-existent assigned user id now fails with a clear error instead of saving the task.

**Where**
- `TaskService.java`: `createTask()` calls `userRepository.findById(request.getAssignedUserId()).orElseThrow(...)` before creating/saving the task.

**How to test**
- **UI:** Create task normally with a valid **Assigned User** from the dropdown → should succeed. (Invalid ids are hard to hit from UI unless you use devtools/API.)
- **API:** `POST /api/tasks` with `assignedUserId: 99999` (no such user) → 400 with message containing “Assigned user not found”.
- Unit test: `createTask_throwsWhenAssignedUserNotFound`, `createTask_createsAndReturnsTask` (still passes with valid user).

---

## 5. Email simulation (notification)

**What changed**
- On approve/reject, the app now does **console log** plus **email simulation**: it logs the would-be email (To, Subject, Body) instead of sending a real email.

**Where**
- `EmailService.java`: `sendSimulation(to, subject, body)` logs `[EMAIL SIMULATION] To: ... | Subject: ...` and body; `sendTaskStatusNotification(toEmail, taskTitle, approved, approvedBy)` builds subject/body for task status.
- `NotificationService.java`: After logging `[NOTIFICATION]`, looks up assigned user, builds `username@example.com`, calls `EmailService.sendTaskStatusNotification(...)`.

**How to test**
- Log in as **user** → create a task (assign to any user).
- Log in as **manager** → **Tasks** → click **Approve** or **Reject** on that task.
- In the **terminal** where `mvn spring-boot:run` is running, confirm you see both:
  - `[NOTIFICATION] Task '...' was APPROVED/REJECTED by Manager ...`
  - `[EMAIL SIMULATION] To: user@example.com | Subject: Task APPROVED: ...` (or REJECTED) and the body.
- The UI does not show the email; it is only in the server console.

---

## 6. Unit tests added/updated (service layer)

**What changed**
- Assigned-user validation in `createTask()` (see above).
- Many new tests so service behavior and edge cases are covered.

**New/updated tests in `TaskServiceTest.java`**
- `createTask_throwsWhenAssignedUserNotFound` – invalid assigned user → exception, no save.
- `approveOrReject_managerCanReject` – Manager can reject; status, approver, notification.
- `approveOrReject_throwsWhenTaskNotFound` – task id not in DB.
- `approveOrReject_throwsWhenApproverNotFound` – approver user id not in DB.
- `approveOrReject_throwsWhenStatusIsPending` – status PENDING not allowed for approve/reject; no DB calls.
- `listTasks_withoutStatus_returnsAllTasks` – `listTasks(null, null)` uses `findAll()`.
- `listTasks_sortByDate_returnsNewestFirst` – date sort returns newer task first.
- `getTask_returnsTaskWhenFound` – getTask returns correct task.
- `getTask_throwsWhenNotFound` – getTask throws when task missing.
- `getTasksForCalendar_returnsOnlyTasksForGivenMonth` – calendar filters by month.

**How to test**
- Run: `mvn test -Dtest=TaskServiceTest`
- All tests should pass.

---

## Quick test checklist (UI)

| # | What to do | Expected |
|---|------------|----------|
| 1 | Log in as **manager** → **Tasks** | Approve/Reject on PENDING tasks; PENDING and buttons same size. |
| 2 | Log in as **admin** → **Tasks** | No Approve/Reject on PENDING tasks. |
| 3 | **Tasks** → Status **All** → Sort **Priority** → Apply | Order: Urgent → High → Medium → Low. |
| 4 | **New Task** → fill form, pick **Assigned User** from list → Create | Task created. |
| 5 | **Dashboard** | Counts for Pending/Approved/Rejected; recent tasks. |
| 6 | **Calendar** | Month grid and “Tasks this month” list. |
| 7 | **Tasks** → **Export CSV** | CSV file downloads. |
| 8 | **Email simulation** | As manager, approve/reject a task → check server console for `[NOTIFICATION]` and `[EMAIL SIMULATION]`. |
| 9 | Log out and log in again | Login/logout works. |

---

## Quick test checklist (backend)

| # | What to do | Expected |
|---|------------|----------|
| 1 | `mvn test -Dtest=TaskServiceTest` | All tests pass. |
| 2 | `mvn spring-boot:run` | App starts; http://localhost:8080 works. |

---

## Run the app

```bash
cd task-booking-app
mvn spring-boot:run
```

Then open **http://localhost:8080** and use **manager** / **manager** (or **admin** / **admin**, **user** / **user**).

## Run only service tests

```bash
cd task-booking-app
mvn test -Dtest=TaskServiceTest
```
