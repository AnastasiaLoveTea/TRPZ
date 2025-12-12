package org.example.dlm.p2p;

import java.util.UUID;

public record PeerDownloadDto(
        UUID id,
        String name,
        long size,
        String url
) {}
