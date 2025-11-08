package org.example.dlm.command;

import java.util.List;

public class CommandBus {

    public void execute(Command cmd) {
        if (cmd == null || !cmd.isEnabled()) return;
        cmd.execute();
    }

    public void executeAll(List<? extends Command> cmds) {
        if (cmds == null) return;
        for (Command c : cmds) execute(c);
    }
}
