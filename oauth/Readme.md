# OAuth 2.0 Spring Boot Demo Application

A complete OAuth 2.0 implementation using Spring Boot that demonstrates the Authorization Code flow with JWT tokens.

## Understanding OAuth 2.0 - A Beginner's Guide

### Real World Analogy: Hotel Key Card System

Imagine you're staying at a fancy hotel. This is exactly how OAuth 2.0 works:

#### The Characters

- **You** = The User
- **Hotel Front Desk** = Authorization Server (your Spring Boot app)
- **Hotel Room, Gym, Pool** = Protected Resources (API endpoints like `/api/user/profile`)
- **Your Key Card** = Access Token

#### The Story

**Step 1: You arrive at the hotel**
- You walk to the front desk and say "I'd like to access my room"
- Front desk asks: "Are you a guest here? Please show me your ID"
- This is like clicking "Start OAuth Flow" button

**Step 2: You prove who you are**
- You show your driver's license (username/password login)
- Front desk verifies: "Yes, this is Mr. User, he has a reservation"
- This is the login page where you entered `user/password`

**Step 3: Front desk asks what access you need**
- Clerk: "What areas do you need access to? Room? Gym? Pool? Restaurant?"
- You say: "Yes, I need access to all of them"
- This is the consent page where you approved scopes (read, write)

**Step 4: You get a temporary voucher**
- Clerk gives you a paper slip with a number: "Take this to the key card machine"
- This paper slip is the **Authorization Code** (that long string in your URL)
- **Important:** This paper slip is useless to anyone else and expires in 10 minutes

**Step 5: Exchange voucher for key card**
- You go to the key card machine
- Insert the paper slip + prove you're really you (client secret)
- Machine gives you an actual **Key Card (Access Token)**
- This is what the cURL command does

**Step 6: Use your key card**
- Now you can swipe your key card at your room door
- Swipe at gym door
- Swipe at pool door
- This is using the token to access `/api/user/profile` etc.

**Step 7: Key card expires**
- After checkout time (30 minutes in our app), key card stops working
- You need a new one (refresh token or login again)

---

## How This Maps to Your Application

### Step-by-Step Technical Flow

```
1. YOU CLICK: "Start OAuth Flow"
   Browser goes to: /oauth2/authorize?client_id=demo-client&...
   
2. AUTHORIZATION SERVER says: "Who are you?"
   Shows LOGIN PAGE
   You enter: user / password
   
3. SERVER verifies you're legit
   Shows CONSENT PAGE: "demo-client wants to access your data"
   You click: "Allow" (approve scopes: read, write)
   
4. SERVER generates Authorization Code
   Redirects to: /authorized?code=d1UPDnFk9W4...
   (This is the "paper voucher")
   
5. YOU EXCHANGE CODE FOR TOKEN
   POST /oauth2/token with:
   - code (the voucher)
   - client_id + client_secret (proof you're the real app)
   
   Server responds with:
   {
     "access_token": "eyJhbGc...",  <- Your key card
     "expires_in": 1800,            <- Valid for 30 minutes
     "token_type": "Bearer"
   }
   
6. USE THE TOKEN
   GET /api/user/profile
   Header: Authorization: Bearer eyJhbGc...
   
   Server checks: "Is this token valid? Does it have 'read' scope?"
   Yes! Returns your profile data
```

---

## Why So Complicated?

You might ask: "Why not just send username/password every time?"

**Security reasons:**

1. **Password Never Leaves Your Control**
   - App never sees your password
   - Only the authorization server (front desk) sees it

2. **Limited Access**
   - Token only gives specific permissions (scopes)
   - Like a key card that only opens certain doors

3. **Tokens Expire**
   - If someone steals your token, it stops working after 30 minutes
   - If someone steals your password, they have it forever

4. **Easy to Revoke**
   - You can cancel a token without changing password
   - Like deactivating a lost key card

---

## Quick Start Guide

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Google Cloud Console account (for Google Sign-In)

### Running the Application

```bash
cd oauth-demo
mvn clean install
mvn spring-boot:run
```

Open browser: `http://localhost:8080`

### Test Credentials

**User Accounts:**
- Username: `user` / Password: `password`
- Username: `admin` / Password: `admin`

**OAuth Client:**
- Client ID: `demo-client`
- Client Secret: `secret`

