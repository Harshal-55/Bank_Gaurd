# BankGuard Vercel Deployment Setup Guide

## Overview
This guide explains how to properly configure environment variables on Vercel for both the Admin Frontend and Customer Frontend to communicate with the backend API services deployed on Render.

---

## Prerequisites
- Both frontends deployed on Vercel
- All backend services deployed on Render with CORS configuration updated
- Admin: https://bank-gaurd-frontend.vercel.app
- Customer: https://bank-gaurd-customer.vercel.app
- API Gateway: https://api-gateway-yc8a.onrender.com

---

## 1. Admin Frontend (frontend) - Vercel Setup

### Environment Variable Required:
```
VITE_API_BASE_URL=https://api-gateway-yc8a.onrender.com
```

### Steps to Configure on Vercel:
1. Go to your Vercel project: **frontend**
2. Navigate to: **Settings → Environment Variables**
3. Add a new environment variable:
   - **Name:** `VITE_API_BASE_URL`
   - **Value:** `https://api-gateway-yc8a.onrender.com`
   - **Environments:** Select all (Production, Preview, Development)
4. Click **Save**
5. Trigger a new deployment (redeploy) to apply changes

### Verification:
- The admin frontend should now connect to the API Gateway
- Test login at: https://bank-gaurd-frontend.vercel.app
- Check Browser Console (F12) for any CORS errors

---

## 2. Customer Frontend (customer-frontend) - Vercel Setup

### Environment Variable Required:
```
VITE_API_BASE_URL=https://api-gateway-yc8a.onrender.com
```

### Steps to Configure on Vercel:
1. Go to your Vercel project: **customer-frontend**
2. Navigate to: **Settings → Environment Variables**
3. Add a new environment variable:
   - **Name:** `VITE_API_BASE_URL`
   - **Value:** `https://api-gateway-yc8a.onrender.com`
   - **Environments:** Select all (Production, Preview, Development)
4. Click **Save**
5. Trigger a new deployment (redeploy) to apply changes

### Verification:
- The customer frontend should now connect to the API Gateway
- Test signup at: https://bank-gaurd-customer.vercel.app/signup
- Check Browser Console (F12) for any CORS errors

---

## 3. Backend CORS Configuration (Already Updated)

All backend services have been updated to accept CORS requests from the production Vercel domains:

✅ **API Gateway** - `SecurityConfig.java`
  - Configured with `CorsWebFilter` bean
  - Allows both Vercel production domains

✅ **Transaction Service** - `CorsConfig.java`
  - Updated to allow Vercel production domains

✅ **Alert Case Service** - `CorsConfig.java`
  - Updated to allow Vercel production domains

✅ **Enrichment Service** - `CorsConfig.java` (NEW)
  - Created with full CORS support

✅ **Decision Engine Service** - `CorsConfig.java` (NEW)
  - Created with full CORS support

✅ **SAR Report Service** - `CorsConfig.java` (NEW)
  - Created with full CORS support

All services allow:
- `https://bank-gaurd-frontend.vercel.app` (Admin)
- `https://bank-gaurd-customer.vercel.app` (Customer)
- `http://localhost:3000`, `http://localhost:5173`, `http://localhost:5174` (Local development)

---

## 4. Testing the Integration

### Admin Frontend Tests:
1. Navigate to: https://bank-gaurd-frontend.vercel.app
2. Try logging in with admin credentials
3. Verify you can access:
   - Users management
   - Transactions list
   - Investigation cases
   - SAR reports
4. Open DevTools (F12) → Console tab, verify no CORS errors

### Customer Frontend Tests:
1. Navigate to: https://bank-gaurd-customer.vercel.app
2. Try signing up with new account
3. Try logging in
4. Verify you can see:
   - Dashboard
   - Transactions
   - Profile
5. Open DevTools (F12) → Console tab, verify no CORS errors

### Browser Console - What to Look For:
**✅ Good:** No errors related to CORS or "Access-Control-Allow-Origin"
**❌ Bad:** Errors like:
- "Access to XMLHttpRequest blocked by CORS policy"
- "Response to preflight request doesn't pass access control check"

---

## 5. Troubleshooting

### Issue: Still getting CORS errors after deployment

**Solution:**
1. Hard refresh the browser (Ctrl+Shift+R on Windows, Cmd+Shift+R on Mac)
2. Clear browser cache and cookies for the domain
3. Wait 5-10 minutes for Vercel to fully deploy and cache to clear
4. Re-deploy the Vercel project manually

### Issue: Environment variable not being picked up

**Solution:**
1. Verify the environment variable name is exactly: `VITE_API_BASE_URL` (case-sensitive)
2. Check that it's selected for the correct environment (Production, Preview, Development)
3. Trigger a new deployment after adding/modifying environment variables
4. Check Vercel's deployment logs for any build errors

### Issue: API calls timing out or failing with 500 errors

**Solution:**
1. Verify all backend services are running on Render
2. Check that the API Gateway is healthy: `curl https://api-gateway-yc8a.onrender.com/health`
3. Verify the backend services have the correct CORS configuration deployed
4. Check Render service logs for any errors

### Issue: Specific API endpoint returning 401 Unauthorized

**Solution:**
1. Verify JWT token is being sent correctly in Authorization header
2. Check that the JWT secret on the gateway matches the one used to generate tokens
3. Verify token hasn't expired (default: 24 hours)
4. Check AuthContext interceptor is adding "Bearer" prefix correctly

---

## 6. Local Development (No Changes Needed)

For local development, the setup remains unchanged:
- Frontend runs on `http://localhost:5173` with Vite proxy to `http://localhost:1001` (API Gateway)
- Customer Frontend runs on `http://localhost:3000` with environment variable support
- All CORS configurations allow localhost dev ports

---

## 7. Redeploy Instructions

After any backend CORS configuration changes:

### On Render (Backend):
1. Go to your Render service
2. Click "Manual Deploy" or connect to GitHub for auto-deploy on push
3. Wait for deployment to complete
4. Monitor logs for any startup errors

### On Vercel (Frontend):
1. Go to your Vercel project
2. Go to Deployments
3. Click "Redeploy" on the latest deployment, or
4. Push new changes to trigger automatic deployment
5. Verify environment variables are set in project settings

---

## Summary of Changes Made

### Backend Changes:
- ✅ API Gateway: Added `CorsWebFilter` bean with production domains
- ✅ Transaction Service: Updated `CorsConfig.java` with production domains
- ✅ Alert Case Service: Updated `CorsConfig.java` with production domains
- ✅ Enrichment Service: Created new `CorsConfig.java`
- ✅ Decision Engine Service: Created new `CorsConfig.java`
- ✅ SAR Report Service: Created new `CorsConfig.java`

### Frontend Changes:
- Environment variables required (set on Vercel, already configured in code):
  - `VITE_API_BASE_URL` for both Admin and Customer frontends

### No changes needed for:
- Local development setup
- Vite configurations
- Frontend application code
- JWT authentication flow

---

## Support

If you continue to experience issues:
1. Check all CORS configurations are deployed to Render
2. Verify Vercel environment variables are set correctly
3. Wait for caches to clear (5-10 minutes)
4. Hard refresh browser and clear cache
5. Check browser DevTools Console for specific error messages
6. Review backend service logs on Render for detailed error information

---

**Last Updated:** June 18, 2026
**Status:** All CORS configurations deployed and production domains whitelisted
