# 🔍 Complete Authentication Flow Analysis & Issues Found

## 🚨 CRITICAL ISSUES IDENTIFIED

### Issue #1: **Customer Frontend - Missing Credentials Mode** ⚠️ CRITICAL
**File:** `customer-frontend/src/auth/AuthContext.jsx`

**Problem:**
```javascript
const res = await fetch(`${API_BASE}/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, password }),
  // ❌ MISSING: credentials: "include"
});
```

**Why it fails:** 
- Browser doesn't send credentials in cross-origin requests by default
- Even with CORS `allowCredentials: true`, fetch needs explicit `credentials: "include"`
- Result: CORS preflight fails with "Response to preflight request doesn't pass access control check"

**Fix:** Add `credentials: "include"` to all fetch calls

---

### Issue #2: **API Gateway Config Server YAML - allowCredentials: false** ⚠️ CRITICAL
**File:** `api-gateway-dev.yml` (in Config Server repo)

**Current:**
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowCredentials: false  # ❌ This blocks JWT auth!
```

**Why it fails:**
- CORS specification: If `allowCredentials: true` is sent with `Access-Control-Allow-Credentials: true` header
- Backend can't send `Access-Control-Allow-Credentials: true` when config says `allowCredentials: false`
- Result: Browser blocks the response due to CORS policy violation

**Fix:** Change to `allowCredentials: true`

---

### Issue #3: **Admin Frontend - Missing withCredentials in Axios** ⚠️ CRITICAL
**File:** `frontend/src/services/api.config.jsx`

**Current:**
```javascript
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000
  // ❌ MISSING: withCredentials: true
});
```

**Why it fails:**
- Axios also requires explicit `withCredentials: true` for CORS requests with credentials
- Without it, JWT tokens in Authorization header might not be properly handled
- Browser blocks credentialed requests by default in CORS

**Fix:** Add `withCredentials: true`

---

### Issue #4: **Vercel Environment Variables Not Set** ⚠️ IMPORTANT
**Missing in both frontends on Vercel:**
- `VITE_API_BASE_URL=https://api-gateway-yc8a.onrender.com`

**Why it fails:**
- Without this env var, frontend tries to use `undefined` or wrong URL
- Results in API calls going to wrong endpoint

---

## 🔄 Complete Authentication Flow

### Admin Frontend Flow:
```
1. User enters credentials on Login page
   └─> LoginPage.jsx calls login({ username, password })
   
2. Sends POST to /auth/login with credentials
   └─> api.config.jsx (axios with baseURL + interceptors)
   └─> BaseURL from constants.jsx → VITE_API_BASE_URL
   
3. API Gateway receives request
   └─> Preflight OPTIONS request checked against CORS config
   └─> If allowCredentials: false ❌ CORS error
   └─> If allowCredentials: true ✅ continues
   
4. API Gateway routes to /auth/login (no routing rules match, but /auth is public)
   └─> AuthController.login() processes request
   └─> Returns JWT token in response
   
5. Frontend stores token in localStorage
   └─> AuthContext stores token/role/username
   └─> Interceptor attaches token to all future requests: Authorization: Bearer <token>
   
6. Subsequent requests include JWT
   └─> API Gateway checks SecurityConfig rules
   └─> JwtAuthenticationFilter validates token
   └─> Routes to correct microservice
```

