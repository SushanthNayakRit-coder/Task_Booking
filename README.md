# Task / Booking Management Application

A form-based web application for creating, managing, and approving tasks or bookings, built with **Spring Boot** (Java) and a simple approval workflow.

## Features

- **User authentication**: Login / logout with role-based access (Admin, Manager, User)
- **Dashboard**: Counts of Pending, Approved, and Rejected tasks; recent tasks list
- **Create Task/Booking**: Title, Description, Date/Time, Priority, Assigned User
- **List view**: Filter by status (Pending, Approved, Rejected), sort by date or priority
- **Approval workflow**: Only users with **Manager** role can approve or reject Pending tasks
- **Notifications**: Console log + email simulation on approve/reject (see [WORKFLOW.md](WORKFLOW.md))
- **Bonus**: Calendar view, Export tasks as CSV, Unit tests for service layer

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA
- **Frontend**: HTML, CSS, JavaScript (vanilla), Thymeleaf for server-rendered pages
- **Database**: H2 (in-memory by default; configurable for MySQL/PostgreSQL)

## Setup

### Prerequisites

- Java 17+
- Maven 3.6+

### Run the application

```bash
cd task-booking-app
mvn spring-boot:run
```

- **App URL**: http://localhost:8080 (or the port set in `application.properties`, e.g. 8082)  
- **Login**: Use one of the seeded users (see below).  
- **H2 Console** (optional): http://localhost:8080/h2-console (use same host/port as app)  
  - JDBC URL: `jdbc:h2:mem:taskdb`  
  - Username: `sa`  
  - Password: (leave empty)

### Default users (created on first run)

| Username | Password | Role    |
|----------|----------|---------|
| admin    | admin    | ADMIN   |
| manager  | manager  | MANAGER |
| user     | user     | USER    |

Only **Manager** can approve or reject tasks. For workflow and notification details, see **[WORKFLOW.md](WORKFLOW.md)**.

## REST API

All API requests require authentication (session cookie after login).

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST   | `/api/tasks` | Create a task (JSON body: title, description, dateTime, priority, assignedUserId) |
| GET    | `/api/tasks` | List tasks. Query: `status` (PENDING/APPROVED/REJECTED), `sortBy` (date/priority) |
| GET    | `/api/tasks/{id}` | Get one task |
| PUT    | `/api/tasks/{id}/approve` | Approve or reject (body: `{"status":"APPROVED"}` or `"REJECTED"`) |
| GET    | `/api/tasks/export` | Download tasks as CSV (optional `status` query) |
| GET    | `/api/tasks/calendar?year=&month=` | Tasks for calendar view |
| GET    | `/api/users` | List users (for assignee dropdown) |
| GET    | `/api/auth/me` | Current user info |

## Project structure

```
src/main/java/com/taskbooking/
  entity/       User, Task, TaskStatus, Priority, UserRole
  repository/   UserRepository, TaskRepository
  dto/          TaskRequest, TaskResponse, ApproveRequest, UserResponse
  service/      TaskService, UserService, NotificationService, EmailService
  security/     SecurityConfig, UserDetailsServiceImpl
  controller/   PageController, TaskController, UserController, AuthController, GlobalExceptionHandler
  config/       DataInitializer
src/main/resources/
  templates/    login, dashboard, tasks, task-form, calendar, layout
  static/css/   style.css
  static/js/    app.js
```

## Using MySQL or PostgreSQL

In `src/main/resources/application.properties`, comment out the H2 config and set:

**MySQL example:**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/taskdb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

Create the database (`taskdb`) before starting the app. For production, set `spring.jpa.hibernate.ddl-auto=validate` or use Flyway/Liquibase.

## Tests

```bash
mvn test
```

Unit tests cover `TaskService` (create, list, approve/reject, validation). For approval workflow and notification logic, see **[WORKFLOW.md](WORKFLOW.md)**.
