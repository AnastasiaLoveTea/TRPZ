package org.example.dlm.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

@Component
public class ActiveTaskRegistry {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Future<?>>> map =
            new ConcurrentHashMap<>();

    public void register(UUID downloadId, Future<?> future) {
        map.computeIfAbsent(downloadId, id -> new CopyOnWriteArrayList<>())
                .add(future);
    }

    public List<Future<?>> drain(UUID downloadId) {
        var list = map.remove(downloadId);
        return list != null ? list : List.of();
    }
}
