package org.example.dlm.web;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.repo.StatsRepo;
import org.example.dlm.repo.UserRepo;
import org.example.dlm.service.DownloadService;
import org.example.dlm.service.integration.ManualUrlHandler;
import org.example.dlm.web.dto.DownloadForm;
import org.example.dlm.web.dto.DownloadProgressDto;
import org.example.dlm.p2p.PeerClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/downloads")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final UserRepo users;
    private final StatsRepo stats;
    private final PeerClient peerClient;

    private final ManualUrlHandler manualUrlHandler;

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
            manualUrlHandler.handle(user.getId(), form.getUrl());
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
        downloadService.setStatusForUser(user.getId(), id, status);
        return "redirect:/downloads";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        downloadService.deleteForUser(user.getId(), id);
        return "redirect:/downloads?deleted";
    }

    @PostMapping("/pause-all")
    public String pauseAll(@AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();
        downloadService.pauseAllForUser(user.getId());
        return "redirect:/downloads";
    }

    @PostMapping("/import")
    public String importFromPeer(
            @RequestParam String peer,
            @AuthenticationPrincipal UserDetails auth
    ) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();

        var remoteDownloads = peerClient.fetchDownloads(peer);

        for (var rd : remoteDownloads) {
            downloadService.addUrl(user.getId(), rd.url());
        }

        return "redirect:/downloads";
    }

    @GetMapping("/progress")
    @ResponseBody
    public List<DownloadProgressDto> progress(@AuthenticationPrincipal UserDetails auth) {
        var user = users.findByUsername(auth.getUsername()).orElseThrow();

        return downloadService.listByUser(user.getId())
                .stream()
                .map(d -> {
                    var statsOpt = stats.findById(d.getId());

                    double avgSpeed = 0.0;
                    double maxSpeed = 0.0;
                    Instant lastStartedAt = null;
                    Instant lastFinishedAt = null;
                    int retries = 0;

                    if (statsOpt.isPresent()) {
                        var s = statsOpt.get();
                        avgSpeed = s.getAvgSpeedBps();
                        maxSpeed = s.getMaxSpeedBps();
                        lastStartedAt = s.getLastStartedAt();
                        lastFinishedAt = s.getLastFinishedAt();
                        retries = s.getRetries();
                    }

                    return new DownloadProgressDto(
                            d.getId(),
                            d.getReceivedBytes(),
                            d.getTotalBytes(),
                            d.getStatus() != null ? d.getStatus().name() : "QUEUED",
                            avgSpeed,
                            maxSpeed,
                            d.getCreatedAt(),
                            lastStartedAt,
                            lastFinishedAt,
                            retries
                    );
                })
                .toList();
    }
}
