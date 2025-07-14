package com.easymed.html2pdf.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class SessionService {
    
    private static final String SESSION_COOKIE_NAME = "PDF_SESSION_ID";
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60;
    
    public String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        
        if (sessionId == null) {
            sessionId = generateNewSessionId();
            setSessionCookie(response, sessionId);
        }
        
        return sessionId;
    }
    
    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    private String generateNewSessionId() {
        return UUID.randomUUID().toString();
    }
    
    private void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
} 