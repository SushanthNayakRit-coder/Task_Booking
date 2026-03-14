package com.taskbooking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/tasks")
    public String tasksList() {
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTask() {
        return "task-form";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "calendar";
    }
}
