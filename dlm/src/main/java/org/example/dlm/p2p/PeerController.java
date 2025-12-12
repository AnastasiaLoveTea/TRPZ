package org.example.dlm.p2p;

import lombok.RequiredArgsConstructor;
import org.example.dlm.repo.DownloadRepo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/peer")
@RequiredArgsConstructor
public class PeerController {

    private final DownloadRepo downloads;

    @GetMapping("/downloads")
    public List<PeerDownloadDto> getDownloads() {
        return downloads.findAll().stream()
                .map(d -> new PeerDownloadDto(
                        d.getId(),
                        d.getFileName(),
                        d.getTotalBytes(),
                        d.getUrl()
                ))
                .toList();
    }
}
