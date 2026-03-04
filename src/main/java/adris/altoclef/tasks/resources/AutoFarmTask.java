package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.function.Predicate;

/**
 * Advanced Auto-Farm task with farmland expansion.
 *
 * Farmland crops:  wheat, carrot, potato, beetroot
 *   - Auto-expand: builds 9x9 farm plots with water center
 *   - Auto-till dirt, plant seeds, harvest mature crops
 *   - Bone meal acceleration when available
 *   - Auto-replant after harvest
 *
 * Ground crops:    sugar_cane, cactus, bamboo, sweet_berries, melon, pumpkin
 * Nether crops:    nether_wart
 * Tree crops:      cocoa_beans
 *
 * Usage: @auto-farm wheat 64
 */
public class AutoFarmTask extends ResourceTask {

    public enum FarmState {
        HARVESTING,
        REPLANTING,
        BONE_MEALING,
        EXPANDING,
        TILLING,
        GETTING_SEEDS,
        GETTING_HOE,
        PICKING_UP,
        SEARCHING
    }

    public enum CropType {
        FARMLAND_CROP,
        GROUND_PLANT,
        NETHER_CROP,
        TREE_CROP
    }

    private final ItemTarget _target;
    private final String _resourceName;
    private FarmState _farmState = FarmState.SEARCHING;

    private final Set<BlockPos> _emptyCropland = new HashSet<>();
    private final Set<BlockPos> _tilledPositions = new HashSet<>();
    private final HashSet<BlockPos> _wasFullyGrown = new HashSet<>();

    // Farm expansion: track water sources we've built around
    private final Set<BlockPos> _expandedWaterSources = new HashSet<>();
    // 9x9 farm layout: water in center, 4-block radius of farmland
    private static final int FARM_RADIUS = 4;
    // Max farm plots to expand to
    private static final int MAX_FARM_PLOTS = 4;

    private static final Map<String, FarmInfo> FARM_REGISTRY = new LinkedHashMap<>();
    private static final Item[] ALL_HOES = new Item[]{
            Items.NETHERITE_HOE, Items.DIAMOND_HOE, Items.IRON_HOE,
            Items.GOLDEN_HOE, Items.STONE_HOE, Items.WOODEN_HOE
    };

    static {
        // Farmland crops (need farmland + water, replantable, expandable)
        register("wheat", CropType.FARMLAND_CROP, Items.WHEAT, Blocks.WHEAT, Items.WHEAT_SEEDS);
        register("carrot", CropType.FARMLAND_CROP, Items.CARROT, Blocks.CARROTS, Items.CARROT);
        register("potato", CropType.FARMLAND_CROP, Items.POTATO, Blocks.POTATOES, Items.POTATO);
        register("beetroot", CropType.FARMLAND_CROP, Items.BEETROOT, Blocks.BEETROOTS, Items.BEETROOT_SEEDS);

        // Ground plants (break to harvest, no farmland needed)
        register("sugar_cane", CropType.GROUND_PLANT, Items.SUGAR_CANE, Blocks.SUGAR_CANE, null);
        register("cactus", CropType.GROUND_PLANT, Items.CACTUS, Blocks.CACTUS, null);
        register("bamboo", CropType.GROUND_PLANT, Items.BAMBOO, Blocks.BAMBOO, null);
        register("sweet_berries", CropType.GROUND_PLANT, Items.SWEET_BERRIES, Blocks.SWEET_BERRY_BUSH, null);

        // Stem crops (break fruit, not stem)
        register("melon", CropType.GROUND_PLANT, Items.MELON_SLICE, Blocks.MELON, null);
        register("pumpkin", CropType.GROUND_PLANT, Items.PUMPKIN, Blocks.PUMPKIN, null);

        // Nether crops
        register("nether_wart", CropType.NETHER_CROP, Items.NETHER_WART, Blocks.NETHER_WART, Items.NETHER_WART);

        // Tree crops
        register("cocoa_beans", CropType.TREE_CROP, Items.COCOA_BEANS, Blocks.COCOA, Items.COCOA_BEANS);
    }

    private static void register(String name, CropType type, Item product, Block cropBlock, Item seed) {
        FARM_REGISTRY.put(name, new FarmInfo(name, type, product, cropBlock, seed));
    }

    public AutoFarmTask(String resourceName, int count) {
        super(new ItemTarget(resourceName, count));
        _resourceName = resourceName.toLowerCase();
        _target = new ItemTarget(resourceName, count);
    }

