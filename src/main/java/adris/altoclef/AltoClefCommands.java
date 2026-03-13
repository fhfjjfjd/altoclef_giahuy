package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commandsystem.CommandException;

/**
 * Initializes altoclef's built in commands.
 */
public class AltoClefCommands {

    public AltoClefCommands() throws CommandException {
        // List commands here
        AltoClef.getCommandExecutor().registerNewCommand(
                new HelpCommand(),
                new GetCommand(),
                new FollowCommand(),
                new GiveCommand(),
                new GotoCommand(),
                new CoordsCommand(),
                new StatusCommand(),
                new InventoryCommand(),
                new LocateStructureCommand(),
                new StopCommand(),
                new TestCommand(),
                new FoodCommand(),
                new ReloadSettingsCommand(),
                new GamerCommand(),
                new PunkCommand(),
                new SetGammaCommand(),
                new ListCommand(),
                new RoundtripCommand(),
                new BuildCommand(),
                new MultiBuildCommand(),
                new AutoLightBuildCommand(),
                new UndoBuildCommand(),
                new WaypointCommand(),
                new AutoExploreCommand(),
                new AutoMineCommand(),
                new AutoSmeltCommand(),
                new AutoTradeCommand(),
                new ChestManagerCommand(),
                new FillTargetChestCommand(),
                new InfoCommand(),
                new AutoFarmCommand()
                //new TestMoveInventoryCommand(),
                //    new TestSwapInventoryCommand()
        );
    }
}