### Customer Frontend Flow:
```
1. User enters credentials on LoginPage
   └─> calls login(email, password)
   └─> Uses raw fetch() to /api/customers/login
   
2. API Gateway routes to transactionService (via /api/customers/* route)
   └─> CustomerController.loginCustomer() in transactionService
   └─> Returns customer data
   
3. Frontend stores in localStorage and context
   └─> Can make requests to /api/customers/* endpoints
   └─> Also needs to access /api/transactions if needed
```

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Browser (Vercel Frontends)                  │
├─────────────────────────────────────────────────────────────────┤
│  Admin: https://bank-gaurd-frontend.vercel.app                 │
│  Customer: https://bank-gaurd-customer.vercel.app              │
└──────────────────────┬──────────────────────────────────────────┘
                       │
        (CORS Check #1: preflight OPTIONS)
        (Issue: allowCredentials: false ❌)
                       │
           ┌───────────▼──────────┐
           │  API Gateway         │
           │  :1001 (local)       │
           │  (onrender.com prod) │
           └────────┬─────────────┘
                    │ (Routes requests)
        ┌───────────┼───────────┐
        │           │           │
    ┌───▼───┐   ┌───▼─────┐  ┌─▼────┐
    │ Auth  │   │Enrichm. │  │Alert │
    │(GW)   │   │Service  │  │Case  │
    └───────┘   └─────────┘  └──────┘
        │           │           │
        └───────────┼───────────┘
        (Response includes JWT token for admin)
                    │
        ┌───────────▼──────────┐
        │ Browser receives     │
        │ JWT Token            │
        └───────────┬──────────┘
                    │ (Stores in localStorage)
                    │ (Attaches to future requests)
                    │
        ┌───────────▼──────────┐
        │ Subsequent requests  │
        │ + Authorization:     │
        │   Bearer <token>     │
        └───────────┬──────────┘
                    │
        (CORS Check #2: with credentials)
        (Issue: allowCredentials must be true!)
```

---

## 🔧 All Fixes Required

### 1. **Config Server Repository**
**File:** `api-gateway-dev.yml`

**Change:**
```yaml
# Before:
allowCredentials: false

# After:
allowCredentials: true
```

### 2. **Customer Frontend**
**File:** `customer-frontend/src/auth/AuthContext.jsx`

**Changes:**
```javascript
// Add credentials: "include" to ALL fetch calls
const res = await fetch(`${API_BASE}/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email, password }),
  credentials: "include",  // ADD THIS
});

// And in signup:
const res = await fetch(API_BASE, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload),
  credentials: "include",  // ADD THIS
});
```

### 3. **Admin Frontend**
**File:** `frontend/src/services/api.config.jsx`

**Change:**
```javascript
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
  withCredentials: true  // ADD THIS
});
```

### 4. **Vercel Environment Variables**

**For Admin Frontend:**
- Go to Settings → Environment Variables
- Add: `VITE_API_BASE_URL=https://api-gateway-yc8a.onrender.com`
- Select all environments
- Redeploy

**For Customer Frontend:**
- Go to Settings → Environment Variables
- Add: `VITE_API_BASE_URL=https://api-gateway-yc8a.onrender.com`
- Select all environments
- Redeploy

### 5. **API Gateway SecurityConfig** (Already Done ✅)
- Removed duplicate CorsWebFilter bean
- Now relies on YAML-based CORS from Config Server

---

## ✅ Verification Checklist

After making all fixes:

1. **Config Server:**
   - [ ] Push updated `api-gateway-dev.yml` with `allowCredentials: true`
   - [ ] API Gateway pulls new config (or manually redeploy)

2. **Customer Frontend:**
   - [ ] Add `credentials: "include"` to fetch calls
   - [ ] Set `VITE_API_BASE_URL` on Vercel
   - [ ] Redeploy on Vercel
   - [ ] Test signup at https://bank-gaurd-customer.vercel.app/signup
   - [ ] Check DevTools console for CORS errors

3. **Admin Frontend:**
   - [ ] Add `withCredentials: true` to axios
   - [ ] Set `VITE_API_BASE_URL` on Vercel
   - [ ] Redeploy on Vercel
   - [ ] Test login at https://bank-gaurd-frontend.vercel.app
   - [ ] Check DevTools console for CORS errors

4. **Backend:**
   - [ ] API Gateway has correct CORS config from YAML
   - [ ] All services running on Render
   - [ ] All microservices have CorsConfig.java with production domains

---

## 🎯 Root Cause Summary

| Component | Issue | Impact |
|-----------|-------|--------|
| Config Server YAML | `allowCredentials: false` | CORS blocks all credentialed requests |
| Customer Frontend | Missing `credentials: "include"` | Fetch doesn't send credentials |
| Admin Frontend | Missing `withCredentials: true` | Axios doesn't handle credentials properly |
| Vercel Env Vars | Not set | Frontend uses wrong/undefined API URL |
| SecurityConfig | Removed bean (✅ Fixed) | Now uses YAML-based CORS (better) |

---

## 🚀 Priority Order

1. **FIRST:** Update Config Server `api-gateway-dev.yml` - `allowCredentials: true`
2. **SECOND:** Add `credentials: "include"` to customer-frontend fetch calls
3. **THIRD:** Add `withCredentials: true` to admin-frontend axios
4. **FOURTH:** Set Vercel environment variables for both frontends
5. **FIFTH:** Redeploy everything and test

**All issues must be fixed for authentication to work!**
