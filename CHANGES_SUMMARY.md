# BankGuard JWT Authentication Fix - Summary of Changes

## 📝 Files Created

### Backend (API Gateway)
1. **new:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/dto/CustomerSignupRequest.java`
   - DTO for customer signup request with email, password, bank details

2. **new:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/dto/CustomerAuthResponse.java`
   - DTO for customer auth response including JWT token and customer data

3. **new:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/config/CorsConfig.java`
   - CORS configuration bean with allowCredentials: true
   - Whitelisted dev and production origins

### Frontend (Customer)
4. **new:** `customer-frontend/src/services/api.js`
   - JWT-based API client that automatically attaches Bearer token to all requests
   - Handles 401 responses with automatic logout

---

## 📝 Files Modified

### Backend

1. **modified:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/model/User.java`
   - Added customer-specific fields: email, bankName, accountNo, accountType, balance, riskScore
   - Now supports both admin users and customer users in single table

2. **modified:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/controller/AuthController.java`
   - Added imports for new DTOs
   - Added `POST /auth/customer/signup` endpoint (public)
   - Added `POST /auth/customer/login` endpoint (public)
   - Customer signup auto-hashes password and issues JWT immediately
   - Customer login validates email and password, issues JWT

3. **modified:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/security/JwtAuthenticationFilter.java`
   - Added `/auth/customer/login` to public endpoints
   - Added `/auth/customer/signup` to public endpoints

