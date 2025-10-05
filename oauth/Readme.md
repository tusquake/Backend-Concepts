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

## Testing the Flow

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

## Key Terms Explained

| Term | Simple Explanation | Hotel Analogy |
|------|-------------------|---------------|
| **Authorization Code** | Temporary voucher proving you logged in | Paper slip from front desk |
| **Access Token** | The actual key to access resources | Hotel key card |
| **Client ID/Secret** | App's credentials | Hotel knows which company issued the voucher |
| **Scopes** | What permissions you granted | Which doors the key card opens |
| **Resource Server** | The thing you want to access | Hotel room, gym, pool |
| **Authorization Server** | Where you login and get tokens | Hotel front desk |

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