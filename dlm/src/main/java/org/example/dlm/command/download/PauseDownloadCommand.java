package org.example.dlm.command.download;

import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.service.DownloadService;

import java.util.UUID;

public class PauseDownloadCommand extends BaseDownloadCommand {
    public PauseDownloadCommand(DownloadService s, Long userId, UUID id) { super(s, userId, id); }
    @Override protected DownloadStatus targetStatus() { return DownloadStatus.PAUSED; }
}
