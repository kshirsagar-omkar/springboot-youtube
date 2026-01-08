package com.org.stem_project.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.org.stem_project.model.User;
import com.org.stem_project.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {
    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OLD APPROACH (RISKY): Reading token from Authorization header
        // Headers can be intercepted/stolen in XSS attacks, not secure for token storage
        // Tokens in headers are exposed in network logs and browser history
        /*
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("Filter: No Bearer Token found in header");
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        */

        // NEW APPROACH (SECURE): Reading token from HttpOnly Cookie
        // HttpOnly cookies cannot be accessed by JavaScript, preventing XSS attacks
        // Cookies are automatically sent with requests, providing better security
        // CSRF protection can be added for additional security layer
        String token = null;
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("authToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // DEBUG 1: Did we get a token from cookie?
        if (token == null || token.isEmpty()) {
            System.out.println("Filter: No Auth Token found in cookies");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // DEBUG 2: Verifying...
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String email = decodedToken.getEmail();

            System.out.println("Filter: Token Valid for email: " + email);

            List<GrantedAuthority> authorities = new ArrayList<>();
            Optional<User> userOpt = userService.findByEmailT(email);

            /**
             * Here we are checking the user is present in the db or not
             * if present then we are assigning the role as per the db
             * else we treat them as user only
             */

            if (userOpt.isPresent()){
                String role = userOpt.get().getRole();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            else {
                authorities.add(new SimpleGrantedAuthority("NEW_USER"));
            }

            // Authenticate
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, decodedToken, authorities);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (FirebaseAuthException e) {
            System.out.println(" Filter: Firebase Verification Failed! Error: " + e.getMessage());

        }

        filterChain.doFilter(request, response);
    }
}