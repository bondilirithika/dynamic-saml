package com.example.flutto.controller;

import com.example.flutto.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final JwtService jwtService;

    public AuthApiController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        if (token == null || !jwtService.isTokenValid(token)) {
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }
        Claims claims = jwtService.extractClaims(token);
        response.put("valid", true);
        response.put("username", claims.get("username"));
        response.put("email", claims.get("email"));
        response.put("roles", claims.get("roles"));
        return ResponseEntity.ok(response);
    }


    // Custom logout endpoint
    @GetMapping("/custom-logout")
    public void customLogout(@RequestParam("redirect_uri") String redirectUri,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("Invalidating session: " + session.getId());
            session.invalidate();
        } else {
            System.out.println("No session to invalidate.");
        }

        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        response.sendRedirect(redirectUri);
    }

    // Custom login endpoint
    @GetMapping("/custom-login")
    public void customLogin(@RequestParam String redirectUri,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        // Store redirect URI in session
        HttpSession session = request.getSession(true);
        session.setAttribute("SAML_REDIRECT_URI", redirectUri);

        // Log the stored redirect URI
        Logger logger = LoggerFactory.getLogger(AuthApiController.class);
        logger.info("Custom Login: Stored redirectUri in session: {}", redirectUri);

        // Redirect to SAML login
        response.sendRedirect("/saml2/authenticate/google");
    }
}