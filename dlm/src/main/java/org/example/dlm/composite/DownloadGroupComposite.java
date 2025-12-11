package org.example.dlm.composite;

import java.util.ArrayList;
import java.util.List;

public class DownloadGroupComposite implements DownloadComponent {

    private final String name;
    private final List<DownloadComponent> children = new ArrayList<>();

    public DownloadGroupComposite(String name) {
        this.name = name;
    }

    public void add(DownloadComponent child) {
        children.add(child);
    }

    public void remove(DownloadComponent child) {
        children.remove(child);
    }

    public DownloadComponent getChild(int index) {
        return children.get(index);
    }

    @Override
    public void start() {
        for (DownloadComponent child : children) {
            child.start();
        }
    }

    @Override
    public void pause() {
        for (DownloadComponent child : children) {
            child.pause();
        }
    }

    @Override
    public void cancel() {
        for (DownloadComponent child : children) {
            child.cancel();
        }
    }

    @Override
    public long getTotalBytes() {
        return children.stream()
                .mapToLong(DownloadComponent::getTotalBytes)
                .sum();
    }

    @Override
    public long getReceivedBytes() {
        return children.stream()
                .mapToLong(DownloadComponent::getReceivedBytes)
                .sum();
    }

    @Override
    public String getName() {
        return name;
    }

    public List<DownloadComponent> getChildren() {
        return List.copyOf(children);
    }
}

