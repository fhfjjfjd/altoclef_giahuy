package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FollowPlayerTask extends Task {

    private final String _playerName;
    private final double _followDistance;

    public FollowPlayerTask(String playerName) {
        this(playerName, 2.0); // Default distance of 2 blocks
    }
    
    public FollowPlayerTask(String playerName, double followDistance) {
        _playerName = playerName;
        _followDistance = followDistance;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {

        Vec3d target = mod.getEntityTracker().getPlayerMostRecentPosition(_playerName);
        if (target == null) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\" because we have no idea where they are.");
            stop(mod);
            return null;
        }

        if (target.isInRange(mod.getPlayer().getPos(), 1) && !mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\". We moved to where we last saw them but now have no idea where they are.");
            stop(mod);
            return null;
        }

        if (!mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            // Go to last location
            return new GetToBlockTask(new BlockPos((int) target.x, (int) target.y, (int) target.z), false);
        }
        return new GetToEntityTask(mod.getEntityTracker().getPlayerEntity(_playerName), _followDistance);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof FollowPlayerTask task) {
            return task._playerName.equals(_playerName) && task._followDistance == _followDistance;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Following player " + _playerName + " at distance " + _followDistance;
    }
}
