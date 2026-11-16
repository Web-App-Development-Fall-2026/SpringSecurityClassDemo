# Spring Security Cheat Sheet (Authentication & Authorization)

This cheat sheet covers both **Authentication** (who the user is) and **Authorization** (what roles/pages the user can access) in Spring Security 6 / Spring Boot.

---

## 1. The Big Picture: How Spring Security Works

```
Browser (POST /login)
   │
   ▼
SecurityFilterChain (Checks URL rules: permitAll / hasRole / hasAnyRole / authenticated)
   │
   ▼
AuthenticationManager
   │
   ├──► UserDetailsService.loadUserByUsername(username) ──► UserRepository (Database)
   │
   └──► PasswordEncoder.matches(rawPassword, encodedPasswordFromDB)
          │
          ├── Match!    ──► Saves User + Authorities in SecurityContext ──► Redirects to "/"
          └── No Match! ──► Redirects to "/login?error"
```

---

## 2. Core Interfaces & Annotations

| Component / Annotation | Purpose | Where We Use It |
| :--- | :--- | :--- |
| `UserDetails` | Contract representing a user (`getUsername()`, `getPassword()`, `getAuthorities()`). | Implemented by `User.java` |
| `SimpleGrantedAuthority` | Wraps a role string (e.g. `"ROLE_ADMIN"`) so Spring Security understands it. | Used in `User.getAuthorities()` |
| `UserDetailsService` | Contract with one method `loadUserByUsername(String username)` to fetch a user from the DB. | Implemented by `UserService.java` |
| `PasswordEncoder` | Hashes passwords before saving (`encode()`) and verifies them on login (`matches()`). | Bean in `SecurityConfiguration.java` |
| `SecurityFilterChain` | Defines URL access rules (`permitAll()`, `hasRole()`, `hasAnyRole()`, `authenticated()`). | Bean in `SecurityConfiguration.java` |
| `@AuthenticationPrincipal` | Injects the currently logged-in `User` object directly into a Controller method. | Used in `WelcomePage.java` |
| `sec:authorize` | Thymeleaf attribute that shows/hides HTML elements based on the user's role. | Used in `home.html` |

---

## 3. Step-by-Step Code Reference

### Step 1: Entity Implementing `UserDetails` (`User.java`)
Spring Security's `hasRole("ADMIN")` automatically looks for an authority with the `"ROLE_"` prefix (`"ROLE_ADMIN"`).

```java
@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String role; // e.g., "ROLE_USER", "ROLE_MANAGER", "ROLE_ADMIN"

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }
}
```

---

### Step 2: Service Implementing `UserDetailsService` (`UserService.java`)
All new users signing up through `/register` are assigned `"ROLE_USER"` by default.

```java
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public boolean registerUser(String username, String password) {
        if (repo.findByUsername(username).isPresent()) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        repo.save(user);
        return true;
    }
}
```

---

### Step 3: Pre-Seeded Test Accounts (`DataLoader.java`)
| Username | Password | Role | Pages Accessible |
| :--- | :--- | :--- | :--- |
| `chris` | `1234567` | `ROLE_USER` | `/` |
| `manager` | `1234567` | `ROLE_MANAGER` | `/`, `/manager` |
| `admin` | `1234567` | `ROLE_ADMIN` | `/`, `/manager`, `/admin` |

---

### Step 4: Security Filter Chain (`SecurityConfiguration.java`)
- **`hasAnyRole("MANAGER", "ADMIN")`**: Checks for `ROLE_MANAGER` or `ROLE_ADMIN`.
- **`hasRole("ADMIN")`**: Checks for `ROLE_ADMIN` (do **not** pass `"ROLE_"` into `hasRole`, Spring adds it automatically!).

```java
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // For H2 Console
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/h2-console/**").permitAll()
                .requestMatchers("/manager").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            );

        return http.build();
    }
```

---

### Step 5: Hiding Buttons in Thymeleaf (`home.html`)
Add the Spring Security namespace to `<html>`:
```html
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```
Then use `sec:authorize` on any link or button:
```html
<!-- Shown ONLY to MANAGER and ADMIN -->
<a sec:authorize="hasAnyRole('MANAGER', 'ADMIN')" th:href="@{/manager}" class="link-btn manager-btn">
    Manager Dashboard
</a>

<!-- Shown ONLY to ADMIN -->
<a sec:authorize="hasRole('ADMIN')" th:href="@{/admin}" class="link-btn admin-btn">
    Admin Dashboard
</a>
```
