package org.example.dlm.web;

import lombok.RequiredArgsConstructor;
import org.example.dlm.command.CommandBus;
import org.example.dlm.command.download.CancelDownloadCommand;
import org.example.dlm.command.download.DeleteDownloadCommand;
import org.example.dlm.command.download.PauseDownloadCommand;
import org.example.dlm.command.download.StartDownloadCommand;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.service.DownloadService;
import org.example.dlm.repo.UserRepo;
import org.example.dlm.web.dto.DownloadForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/downloads")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final UserRepo users;
    private final CommandBus bus;

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        model.addAttribute("items", downloadService.listByUser(user.getId()));
        model.addAttribute("form", new DownloadForm());
        return "downloads/list";
    }

    @PostMapping
    public String add(@ModelAttribute("form") DownloadForm form,
                      @AuthenticationPrincipal UserDetails auth,
                      Model model) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();

        if (form.getUrl() == null || form.getUrl().isBlank()) {
            model.addAttribute("error", "URL не може бути порожнім");
            return list(model, auth);
        }

        try {
            downloadService.addUrl(user.getId(), form.getUrl());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return list(model, auth);
        }

        return "redirect:/downloads";
    }

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable UUID id,
                            @RequestParam("s") DownloadStatus status,
                            @AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        switch (status) {
            case RUNNING -> bus.execute(new StartDownloadCommand(downloadService, user.getId(), id));
            case PAUSED  -> bus.execute(new PauseDownloadCommand(downloadService, user.getId(), id));
            case CANCELED-> bus.execute(new CancelDownloadCommand(downloadService, user.getId(), id));
            default      -> downloadService.setStatusForUser(user.getId(), id, status); // fallback
        }
        return "redirect:/downloads";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        bus.execute(new DeleteDownloadCommand(downloadService, user.getId(), id));
        return "redirect:/downloads?deleted";
    }
}