4. **modified:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/config/SecurityConfig.java`
   - Added CorsConfig injection
   - Added `/auth/customer/signup` permitAll()
   - Added `/auth/customer/login` permitAll()
   - Changed `/api/customers/**` from permitAll() to hasRole("CUSTOMER")
   - Changed `/api/transactions/customer/**` to hasRole("CUSTOMER")

5. **modified:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/repository/UserRepository.java`
   - Added `Mono<User> findByEmail(String email)` method

### Frontend (Customer)

6. **modified:** `customer-frontend/src/auth/AuthContext.jsx`
   - Changed from storing full customer object to storing JWT token
   - Updated STORAGE_KEYS: TOKEN, CUSTOMER_EMAIL, CUSTOMER_ROLE
   - Updated API endpoints: `/auth/customer/signup` and `/auth/customer/login`
   - Added JWT token management functions: `getToken()`
   - Updated signup() and login() to store JWT token instead of customer object
   - Added error handling and loading states

7. **modified:** `customer-frontend/src/customer/TransactionsPage.jsx`
   - Changed from raw `fetch()` to `apiGet()` from new API service
   - Now uses API Gateway route with JWT authentication

8. **modified:** `customer-frontend/src/customer/ProfilePage.jsx`
   - Changed from raw `fetch()` to `apiGet()` from new API service
   - Now uses API Gateway route with JWT authentication

9. **modified:** `customer-frontend/src/customer/PayPage.jsx`
   - Changed from raw `fetch()` to `apiPost()` from new API service
   - Updated to use `customerEmail` instead of `customerId` for identifying customer
   - Now includes JWT token in all requests

---

## 🗄️ Database Changes Required

### bankguard_auth Database (Users Table)

Add these columns:
```sql
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN bank_name VARCHAR(255);
ALTER TABLE users ADD COLUMN account_no VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN account_type VARCHAR(255);
ALTER TABLE users ADD COLUMN balance DECIMAL(18,2) DEFAULT 0;
ALTER TABLE users ADD COLUMN risk_score DECIMAL(5,2) DEFAULT 0;
```

---

## 🔄 API Endpoint Changes

### New Public Endpoints (No JWT Required)

1. **POST /auth/customer/signup**
   - Request: `{ email, password, name, bankName, accountNo, accountType, balance }`
   - Response: `{ token, email, username, role: "CUSTOMER", ... }`
   - Status: 201 Created

2. **POST /auth/customer/login**
   - Request: `{ username: email, password }`
   - Response: `{ token, email, username, role: "CUSTOMER", ... }`
   - Status: 200 OK

### Modified Protected Endpoints (Now Require JWT)

1. **GET/POST /api/customers/** → Now requires CUSTOMER role
   - Was: permitAll()
   - Now: hasRole("CUSTOMER")

2. **GET/POST /api/transactions/customer/** → Now requires CUSTOMER role
   - Was: permitAll()
   - Now: hasRole("CUSTOMER")

---

## 🔐 Authentication Flow Changes

### Before
```
Customer Frontend (dev: localhost:3000)
    ↓
POST /api/customers/login (direct to transactionService)
    ↓
Response: Full customer object
    ↓
localStorage: { vaultx_customer: {...} }
    ↓
All requests: No Authorization header (public endpoints)
```

### After
```
Customer Frontend (dev: localhost:3000)
    ↓
POST /auth/customer/login (API Gateway)
    ↓
Response: JWT token + customer data
    ↓
localStorage: { customer_auth_token: "eyJ...", customer_email: "...", customer_role: "CUSTOMER" }
    ↓
All requests: Authorization: Bearer eyJ...
    ↓
API Gateway validates JWT, extracts role
    ↓
Route to service if authorized
```

---

## 🧪 Quick Testing Checklist

### Customer Signup
```bash
curl -X POST http://localhost:1001/auth/customer/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@bank.com","password":"Pass@123","name":"Test User","bankName":"HDFC","accountNo":"123456","accountType":"Savings","balance":50000}'

# Expected: 201 Created with JWT token
```

### Customer Login
```bash
curl -X POST http://localhost:1001/auth/customer/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@bank.com","password":"Pass@123"}'

# Expected: 200 OK with JWT token
```

### Protected Endpoint (With Token)
```bash
curl -X GET http://localhost:1001/api/customers/test@bank.com \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json"

# Expected: 200 OK with customer data
```

### Protected Endpoint (Without Token)
```bash
curl -X GET http://localhost:1001/api/customers/test@bank.com

# Expected: 401 Unauthorized
```

---

## 🚀 Deployment Steps

### 1. Backend Deployment

1. Update `bankguard_auth` database schema (add customer columns to users table)
2. Build and deploy `apiGateway` service
   ```bash
   cd Backend/apiGateway
   ./mvnw clean package
   docker build -t api-gateway:latest .
   docker push api-gateway:latest
   ```
3. Verify JWT config in Config Server:
   - `jwt.secret`: Set in environment or config server YAML
   - `jwt.expiration`: 86400000 (24 hours)

### 2. Frontend Deployment (Customer)

1. Build and deploy `customer-frontend`
   ```bash
   cd customer-frontend
   npm install
   npm run build
   npm run preview  # Test locally first
   ```
2. Deploy to Vercel/hosting
3. Ensure environment variable is set:
   - `VITE_API_BASE_URL`: https://api-gateway-yc8a.onrender.com

### 3. Verification

1. Test customer signup flow
2. Test customer login flow
3. Test protected endpoints with JWT
4. Test CORS preflight requests
5. Check browser console for 401/403 errors
6. Monitor backend logs for failed authentications

---

## ⚠️ Important Notes

### Security
- Passwords are now BCrypt hashed for customers (was plain text before)
- JWT tokens include role claim for authorization checks
- CORS allowCredentials must be true for JWT to work

### Backward Compatibility
- Existing customer accounts in `transactionService` will NOT automatically work
- Options:
  1. Customers signup again on new system
  2. Create database migration script to move existing customers to auth database

### Configuration
- API Gateway must have CORS config with `allowCredentials: true`
- All backend services must have same CORS config
- JWT secret and expiration must be consistent across services

---

## 📋 Rollback Plan

If issues occur:

1. **Revert database** - No data was deleted, just columns added (safe to keep)
2. **Revert apiGateway** - Deploy previous version that didn't have customer auth endpoints
3. **Revert customer frontend** - Deploy previous version that used direct API calls
4. **Clear browser cache** - localStorage needs to be cleared for old format

---

## 📚 Reference Files

- Implementation Guide: `JWT_AUTH_IMPLEMENTATION_GUIDE.md`
- Admin Frontend Example: `frontend/src/services/api.config.jsx` (reference for JWT interceptor)
- Admin Frontend Auth: `frontend/src/context/AuthContext.jsx` (reference for JWT flow)

---

## ✅ Sign-Off Checklist

- [ ] All files reviewed and deployed
- [ ] Database migration executed
- [ ] API Gateway running with new endpoints
- [ ] Customer frontend using JWT auth
- [ ] CORS configuration verified across services
- [ ] Customer signup works end-to-end
- [ ] Customer login works end-to-end
- [ ] Admin authentication still works
- [ ] Protected endpoints properly enforced
- [ ] 401/403 errors properly returned
- [ ] CORS preflight requests working
- [ ] Production URLs whitelisted in CORS
- [ ] No plain-text passwords in logs
- [ ] JWT tokens properly validated
- [ ] Monitoring alerts configured

