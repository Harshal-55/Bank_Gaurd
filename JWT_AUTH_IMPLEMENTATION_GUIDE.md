# BankGuard JWT Authentication and CORS Fix - Implementation Guide

## Overview
This document outlines the complete JWT authentication system implementation for BankGuard, including fixes for both Admin and Customer frontends to properly authenticate through the API Gateway with JWT tokens.

---

## 🔑 Key Changes Implemented

### 1. **Backend - API Gateway Changes**

#### User Entity Extended
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/model/User.java`

Added customer-specific fields to the User entity:
```java
@Column("email")
private String email;

@Column("bank_name")
private String bankName;

@Column("account_no")
private String accountNo;

@Column("account_type")
private String accountType;

@Column("balance")
private Double balance;

@Column("risk_score")
private Double riskScore;
```

This allows storing both admin users and customer users in the same `users` table, identified by their `role` field.

#### New DTOs Created

**CustomerSignupRequest.java** - For customer registration
```java
{
  "email": "customer@bank.com",
  "password": "secure_password",
  "name": "John Doe",
  "bankName": "HDFC Bank",
  "accountNo": "12345678",
  "accountType": "Savings",
  "balance": 50000.00
}
```

**CustomerAuthResponse.java** - Response with JWT token and customer data
```java
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "customer@bank.com",
  "username": "customer@bank.com",
  "bankName": "HDFC Bank",
  "accountNo": "12345678",
  "balance": 50000.00,
  "riskScore": 0.0,
  "role": "CUSTOMER",
  "message": "Customer account created successfully"
}
```

#### AuthController Updates
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/controller/AuthController.java`

Two new endpoints added:

1. **POST /auth/customer/signup** (Public)
   - Creates customer account in auth database with CUSTOMER role
   - Hashes password with BCrypt
   - Issues JWT token immediately
   - No approval needed (auto-approved)

2. **POST /auth/customer/login** (Public)
   - Authenticates customer by email
   - Validates BCrypt hashed password
   - Issues JWT token with CUSTOMER role
   - Returns full customer data

#### JWT Filter Update
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/security/JwtAuthenticationFilter.java`

Added public endpoints:
- `/auth/customer/signup` - No JWT required
- `/auth/customer/login` - No JWT required

#### Security Config Update
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/config/SecurityConfig.java`

Role-based authorization rules added:
```java
// Customer public endpoints
.pathMatchers(HttpMethod.POST, "/auth/customer/signup").permitAll()
.pathMatchers(HttpMethod.POST, "/auth/customer/login").permitAll()

// Customer API endpoints - require CUSTOMER role
.pathMatchers(HttpMethod.GET, "/api/customers/**").hasRole("CUSTOMER")
.pathMatchers(HttpMethod.POST, "/api/customers/**").hasRole("CUSTOMER")
.pathMatchers(HttpMethod.PUT, "/api/customers/**").hasRole("CUSTOMER")

// Transaction customer endpoints - require CUSTOMER role
.pathMatchers(HttpMethod.GET, "/api/transactions/customer/**").hasRole("CUSTOMER")
.pathMatchers(HttpMethod.POST, "/api/transactions/customer/**").hasRole("CUSTOMER")
```

#### CORS Configuration Added
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/config/CorsConfig.java`

**CRITICAL:** `allowCredentials: true` is set to allow JWT tokens to be sent with credentials.

```java
config.setAllowCredentials(true);

// Dev URLs
config.addAllowedOrigin("http://localhost:3000");   // customer-frontend
config.addAllowedOrigin("http://localhost:5173");   // admin frontend
config.addAllowedOrigin("http://localhost:5174");   // legacy

// Production URLs
config.addAllowedOrigin("https://bank-gaurd-frontend.vercel.app");
config.addAllowedOrigin("https://bank-gaurd-customer-frontend.vercel.app");

