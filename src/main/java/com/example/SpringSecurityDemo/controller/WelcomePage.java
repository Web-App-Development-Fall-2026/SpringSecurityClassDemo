package com.example.SpringSecurityDemo.controller;

import com.example.SpringSecurityDemo.model.User;
import com.example.SpringSecurityDemo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WelcomePage {

    private final UserService userService;

    public WelcomePage(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String displayWelcome(@AuthenticationPrincipal User user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            model.addAttribute("role", user.getRole());
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("role", "NONE");
        }
        return "home";
    }

    @GetMapping("/manager")
    public String displayManager(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("username", user != null ? user.getUsername() : "Manager");
        return "manager";
    }

    @GetMapping("/admin")
    public String displayAdmin(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("username", user != null ? user.getUsername() : "Admin");
        return "admin";
    }

    @GetMapping("/login")
    public String displayLogin() {
        return "login";
    }

    @GetMapping("/register")
    public String displayRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               Model model) {
        boolean created = userService.registerUser(username, password);
        if (!created) {
            model.addAttribute("error", "Username already exists!");
            return "register";
        }
        return "redirect:/login?registered";
    }
}
