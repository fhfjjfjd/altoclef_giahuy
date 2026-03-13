package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.FollowPlayerTask;

public class FollowCommand extends Command {
    public FollowCommand() throws CommandException {
        super("follow", "Follows you or someone else. Usage: @follow [username] [distance]", 
              new Arg(String.class, "username", null, 0),
              new Arg(Double.class, "distance", 3.0, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String username = parser.get(String.class);
        Double distance = parser.get(Double.class);

        if (username == null) {
            if (mod.getButler().hasCurrentUser()) {
                username = mod.getButler().getCurrentUser();
            } else {
                mod.logWarning("No butler user currently present. Running this command with no user argument can ONLY be done via butler.");
                finish();
                return;
            }
        }

        mod.log("Following " + username + " with distance " + distance + " blocks.");
        mod.runUserTask(new FollowPlayerTask(username, distance), this::finish);
    }
}