package org.example.dlm.composite;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.service.DownloadService;

import java.util.UUID;

@RequiredArgsConstructor
public class SingleDownloadLeaf implements DownloadComponent {

    private final Long userId;
    private final UUID downloadId;

    private final DownloadService downloadService;
    private final DownloadRepo downloads;

    @Override
    public void start() {
        downloadService.setStatusForUser(userId, downloadId, DownloadStatus.RUNNING);
    }

    @Override
    public void pause() {
        downloadService.setStatusForUser(userId, downloadId, DownloadStatus.PAUSED);
    }

    @Override
    public void cancel() {
        downloadService.setStatusForUser(userId, downloadId, DownloadStatus.CANCELED);
    }

    @Override
    public long getTotalBytes() {
        return downloads.findById(downloadId)
                .map(Download::getTotalBytes)
                .orElse(0L);
    }

    @Override
    public long getReceivedBytes() {
        return downloads.findById(downloadId)
                .map(Download::getReceivedBytes)
                .orElse(0L);
    }

    @Override
    public String getName() {
        return downloads.findById(downloadId)
                .map(Download::getFileName)
                .orElse(downloadId.toString());
    }
}