    public static Set<String> getSupportedCrops() {
        return FARM_REGISTRY.keySet();
    }

    public static boolean isSupported(String name) {
        return FARM_REGISTRY.containsKey(name.toLowerCase());
    }

    public FarmState getFarmState() {
        return _farmState;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        _emptyCropland.clear();
        _tilledPositions.clear();
        _wasFullyGrown.clear();
        _expandedWaterSources.clear();

        FarmInfo info = FARM_REGISTRY.get(_resourceName);
        if (info != null) {
            mod.getBlockTracker().trackBlock(info.cropBlock);
        }
        mod.getBlockTracker().trackBlock(Blocks.FARMLAND);
        mod.getBlockTracker().trackBlock(Blocks.WATER);
        mod.getBlockTracker().trackBlock(Blocks.GRASS_BLOCK);
        mod.getBlockTracker().trackBlock(Blocks.DIRT);
        if (_resourceName.equals("nether_wart")) {
            mod.getBlockTracker().trackBlock(Blocks.SOUL_SAND);
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        FarmInfo info = FARM_REGISTRY.get(_resourceName);
        if (info == null) {
            mod.logWarning("Unknown crop: " + _resourceName
                    + ". Supported: " + String.join(", ", FARM_REGISTRY.keySet()));
            return null;
        }

        // Route based on crop type
        switch (info.type) {
            case FARMLAND_CROP:
                return tickFarmlandCrop(mod, info);
            case GROUND_PLANT:
                return tickGroundPlant(mod, info);
            case NETHER_CROP:
                return tickNetherCrop(mod, info);
            case TREE_CROP:
                return tickTreeCrop(mod, info);
        }
        return null;
    }

    /**
     * Farmland crops: wheat, carrot, potato, beetroot
     * Full cycle: harvest → pick up drops → replant → bone meal → expand farmland → till → plant → get seeds
     */
    private Task tickFarmlandCrop(AltoClef mod, FarmInfo info) {
        // 1. Pick up nearby dropped items first
        Task pickupTask = tryPickupDrops(mod, info);
        if (pickupTask != null) return pickupTask;

        // 2. Harvest mature crops
        if (hasMatureCrops(mod, info)) {
            _farmState = FarmState.HARVESTING;
            setDebugState("Harvesting mature " + _resourceName);
            return new CollectCropTask(_target, info.cropBlock, info.seedItem);
        }

        // 3. Replant on empty farmland (if we have seeds)
        if (info.seedItem != null && mod.getInventoryTracker().hasItem(info.seedItem)) {
            BlockPos emptyFarmland = findEmptyFarmland(mod);
            if (emptyFarmland != null) {
                _farmState = FarmState.REPLANTING;
                setDebugState("Planting " + _resourceName + " on farmland");
                return new InteractWithBlockTask(new ItemTarget(info.seedItem, 1), Direction.UP, emptyFarmland, true);
            }
        }

        // 4. Use bone meal on immature crops if available
        Task boneMealTask = tryBoneMeal(mod, info);
        if (boneMealTask != null) return boneMealTask;

        // 5. EXPAND FARMLAND - create new 9x9 farm plots
        Task expandTask = tryExpandFarmland(mod, info);
        if (expandTask != null) return expandTask;

        // 6. Get seeds if we have none
        if (info.seedItem != null && !mod.getInventoryTracker().hasItem(info.seedItem)) {
            _farmState = FarmState.GETTING_SEEDS;
            setDebugState("Getting seeds for " + _resourceName);
            if (_resourceName.equals("wheat")) {
                return TaskCatalogue.getItemTask("wheat_seeds", 1);
            } else {
                return TaskCatalogue.getItemTask(_resourceName, 1);
            }
        }

        // 7. Wander to find more
        _farmState = FarmState.SEARCHING;
        setDebugState("Searching for crops...");
        return new TimeoutWanderTask(20);
    }

    /**
     * Ground plants: sugar_cane, cactus, bamboo, sweet_berries, melon, pumpkin
     * Just break and collect. For tall plants (sugar_cane, cactus, bamboo), break upper blocks only.
     */
    private Task tickGroundPlant(AltoClef mod, FarmInfo info) {
        // Pick up drops
        Task pickupTask = tryPickupDrops(mod, info);
        if (pickupTask != null) return pickupTask;

        boolean isTallPlant = _resourceName.equals("sugar_cane") || _resourceName.equals("cactus") || _resourceName.equals("bamboo");

        Predicate<BlockPos> validBlock;
        if (isTallPlant) {
            // Only break blocks that have the same block below (preserve base)
            validBlock = pos -> {
                Block below = mod.getWorld().getBlockState(pos.down()).getBlock();
                return below == info.cropBlock;
            };
        } else if (_resourceName.equals("sweet_berries")) {
            // Sweet berries: only harvest when age >= 2
            validBlock = pos -> {
                BlockState state = mod.getWorld().getBlockState(pos);
                if (state.getBlock() != Blocks.SWEET_BERRY_BUSH) return false;
                return state.get(SweetBerryBushBlock.AGE) >= 2;
            };
        } else {
            // Melon/pumpkin: just break the fruit block
            validBlock = pos -> true;
        }

        if (mod.getBlockTracker().anyFound(validBlock, info.cropBlock)) {
            _farmState = FarmState.HARVESTING;
            setDebugState("Harvesting " + _resourceName);
            if (_resourceName.equals("sweet_berries")) {
                // Right-click to harvest berries (don't destroy bush)
                return new DoToClosestBlockTask(
                        pos -> new InteractWithBlockTask(new ItemTarget(Items.AIR, 0), Direction.UP, pos, true),
                        validBlock,
                        info.cropBlock
                );
            }
            return new DoToClosestBlockTask(DestroyBlockTask::new, validBlock, info.cropBlock);
        }

        _farmState = FarmState.SEARCHING;
        setDebugState("Searching for " + _resourceName + "...");
        return new TimeoutWanderTask(20);
    }

    /**
     * Nether crops: nether_wart
     * Grows on soul sand, harvest when mature (age 3).
     */
    private Task tickNetherCrop(AltoClef mod, FarmInfo info) {
        Task pickupTask = tryPickupDrops(mod, info);
        if (pickupTask != null) return pickupTask;

        // Harvest mature nether wart (age 3)
        Predicate<BlockPos> matureWart = pos -> {
            BlockState state = mod.getWorld().getBlockState(pos);
            if (state.getBlock() != Blocks.NETHER_WART) return false;
            return state.get(NetherWartBlock.AGE) >= 3;
        };

        if (mod.getBlockTracker().anyFound(matureWart, Blocks.NETHER_WART)) {
            _farmState = FarmState.HARVESTING;
            setDebugState("Harvesting mature nether wart");
            return new DoToClosestBlockTask(DestroyBlockTask::new, matureWart, Blocks.NETHER_WART);
        }

        // Replant on empty soul sand
        if (mod.getInventoryTracker().hasItem(Items.NETHER_WART)) {
            BlockPos emptySoulSand = mod.getBlockTracker().getNearestTracking(
                    pos -> mod.getWorld().getBlockState(pos).getBlock() == Blocks.SOUL_SAND
                            && WorldHelper.isAir(mod, pos.up()),
                    Blocks.SOUL_SAND
            );
            if (emptySoulSand != null) {
                _farmState = FarmState.REPLANTING;
                setDebugState("Planting nether wart");
                return new InteractWithBlockTask(new ItemTarget(Items.NETHER_WART, 1), Direction.UP, emptySoulSand, true);
            }
        }

        _farmState = FarmState.SEARCHING;
        setDebugState("Searching for nether wart...");
        return new TimeoutWanderTask(20);
    }

    /**
     * Tree crops: cocoa_beans
     * Grows on jungle logs, harvest when mature (age 2).
     */
    private Task tickTreeCrop(AltoClef mod, FarmInfo info) {
        Task pickupTask = tryPickupDrops(mod, info);
        if (pickupTask != null) return pickupTask;

        // Harvest mature cocoa (age 2)
        Predicate<BlockPos> matureCocoa = pos -> {
            if (!mod.getChunkTracker().isChunkLoaded(pos)) return _wasFullyGrown.contains(pos);
            BlockState state = mod.getWorld().getBlockState(pos);
            if (state.getBlock() != Blocks.COCOA) return false;
            boolean mature = state.get(CocoaBlock.AGE) >= 2;
            if (mature) _wasFullyGrown.add(pos);
            else _wasFullyGrown.remove(pos);
            return mature;
        };

        if (mod.getBlockTracker().anyFound(matureCocoa, Blocks.COCOA)) {
            _farmState = FarmState.HARVESTING;
            setDebugState("Harvesting mature cocoa");
            return new DoToClosestBlockTask(DestroyBlockTask::new, matureCocoa, Blocks.COCOA);
        }

        _farmState = FarmState.SEARCHING;
        setDebugState("Searching for cocoa...");
        return new TimeoutWanderTask(20);
    }

    // ==================== FARMLAND EXPANSION ====================

    /**
     * Expand farmland: find water, till dirt in 9x9 pattern, plant seeds.
     * Layout:  4 blocks of farmland in each direction from water center.
     *
     *   FFFFFFFFF
     *   FFFFFFFFF
     *   FFFFFFFFF
     *   FFFFFFFFF
     *   FFFFWFFFF    (W = water)
     *   FFFFFFFFF
     *   FFFFFFFFF
     *   FFFFFFFFF
     *   FFFFFFFFF
     */
    private Task tryExpandFarmland(AltoClef mod, FarmInfo info) {
        // Don't expand if we already have enough farm plots
        if (_expandedWaterSources.size() >= MAX_FARM_PLOTS) return null;

        // Step 1: Ensure we have a hoe
        if (!mod.getInventoryTracker().hasItem(ALL_HOES)) {
            _farmState = FarmState.GETTING_HOE;
            setDebugState("Crafting hoe for farm expansion");
            return TaskCatalogue.getItemTask("wooden_hoe", 1);
        }

        // Step 2: Find a water source to expand around
        BlockPos waterSource = findWaterForExpansion(mod);
        if (waterSource == null) {
            // No water nearby, wander to find some
            _farmState = FarmState.SEARCHING;
            setDebugState("Looking for water source...");
            return new TimeoutWanderTask(20);
        }

        // Step 3: Find untilled dirt/grass within range of this water
        BlockPos dirtToTill = findDirtToTill(mod, waterSource);
        if (dirtToTill != null) {
            _farmState = FarmState.TILLING;
            setDebugState("Expanding farmland (" + _expandedWaterSources.size() + "/" + MAX_FARM_PLOTS + " plots)");
            _tilledPositions.add(dirtToTill);
            return new InteractWithBlockTask(new ItemTarget(ALL_HOES, 1), Direction.UP, dirtToTill, true);
        }

        // Step 4: Plant seeds on newly tilled farmland
        if (info.seedItem != null && mod.getInventoryTracker().hasItem(info.seedItem)) {
            BlockPos emptyFarmland = findEmptyFarmlandNear(mod, waterSource);
            if (emptyFarmland != null) {
                _farmState = FarmState.REPLANTING;
                setDebugState("Planting on expanded farmland");
                return new InteractWithBlockTask(new ItemTarget(info.seedItem, 1), Direction.UP, emptyFarmland, true);
            }
        }

        // This water source is fully expanded
        _expandedWaterSources.add(waterSource);
        return null;
    }

    /**
     * Find a water source block suitable for farm expansion.
     * Prefers water at surface level with dirt/grass nearby.
     */
    private BlockPos findWaterForExpansion(AltoClef mod) {
        return mod.getBlockTracker().getNearestTracking(
                pos -> {
                    // Skip already expanded sources
                    for (BlockPos expanded : _expandedWaterSources) {
                        if (pos.isWithinDistance(expanded, 2)) return false;
                    }
                    // Must be at surface (air above)
                    if (!WorldHelper.isAir(mod, pos.up())) return false;
                    // Must have some dirt/grass nearby to till
                    return hasDirtNearby(mod, pos);
                },
                Blocks.WATER
        );
    }

    private boolean hasDirtNearby(AltoClef mod, BlockPos center) {
        for (int dx = -FARM_RADIUS; dx <= FARM_RADIUS; dx++) {
            for (int dz = -FARM_RADIUS; dz <= FARM_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos check = center.add(dx, 0, dz);
                Block block = mod.getWorld().getBlockState(check).getBlock();
                if ((block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) && WorldHelper.isAir(mod, check.up())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find dirt/grass block within FARM_RADIUS of water that can be tilled.
     */
    private BlockPos findDirtToTill(AltoClef mod, BlockPos waterCenter) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int dx = -FARM_RADIUS; dx <= FARM_RADIUS; dx++) {
            for (int dz = -FARM_RADIUS; dz <= FARM_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = waterCenter.add(dx, 0, dz);
                Block block = mod.getWorld().getBlockState(pos).getBlock();
                if ((block == Blocks.DIRT || block == Blocks.GRASS_BLOCK)
                        && WorldHelper.isAir(mod, pos.up())
                        && !_tilledPositions.contains(pos)) {
                    double dist = pos.getSquaredDistance(mod.getPlayer().getPos(), false);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = pos;
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Find empty farmland near a specific water source.
     */
    private BlockPos findEmptyFarmlandNear(AltoClef mod, BlockPos waterCenter) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int dx = -FARM_RADIUS; dx <= FARM_RADIUS; dx++) {
            for (int dz = -FARM_RADIUS; dz <= FARM_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos pos = waterCenter.add(dx, 0, dz);
                if (mod.getWorld().getBlockState(pos).getBlock() == Blocks.FARMLAND
                        && WorldHelper.isAir(mod, pos.up())) {
                    double dist = pos.getSquaredDistance(mod.getPlayer().getPos(), false);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = pos;
                    }
                }
            }
        }
        return nearest;
    }

    // ==================== HELPERS ====================

    private BlockPos findEmptyFarmland(AltoClef mod) {
        return mod.getBlockTracker().getNearestTracking(
                pos -> mod.getWorld().getBlockState(pos).getBlock() == Blocks.FARMLAND
                        && WorldHelper.isAir(mod, pos.up()),
                Blocks.FARMLAND
        );
    }

    private boolean hasMatureCrops(AltoClef mod, FarmInfo info) {
        return mod.getBlockTracker().anyFound(pos -> isMature(mod, pos), info.cropBlock);
    }

    private boolean isMature(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos) || mod.getBlockTracker().unreachable(pos)) {
            return _wasFullyGrown.contains(pos);
        }
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            boolean mature = crop.isMature(state);
            if (mature) _wasFullyGrown.add(pos);
            else _wasFullyGrown.remove(pos);
            return mature;
        }
        return false;
    }

    /**
     * Use bone meal on immature crops to speed up growth.
     */
    private Task tryBoneMeal(AltoClef mod, FarmInfo info) {
        if (!mod.getInventoryTracker().hasItem(Items.BONE_MEAL)) return null;

        // Find immature crop to bone meal
        BlockPos immatureCrop = mod.getBlockTracker().getNearestTracking(
                pos -> {
                    BlockState state = mod.getWorld().getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock crop)) return false;
                    return !crop.isMature(state);
                },
                info.cropBlock
        );

