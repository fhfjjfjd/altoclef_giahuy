package adris.altoclef.commandsystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

public class CommandExecutor {

    private final HashMap<String, Command> _commandSheet = new HashMap<>();
    private final AltoClef _mod;
    private final String _commandPrefix;

    public CommandExecutor(AltoClef mod, String commandPrefix) {
        _mod = mod;
        _commandPrefix = commandPrefix;
    }

    public void registerNewCommand(Command ...commands) {
        for (Command command : commands) {
            if (_commandSheet.containsKey(command.getName())) {
                Debug.logInternal("Command with name " + command.getName() + " already exists! Can't register that name twice.");
                continue;
            }
            _commandSheet.put(command.getName(), command);
        }
    }

    public boolean isClientCommand(String line) {
        return line.startsWith(_commandPrefix);
    }

    public void execute(String line, Consumer onFinish) throws CommandException {
        if (!isClientCommand(line)) return;
        line = line.substring(_commandPrefix.length());
        Command c = getCommand(line);
        if (c != null) {
            try {
                c.run(_mod, line, onFinish);
            } catch (CommandException ae) {
                throw new CommandException(ae.getMessage() + "\nUsage: " + c.getHelpRepresentation(), ae);
            }
        }
    }

    public void execute(String line) throws CommandException {
        execute(line, null);
    }

    private Command getCommand(String line) throws CommandException {

        if (line.length() != 0) {
            String command = line;
            int firstSpace = line.indexOf(' ');
            if (firstSpace != -1) {
                command = line.substring(0, firstSpace);
            }

            if (!_commandSheet.containsKey(command)) {
                String suggestions = getCommandSuggestions(command);
                if (suggestions.isEmpty()) {
                    throw new CommandException("Command " + command + " does not exist.");
                }
                throw new CommandException("Command " + command + " does not exist. Did you mean: " + suggestions + "?");
            }

            return _commandSheet.get(command);
        }
        return null;

    }

    public Collection<Command> allCommands() {
        return _commandSheet.values();
    }

    public Command get(String name) {
        return (_commandSheet.getOrDefault(name, null));
    }

    private String getCommandSuggestions(String input) {
        List<String> names = new ArrayList<>(_commandSheet.keySet());
        names.sort(Comparator.comparingInt(name -> levenshteinDistance(name, input)));
        List<String> suggestions = new ArrayList<>();
        for (String name : names) {
            int distance = levenshteinDistance(name, input);
            if (distance <= 3 || name.startsWith(input) || name.contains(input)) {
                suggestions.add(name);
            }
            if (suggestions.size() >= 3) {
                break;
            }
        }
        return String.join(", ", suggestions);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}
