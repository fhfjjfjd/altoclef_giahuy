package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.EscapeFromLavaTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class WorldSurvivalChain extends SingleTaskChain {

    private final TimerGame _wasInLavaTimer = new TimerGame(1);
    private boolean _wasAvoidingDrowning;
    private boolean _wasStuckInPortal;
    private int _portalStuckTimer;
    private boolean _wasSneakingAtEdge;

    public WorldSurvivalChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        // Edge protection - crouch near dangerous drops
        handleEdgeProtection(mod);

        // Drowning
        handleDrowning(mod);

        // Lava Escape
        if (isInLavaOhShit(mod)) {
            setTask(new EscapeFromLavaTask());
            return 100;
        }

        // Portal stuck
        if (isStuckInNetherPortal(mod)) {
            _portalStuckTimer++;
            _wasStuckInPortal = true;
        } else {
            _portalStuckTimer = 0;
        }
        if (_portalStuckTimer > 10) {
            // We're stuck inside a portal, so get out.
            // Don't allow breaking while we're inside the portal.
            setTask(new SafeRandomShimmyTask());
            return 60;
        }
        _wasStuckInPortal = false;

        return Float.NEGATIVE_INFINITY;
    }

    private void handleDrowning(AltoClef mod) {
        // Swim
        boolean avoidedDrowning = false;
        if (mod.getModSettings().shouldAvoidDrowning()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                if (mod.getPlayer().isTouchingWater() && mod.getPlayer().getAir() < mod.getPlayer().getMaxAir()) {
                    // Swim up!
                    mod.getInputControls().hold(Input.JUMP);
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    avoidedDrowning = true;
                    _wasAvoidingDrowning = true;
                }
            }
        }
        // Stop swimming up if we just swam.
        if (_wasAvoidingDrowning && !avoidedDrowning) {
            _wasAvoidingDrowning = false;
            mod.getInputControls().release(Input.JUMP);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
    }

    private void handleEdgeProtection(AltoClef mod) {
        if (isNearDangerousEdge(mod)) {
            mod.getInputControls().hold(Input.SNEAK);
            _wasSneakingAtEdge = true;
        } else if (_wasSneakingAtEdge) {
            mod.getInputControls().release(Input.SNEAK);
            _wasSneakingAtEdge = false;
        }
    }

    private boolean isNearDangerousEdge(AltoClef mod) {
        if (!mod.getPlayer().isOnGround()) return false;
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos adjacent = playerPos.offset(dir);
            int dropDepth = 0;
            BlockPos check = adjacent;
            while (dropDepth < 10 && mod.getWorld().getBlockState(check.down()).isAir()) {
                dropDepth++;
                check = check.down();
            }
            if (dropDepth >= 4) return true;
        }
        return false;
    }

    private boolean isInLavaOhShit(AltoClef mod) {
        if (mod.getPlayer().isInLava() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            _wasInLavaTimer.reset();
            return true;
        }
        return mod.getPlayer().isOnFire() && !_wasInLavaTimer.elapsed();
    }

    private boolean isStuckInNetherPortal(AltoClef mod) {
        // We're stuck if we're inside a portal, are breaking it and can ONLY look at the portal.
        boolean inPortal = mod.getBlockTracker().blockIsValid(mod.getPlayer().getBlockPos(), Blocks.NETHER_PORTAL);
        boolean breakingPortal = mod.getControllerExtras().isBreakingBlock() && mod.getBlockTracker().blockIsValid(mod.getControllerExtras().getBreakingBlockPos(), Blocks.NETHER_PORTAL);
        if (MinecraftClient.getInstance().crosshairTarget != null && MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult currentLook = (BlockHitResult) MinecraftClient.getInstance().crosshairTarget;
            boolean collidingWithportal = (currentLook != null && mod.getBlockTracker().blockIsValid(currentLook.getBlockPos(), Blocks.NETHER_PORTAL));
            return inPortal && collidingWithportal && (breakingPortal || _wasStuckInPortal);
        }
        return false;
    }

    @Override
    public String getName() {
        return "Misc World Survival Chain";
    }

    @Override
    public boolean isActive() {
        // Always check for survival.
        return true;
    }
}
