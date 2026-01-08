# Cookie-Based Authentication - Quick Reference

## For Frontend Developers

### 1. Login Flow
```javascript
// After Firebase login, store token in cookie
async function login() {
    await signInWithPopup(auth, provider);
    const token = await auth.currentUser.getIdToken(true);
    
    await fetch('http://localhost:8080/api/auth/set-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',  // ← IMPORTANT!
        body: JSON.stringify({ token })
    });
}
```

### 2. Making Authenticated Requests
```javascript
// Just add credentials: 'include' - that's it!
const response = await fetch('http://localhost:8080/api/products', {
    credentials: 'include'  // ← Cookie automatically included
});
```

### 3. Logout Flow
```javascript
async function logout() {
    await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
    });
    await signOut(auth);
}
```

### Common Mistakes ❌
```javascript
// ❌ WRONG: Forgot credentials
fetch('/api/products')  // Cookie won't be sent!

// ❌ WRONG: Trying to access cookie in JavaScript
const token = document.cookie;  // Won't work - it's HttpOnly!

// ❌ WRONG: Adding Authorization header
fetch('/api/products', {
    headers: { 'Authorization': 'Bearer ' + token }  // Don't do this!
})
```

### Correct Usage ✅
```javascript
// ✅ RIGHT: Always include credentials
fetch('/api/products', { credentials: 'include' })

// ✅ RIGHT: Let browser manage cookies
// No need to manually handle tokens!

// ✅ RIGHT: Trust the backend
// Cookie is set and managed by backend
```

---

## For Backend Developers

### 1. Reading Token from Cookie
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

### 2. Setting Cookie
```java
Cookie authCookie = new Cookie("authToken", token);
authCookie.setHttpOnly(true);   // XSS protection
authCookie.setSecure(false);    // true in production
authCookie.setPath("/");        
authCookie.setMaxAge(3600);     // 1 hour
response.addCookie(authCookie);
```

### 3. Clearing Cookie (Logout)
```java
Cookie authCookie = new Cookie("authToken", null);
authCookie.setHttpOnly(true);
authCookie.setPath("/");
authCookie.setMaxAge(0);  // Delete immediately
response.addCookie(authCookie);
```

---

## Security Checklist

### Development ✅
- [x] HttpOnly flag enabled
- [x] Cookies working on localhost
- [x] CORS allows credentials
- [x] Token expiration set

### Production (Before Deployment) ⚠️
- [ ] Change `setSecure(false)` to `setSecure(true)`
- [ ] Enable CSRF protection
- [ ] Set SameSite attribute
- [ ] Update CORS allowed origins
- [ ] Test on production domain

---

## Troubleshooting

### "No Auth Token found in cookies"
**Fix:** Make sure you called `/api/auth/set-token` after Firebase login

### "CORS error"
**Fix:** Add `credentials: 'include'` to fetch AND ensure CORS config has `allowCredentials(true)`

### "Cookie not visible in DevTools"
**Note:** HttpOnly cookies ARE there but hidden from JavaScript - check Application > Cookies tab

### "401 Unauthorized"
**Check:**
1. Is cookie set? (DevTools > Application > Cookies)
2. Is token expired? (Refresh it)
3. Is user in database? (Call `/api/auth/check-user`)

---

## Key Differences

| Old (Headers) | New (Cookies) |
|---------------|---------------|
| `Authorization: Bearer <token>` | `Cookie: authToken=<token>` |
| Manual header management | Automatic by browser |
| Vulnerable to XSS | Protected by HttpOnly |
| localStorage.setItem() | Backend sets cookie |
| Manual expiration | Automatic expiration |

---

## Code Snippets for Common Tasks

### Check if User is Authenticated
```javascript
// Frontend
const response = await fetch('/api/auth/user', {
    credentials: 'include'
});

if (response.ok) {
    const user = await response.json();
    console.log('Logged in as:', user.email);
} else {
    console.log('Not authenticated');
}
```

### Create Product (Admin only)
```javascript
const product = {
    name: 'Laptop',
    price: 1299.99,
    category: 'Electronics',
    stock: 50
};

const response = await fetch('/api/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(product)
});
```

### Get All Products
```javascript
const response = await fetch('/api/products', {
    credentials: 'include'
});
const products = await response.json();
```

---

## Remember
- 🍪 Cookies are MORE secure than localStorage
- 🔒 HttpOnly prevents XSS attacks
- 🤖 Browser handles cookie lifecycle
- ✨ Less code to write and maintain!

---

**Last Updated:** December 14, 2025

