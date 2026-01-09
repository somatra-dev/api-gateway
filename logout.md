Current Logout Behavior

1. ✅ Clears Gateway session - OAuth2 tokens are invalidated
2. ✅ Redirects to frontend - User sees logged out state
3. ⚠️ Auth Server session remains - If user logs in again within Auth Server session timeout, they won't need to re-enter credentials

Why OIDC Logout Failed

Spring Authorization Server's OIDC logout requires complex session registry configuration that tracks:
- Session ID → User mapping
- Client → Session mapping

This is typically needed for:
- Back-channel logout (server-to-server)
- Front-channel logout (multiple apps)

For Your BFF Pattern

Simple logout is sufficient because:
- Gateway session controls access to your app
- When Gateway session is cleared, user must re-authenticate
- Even if Auth Server session exists, user goes through OAuth2 flow again

If you need full OIDC logout later, it requires additional session management configuration in the Authorization Server.


---
CSRF (Cross-Site Request Forgery) protection prevents attackers from tricking authenticated users into performing unwanted actions on a web application.

How CSRF Attacks Work

1. User logs into a trusted site (e.g., their bank)
2. User visits a malicious site while still logged in
3. Malicious site sends a hidden request to the trusted site using the user's session
4. The trusted site processes the request as if the user intended it

Benefits of CSRF Protection

Prevents unauthorized state changes - Attackers can't force users to:
- Transfer money
- Change email/password
- Make purchases
- Delete accounts
- Modify settings

Session integrity - Ensures requests originate from your application, not external sources

Defense in depth - Works alongside authentication to verify request legitimacy

Common Protection Methods

| Method               | Description                                                                        |
|----------------------|------------------------------------------------------------------------------------|
| Synchronizer Token   | Server generates a unique token per session/request that must be included in forms |
| Double Submit Cookie | Token sent in both cookie and request body; server verifies they match             |
| SameSite Cookies     | Browser attribute preventing cookies from being sent with cross-origin requests    |
| Custom Headers       | Requiring headers like X-Requested-With that can't be set cross-origin             |

When You Need It

CSRF protection is essential for any endpoint that:
- Modifies data (POST, PUT, DELETE)
- Relies on cookie-based authentication
- Performs sensitive operations

GET requests should be idempotent and not require CSRF protection.
