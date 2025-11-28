package org.example.dlm.service.integration;

import org.example.dlm.service.DownloadService;
import org.springframework.stereotype.Service;

@Service
public class ManualUrlHandler extends AbstractLinkHandler {

    public ManualUrlHandler(DownloadService downloadService) {
        super(downloadService);
    }

}
