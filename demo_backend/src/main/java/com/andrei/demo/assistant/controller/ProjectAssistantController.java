package com.andrei.demo.assistant.controller;

import com.andrei.demo.assistant.model.RecommendationRequest;
import com.andrei.demo.assistant.model.RecommendationSession;
import com.andrei.demo.assistant.model.RecommendationSubmitRequest;
import com.andrei.demo.assistant.service.ProjectAssistantService;
import com.andrei.demo.config.ValidationException;
import com.andrei.demo.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/assistant")
@AllArgsConstructor
@CrossOrigin
public class ProjectAssistantController {

    private final ProjectAssistantService projectAssistantService;
    private final JwtUtil jwtUtil;

    @PostMapping("/recommend")
    public RecommendationSession recommend(
            @Valid @RequestBody RecommendationRequest request,
            @RequestHeader("Authorization") String authHeader
    ) throws ValidationException {
        String email = extractEmail(authHeader);
        return projectAssistantService.recommend(email, request);
    }

    @GetMapping("/recommend/{sessionId}")
    public RecommendationSession getSession(
            @PathVariable UUID sessionId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String email = extractEmail(authHeader);
        return projectAssistantService.getSession(sessionId, email);
    }

    @PostMapping("/recommend/{sessionId}/submit")
    public RecommendationSession submit(
            @PathVariable UUID sessionId,
            @Valid @RequestBody RecommendationSubmitRequest request,
            @RequestHeader("Authorization") String authHeader
    ) throws ValidationException {
        String email = extractEmail(authHeader);
        return projectAssistantService.submit(sessionId, email, request);
    }

    private String extractEmail(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getEmailFromToken(token);
    }
}