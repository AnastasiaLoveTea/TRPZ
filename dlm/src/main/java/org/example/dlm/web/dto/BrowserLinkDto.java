package org.example.dlm.web.dto;

public record BrowserLinkDto(
        String url,
        String fileName,
        String browserId
) { }
