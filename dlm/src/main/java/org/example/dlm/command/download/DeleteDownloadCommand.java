package org.example.dlm.command.download;

import org.example.dlm.command.Command;
import org.example.dlm.service.DownloadService;

import java.util.UUID;

public class DeleteDownloadCommand implements Command {
    private final DownloadService receiver;
    private final Long userId;
    private final UUID downloadId;

    public DeleteDownloadCommand(DownloadService r, Long userId, UUID id) {
        this.receiver = r; this.userId = userId; this.downloadId = id;
    }
    @Override public void execute() { receiver.deleteForUser(userId, downloadId); }
}
