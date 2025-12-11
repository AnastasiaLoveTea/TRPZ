package org.example.dlm.composite;

import java.util.UUID;

public interface DownloadComponent {

    void start();
    void pause();
    void cancel();

    long getTotalBytes();
    long getReceivedBytes();

    String getName();
}

