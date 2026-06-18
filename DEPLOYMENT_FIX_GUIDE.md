# 🚀 COMPLETE DEPLOYMENT FIX GUIDE - CORS & Authentication

## 📋 What Was Wrong

Your deployment failed because of **FOUR critical misconfigurations**:

1. ❌ **Config Server:** `allowCredentials: false` (should be `true`)
2. ❌ **Customer Frontend:** Missing `credentials: "include"` in fetch calls
3. ❌ **Admin Frontend:** Missing `withCredentials: true` in axios
4. ❌ **Vercel:** Environment variables not set for API endpoint

---

## ✅ What Has Been Fixed (In This Repo)

### 1. **Backend - SecurityConfig.java** ✅
- Removed duplicate `CorsWebFilter` bean (was conflicting with YAML config)
- Removed unused CORS imports
- Now uses YAML-based CORS from Config Server (cleaner approach)

### 2. **Admin Frontend - api.config.jsx** ✅
- Added `withCredentials: true` to axios configuration
- All requests now properly handle credentials for JWT auth

### 3. **Customer Frontend - AuthContext.jsx** ✅
- Added `credentials: "include"` to login fetch call
- Added `credentials: "include"` to signup fetch call
- Browser will now send credentials in CORS requests

### 4. **Documentation** ✅
- Created `AUTHENTICATION_FLOW_ANALYSIS.md` with complete flow diagrams
- Explains exactly how data flows from frontend to backend

---

## 🔴 CRITICAL - What MUST Be Fixed in Config Server

**Your Config Server Repository:**
`https://github.com/Harshal-55/Config-Server_Bankguard`

**File to Update:** `api-gateway-dev.yml` (or similar)

### Change This:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowCredentials: false  # ❌ CHANGE THIS
            allowedOrigins:
              - "https://bank-gaurd-frontend.vercel.app"
              - "https://bank-gaurd-customer.vercel.app"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders: "*"
            exposedHeaders: "*"
```

### To This:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowCredentials: true  # ✅ CHANGE TO true
            allowedOrigins:
              - "http://localhost:3000"           # Dev
              - "http://localhost:5173"           # Dev
              - "http://localhost:5174"           # Dev
              - "https://bank-gaurd-frontend.vercel.app"
              - "https://bank-gaurd-customer.vercel.app"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders: "*"
            exposedHeaders: "*"
```

---

## 🎯 Deployment Steps (IN ORDER)

### Step 1: Update Config Server Repository
1. Clone/open `https://github.com/Harshal-55/Config-Server_Bankguard`
2. Find `api-gateway-dev.yml`
3. Change `allowCredentials: false` → `allowCredentials: true`
4. Add localhost origins for dev
5. Commit and push to `main` branch
   ```
   git add api-gateway-dev.yml
   git commit -m "fix: Enable CORS credentials for JWT authentication"
   git push origin main
   ```

### Step 2: Redeploy API Gateway on Render
1. Go to https://dashboard.render.com
2. Select **api-gateway** service
3. Click **"Manual Deploy"** or wait for auto-deploy from GitHub
4. Wait for deployment to complete (~5 minutes)
5. Check logs to confirm config was loaded

### Step 3: Update Frontend Environment Variables on Vercel

#### For Admin Frontend:
1. Go to https://vercel.com/dashboard
2. Select **bank-gaurd-frontend** project
3. Go to **Settings → Environment Variables**
4. Add new variable:
   - **Name:** `VITE_API_BASE_URL`
   - **Value:** `https://api-gateway-yc8a.onrender.com`
   - **Environments:** Check all (Production, Preview, Development)
5. Click **Save**
6. Trigger redeploy: **Deployments → Redeploy** (or just push to GitHub)

#### For Customer Frontend:
1. Go to https://vercel.com/dashboard
2. Select **bank-gaurd-customer** project
3. Go to **Settings → Environment Variables**
4. Add new variable:
   - **Name:** `VITE_API_BASE_URL`
   - **Value:** `https://api-gateway-yc8a.onrender.com`
   - **Environments:** Check all (Production, Preview, Development)
5. Click **Save**
6. Trigger redeploy: **Deployments → Redeploy** (or just push to GitHub)

### Step 4: Pull Latest Code from Main Repo
```bash
cd /path/to/BankGuard-main
git pull origin main
npm install  # If needed
```

The following are already fixed in the repo:
- ✅ `frontend/src/services/api.config.jsx` - Added `withCredentials: true`
- ✅ `customer-frontend/src/auth/AuthContext.jsx` - Added `credentials: "include"`
- ✅ `Backend/apiGateway/src/main/java/.../SecurityConfig.java` - Removed duplicate bean

### Step 5: Test Everything

#### Test Admin Frontend:
1. Go to https://bank-gaurd-frontend.vercel.app
2. **Important:** Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
3. Try to log in with admin credentials
4. Open DevTools (F12) → Console tab
5. Look for any CORS or authentication errors
6. ✅ Should successfully log in and see dashboard

