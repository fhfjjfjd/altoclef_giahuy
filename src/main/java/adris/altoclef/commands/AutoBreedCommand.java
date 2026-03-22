package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoBreedTask;

public class AutoBreedCommand extends Command {
    public AutoBreedCommand() throws CommandException {
        super("autobreed", "Automatically breed animals. Usage: @autobreed <animal> (cow, sheep, pig, chicken)",
                new Arg(String.class, "animal"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String animal = parser.get(String.class);

        String lower = animal.toLowerCase();
        if (!lower.equals("cow") && !lower.equals("sheep") && !lower.equals("pig") && !lower.equals("chicken")) {
            mod.log("Unknown animal: " + animal + ". Supported: cow, sheep, pig, chicken");
            finish();
            return;
        }

        mod.log("Starting auto-breeding of " + lower + "...");
        mod.runUserTask(new AutoBreedTask(lower), this::finish);
    }
}
