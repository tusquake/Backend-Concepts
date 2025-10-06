package com.example.oauth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Controller
public class GoogleAuthController {

    @GetMapping("/login")
    public void loginPage(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().write(generateLoginHtml());
    }

    @GetMapping("/dashboard")
    @ResponseBody
    public String dashboard(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "Not authenticated";
        }

        String name = principal.getAttribute("name");
        String email = principal.getAttribute("email");
        String picture = principal.getAttribute("picture");

        return generateDashboardHtml(name, email, picture, principal.getAttributes());
    }

    @GetMapping("/api/user/info")
    @ResponseBody
    public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes();
    }

    private String generateLoginHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .login-container {
            background: white;
            padding: 40px;
            border-radius: 10px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            text-align: center;
            max-width: 400px;
            width: 90%;
        }
        h1 {
            color: #333;
            margin-bottom: 30px;
        }
        .login-options {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }
        .google-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            background: white;
            color: #444;
            border: 2px solid #ddd;
            padding: 12px 24px;
            border-radius: 4px;
            font-size: 16px;
            font-weight: 500;
            cursor: pointer;
            text-decoration: none;
            transition: all 0.3s;
        }
        .google-btn:hover {
            background: #f8f8f8;
            border-color: #999;
        }
        .google-icon {
            width: 20px;
            height: 20px;
        }
        .divider {
            margin: 20px 0;
            color: #999;
        }
        .form-login {
            display: flex;
            flex-direction: column;
            gap: 15px;
            margin-top: 20px;
        }
        input {
            padding: 12px;
            border: 2px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
        }
        .submit-btn {
            background: #667eea;
            color: white;
            border: none;
            padding: 12px;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
            transition: background 0.3s;
        }
        .submit-btn:hover {
            background: #5568d3;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <h1>Welcome Back</h1>
        <p>Sign in to continue</p>
        
        <div class="login-options">
            <a href="/oauth2/authorization/google" class="google-btn">
                <svg class="google-icon" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                Sign in with Google
            </a>
        </div>

        <div class="divider">OR</div>

        <form action="/login" method="post" class="form-login">
            <input type="text" name="username" placeholder="Username" required>
            <input type="password" name="password" placeholder="Password" required>
            <button type="submit" class="submit-btn">Sign In</button>
        </form>
        
        <p style="margin-top: 20px; color: #666; font-size: 14px;">
            Demo credentials: user/password or admin/admin
        </p>
    </div>
</body>
</html>
                """;
    }

    private String generateDashboardHtml(String name, String email, String picture, Map<String, Object> attributes) {
        // Use String concatenation instead of .formatted() to avoid issues with CSS semicolons
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Dashboard</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            max-width: 800px;\n" +
                "            margin: 50px auto;\n" +
                "            padding: 20px;\n" +
                "            background: #f5f5f5;\n" +
                "        }\n" +
                "        .container {\n" +
                "            background: white;\n" +
                "            padding: 30px;\n" +
                "            border-radius: 8px;\n" +
                "            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .profile {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 20px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .profile img {\n" +
                "            border-radius: 50%;\n" +
                "            width: 80px;\n" +
                "            height: 80px;\n" +
                "        }\n" +
                "        .user-info {\n" +
                "            flex: 1;\n" +
                "        }\n" +
                "        .user-info h2 {\n" +
                "            margin: 0 0 5px 0;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        .user-info p {\n" +
                "            margin: 0;\n" +
                "            color: #666;\n" +
                "        }\n" +
                "        .btn {\n" +
                "            background: #4CAF50;\n" +
                "            color: white;\n" +
                "            padding: 10px 20px;\n" +
                "            border: none;\n" +
                "            border-radius: 4px;\n" +
                "            cursor: pointer;\n" +
                "            text-decoration: none;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .btn-danger {\n" +
                "            background: #f44336;\n" +
                "        }\n" +
                "        .attributes {\n" +
                "            background: #f9f9f9;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 4px;\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "        .attributes pre {\n" +
                "            background: #2d2d2d;\n" +
                "            color: #f8f8f2;\n" +
                "            padding: 15px;\n" +
                "            border-radius: 4px;\n" +
                "            overflow-x: auto;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>Welcome to Your Dashboard</h1>\n" +
                "        \n" +
                "        <div class=\"profile\">\n" +
                "            <img src=\"" + picture + "\" alt=\"Profile Picture\">\n" +
                "            <div class=\"user-info\">\n" +
                "                <h2>" + name + "</h2>\n" +
                "                <p>" + email + "</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div>\n" +
                "            <a href=\"/api/user/info\" class=\"btn\">View Full Profile JSON</a>\n" +
                "            <a href=\"/logout\" class=\"btn btn-danger\">Logout</a>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"attributes\">\n" +
                "            <h3>All Attributes from Google</h3>\n" +
                "            <pre>" + formatJson(attributes) + "</pre>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String formatJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{\n");
        map.forEach((key, value) ->
                sb.append("  \"").append(key).append("\": \"").append(value).append("\",\n")
        );
        sb.append("}");
        return sb.toString();
    }
}