config.addAllowedHeader("*");
config.addAllowedMethod("*");
```

#### UserRepository Updated
**File:** `Backend/apiGateway/src/main/java/com/cts/apiGateway/repository/UserRepository.java`

Added method:
```java
Mono<User> findByEmail(String email);
```

---

### 2. **Frontend - Customer Frontend Changes**

#### Updated Auth Context
**File:** `customer-frontend/src/auth/AuthContext.jsx`

**Before:** Stored full customer object in localStorage
**After:** Stores JWT token and customer metadata

```javascript
// Storage keys
STORAGE_KEYS.TOKEN              // JWT token
STORAGE_KEYS.CUSTOMER_EMAIL     // Customer email
STORAGE_KEYS.CUSTOMER_ROLE      // Role (always "CUSTOMER")
```

**API Endpoint Changes:**
- Signup: POST `/auth/customer/signup` (was `/api/customers`)
- Login: POST `/auth/customer/login` (was `/api/customers/login`)

#### New API Service
**File:** `customer-frontend/src/services/api.js`

Provides authenticated API calls with JWT token:

```javascript
// Each request automatically includes Bearer token
api.call(url, options)  // Generic call
api.get(endpoint)        // GET request
api.post(endpoint, data) // POST request
api.put(endpoint, data)  // PUT request
api.delete(endpoint)     // DELETE request

// On 401 response → auto logout and redirect to login
```

#### Component Updates

**TransactionsPage.jsx**
- Updated to use `apiGet()` instead of raw `fetch()`
- Automatically includes JWT token in Authorization header

**ProfilePage.jsx**
- Updated to use `apiGet()` for customer profile fetch
- Endpoint changed to use API Gateway route

**PayPage.jsx**
- Updated to use `apiPost()` for transaction creation
- Changed `customerId` to `customerEmail` in payload
- Automatically includes JWT token

---

### 3. **Database Schema Changes**

#### Users Table (auth database)

Add these columns to support customers:

```sql
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN bank_name VARCHAR(255);
ALTER TABLE users ADD COLUMN account_no VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN account_type VARCHAR(255);
ALTER TABLE users ADD COLUMN balance DECIMAL(18,2) DEFAULT 0;
ALTER TABLE users ADD COLUMN risk_score DECIMAL(5,2) DEFAULT 0;
```

---

## 🔄 Authentication Flow Comparison

### Admin Authentication Flow (Unchanged)
```
┌─────────────────────────────────────────┐
│ Admin Frontend                           │
│ (frontend)                               │
└────────────────┬──────────────────────┘
                 │
                 ├─→ POST /auth/login
                 │   {"username": "...", "password": "..."}
                 │
                 ▼
┌─────────────────────────────────────────┐
│ API Gateway                              │
│ AuthController                           │
│ - Verify password                        │
│ - Check isApproved status                │
│ - Generate JWT (role: SUPER_ADMIN)       │
└────────────────┬──────────────────────┘
                 │
                 ├─→ Response: JWT token + username + role
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Frontend stores:                         │
│ - auth_token: JWT                        │
│ - auth_role: SUPER_ADMIN                 │
│ - auth_username: username                │
│                                          │
│ All subsequent requests include:         │
│ Authorization: Bearer <jwt>              │
└─────────────────────────────────────────┘
```

### Customer Authentication Flow (NEW)
```
┌─────────────────────────────────────────┐
│ Customer Frontend                        │
│ (customer-frontend)                      │
└────────────────┬──────────────────────┘
                 │
                 ├─→ POST /auth/customer/signup
                 │   {
                 │     "email": "...",
                 │     "password": "...",
                 │     "bankName": "...",
                 │     "accountNo": "...",
                 │     "balance": 50000,
                 │     ...
                 │   }
                 │
                 ▼