---

## Google Authentication Setup

### Understanding Google Sign-In

When you use "Sign in with Google", the flow is different from your own OAuth server:

**Your OAuth Server:**
- YOUR app stores and verifies passwords
- YOUR app issues tokens
- Users create accounts in YOUR system

**Google OAuth:**
- GOOGLE stores and verifies passwords
- GOOGLE issues tokens
- Users use their existing Google accounts
- Your app just receives user information from Google

### Step 1: Register Your App with Google

1. Go to Google Cloud Console: https://console.cloud.google.com/
2. Create a new project or select an existing one
3. Navigate to "APIs & Services" > "Credentials"
4. Click "Create Credentials" > "OAuth client ID"
5. Choose "Web application"
6. Add Authorized redirect URI:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
7. Click "Create" and save your credentials:
   - Client ID: `123456789-abcdefg.apps.googleusercontent.com`
   - Client Secret: `GOCSPX-xxxxxxxxxxxxx`

### Step 2: Update application.yml

Add your Google credentials to `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope:
              - openid
              - profile
              - email
```

### Step 3: Add Required Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### Step 4: Test Google Sign-In

1. Restart your application
2. Visit: `http://localhost:8080/login`
3. Click "Sign in with Google" button
4. Authenticate with your Google account
5. You'll be redirected to `/dashboard` with your Google profile

### Google Authentication Flow Explained

```
1. USER CLICKS: "Sign in with Google"
   Browser redirects to Google
   
2. GOOGLE LOGIN PAGE appears
   User enters Google email/password
   (Your app NEVER sees the password)
   
3. GOOGLE asks: "Allow demo-app to access your profile?"
   User clicks "Allow"
   
4. GOOGLE redirects back to your app
   URL: /login/oauth2/code/google?code=...
   
5. YOUR APP exchanges code with GOOGLE
   POST to: https://oauth2.googleapis.com/token
   
6. GOOGLE sends user information
   {
     "sub": "1234567890",
     "name": "John Doe",
     "email": "john@gmail.com",
     "picture": "https://..."
   }
   
7. YOUR APP creates a session
   User is now logged in with Google account
```

### Key Differences: Your OAuth vs Google OAuth

| Aspect | Your OAuth Server | Google OAuth |
|--------|-------------------|--------------|
| Who verifies password? | Your app | Google |
| Where are users stored? | Your database | Google's servers |
| Who issues tokens? | Your app | Google |
| User experience | Create new account | Use existing Google account |
| Trust model | Users trust your app | Users trust Google |

---

## Testing the Flow

### Option 1: Test Your Own OAuth Server

### Step 1: Get Authorization Code

Open in browser:
```
http://localhost:8080/oauth2/authorize?response_type=code&client_id=demo-client&redirect_uri=http://localhost:8080/authorized&scope=read%20write
```

Login and approve. You'll get redirected with a code in the URL.

### Step 2: Exchange Code for Token

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u demo-client:secret \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_CODE_FROM_STEP_1" \
  -d "redirect_uri=http://localhost:8080/authorized"
```

You'll get:
```json
{
  "access_token": "eyJraWQiOiI5ZjU2Y...",
  "token_type": "Bearer",
  "expires_in": 1799
}
```

### Step 3: Use the Token

```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### Option 2: Test Google Sign-In

### Step 1: Visit Login Page

Open in browser:
```
http://localhost:8080/login
```

### Step 2: Click "Sign in with Google"

You'll be redirected to Google's login page

### Step 3: Authenticate with Google

- Enter your Google email and password
- Click "Allow" to grant permissions

### Step 4: View Your Dashboard

After successful authentication, you'll be redirected to:
```
http://localhost:8080/dashboard
```

You'll see:
- Your Google profile picture
- Your name from Google
- Your email from Google
- All attributes Google provided

### Step 5: Access Protected Endpoints

Once authenticated with Google, you can access:

```bash
curl http://localhost:8080/api/user/info \
  -H "Cookie: JSESSIONID=YOUR_SESSION_ID"
```

Note: With Google login, you're using session-based authentication, not JWT tokens.

---

## Key Terms Explained

