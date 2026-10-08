package com.example.containermanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes browser requests to Thymeleaf view pages.
 *
 * This controller does NOT talk to the service or repository layers.
 * All data on these pages is loaded client-side via JavaScript calling
 * the existing REST endpoints in ContainerController. This class exists
 * purely to resolve URLs to template names.
 */
@Controller
public class ViewController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    @GetMapping("/containers")
    public String containerListPage() {
        return "container-list";
    }

    @GetMapping("/containers/add")
    public String addContainerPage() {
        return "add-container";
    }

    @GetMapping("/containers/edit/{id}")
    public String editContainerPage() {
        return "edit-container";
    }

}