┌─────────────────────────────────────────┐
│ API Gateway                              │
│ AuthController                           │
│ - Check email uniqueness                 │
│ - Hash password with BCrypt              │
│ - Save to users table with role:CUSTOMER │
│ - Generate JWT (role: CUSTOMER)          │
│ - Auto-approve customer                  │
└────────────────┬──────────────────────┘
                 │
                 ├─→ Response: JWT token + customer data
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Frontend stores:                         │
│ - customer_auth_token: JWT               │
│ - customer_email: email@bank.com         │
│ - customer_role: CUSTOMER                │
│                                          │
│ All subsequent requests include:         │
│ Authorization: Bearer <jwt>              │
│ credentials: 'include'                   │
└─────────────────────────────────────────┘
```

---

## 🔐 Key Security Features

### 1. **JWT Configuration**
- Algorithm: HS256 (HMAC SHA-256)
- Expiration: 24 hours (86400000 ms)
- Secret: `BankGuardSuperSecretKeyForJWT2025ThisMustBeLongEnoughForHS256Algorithm`
- Includes role claim for authorization checks

### 2. **Password Security**
- **Admin Passwords:** BCrypt hashed (always)
- **Customer Passwords:** BCrypt hashed (NEW - was plain text before)
- On login: `passwordEncoder.matches()` validates hash

### 3. **CORS Security**
- `allowCredentials: true` - Required for JWT with credentials
- Specific origin whitelisting (no wildcard `*`)
- Dev: localhost:3000, 5173, 5174
- Production: Vercel deployments only

### 4. **Role-Based Authorization**
- Admin: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER
- Customer: CUSTOMER (new)
- Gateway enforces role-based access on all protected endpoints

---

## 🧪 Testing Endpoints

### Customer Signup
```bash
curl -X POST http://localhost:1001/auth/customer/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass@123",
    "name": "John Doe",
    "bankName": "HDFC Bank",
    "accountNo": "1234567890",
    "accountType": "Savings",
    "balance": 50000.00
  }'
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "username": "john@example.com",
  "bankName": "HDFC Bank",
  "accountNo": "1234567890",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0,
  "role": "CUSTOMER",
  "message": "Customer account created successfully"
}
```

### Customer Login
```bash
curl -X POST http://localhost:1001/auth/customer/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john@example.com",
    "password": "SecurePass@123"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "username": "john@example.com",
  "bankName": "HDFC Bank",
  "accountNo": "1234567890",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0,
  "role": "CUSTOMER",
  "message": "Login successful"
}
```

### Access Protected Endpoint (with JWT)
```bash
curl -X GET http://localhost:1001/api/customers/john@example.com \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

**Response (200 OK):** Customer data

### Access Protected Endpoint (without JWT)
```bash
curl -X GET http://localhost:1001/api/customers/john@example.com
```

**Response (401 Unauthorized):** Authentication required

---

## 🚀 Deployment Checklist

### Backend

- [ ] Update database schema (add columns to users table)
- [ ] Deploy updated apiGateway service with new endpoints and CORS config
- [ ] Verify JWT secret is set in config server
- [ ] Test customer signup/login endpoints locally

### Frontend (Customer)

- [ ] Deploy updated customer-frontend with JWT auth
- [ ] Verify AuthContext changes
- [ ] Test signup flow
- [ ] Test login flow
- [ ] Test protected endpoints (transactions, profile)

### Frontend (Admin)

- [ ] Verify no breaking changes
- [ ] Test admin login still works
- [ ] Test JWT token is properly attached to requests

### All Services

- [ ] Verify CORS configurations have `allowCredentials: true`
- [ ] Test preflight requests (OPTIONS) return 200
- [ ] Test requests with Authorization headers work
- [ ] Monitor 401/403 errors in logs

---

## 🐛 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| CORS errors in browser | allowCredentials not set | Ensure `config.setAllowCredentials(true)` in CORS config |
| "No 'Access-Control-Allow-Credentials'" | Origin not whitelisted | Add origin to corsConfiguration.addAllowedOrigin() |
| JWT token not included in request | Missing Authorization header | Check frontend includes `Authorization: Bearer <token>` |
| 401 Unauthorized on protected endpoint | Expired or invalid token | User must re-login to get new token |
| Preflight request fails | OPTIONS not allowed | Ensure `.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()` |
| Plain text passwords in logs | Passwords not hashed | Verify BCryptPasswordEncoder is used on signup/login |

---

## 📋 Migration Guide for Existing Customers

Existing customers in the **transactionService** database do NOT have entries in the **auth database**. 

**Two options:**
1. **Immediate:** Customers must signup again on the new system
2. **Migration:** Create database script to migrate existing customers to auth database with hashed passwords

