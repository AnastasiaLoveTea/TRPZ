package org.example.dlm.web;

import lombok.RequiredArgsConstructor;
import org.example.dlm.repo.UserRepo;
import org.example.dlm.service.integration.BrowserUrlHandler;
import org.example.dlm.web.dto.BrowserLinkDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class BrowserIntegrationController {

    private final BrowserUrlHandler browserUrlHandler;
    private final UserRepo users;

    @PostMapping("/add")
    public ResponseEntity<?> addFromBrowser(@RequestBody BrowserLinkDto dto,
                                            @AuthenticationPrincipal UserDetails auth) {
        if (dto == null || dto.url() == null || dto.url().isBlank()) {
            return ResponseEntity.badRequest().body("URL є обов'язковим");
        }

        if (auth == null) {
            return ResponseEntity.status(401).body("Необхідно увійти в систему");
        }

        var user = users.findByUsername(auth.getUsername()).orElseThrow();

        try {
            browserUrlHandler.handle(user.getId(), dto.url());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        return ResponseEntity.accepted().build();
    }
}