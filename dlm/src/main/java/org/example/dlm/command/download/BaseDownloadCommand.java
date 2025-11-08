package org.example.dlm.command.download;

import lombok.RequiredArgsConstructor;
import org.example.dlm.command.Command;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.service.DownloadService;

import java.util.UUID;

@RequiredArgsConstructor
public abstract class BaseDownloadCommand implements Command {
    protected final DownloadService receiver;
    protected final Long userId;
    protected final UUID downloadId;

    protected abstract DownloadStatus targetStatus();

    @Override
    public void execute() {
        receiver.setStatusForUser(userId, downloadId, targetStatus());
    }
}
