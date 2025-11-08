package org.example.dlm.command;

public interface Command {
    void execute();
    default boolean isEnabled() { return true; }
}
