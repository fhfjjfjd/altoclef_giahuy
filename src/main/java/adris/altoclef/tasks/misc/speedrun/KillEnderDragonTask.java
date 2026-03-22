package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.entity.AbstractKillEntityTask;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.baritone.GoalAnd;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Here we go
 * the final stretch
 * <p>
 * Until something inevitably fucks up and I gotta go back here to fix it
 * in which case this'll be pretty ironic.
 */
public class KillEnderDragonTask extends Task {

    private static final String[] DIAMOND_ARMORS = new String[]{"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    // Don't accidentally anger endermen lol
    private final TimerGame _lookDownTimer = new TimerGame(0.5);
    private final Task _collectBuildMaterialsTask = new MineAndCollectTask(new ItemTarget(Items.END_STONE, 100), new Block[]{Blocks.END_STONE}, MiningRequirement.WOOD);
    private final PunkEnderDragonTask _punkTask = new PunkEnderDragonTask();
    private BlockPos _exitPortalTop;

    private static Task getPickupTaskIfAny(AltoClef mod, Item... itemsToPickup) {
        for (Item check : itemsToPickup) {
            if (mod.getEntityTracker().itemDropped(check)) {
                return new PickupDroppedItemTask(new ItemTarget(check), true);
            }
        }
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addThrowawayItems(Items.END_STONE);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL);
        // Don't forcefield endermen.
        mod.getBehaviour().addForceFieldExclusion(entity -> entity instanceof EndermanEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart);
        mod.getBehaviour().setPreferredStairs(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_exitPortalTop == null) {
            _exitPortalTop = locateExitPortalTop(mod);
        }

        // Collect the following if dropped:
        // - Diamond Sword
        // - Diamond Armor
        // - Food (List)

        List<Item> toPickUp = new ArrayList<>(Arrays.asList(Items.DIAMOND_SWORD, Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET));
        if (mod.getInventoryTracker().totalFoodScore() < 10) {
            toPickUp.addAll(Arrays.asList(
                    Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.COOKED_PORKCHOP
            ));
        }

        Task pickupDrops = getPickupTaskIfAny(mod, toPickUp.toArray(Item[]::new));
        if (pickupDrops != null) {
            setDebugState("Picking up drops in end.");
            return pickupDrops;
        }

        // If not equipped diamond armor and we have any, equip it.
        for (Item armor : ItemHelper.DIAMOND_ARMORS) {
            try {
                if (mod.getInventoryTracker().hasItem(armor) && !mod.getInventoryTracker().isArmorEquipped(armor)) {
                    setDebugState("Equipping " + armor);
                    return new EquipArmorTask(armor);
                }
            } catch (NullPointerException e) {
                // Should never happen.
                Debug.logError("NullpointerException that Should never happen.");
                e.printStackTrace();
            }
        }

        if (!isRailingOnDragon() && _lookDownTimer.elapsed()) {
            if (mod.getPlayer().isOnGround()) {
                _lookDownTimer.reset();
                mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0f, -90f), true);
            }
        }

        // If there is a portal, enter it.
        if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
            setDebugState("Entering portal to beat the game.");
            return new DoToClosestBlockTask(
                    blockPos -> new GetToBlockTask(blockPos.up(), false),
                    Blocks.END_PORTAL
            );
        }

        // If we have no building materials (stone + cobble + end stone), get end stone
        // If there are crystals, suicide blow em up.
        // If there are no crystals, punk the dragon if it's close.
        int MINIMUM_BUILDING_BLOCKS = 1;
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class) && mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.END_STONE) < MINIMUM_BUILDING_BLOCKS || (_collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod))) {
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD)) {
                mod.getBehaviour().addProtectedItems(Items.END_STONE);
                setDebugState("Collecting building blocks to pillar to crystals");
                return _collectBuildMaterialsTask;
            }
        } else {
            mod.getBehaviour().removeProtectedItems(Items.END_STONE);
        }

        // Blow up the nearest end crystal
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class)) {
            setDebugState("Kamakazeeing crystals");
            return new DoToClosestEntityTask(
                (toDestroy) -> {
                    if (toDestroy.isInRange(mod.getPlayer(), 7)) {
                        mod.getControllerExtras().attack(toDestroy);
                    }
                    // Go next to the crystal, arbitrary where we just need to get close.
                    return new GetToBlockTask(toDestroy.getBlockPos().add(1, 0, 0), false);
                },
                EndCrystalEntity.class
            );
        }

        // Punk dragon
        if (mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
            setDebugState("Punking dragon");
            return _punkTask;
        }
        setDebugState("Couldn't find ender dragon... This can be very good or bad news.");
        return null;
        //return new KillEntitiesTask(EnderDragonEntity.class);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof KillEnderDragonTask;
    }

    @Override
    protected String toDebugString() {
        return "Killing Ender Dragon";
    }

    private boolean isRailingOnDragon() {
        return _punkTask.getMode() == Mode.RAILING;
    }

    private BlockPos locateExitPortalTop(AltoClef mod) {
        if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) return null;
        int height = WorldHelper.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
        if (height != -1) return new BlockPos(0, height, 0);
        return null;
    }

    private enum Mode {
        WAITING_FOR_PERCH,
        RAILING
    }

    private class PunkEnderDragonTask extends Task {

        private final HashMap<BlockPos, Double> _breathCostMap = new HashMap<>();
        private final TimerGame _hitHoldTimer = new TimerGame(0.1);
        private final TimerGame _hitResetTimer = new TimerGame(2);
        private final TimerGame _randomWanderChangeTimeout = new TimerGame(20);
        private final TimerGame _bowCooldown = new TimerGame(1.5);
        private final TimerGame _fireballCheckTimer = new TimerGame(0.25);
        private Mode _mode = Mode.WAITING_FOR_PERCH;

        private BlockPos _randomWanderPos;
        private boolean _wasHitting;
        private boolean _wasReleased;
        private boolean _isBowDrawing;
        private final TimerGame _bowDrawTimer = new TimerGame(1.0);

        private PunkEnderDragonTask() {
        }

        public Mode getMode() {
            return _mode;
        }

        private void hit(AltoClef mod) {
            mod.getExtraBaritoneSettings().setInteractionPaused(true);
            if (!_wasHitting) {
                _wasHitting = true;
                _wasReleased = false;
                _hitHoldTimer.reset();
                _hitResetTimer.reset();
                Debug.logInternal("HIT");
                mod.getInputControls().tryPress(Input.CLICK_LEFT);
            }
            if (_hitHoldTimer.elapsed()) {
                if (!_wasReleased) {
                    Debug.logInternal("    up");
                    _wasReleased = true;
                }
            }
            if (_wasHitting && _hitResetTimer.elapsed() || mod.getPlayer().getAttackCooldownProgress(0) > 0.99) {
                _wasHitting = false;
                mod.getExtraBaritoneSettings().setInteractionPaused(false);
            }
        }

        private void stopHitting(AltoClef mod) {
            if (_wasHitting) {
                if (!_wasReleased) {
                    mod.getExtraBaritoneSettings().setInteractionPaused(false);
                    _wasReleased = true;
                }
                _wasHitting = false;
            }
        }

        private boolean shouldEat(AltoClef mod) {
            return mod.getPlayer().getHealth() < 10 || mod.getPlayer().getHungerManager().getFoodLevel() < 8;
        }

        private boolean tryShootBow(AltoClef mod, EnderDragonEntity dragon) {
            if (!mod.getInventoryTracker().hasItem(Items.BOW) || !mod.getInventoryTracker().hasItem(Items.ARROW)) {
                return false;
            }
            if (!_bowCooldown.elapsed()) {
                return false;
            }

            Vec3d dragonPos = dragon.getPos();
            double dist = dragonPos.distanceTo(mod.getPlayer().getPos());
            if (dist > 64 || dist < 10) {
                return false;
            }

            mod.getSlotHandler().forceEquipItem(Items.BOW);

            Vec3d leadPos = dragonPos.add(dragon.getVelocity().multiply(dist / 3.0));
            leadPos = leadPos.add(0, 2, 0);
            Rotation targetRotation = RotationUtils.calcRotationFromVec3d(
                    mod.getClientBaritone().getPlayerContext().playerHead(),
                    leadPos,
                    mod.getClientBaritone().getPlayerContext().playerRotations()
            );
            mod.getClientBaritone().getLookBehavior().updateTarget(targetRotation, true);

            if (!_isBowDrawing) {
                _isBowDrawing = true;
                _bowDrawTimer.reset();
                mod.getInputControls().hold(Input.CLICK_RIGHT);
            }

            if (_bowDrawTimer.elapsed()) {
                mod.getInputControls().release(Input.CLICK_RIGHT);
                _isBowDrawing = false;
                _bowCooldown.reset();
                Debug.logInternal("Shot arrow at dragon (dist=" + (int) dist + ")");
            }
            return true;
        }

        private void stopBow(AltoClef mod) {
            if (_isBowDrawing) {
                mod.getInputControls().release(Input.CLICK_RIGHT);
                _isBowDrawing = false;
            }
        }

        private boolean shouldDodgeFireball(AltoClef mod) {
            if (!_fireballCheckTimer.elapsed()) return false;
            _fireballCheckTimer.reset();
            for (DragonFireballEntity fireball : mod.getEntityTracker().getTrackedEntities(DragonFireballEntity.class)) {
                if (fireball.isInRange(mod.getPlayer(), 15)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void onStart(AltoClef mod) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }

        @Override
        protected Task onTick(AltoClef mod) {

            updateBreathCostMap(mod);

            if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
                setDebugState("No dragon found.");
                return null;
            }
            EnderDragonEntity dragon = mod.getEntityTracker().getTrackedEntities(EnderDragonEntity.class).get(0);

            // Eat food when health/hunger is low
            if (shouldEat(mod) && mod.getInventoryTracker().totalFoodScore() > 0) {
                setDebugState("Eating to recover (HP=" + (int) mod.getPlayer().getHealth() + ")");
                return null;
            }

            // Dodge incoming fireballs
            if (shouldDodgeFireball(mod)) {
                stopHitting(mod);
                stopBow(mod);
                BlockPos safePos = getRandomWanderPos(mod);
                if (safePos != null) {
                    mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                    mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                            new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(safePos))
                    );
                    setDebugState("Dodging dragon fireball!");
                    return null;
                }
            }

            Phase dragonPhase = dragon.getPhaseManager().getCurrent();

            boolean perchingOrGettingReady = dragonPhase.getType() == PhaseType.LANDING || dragonPhase.isSittingOrHovering();

            switch (_mode) {
                case RAILING -> {
                    stopBow(mod);
                    if (!perchingOrGettingReady) {
                        Debug.logMessage("Dragon no longer perching.");
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                        _mode = Mode.WAITING_FOR_PERCH;
                        break;
                    }

                    Entity head = dragon.head;
                    if (head.isInRange(mod.getPlayer(), 7.5) && dragon.ticksSinceDeath <= 1) {
                        AbstractKillEntityTask.equipWeapon(mod);
                        Vec3d targetLookPos = head.getPos().add(0, 3, 0);
                        Rotation targetRotation = RotationUtils.calcRotationFromVec3d(mod.getClientBaritone().getPlayerContext().playerHead(), targetLookPos, mod.getClientBaritone().getPlayerContext().playerRotations());
                        mod.getClientBaritone().getLookBehavior().updateTarget(targetRotation, true);
                        MinecraftClient.getInstance().options.autoJump = false;
                        // Sprint for extra damage
                        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
                        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                        hit(mod);
                    } else {
                        stopHitting(mod);
                        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SPRINT, false);
                    }
                    if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                        if (_exitPortalTop != null) {
                            int bottomYDelta = -3;
                            BlockPos closest = null;
                            double closestDist = Double.POSITIVE_INFINITY;
                            for (int dx = -2; dx <= 2; ++dx) {
                                for (int dz = -2; dz <= 2; ++dz) {
                                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                                    BlockPos toCheck = _exitPortalTop.add(dx, bottomYDelta, dz);
                                    double distSq = toCheck.getSquaredDistance(head.getPos(), false);
                                    if (distSq < closestDist) {
                                        closest = toCheck;
                                        closestDist = distSq;
                                    }
                                }
                            }
                            if (closest != null) {
                                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                        new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(closest))
                                );
                            }
                        }
                    }
                    setDebugState("Railing on dragon (HP=" + (int) dragon.getHealth() + ")");
                }
                case WAITING_FOR_PERCH -> {
                    stopHitting(mod);
                    if (perchingOrGettingReady) {
                        stopBow(mod);
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                        Debug.logMessage("Dragon perching detected!");
                        _mode = Mode.RAILING;
                        break;
                    }

                    // Shoot bow at dragon while waiting
                    if (tryShootBow(mod, dragon)) {
                        setDebugState("Shooting dragon with bow (HP=" + (int) dragon.getHealth() + ")");
                        // Stay near portal center for quick access when dragon perches
                        if (_exitPortalTop != null && !mod.getPlayer().getBlockPos().isWithinDistance(_exitPortalTop, 15)) {
                            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                        new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(_exitPortalTop.add(5, -3, 0)))
                                );
                            }
                        }
                        return null;
                    }

                    // Stay near portal center while waiting
                    if (_exitPortalTop != null) {
                        double distToPortal = mod.getPlayer().getPos().distanceTo(new Vec3d(_exitPortalTop.getX(), _exitPortalTop.getY() - 3, _exitPortalTop.getZ()));
                        if (distToPortal > 20) {
                            _randomWanderPos = null;
                            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                                BlockPos nearPortal = _exitPortalTop.add((int) (Math.random() * 10 - 5), -3, (int) (Math.random() * 10 - 5));
                                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                        new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(nearPortal))
                                );
                            }
                            setDebugState("Moving near portal (waiting for perch)");
                            return null;
                        }
                    }

                    // Wander near portal while dodging breath
                    if (_randomWanderPos != null && mod.getPlayer().getBlockPos().isWithinDistance(_randomWanderPos, 2)) {
                        _randomWanderPos = null;
                    }
                    if (_randomWanderPos != null && _randomWanderChangeTimeout.elapsed()) {
                        _randomWanderPos = null;
                    }
                    if (_randomWanderPos == null) {
                        _randomWanderPos = getRandomWanderPos(mod);
                        _randomWanderChangeTimeout.reset();
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                    }
                    if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                        mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(_randomWanderPos))
                        );
                    }
                    setDebugState("Waiting for perch (HP=" + (int) dragon.getHealth() + ")");
                }
            }
            return null;
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, false);
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SPRINT, false);
            stopBow(mod);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof PunkEnderDragonTask;
        }

        @Override
        protected String toDebugString() {
            return "Punking the dragon (smart)";
        }

        private BlockPos getRandomWanderPos(AltoClef mod) {
            double RADIUS_RANGE = 20;
            double MIN_RADIUS = 5;
            BlockPos pos = null;
            int allowed = 5000;

            while (pos == null) {
                if (allowed-- < 0) {
                    Debug.logWarning("Failed to find random solid ground in end, this may lead to problems.");
                    return null;
                }
                double radius = MIN_RADIUS + (RADIUS_RANGE - MIN_RADIUS) * Math.random();
                double angle = Math.PI * 2 * Math.random();
                int x = (int) (radius * Math.cos(angle)),
                        z = (int) (radius * Math.sin(angle));
                int y = WorldHelper.getGroundHeight(mod, x, z);
                if (y == -1) continue;
                BlockPos check = new BlockPos(x, y, z);
                if (mod.getWorld().getBlockState(check).getBlock() == Blocks.END_STONE) {
                    pos = check.up();
                }
            }
            return pos;
        }


        private void updateBreathCostMap(AltoClef mod) {
            _breathCostMap.clear();
            double radius = 5;
            for (AreaEffectCloudEntity cloud : mod.getEntityTracker().getTrackedEntities(AreaEffectCloudEntity.class)) {
                Vec3d c = cloud.getPos();
                for (int x = (int) (c.getX() - radius); x <= (int) (c.getX() + radius); ++x) {
                    for (int z = (int) (c.getZ() - radius); z <= (int) (c.getZ() + radius); ++z) {
                        BlockPos p = new BlockPos(x, cloud.getBlockPos().getY(), z);
                        double sqDist = p.getSquaredDistance(c, false);
                        if (sqDist < radius) {
                            double cost = 1500.0 / (sqDist + 1);
                            _breathCostMap.put(p, cost);
                            _breathCostMap.put(p.up(), cost);
                            _breathCostMap.put(p.down(), cost);
                        }
                    }
                }
            }
            // Also avoid dragon fireball impact zones
            for (DragonFireballEntity fireball : mod.getEntityTracker().getTrackedEntities(DragonFireballEntity.class)) {
                BlockPos fp = fireball.getBlockPos();
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        BlockPos p = fp.add(dx, 0, dz);
                        _breathCostMap.put(p, 2000.0);
                        _breathCostMap.put(p.up(), 2000.0);
                    }
                }
            }
        }

        private class AvoidDragonFireGoal implements Goal {

            @Override
            public boolean isInGoal(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return !_breathCostMap.containsKey(pos);
            }

            @Override
            public double heuristic(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return _breathCostMap.getOrDefault(pos, 0.0);
            }
        }
    }
}
