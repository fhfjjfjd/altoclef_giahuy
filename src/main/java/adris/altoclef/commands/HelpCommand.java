package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
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

    public HelpCommand() {
        super("help", "Lists all commands grouped by category");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.log("========== ALTOCLEF GIAHUY HELP ==========", MessagePriority.OPTIONAL);

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
        int pad = 14 - c.getName().length();
        for (int i = 0; i < pad; ++i) line.append(" ");
        line.append(c.getDescription());
        mod.log(line.toString(), MessagePriority.OPTIONAL);
    }
}
