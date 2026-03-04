package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.misc.speedrun.BeatMinecraft2Task;

public class GamerCommand extends Command {
    public GamerCommand() {
        super("gamer", "Auto speedrun - beats Minecraft (kill Ender Dragon)");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.runUserTask(new BeatMinecraft2Task(true, 12, 14, 9), this::finish);
    }
}
