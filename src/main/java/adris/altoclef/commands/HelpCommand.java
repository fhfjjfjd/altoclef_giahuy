package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.ui.MessagePriority;

import java.util.*;

public class HelpCommand extends Command {

    private static final Map<String, String> COMMAND_CATEGORIES = new LinkedHashMap<>();

    static {
        // Building
        COMMAND_CATEGORIES.put("build", "Building");
        COMMAND_CATEGORIES.put("autofill", "Building");
        // Farming & Resources
        COMMAND_CATEGORIES.put("auto-farm", "Farming");
        COMMAND_CATEGORIES.put("food", "Resources");
        COMMAND_CATEGORIES.put("get", "Resources");
        COMMAND_CATEGORIES.put("list", "Resources");
        // Navigation
        COMMAND_CATEGORIES.put("goto", "Navigation");
        COMMAND_CATEGORIES.put("follow", "Navigation");
        COMMAND_CATEGORIES.put("roundtrip", "Navigation");
        COMMAND_CATEGORIES.put("coords", "Navigation");
        COMMAND_CATEGORIES.put("locate_structure", "Navigation");
        // Combat & Player
        COMMAND_CATEGORIES.put("punk", "Combat");
        COMMAND_CATEGORIES.put("gamer", "Combat");
        COMMAND_CATEGORIES.put("give", "Player");
        COMMAND_CATEGORIES.put("inventory", "Player");
        // System
        COMMAND_CATEGORIES.put("help", "System");
        COMMAND_CATEGORIES.put("status", "System");
        COMMAND_CATEGORIES.put("stop", "System");
        COMMAND_CATEGORIES.put("reload_settings", "System");
        COMMAND_CATEGORIES.put("gamma", "System");
        COMMAND_CATEGORIES.put("info", "System");
        COMMAND_CATEGORIES.put("test", "System");
    }

    public HelpCommand() throws CommandException {
        super("help", "Lists all commands grouped by category or detailed usage for a specific command", new Arg<>(String.class, "command", "", 0, false));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String commandName = parser.get(String.class).trim();
        if (!commandName.isEmpty()) {
            Command found = mod.getCommandExecutor().get(commandName);
            if (found == null) {
                throw new CommandException("No command named '" + commandName + "' exists.");
            }

            mod.log("========== COMMAND HELP ==========" , MessagePriority.OPTIONAL);
            mod.log("@" + found.getName() + " - " + found.getDescription(), MessagePriority.OPTIONAL);
            mod.log("Usage: @" + found.getHelpRepresentation(), MessagePriority.OPTIONAL);
            mod.log("Category: " + COMMAND_CATEGORIES.getOrDefault(found.getName(), "Other"), MessagePriority.OPTIONAL);
            mod.log("==================================", MessagePriority.OPTIONAL);
            finish();
            return;
        }

        mod.log("========== ALTOCLEF GIAHUY HELP ==========" , MessagePriority.OPTIONAL);

        // Group commands by category
        Map<String, List<Command>> groups = new LinkedHashMap<>();
        List<Command> uncategorized = new ArrayList<>();

        for (Command c : mod.getCommandExecutor().allCommands()) {
            String category = COMMAND_CATEGORIES.get(c.getName());
            if (category != null) {
                groups.computeIfAbsent(category, k -> new ArrayList<>()).add(c);
            } else {
                uncategorized.add(c);
            }
        }

        for (List<Command> group : groups.values()) {
            group.sort(Comparator.comparing(Command::getName));
        }
        uncategorized.sort(Comparator.comparing(Command::getName));

        // Print each category
        for (Map.Entry<String, List<Command>> entry : groups.entrySet()) {
            mod.log("--- " + entry.getKey() + " ---", MessagePriority.OPTIONAL);
            for (Command c : entry.getValue()) {
                printCommand(mod, c);
            }
        }

        // Print uncategorized
        if (!uncategorized.isEmpty()) {
            mod.log("--- Other ---", MessagePriority.OPTIONAL);
            for (Command c : uncategorized) {
                printCommand(mod, c);
            }
        }

        mod.log("==========================================", MessagePriority.OPTIONAL);
        finish();
    }

    private void printCommand(AltoClef mod, Command c) {
        StringBuilder line = new StringBuilder();
        line.append("  @").append(c.getName());
        int pad = Math.max(1, 14 - c.getName().length());
        for (int i = 0; i < pad; ++i) line.append(" ");
        line.append(c.getDescription()).append(" | Usage: @").append(c.getHelpRepresentation());
        mod.log(line.toString(), MessagePriority.OPTIONAL);
    }
}
