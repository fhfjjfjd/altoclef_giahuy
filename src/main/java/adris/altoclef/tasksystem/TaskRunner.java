package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class TaskRunner {

    private final ArrayList<TaskChain> _chains = new ArrayList<>();
    private final AltoClef _mod;
    private boolean _active;

    private TaskChain _cachedCurrentTaskChain = null;

    // Global stuck watchdog
    private static final long WATCHDOG_TIMEOUT_MS = 30_000;
    private static final double WATCHDOG_MIN_DISTANCE = 1.5;
    private Vec3d _watchdogLastPos = null;
    private long _watchdogLastProgressTime = 0;

    public TaskRunner(AltoClef mod) {
        _mod = mod;
        _active = false;
    }

    public void tick() {
        if (!_active) return;

        // Global stuck watchdog
        tickWatchdog();

        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : _chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority(_mod);
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (_cachedCurrentTaskChain != null && maxChain != _cachedCurrentTaskChain) {
            _cachedCurrentTaskChain.onInterrupt(_mod, maxChain);
        }
        _cachedCurrentTaskChain = maxChain;
        if (maxChain != null) {
            maxChain.tick(_mod);
        }
    }

    private void tickWatchdog() {
        if (!AltoClef.inGame() || _mod.getPlayer() == null) return;

        Vec3d currentPos = _mod.getPlayer().getPos();
        long now = System.currentTimeMillis();

        if (_watchdogLastPos == null) {
            _watchdogLastPos = currentPos;
            _watchdogLastProgressTime = now;
            return;
        }

        boolean moved = currentPos.distanceTo(_watchdogLastPos) > WATCHDOG_MIN_DISTANCE;
        boolean breaking = _mod.getControllerExtras().isBreakingBlock();

        if (moved || breaking) {
            _watchdogLastPos = currentPos;
            _watchdogLastProgressTime = now;
            return;
        }

        if (now - _watchdogLastProgressTime > WATCHDOG_TIMEOUT_MS) {
            Debug.logWarning("[Watchdog] No progress for " + (WATCHDOG_TIMEOUT_MS / 1000) + "s. Resetting current task chain.");
            if (_cachedCurrentTaskChain != null) {
                _cachedCurrentTaskChain.stop(_mod);
            }
            _watchdogLastPos = currentPos;
            _watchdogLastProgressTime = now;
        }
    }

    public void addTaskChain(TaskChain chain) {
        _chains.add(chain);
    }

    public void enable() {
        if (!_active) {
            _mod.getBehaviour().push();
            _mod.getBehaviour().setPauseOnLostFocus(false);
        }
        _active = true;
    }

    public void disable() {
        if (_active) {
            _mod.getBehaviour().pop();
        }
        for (TaskChain chain : _chains) {
            chain.stop(_mod);
        }
        _active = false;

        Debug.logMessage("Stopped");
    }

    public TaskChain getCurrentTaskChain() {
        return _cachedCurrentTaskChain;
    }

    // Kinda jank ngl
    public AltoClef getMod() {
        return _mod;
    }
}
