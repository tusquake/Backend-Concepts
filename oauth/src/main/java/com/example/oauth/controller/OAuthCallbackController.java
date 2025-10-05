package com.example.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class OAuthCallbackController {

    @GetMapping("/authorized")
    public void authorized(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String error,
                           HttpServletResponse response) throws IOException {

        if (error != null) {
            response.setContentType("text/html");
            response.getWriter().write(generateErrorPage(error));
            return;
        }

        response.setContentType("text/html");
        response.getWriter().write(generateSuccessPage(code));
    }

    private String generateSuccessPage(String code) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Authorization Success</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 900px;
            margin: 50px auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { color: #4CAF50; }
        .success-box {
            background: #d4edda;
            border: 1px solid #c3e6cb;
            padding: 20px;
            border-radius: 4px;
            margin: 20px 0;
        }
        .code-box {
            background: #2d2d2d;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 4px;
            overflow-x: auto;
            font-family: 'Courier New', monospace;
            word-break: break-all;
            margin: 15px 0;
            position: relative;
        }
        .btn {
            background: #4CAF50;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            margin: 5px;
            text-decoration: none;
            display: inline-block;
        }
        .btn:hover { background: #45a049; }
        .copy-btn {
            background: #2196F3;
        }
        .copy-btn:hover { background: #0b7dda; }
        .step {
            background: #f9f9f9;
            padding: 15px;
            margin: 15px 0;
            border-left: 4px solid #2196F3;
        }
        .copied {
            background: #4CAF50 !important;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Authorization Successful!</h1>
        
        <div class="success-box">
            <h3>Authorization Code Received</h3>
            <p>You have successfully authorized the application. Here's your authorization code:</p>
            <div class="code-box" id="authCode">""" + code + """
</div>
            <button class="btn copy-btn" id="copyCodeBtn" onclick="copyCode()">Copy Code</button>
        </div>

        <h2>Next Steps: Exchange Code for Access Token</h2>

        <div class="step">
            <h3>Step 1: Use cURL to get the access token</h3>
            <div class="code-box" id="curlCommand">curl -X POST http://localhost:8080/oauth2/token \\
  -u demo-client:secret \\
  -H "Content-Type: application/x-www-form-urlencoded" \\
  -d "grant_type=authorization_code" \\
  -d "code=""" + code + """
" \\
  -d "redirect_uri=http://localhost:8080/authorized"</div>
            <button class="btn copy-btn" id="copyCurlBtn" onclick="copyCurl()">Copy cURL Command</button>
        </div>

        <div class="step">
            <h3>Step 2: Use the access token</h3>
            <p>Once you have the access token from Step 1, use it to call protected endpoints:</p>
            <div class="code-box">curl http://localhost:8080/api/user/profile \\
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"</div>
        </div>

        <div class="step">
            <h3>Alternative: Use Postman</h3>
            <ol>
                <li>Open Postman</li>
                <li>Create a new POST request to: <code>http://localhost:8080/oauth2/token</code></li>
                <li>Go to Authorization tab → Type: Basic Auth
                    <ul>
                        <li>Username: <strong>demo-client</strong></li>
                        <li>Password: <strong>secret</strong></li>
                    </ul>
                </li>
                <li>Go to Body tab → Select <strong>x-www-form-urlencoded</strong></li>
                <li>Add parameters:
                    <ul>
                        <li>grant_type: <strong>authorization_code</strong></li>
                        <li>code: <strong>(paste the authorization code above)</strong></li>
                        <li>redirect_uri: <strong>http://localhost:8080/authorized</strong></li>
                    </ul>
                </li>
                <li>Click Send</li>
            </ol>
        </div>

        <a href="/" class="btn">← Back to Home</a>
    </div>

    <script>
        function copyCode() {
            const code = document.getElementById('authCode').textContent.trim();
            const btn = document.getElementById('copyCodeBtn');
            navigator.clipboard.writeText(code).then(() => {
                btn.textContent = '✓ Copied!';
                btn.classList.add('copied');
                setTimeout(() => {
                    btn.textContent = 'Copy Code';
                    btn.classList.remove('copied');
                }, 2000);
            });
        }

        function copyCurl() {
            const curlCommand = document.getElementById('curlCommand').textContent.trim();
            const btn = document.getElementById('copyCurlBtn');
            navigator.clipboard.writeText(curlCommand).then(() => {
                btn.textContent = '✓ Copied!';
                btn.classList.add('copied');
                setTimeout(() => {
                    btn.textContent = 'Copy cURL Command';
                    btn.classList.remove('copied');
                }, 2000);
            });
        }
    </script>
</body>
</html>
                """;
    }

    private String generateErrorPage(String error) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Authorization Error</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { color: #f44336; }
        .error-box {
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            padding: 20px;
            border-radius: 4px;
            margin: 20px 0;
        }
        .btn {
            background: #4CAF50;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Authorization Failed</h1>
        <div class="error-box">
            <p><strong>Error:</strong> """ + error + """
</p>
        </div>
        <a href="/" class="btn">← Back to Home</a>
    </div>
</body>
</html>
                """;
    }
}