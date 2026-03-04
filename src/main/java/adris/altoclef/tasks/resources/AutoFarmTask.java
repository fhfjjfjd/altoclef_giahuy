package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;

public class AutoFarmTask extends ResourceTask {

    private final ItemTarget _target;
    private final String _resourceName;

    private static final Map<String, FarmInfo> FARMABLE_RESOURCES = new HashMap<>();
    private static final Item[] ALL_HOES = new Item[]{
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, 
            Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE
    };

    static {
        FARMABLE_RESOURCES.put("wheat", new FarmInfo(Items.WHEAT, Blocks.WHEAT, Items.WHEAT_SEEDS));
        FARMABLE_RESOURCES.put("carrot", new FarmInfo(Items.CARROT, Blocks.CARROTS, Items.CARROT));
        FARMABLE_RESOURCES.put("potato", new FarmInfo(Items.POTATO, Blocks.POTATOES, Items.POTATO));
        FARMABLE_RESOURCES.put("beetroot", new FarmInfo(Items.BEETROOT, Blocks.BEETROOTS, Items.BEETROOT_SEEDS));
    }

    public AutoFarmTask(String resourceName, int count) {
        super(new ItemTarget(resourceName, count));
        _resourceName = resourceName.toLowerCase();
        _target = new ItemTarget(resourceName, count);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        if (FARMABLE_RESOURCES.containsKey(_resourceName)) {
            mod.getBlockTracker().trackBlock(FARMABLE_RESOURCES.get(_resourceName).cropBlock);
        }
        mod.getBlockTracker().trackBlock(Blocks.FARMLAND);
        mod.getBlockTracker().trackBlock(Blocks.WATER);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!FARMABLE_RESOURCES.containsKey(_resourceName)) {
            mod.logWarning("Resource " + _resourceName + " is not farmable by AutoFarmTask.");
            return null;
        }

        FarmInfo info = FARMABLE_RESOURCES.get(_resourceName);

        // 1. If we have enough, we are done (handled by ResourceTask)
        
        // 2. If there are mature crops, collect them using CollectCropTask
        Task collectTask = new CollectCropTask(_target, info.cropBlock, info.seedItem);
        if (!collectTask.isFinished(mod)) {
            setDebugState("Collecting/Replanting crops");
            return collectTask;
        }

        // 3. If no mature crops but we have seeds, try to plant them on existing FARMLAND
        if (mod.getInventoryTracker().hasItem(info.seedItem)) {
            BlockPos emptyFarmland = mod.getBlockTracker().getNearestTracking(
                pos -> WorldHelper.isAir(mod, pos.up()) && mod.getWorld().getBlockState(pos).getBlock() == Blocks.FARMLAND,
                Blocks.FARMLAND
            );
            if (emptyFarmland != null) {
                setDebugState("Planting seeds on empty farmland");
                return new InteractWithBlockTask(new ItemTarget(info.seedItem, 1), Direction.UP, emptyFarmland, true);
            }
        }

        // 4. If we want to farm more but no farmland, create some!
        setDebugState("Looking for place to create farmland");
        BlockPos water = mod.getBlockTracker().getNearestTracking(Blocks.WATER);
        if (water != null) {
            BlockPos dirtToHoe = mod.getBlockTracker().getNearestTracking(
                pos -> (mod.getWorld().getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK || mod.getWorld().getBlockState(pos).getBlock() == Blocks.DIRT)
                        && WorldHelper.isAir(mod, pos.up())
                        && pos.isWithinDistance(water, 4.5), // Farmland stays hydrated up to 4 blocks away
                Blocks.GRASS_BLOCK, Blocks.DIRT
            );

            if (dirtToHoe != null) {
                // We need a hoe
                if (!mod.getInventoryTracker().hasItem(ALL_HOES)) {
                    setDebugState("Getting a hoe");
                    return TaskCatalogue.getItemTask("iron_hoe", 1);
                }
                setDebugState("Creating farmland");
                return new InteractWithBlockTask(new ItemTarget(ALL_HOES, 1), Direction.UP, dirtToHoe, true);
            }
        }

        // 5. If we don't even have seeds (for wheat), get some by breaking grass
        if (_resourceName.equals("wheat") && !mod.getInventoryTracker().hasItem(Items.WHEAT_SEEDS)) {
            setDebugState("Getting seeds from grass");
            return TaskCatalogue.getItemTask("wheat_seeds", 1);
        }

        // 6. If we can't find water, just get some water or find a lake
        if (water == null) {
            setDebugState("Finding water for farm");
            return new DoToClosestBlockTask(DestroyBlockTask::new, Blocks.WATER); 
        }

        setDebugState("Waiting for crops to grow...");
        return null;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        if (FARMABLE_RESOURCES.containsKey(_resourceName)) {
            mod.getBlockTracker().stopTracking(FARMABLE_RESOURCES.get(_resourceName).cropBlock);
        }
        mod.getBlockTracker().stopTracking(Blocks.FARMLAND);
        mod.getBlockTracker().stopTracking(Blocks.WATER);
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
        return "Auto-Farming " + _resourceName;
    }

    private static class FarmInfo {
        public final Item productItem;
        public final Block cropBlock;
        public final Item seedItem;

        public FarmInfo(Item productItem, Block cropBlock, Item seedItem) {
            this.productItem = productItem;
            this.cropBlock = cropBlock;
            this.seedItem = seedItem;
        }
    }
}
