package org.example.dlm.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpRangeClient {

    public static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static final HttpResponse.BodyHandler<InputStream> STREAM_HANDLER =
            HttpResponse.BodyHandlers.ofInputStream();

    public static HttpRequest buildRequest(URI uri, String rangeHeader) {
        var builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(5))
                .GET();
        if (rangeHeader != null && !rangeHeader.isBlank()) {
            builder.header("Range", rangeHeader);
        }
        return builder.build();
    }


    public static final class ProbeResult {
        public final boolean rangeSupported;
        public final long contentLength;

        public ProbeResult(boolean rangeSupported, long contentLength) {
            this.rangeSupported = rangeSupported;
            this.contentLength = contentLength;
        }
    }

    public ProbeResult probe(URI uri) throws Exception {
        var req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Range", "bytes=0-0")
                .GET()
                .build();

        var resp = CLIENT.send(req, HttpResponse.BodyHandlers.discarding());

        if (resp.statusCode() == 206) {
            long total = parseTotalFromContentRange(resp.headers().firstValue("Content-Range").orElse(null));
            return new ProbeResult(true, total);
        }

        var head = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        var headResp = CLIENT.send(head, HttpResponse.BodyHandlers.discarding());

        boolean range = "bytes".equalsIgnoreCase(headResp.headers().firstValue("Accept-Ranges").orElse(""));
        long len = headResp.headers().firstValue("Content-Length").map(HttpRangeClient::parseLongSafe).orElse(-1L);
        return new ProbeResult(range, len);
    }


    private static long parseTotalFromContentRange(String cr) {
        if (cr == null) return -1;
        int slash = cr.indexOf('/');
        if (slash < 0) return -1;
        String tail = cr.substring(slash + 1).trim();
        return parseLongSafe(tail);
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return -1L;
        }
    }

}
