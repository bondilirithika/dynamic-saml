package com.example.flutto.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Map<String, Object> errorDetails = new HashMap<>();
        
        // Get error status
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = status != null ? Integer.parseInt(status.toString()) : 500;
        
        // Get exception details
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String errorMessage = exception != null ? exception.toString() : "Unknown error";
        
        // Get request details
        String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // Add details to response
        errorDetails.put("status", statusCode);
        errorDetails.put("error", errorMessage);
        errorDetails.put("path", uri);
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(errorDetails, HttpStatus.valueOf(statusCode));
    }
    
    // This is needed for older Spring Boot versions - can be omitted in Spring Boot 2.5+
    public String getErrorPath() {
        return "/error";
    }
}