Example migration SQL:
```sql
INSERT INTO bankguard_auth.users 
(username, password, email, role, is_approved, bank_name, account_no, account_type, balance, risk_score)
SELECT 
  email,
  CONCAT('$2a$10$', MD5(password)), -- Placeholder hash (CHANGE THIS)
  email,
  'CUSTOMER',
  1,
  bank_name,
  account_no,
  account_type,
  balance,
  risk_score
FROM bankguard_db.customers;
```

⚠️ **WARNING:** Migrate passwords properly using BCrypt, not placeholders!

---

## 📖 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend Layer                                               │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  Admin Frontend              Customer Frontend                │
│  - JWT token auth            - JWT token auth                 │
│  - axios + interceptor       - api service wrapper            │
│  - Role-based routes         - Credentials in fetch            │
│                                                               │
└────────────────┬────────────────────────────┬────────────────┘
                 │                            │
                 │ Authorization: Bearer      │ Authorization: Bearer
                 │ Content-Type: application  │ Content-Type: application
                 │                            │
┌────────────────┴────────────────────────────┴────────────────┐
│ API Gateway (apiGateway - Port 1001)                         │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  JwtAuthenticationFilter                                      │
│  ├─ Validates Bearer token                                    │
│  ├─ Extracts username + role                                  │
│  └─ Sets security context                                     │
│                                                               │
│  SecurityConfig (Authorization)                               │
│  ├─ Checks user has required role                             │
│  └─ Grants access if authorized                               │
│                                                               │
│  AuthController                                               │
│  ├─ POST /auth/login          (Admin login)                   │
│  ├─ POST /auth/register       (Admin register)                │
│  ├─ POST /auth/customer/login (Customer login) ← NEW          │
│  └─ POST /auth/customer/signup(Customer signup) ← NEW         │
│                                                               │
│  CORS Filter                                                  │
│  └─ allowCredentials: true (allows JWT with CORS)             │
│                                                               │
└────────────────┬────────────────────────────────────────────┘
                 │
    ┌────────────┴──────────┬──────────────┬──────────────┐
    │                       │              │              │
    ▼                       ▼              ▼              ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────┐
│ Transaction  │  │ Enrichment   │  │ Alert Case   │  │  Other   │
│  Service     │  │   Service    │  │   Service    │  │ Services │
│              │  │              │  │              │  │          │
│ CORS: ✓      │  │ CORS: ✓      │  │ CORS: ✓      │  │          │
│ Credentials  │  │ Credentials  │  │ Credentials  │  │          │
│              │  │              │  │              │  │          │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────┘

┌──────────────────────────────────────────────────────────────┐
│ Data Layer                                                    │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  bankguard_auth DB      bankguard_db          Other DBs       │
│  ├─ users (Admin        ├─ customers          ├─ ...          │
│  │  + Customer)         ├─ transactions       │                │
│  └─ ...                 └─ ...                └─ ...           │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## ✅ Verification Steps

After deployment, run these tests to verify everything works:

### 1. Customer Signup
- [ ] POST /auth/customer/signup returns 201
- [ ] Token is issued
- [ ] User can login with created credentials

### 2. Customer Login
- [ ] POST /auth/customer/login returns 200 with token
- [ ] Token is valid JWT
- [ ] Token contains role: CUSTOMER

### 3. Protected Endpoints
- [ ] GET /api/customers/{id} with token returns 200
- [ ] GET /api/customers/{id} without token returns 401
- [ ] GET /api/transactions/customer/{id} with token returns 200

### 4. CORS Preflight
- [ ] OPTIONS requests return 200
- [ ] Cross-origin requests from allowed origins work
- [ ] Cross-origin requests from disallowed origins fail

### 5. Admin Functionality
- [ ] Admin login still works
- [ ] Admin can access protected admin endpoints
- [ ] Admin JWT tokens still valid

---

## 📚 References

- JWT Token: HS256 algorithm with 24-hour expiration
- Security Filter Chain: JwtAuthenticationFilter → SecurityConfig authorization rules
- CORS Handling: Spring CorsFilter with allowCredentials=true
- Password Encoding: BCryptPasswordEncoder (10 rounds)

