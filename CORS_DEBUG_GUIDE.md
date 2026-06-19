# CORS Debugging & Verification Guide

## ✅ **What Was Fixed**

1. **WebFluxCorsConfig.java** - Created proper WebFlux-compatible CORS bean
2. **SecurityConfig.java** - Updated to use explicit CORS configuration source
3. **Origin URLs** - Corrected to match deployed frontends:
   - `https://bank-gaurd-frontend.vercel.app` ✅
   - `https://bank-gaurd-customer.vercel.app` ✅ (was missing "frontend")

---

## 🧪 **Testing the Fix**

### Step 1: Rebuild and Deploy API Gateway

```bash
cd Backend/apiGateway
./mvnw clean package -DskipTests
docker build -t api-gateway:latest .
docker push api-gateway:latest
# Deploy to Render
```

### Step 2: Test Preflight Request (OPTIONS)

```bash
# Test CORS preflight
curl -X OPTIONS https://api-gateway-yc8a.onrender.com/auth/customer/login \
  -H "Origin: https://bank-gaurd-customer.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

**Expected response headers:**
```
< HTTP/1.1 200 OK
< Access-Control-Allow-Origin: https://bank-gaurd-customer.vercel.app
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
< Access-Control-Allow-Headers: *
< Access-Control-Allow-Credentials: true
```

### Step 3: Test Signup (with CORS)

```bash
curl -X POST https://api-gateway-yc8a.onrender.com/auth/customer/signup \
  -H "Origin: https://bank-gaurd-customer.vercel.app" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email":"test@bank.com",
    "password":"SecurePass@123",
    "name":"Test User",
    "bankName":"HDFC",
    "accountNo":"1234567890",
    "accountType":"Savings",
    "balance":50000
  }' \
  -v
```

**Expected:**
- Status: **201 Created**
- Body: JWT token + customer data
- CORS headers in response

---

## 🔍 **Browser Developer Tools Testing**

### Customer Frontend Login Test

1. Open browser → `https://bank-gaurd-customer.vercel.app/login`
2. Open **Developer Tools** → **Network** tab
3. Enter credentials and submit
4. Check the **OPTIONS** preflight request:
   - Should show **200 OK** (green)
   - Check **Response Headers** for:
     ```
     Access-Control-Allow-Origin: https://bank-gaurd-customer.vercel.app
     Access-Control-Allow-Credentials: true
     ```
5. Check the **POST** request:
   - Should show **200 OK** with JWT token in response
   - Request should include `Content-Type: application/json`
   - Response should have JWT token

### Admin Frontend Login Test

1. Open browser → `https://bank-gaurd-frontend.vercel.app/login`
2. Open **Developer Tools** → **Network** tab
3. Enter admin credentials (if available)
4. Check preflight and POST requests same as above
5. Origin should be: `https://bank-gaurd-frontend.vercel.app`

---

## 🐛 **Common Issues & Solutions**

| Issue | Check | Fix |
|-------|-------|-----|
| **403 Forbidden** | JWT validation | Check that endpoints are in `.permitAll()` list |
| **CORS blocked** | Browser console | Check origin URL matches exactly |
| **OPTIONS 404** | Spring logs | All endpoints should allow OPTIONS |
| **No CORS headers** | Response headers | Check `CorsConfigurationSource` is injected |
| **421 Misdirected** | URL format | Check HTTPS is used for production |

---

## 📋 **Required Config Server Changes**

**Update** `https://github.com/Harshal-55/Config-Server_Bankguard/apiGateway-dev.yaml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://thomas.proxy.rlwy.net:40415/bankguard_auth
    username: root
    password: ${MYSQL_PASSWORD}

  cloud:
    gateway:
      globalcors:
        add-to-simple-url-handler-mapping: true
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:5173"
              - "http://localhost:5174"
              - "https://bank-gaurd-frontend.vercel.app"
              - "https://bank-gaurd-customer.vercel.app"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders:
              - "Content-Type"
              - "Authorization"
              - "*"
            exposedHeaders:
              - "Content-Type"
              - "Authorization"
            allowCredentials: true
            maxAge: 3600

eureka:
  client:
    service-url:
      defaultZone: https://eureka-server-ku09.onrender.com/eureka/
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true

jwt:
  secret: BankGuardSuperSecretKeyForJWT2025ThisMustBeLongEnoughForHS256Algorithm
  expiration: 86400000

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web.cors: DEBUG
    reactor.netty.http.client: INFO
```

---

## 🔧 **Debug Logging**

Add this to check what's happening:

**application-docker.properties:**
```properties
logging.level.org.springframework.web.cors=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.cloud.gateway=DEBUG
```

Then check logs for CORS processing messages.

---

## ✅ **Verification Checklist**

- [ ] API Gateway deployed with new CORS config
- [ ] Config Server updated with correct origin URLs
- [ ] Customer frontend can perform preflight (OPTIONS) request
- [ ] Customer frontend can login and get JWT token
- [ ] Admin frontend can perform preflight request
- [ ] Admin frontend can login and get JWT token
- [ ] Browser Network tab shows 200 OK for OPTIONS requests
- [ ] JWT token is returned in response body
- [ ] localStorage stores JWT token correctly
- [ ] Subsequent requests include `Authorization: Bearer <token>` header
- [ ] Protected endpoints return 200 with JWT, 401 without JWT

---

## 📊 **Request/Response Flow**

```
Browser → OPTIONS request
  ↓
API Gateway CORS filter
  ↓
Return 200 with CORS headers
  ↓
Browser → POST request with payload
  ↓
JwtAuthenticationFilter (skipped for public endpoints)
  ↓
AuthController.customerLogin()
  ↓
Generate JWT token
  ↓
Return 200 with JWT in response body
  ↓
Browser stores JWT in localStorage
  ↓
Subsequent requests include: Authorization: Bearer <token>
```

---

## 🚀 **Deployment Steps**

1. **Rebuild API Gateway** with new CORS config
2. **Update Config Server** with corrected YAML
3. **Restart API Gateway** service on Render
4. **Clear browser cache** (Ctrl+Shift+Delete)
5. **Test customer signup/login** from browser
6. **Test admin login** from browser
7. **Monitor logs** for CORS debug messages

---

## 📞 **If Still Issues**

Check these logs on Render:

```
org.springframework.web.cors - Check for "CORS request"
org.springframework.security - Check for JWT validation
com.cts.apiGateway - Check for routing
```

Look for:
- ✅ "CORS request allowed"
- ✅ "Token validated successfully"
- ❌ "CORS request rejected" (wrong origin)
- ❌ "Token validation failed" (expired or invalid)