#### Test Customer Frontend:
1. Go to https://bank-gaurd-customer.vercel.app
2. **Important:** Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
3. Try to sign up with new account
4. Try to log in
5. Open DevTools (F12) → Console tab
6. Look for any CORS or authentication errors
7. ✅ Should successfully create account and see dashboard

---

## 🔍 Verification - What To Look For

### ✅ Good Signs (No Errors):
```
✓ Login request sends POST to /auth/login
✓ Response includes JWT token
✓ Token stored in localStorage
✓ Subsequent requests include Authorization: Bearer <token>
✓ Can access dashboard/data pages
✓ No CORS errors in console
✓ No "Access-Control-Allow-Credentials" errors
✓ No "Response to preflight request" errors
```

### ❌ Bad Signs (Still Issues):
```
✗ "Access-Control-Allow-Origin" header missing
✗ "Response to preflight request doesn't pass access control check"
✗ "The value of the 'Access-Control-Allow-Credentials' header is ''"
✗ "Cannot reach server. Is the backend running on port 1001?"
✗ "Failed to fetch" (404 or network error)
✗ Blank page after redirect to /dashboard
```

---

## 🐛 Troubleshooting

### Issue: Still getting CORS errors after deployment

**Solution:**
1. **Hard refresh browser** (Ctrl+Shift+R on Windows, Cmd+Shift+R on Mac)
2. **Clear browser cache:**
   - DevTools → Application → Clear site data
3. **Wait 10 minutes** for Render and Vercel caches to update
4. **Verify Config Server was pulled** by API Gateway:
   - Check Render logs for "Config loaded from..."
5. **Verify Vercel env vars are set:**
   - Vercel → Project Settings → Environment Variables
   - Confirm `VITE_API_BASE_URL` is visible

### Issue: Login works but can't load data on dashboard

**Solution:**
1. Check that all microservices are running on Render
2. Verify each service has CORS config with production domains
3. Check browser DevTools for specific endpoint errors
4. Backend logs should show what endpoints are being called

### Issue: Can't access Vercel environment variables

**Solution:**
1. Make sure you have access to the Vercel project
2. Go to Project Settings (not Project Settings in code)
3. Look for "Environment Variables" tab
4. If not visible, you might not have permissions
5. Contact project owner if needed

---

## 📊 Post-Deployment Checklist

- [ ] Config Server updated with `allowCredentials: true`
- [ ] API Gateway redeployed and pulled new config
- [ ] Admin Frontend has `VITE_API_BASE_URL` env var set on Vercel
- [ ] Customer Frontend has `VITE_API_BASE_URL` env var set on Vercel
- [ ] Both frontends redeployed on Vercel
- [ ] Hard refreshed all browser tabs
- [ ] Tested admin login at https://bank-gaurd-frontend.vercel.app
- [ ] Tested customer signup at https://bank-gaurd-customer.vercel.app
- [ ] No CORS errors in browser console
- [ ] Can access dashboard/pages after login
- [ ] Transactions/data load successfully

---

## 🎓 What Was Learned

### Why CORS Failed:
- CORS requires **explicit opt-in from both sides**
- Frontend must set `credentials: "include"` (fetch) or `withCredentials: true` (axios)
- Backend must set `allowCredentials: true` in CORS config
- Both MUST be true for authentication to work

### Why It Worked Locally:
- Localhost doesn't trigger CORS restrictions (same-origin policy bypass)
- Vite proxy redirected requests to local backend on same machine
- No credentials needed locally

### Why It Failed in Production:
- Vercel domain ≠ Render domain (different origins)
- CORS policy enforced strictly in browsers
- Credentials mode wasn't enabled on either side

---

## 📞 Support

If issues persist after following all steps:

1. **Check API Gateway logs on Render:**
   - Look for `globalcors` configuration loaded
   - Check for any startup errors

2. **Check Vercel deployment logs:**
   - Check build logs for env var substitution
   - Verify `VITE_API_BASE_URL` appears in build output

3. **Use browser DevTools:**
   - Network tab: Check preflight OPTIONS requests
   - Console tab: Look for exact CORS error messages
   - Copy the exact error message for debugging

4. **Verify services are running:**
   - Check Render dashboard for service status
   - Try `curl https://api-gateway-yc8a.onrender.com/health` (if endpoint exists)

---

## 🎉 Expected Results After Fix

**Admin Frontend:**
- ✅ Login page loads
- ✅ Can enter credentials
- ✅ JWT token received and stored
- ✅ Dashboard loads with user data
- ✅ Can view transactions, investigations, SAR reports
- ✅ Can manage users (if SuperAdmin)

**Customer Frontend:**
- ✅ Signup page loads
- ✅ Can create new account
- ✅ Can log in with account
- ✅ Dashboard loads with account data
- ✅ Can view transactions
- ✅ Can manage profile

**Console:**
- ✅ No CORS errors
- ✅ No network 404 errors
- ✅ No "Failed to fetch" messages

---

**Last Updated:** June 18, 2026
**Status:** Frontend code fixed ✅ | Config Server update needed ⏳ | Vercel env vars needed ⏳
