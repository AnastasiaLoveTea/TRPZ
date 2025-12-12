package org.example.dlm.p2p;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class PeerClient {

    private final RestTemplate rest = new RestTemplate();

    public List<PeerDownloadDto> fetchDownloads(String host) {
        String url = "http://" + host + "/peer/downloads";
        PeerDownloadDto[] arr = rest.getForObject(url, PeerDownloadDto[].class);
        return arr != null ? Arrays.asList(arr) : List.of();
    }
}
