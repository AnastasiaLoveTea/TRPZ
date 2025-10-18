// org.example.dlm.web.SettingsController
package org.example.dlm.web;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.Settings;
import org.example.dlm.repo.UserRepo;
import org.example.dlm.service.SettingsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepo users;

    @GetMapping
    public String form(Model model, @AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        Settings settings = settingsService.getOrCreate(user.getId());
        model.addAttribute("settings", settings);
        return "settings/form";
    }

    @PostMapping
    public String save(@AuthenticationPrincipal UserDetails auth,
                       @RequestParam int defaultSegments,
                       @RequestParam long globalSpeedLimitBps) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        settingsService.update(user.getId(), defaultSegments, globalSpeedLimitBps);
        return "redirect:/settings?saved";
    }
}
