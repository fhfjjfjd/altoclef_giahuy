package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.ui.MessagePriority;
import baritone.process.BuilderProcess;
import net.minecraft.block.BlockState;

import java.io.File;
import java.util.List;
import java.util.Map;

public class InfoCommand extends Command {
    public InfoCommand() {
        super("info", "Show commands, loaded schematics, and block info");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        // Show all available commands
        mod.log("========== COMMANDS ==========", MessagePriority.OPTIONAL);
        for (Command c : AltoClef.getCommandExecutor().allCommands()) {
            mod.log("  @" + c.getName() + " - " + c.getDescription(), MessagePriority.OPTIONAL);
        }

        // Show loaded schematic files
        mod.log("========== SCHEMATICS ==========", MessagePriority.OPTIONAL);
        File schemDir = new File("schematics/");
        if (schemDir.exists() && schemDir.isDirectory()) {
            String[] files = schemDir.list();
            if (files != null && files.length > 0) {
                for (String f : files) {
                    mod.log("  " + f, MessagePriority.OPTIONAL);
                }
            } else {
                mod.log("  (no schematic files found)", MessagePriority.OPTIONAL);
            }
        } else {
            mod.log("  (schematics/ folder not found)", MessagePriority.OPTIONAL);
        }

        // Show builder status and missing blocks
        mod.log("========== BUILD STATUS ==========", MessagePriority.OPTIONAL);
        BuilderProcess builder = mod.getClientBaritone().getBuilderProcess();
        if (builder.isActive()) {
            mod.log("  Builder: ACTIVE" + (builder.isPaused() ? " (PAUSED)" : ""), MessagePriority.OPTIONAL);
            Map<BlockState, Integer> missing = builder.getMissing();
            if (missing != null && !missing.isEmpty()) {
                mod.log("  Missing blocks:", MessagePriority.OPTIONAL);
                for (Map.Entry<BlockState, Integer> entry : missing.entrySet()) {
                    mod.log("    " + entry.getKey().getBlock().getName().getString() + " x" + entry.getValue(), MessagePriority.OPTIONAL);
                }
            } else {
                mod.log("  No missing blocks", MessagePriority.OPTIONAL);
            }
        } else {
            mod.log("  Builder: INACTIVE", MessagePriority.OPTIONAL);
        }

        // Show current task
        mod.log("========== CURRENT TASK ==========", MessagePriority.OPTIONAL);
        List<Task> tasks = mod.getUserTaskChain().getTasks();
        if (tasks.isEmpty()) {
            mod.log("  (no task running)", MessagePriority.OPTIONAL);
        } else {
            for (Task t : tasks) {
                mod.log("  " + t.toString(), MessagePriority.OPTIONAL);
            }
        }
        mod.log("==================================", MessagePriority.OPTIONAL);

        finish();
    }
}
