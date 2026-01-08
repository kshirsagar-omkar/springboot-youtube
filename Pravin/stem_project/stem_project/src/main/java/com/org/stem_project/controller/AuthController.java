package com.org.stem_project.controller;

import com.google.firebase.auth.FirebaseToken;
import com.org.stem_project.model.User;
import com.org.stem_project.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // DTO to receive role upgrade request
    public static class RoleRequest {
        public String role; // "USER" or "ADMIN"
    }

    // OLD APPROACH: Frontend was responsible for storing tokens
    // This was risky because tokens stored in localStorage are vulnerable to XSS attacks
    // JavaScript can access and steal tokens from localStorage

    // NEW APPROACH: Backend sets HttpOnly cookies
    // HttpOnly flag prevents JavaScript from accessing the cookie
    // Cookies are automatically sent with each request by the browser
    // This provides better security against XSS and token theft

    // 1. LOGIN CHECK: User logs in, we check if they exist. If not, create as USER
    @PostMapping("/check-user")
    public ResponseEntity<?> checkUser(Authentication authentication, HttpServletResponse response) {
        FirebaseToken token = (FirebaseToken) authentication.getCredentials();
        String email = token.getEmail();

        // Get the raw token string to store in cookie
        String tokenString = token.toString(); // This will be set from the frontend

        Optional<User> userOpt = userService.findByEmailT(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of("status", "EXISTS", "role", user.getRole()));
        }

        // Create new user with default USER role
        User user = new User();
        user.setEmail(token.getEmail());
        user.setName(token.getName());
        user.setFirebaseUid(token.getUid());
        user.setRole("USER"); // Default role is USER
        userService.add(user);

        return ResponseEntity.ok(Map.of("status", "NEW_USER", "role", "USER"));
    }

    // NEW ENDPOINT: Store Firebase token in HttpOnly cookie
    // Frontend sends token after Firebase authentication
    // Backend stores it securely in a cookie
    @PostMapping("/set-token")
    public ResponseEntity<?> setAuthToken(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String token = payload.get("token");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        // Create HttpOnly cookie with the token
        // HttpOnly: Prevents JavaScript access (XSS protection)
        // Secure: Only sent over HTTPS in production
        // Path: Cookie available for all API endpoints
        // MaxAge: 1 hour (3600 seconds) - matches Firebase token expiry
        Cookie authCookie = new Cookie("authToken", token);
        authCookie.setHttpOnly(true); // Protection against XSS attacks
        authCookie.setSecure(false); // Set to true in production with HTTPS
        authCookie.setPath("/"); // Available for entire application
        authCookie.setMaxAge(3600); // 1 hour expiry

        response.addCookie(authCookie);

        return ResponseEntity.ok(Map.of("message", "Token stored in secure cookie"));
    }

    // Get current user information
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        FirebaseToken token = (FirebaseToken) authentication.getCredentials();
        String email = token.getEmail();

        Optional<User> userOpt = userService.findByEmailT(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
            ));
        }

        return ResponseEntity.notFound().build();
    }

    // NEW ENDPOINT: Logout - Clear the authentication cookie
    // Removes the HttpOnly cookie by setting MaxAge to 0
    // This invalidates the client's session
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie authCookie = new Cookie("authToken", null);
        authCookie.setHttpOnly(true);
        authCookie.setSecure(false); // Set to true in production
        authCookie.setPath("/");
        authCookie.setMaxAge(0); // Delete cookie immediately

        response.addCookie(authCookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // Optional: Endpoint to upgrade a user to ADMIN (should be protected/manual in production)
    @PostMapping("/upgrade-to-admin")
    public ResponseEntity<?> upgradeToAdmin(Authentication authentication) {
        FirebaseToken token = (FirebaseToken) authentication.getCredentials();
        String email = token.getEmail();

        User user = userService.findByEmailT(email).orElseThrow(() ->
                new RuntimeException("User Not Found"));

        user.setRole("ADMIN");
        userService.add(user);

        return ResponseEntity.ok(Map.of("message", "User upgraded to ADMIN", "role", "ADMIN"));
    }
}