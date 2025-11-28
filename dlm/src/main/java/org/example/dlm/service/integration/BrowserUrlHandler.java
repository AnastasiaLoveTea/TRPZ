package org.example.dlm.service.integration;

import org.example.dlm.service.DownloadService;
import org.springframework.stereotype.Service;

@Service
public class BrowserUrlHandler extends AbstractLinkHandler {

    public BrowserUrlHandler(DownloadService downloadService) {
        super(downloadService);
    }

    @Override
    protected String transform(String url) {
        if (url != null && url.startsWith("view-source:")) {
            return url.substring("view-source:".length()).trim();
        }
        return url;
    }
}
