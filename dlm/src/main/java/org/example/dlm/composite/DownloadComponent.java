package org.example.dlm.composite;

import java.util.UUID;

public interface DownloadComponent {

    // «Operation()» з діаграми – наша основна дія
    void start();
    void pause();
    void cancel();

    // допоміжні операції для відображення прогресу/статистики
    long getTotalBytes();
    long getReceivedBytes();

    String getName();
}