        if (immatureCrop != null) {
            _farmState = FarmState.BONE_MEALING;
            setDebugState("Bone mealing " + _resourceName);
            return new InteractWithBlockTask(new ItemTarget(Items.BONE_MEAL, 1), Direction.UP, immatureCrop, true);
        }
        return null;
    }

    /**
     * Pick up dropped items nearby.
     */
    private Task tryPickupDrops(AltoClef mod, FarmInfo info) {
        if (mod.getEntityTracker().itemDropped(info.productItem)) {
            ItemEntity drop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), info.productItem);
            if (drop != null && drop.isInRange(mod.getPlayer(), 10)) {
                _farmState = FarmState.PICKING_UP;
                setDebugState("Picking up " + _resourceName);
                return new PickupDroppedItemTask(new ItemTarget(info.productItem, 9999), true);
            }
        }
        // Also pick up seeds
        if (info.seedItem != null && info.seedItem != info.productItem && mod.getEntityTracker().itemDropped(info.seedItem)) {
            ItemEntity drop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), info.seedItem);
            if (drop != null && drop.isInRange(mod.getPlayer(), 10)) {
                _farmState = FarmState.PICKING_UP;
                setDebugState("Picking up seeds");
                return new PickupDroppedItemTask(new ItemTarget(info.seedItem, 9999), true);
            }
        }
        return null;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        FarmInfo info = FARM_REGISTRY.get(_resourceName);
        if (info != null) {
            mod.getBlockTracker().stopTracking(info.cropBlock);
        }
        mod.getBlockTracker().stopTracking(Blocks.FARMLAND);
        mod.getBlockTracker().stopTracking(Blocks.WATER);
        mod.getBlockTracker().stopTracking(Blocks.GRASS_BLOCK);
        mod.getBlockTracker().stopTracking(Blocks.DIRT);
        if (_resourceName.equals("nether_wart")) {
            mod.getBlockTracker().stopTracking(Blocks.SOUL_SAND);
        }
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof AutoFarmTask task) {
            return task._resourceName.equals(_resourceName) && task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Auto-Farming " + _resourceName + " [" + _farmState + "]";
    }

    public static class FarmInfo {
        public final String name;
        public final CropType type;
        public final Item productItem;
        public final Block cropBlock;
        public final Item seedItem;

        public FarmInfo(String name, CropType type, Item productItem, Block cropBlock, Item seedItem) {
            this.name = name;
            this.type = type;
            this.productItem = productItem;
            this.cropBlock = cropBlock;
            this.seedItem = seedItem;
        }
    }
}
