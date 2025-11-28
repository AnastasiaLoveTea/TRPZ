package org.example.dlm.service.integration;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.Download;
import org.example.dlm.service.DownloadService;

@RequiredArgsConstructor
public abstract class AbstractLinkHandler {

    protected final DownloadService downloadService;

    public final Download handle(Long userId, String rawUrl) {
        String normalized = normalize(rawUrl);
        String transformed = transform(normalized);
        return doAdd(userId, transformed);
    }

    protected String normalize(String rawUrl) {
        return rawUrl != null ? rawUrl.trim() : "";
    }

    protected String transform(String url) {
        return url;
    }

    protected Download doAdd(Long userId, String url) {
        return downloadService.addUrl(userId, url);
    }
}