| Term | Simple Explanation | Hotel Analogy |
|------|-------------------|---------------|
| **Authorization Code** | Temporary voucher proving you logged in | Paper slip from front desk |
| **Access Token** | The actual key to access resources | Hotel key card |
| **Client ID/Secret** | App's credentials | Hotel knows which company issued the voucher |
| **Scopes** | What permissions you granted | Which doors the key card opens |
| **Resource Server** | The thing you want to access | Hotel room, gym, pool |
| **Authorization Server** | Where you login and get tokens | Hotel front desk |
| **OAuth Client** | Your app when using Google Sign-In | Guest using hotel services |
| **OAuth Provider** | Google, Facebook, etc. | The issuing authority |

---

## Two Authentication Methods Compared

### Method 1: Your Own OAuth Server

**Use Case:** You want other applications to access your API

**Flow:**
```
User → Your Login Page → Your Database → Your Token → Protected API
```

**Pros:**
- Full control over user data
- Custom authentication logic
- No dependency on third parties

**Cons:**
- You manage passwords securely
- Users must create new accounts
- More responsibility for security

### Method 2: Google OAuth (Social Login)

**Use Case:** Let users login with existing Google accounts

**Flow:**
```
User → Google Login → Google Verifies → Your App Gets User Info → Dashboard
```

**Pros:**
- Users don't create new passwords
- Trust Google's security
- Faster user onboarding
- Users trust Google more

**Cons:**
- Dependency on Google
- Less control over authentication
- Need internet connection

### Which One to Use?

**Use Your Own OAuth Server when:**
- Building an API for other developers
- Need full control over authentication
- Building enterprise applications
- Storing sensitive business data

**Use Google OAuth when:**
- Building consumer applications
- Want quick user onboarding
- Don't want to manage passwords
- Users already have Google accounts

**Use Both when:**
- You want flexibility
- Let users choose their login method
- Building a modern web application
- This is what your demo app does

---

## API Endpoints

### Public (No Authentication)
- `GET /api/public/hello` - Public welcome message
- `GET /api/public/info` - Application information

### User Endpoints (Requires 'read' scope)
- `GET /api/user/profile` - User profile information
- `GET /api/user/data` - User-specific data

### Admin Endpoints (Requires 'write' scope)
- `GET /api/admin/dashboard` - Admin dashboard
- `POST /api/admin/action` - Execute admin actions

---

## Troubleshooting

### Your Own OAuth Server

**Problem: Authorization code expired**
- Codes expire after 5 minutes
- Solution: Start a new OAuth flow

**Problem: 401 Unauthorized**
- Check token is in header: `Authorization: Bearer YOUR_TOKEN`
- Check token hasn't expired (30 minutes lifetime)

**Problem: 403 Forbidden**
- Token is valid but lacks required scope
- User endpoints need 'read' scope
- Admin endpoints need 'write' scope

### Google OAuth Integration

**Problem: Redirect URI mismatch**
- Error: `redirect_uri_mismatch`
- Solution: Ensure the redirect URI in Google Console exactly matches:
  ```
  http://localhost:8080/login/oauth2/code/google
  ```

**Problem: Invalid client error**
- Check your `client-id` and `client-secret` in application.yml
- Make sure you copied them correctly from Google Console
- No extra spaces or quotes

**Problem: Circular view path [login]**
- This means Thymeleaf is looking for a template
- Solution: Use the controller that returns HTML directly (see code above)
- Make sure your controller uses `HttpServletResponse` to write HTML

**Problem: After Google login, redirect to error page**
- Check Security Configuration allows `/login/oauth2/code/google`
- Verify OAuth2 client dependency is in pom.xml
- Check application.yml has correct Google configuration

**Problem: Session expired after logout**
- This is normal behavior
- User needs to login again
- Configure session timeout in application.yml if needed:
  ```yaml
  server:
    servlet:
      session:
        timeout: 30m
  ```

### General Issues

**Problem: Port 8080 already in use**
- Change port in application.yml:
  ```yaml
  server:
    port: 8081
  ```
- Remember to update redirect URIs accordingly

**Problem: Application won't start**
- Check all dependencies are in pom.xml
- Run `mvn clean install`
- Check Java version is 17 or higher
- Look for error messages in console

**Problem: Google Sign-In button doesn't redirect**
- Check browser console for JavaScript errors
- Verify the link is `/oauth2/authorization/google`
- Check Spring Security configuration allows the endpoint