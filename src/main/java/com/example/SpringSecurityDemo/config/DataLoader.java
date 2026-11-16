package com.example.SpringSecurityDemo.config;

import com.example.SpringSecurityDemo.model.User;
import com.example.SpringSecurityDemo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository repository;
    private final PasswordEncoder pass;

    public DataLoader(UserRepository repository, PasswordEncoder pass) {
        this.repository = repository;
        this.pass = pass;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUser("chris", "1234567", "ROLE_USER");
        seedUser("manager", "1234567", "ROLE_MANAGER");
        seedUser("admin", "1234567", "ROLE_ADMIN");
    }

    private void seedUser(String username, String rawPassword, String role) {
        if (repository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(pass.encode(rawPassword));
            user.setRole(role);
            repository.save(user);
        }
    }
}
