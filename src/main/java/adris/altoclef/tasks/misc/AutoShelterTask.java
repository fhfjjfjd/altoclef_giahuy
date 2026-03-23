package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Task to build a 5x5x4 shelter when it's nighttime.
 * Supports multiple wall materials (cobblestone > dirt > planks), places torches inside,
 * places a bed if available, crafts a door if needed, and checks for existing shelter.
 */
public class AutoShelterTask extends Task {

    private static final int SHELTER_WIDTH = 5;
    private static final int SHELTER_DEPTH = 5;
    private static final int SHELTER_HEIGHT = 4;
    private static final int REQUIRED_BLOCKS = 100;
    private static final double EXISTING_SHELTER_CHECK_RADIUS = 20.0;

    private BlockPos _origin = null;
    private List<BlockPos> _blocksToPlace = null;
    private int _currentBlockIndex = 0;
    private boolean _collectingMaterials = false;
    private Block _wallMaterial = null;
    private Item _wallMaterialItem = null;
    private boolean _placedTorches = false;
    private boolean _placedBed = false;
    private boolean _placedDoor = false;

    private enum ShelterPhase {
        CHECKING_EXISTING,
        GATHERING_MATERIALS,
        BUILDING_WALLS,
        PLACING_TORCHES,
        PLACING_BED,
        PLACING_DOOR,
        DONE
    }

    private ShelterPhase _phase = ShelterPhase.CHECKING_EXISTING;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-shelter task...");
        _origin = null;
        _blocksToPlace = null;
        _currentBlockIndex = 0;
        _collectingMaterials = false;
        _wallMaterial = null;
        _wallMaterialItem = null;
        _placedTorches = false;
        _placedBed = false;
        _placedDoor = false;
        _phase = ShelterPhase.CHECKING_EXISTING;
    }

    /**
     * Choose the best available wall material: cobblestone > dirt > oak planks.
     */
    private void selectWallMaterial(AltoClef mod) {
        int cobble = mod.getInventoryTracker().getItemCount(Items.COBBLESTONE);
        int dirt = mod.getInventoryTracker().getItemCount(Items.DIRT);
        int planks = mod.getInventoryTracker().getItemCount(Items.OAK_PLANKS);

        if (cobble >= 10 || (cobble >= dirt && cobble >= planks)) {
            _wallMaterial = Blocks.COBBLESTONE;
            _wallMaterialItem = Items.COBBLESTONE;
        } else if (dirt >= 10 || dirt >= planks) {
            _wallMaterial = Blocks.DIRT;
            _wallMaterialItem = Items.DIRT;
        } else {
            _wallMaterial = Blocks.OAK_PLANKS;
            _wallMaterialItem = Items.OAK_PLANKS;
        }
    }

    /**
     * Check if the player already has a shelter nearby (enclosed roof overhead).
     */
    private boolean hasExistingShelterNearby(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        // Check in a radius for a solid block roof above the player
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos ground = playerPos.add(dx, 0, dz);
                boolean hasRoof = false;
                boolean hasWalls = true;
                // Check for roof within 5 blocks above
                for (int dy = 1; dy <= 5; dy++) {
                    if (!mod.getWorld().getBlockState(ground.up(dy)).isAir()) {
                        hasRoof = true;
                        break;
                    }
                }
                if (hasRoof) {
                    // Quick wall check: see if at least 3 sides are blocked at ground level
                    int solidSides = 0;
                    if (!mod.getWorld().getBlockState(ground.north()).isAir()) solidSides++;
                    if (!mod.getWorld().getBlockState(ground.south()).isAir()) solidSides++;
                    if (!mod.getWorld().getBlockState(ground.east()).isAir()) solidSides++;
                    if (!mod.getWorld().getBlockState(ground.west()).isAir()) solidSides++;
                    if (solidSides >= 3) return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if it's nighttime
        long timeOfDay = mod.getWorld().getTimeOfDay() % 24000;
        if (timeOfDay < 13000 && _origin == null) {
            setDebugState("Waiting for nighttime to build shelter...");
            return null;
        }

        // Check for existing shelter nearby before building a new one
        if (_phase == ShelterPhase.CHECKING_EXISTING && _origin == null) {
            if (hasExistingShelterNearby(mod)) {
                mod.log("Found existing shelter nearby, no need to build a new one.");
                _phase = ShelterPhase.DONE;
                return null;
            }
            _phase = ShelterPhase.GATHERING_MATERIALS;
        }

        // Select wall material based on inventory
        if (_wallMaterial == null) {
            selectWallMaterial(mod);
            mod.log("Selected wall material: " + _wallMaterialItem.getName().getString());
        }

        // Ensure we have enough building materials
        int blockCount = mod.getInventoryTracker().getItemCount(_wallMaterialItem);
        if (blockCount < REQUIRED_BLOCKS && _origin == null) {
            setDebugState("Collecting " + _wallMaterialItem.getName().getString() + " (" + blockCount + "/" + REQUIRED_BLOCKS + ")...");
            _collectingMaterials = true;
            _phase = ShelterPhase.GATHERING_MATERIALS;
            Task gatherTask = TaskCatalogue.getItemTask(_wallMaterialItem, REQUIRED_BLOCKS);
            if (gatherTask != null) {
                return gatherTask;
            }
            return null;
        }
        _collectingMaterials = false;

        // Pick a build origin near the player
        if (_origin == null) {
            _origin = mod.getPlayer().getBlockPos().add(2, 0, 2);
            _blocksToPlace = generateShelterBlocks(_origin);
            _currentBlockIndex = 0;
            _phase = ShelterPhase.BUILDING_WALLS;
            mod.log("Building shelter at " + _origin.toShortString() + " with " + _wallMaterialItem.getName().getString());
        }

        // Phase: Building walls and roof
        if (_phase == ShelterPhase.BUILDING_WALLS) {
            if (_currentBlockIndex < _blocksToPlace.size()) {
                BlockPos target = _blocksToPlace.get(_currentBlockIndex);

                // Skip if block is already placed (any solid block)
                if (!mod.getWorld().getBlockState(target).isAir()) {
                    _currentBlockIndex++;
                    return onTick(mod);
                }

                // Check if we still have material to place
                if (mod.getInventoryTracker().getItemCount(_wallMaterialItem) < 1) {
                    // Try fallback materials
                    selectWallMaterial(mod);
                    if (mod.getInventoryTracker().getItemCount(_wallMaterialItem) < 1) {
                        setDebugState("Ran out of building materials, collecting more...");
                        Task gatherTask = TaskCatalogue.getItemTask(_wallMaterialItem, REQUIRED_BLOCKS);
                        if (gatherTask != null) {
                            return gatherTask;
                        }
                        return null;
                    }
                }

                setDebugState("Placing block " + (_currentBlockIndex + 1) + "/" + _blocksToPlace.size());
                return new PlaceBlockTask(target, _wallMaterial);
            }
            mod.log("Walls and roof complete! Adding interior features...");
            _phase = ShelterPhase.PLACING_TORCHES;
        }

        // Phase: Place torches inside for light
        if (_phase == ShelterPhase.PLACING_TORCHES && !_placedTorches) {
            if (mod.getInventoryTracker().hasItem(Items.TORCH)) {
                // Place torches on the interior walls: two torches on opposite walls
                BlockPos torch1 = _origin.add(1, 2, SHELTER_DEPTH / 2);
                BlockPos torch2 = _origin.add(SHELTER_WIDTH - 2, 2, SHELTER_DEPTH / 2);

                if (mod.getWorld().getBlockState(torch1).isAir()) {
                    setDebugState("Placing interior torch 1...");
                    return new PlaceBlockTask(torch1, Blocks.TORCH);
                }
                if (mod.getWorld().getBlockState(torch2).isAir()) {
                    setDebugState("Placing interior torch 2...");
                    return new PlaceBlockTask(torch2, Blocks.TORCH);
                }
            } else {
                mod.log("No torches available for interior lighting.");
            }
            _placedTorches = true;
            _phase = ShelterPhase.PLACING_BED;
        }

        // Phase: Place bed if available
        if (_phase == ShelterPhase.PLACING_BED && !_placedBed) {
            Item bedItem = findBedItem(mod);
            if (bedItem != null) {
                BlockPos bedPos = _origin.add(SHELTER_WIDTH - 2, 1, SHELTER_DEPTH - 2);
                if (mod.getWorld().getBlockState(bedPos).isAir()) {
                    setDebugState("Placing bed inside shelter...");
                    Block bedBlock = getBedBlock(bedItem);
                    if (bedBlock != null) {
                        return new PlaceBlockTask(bedPos, bedBlock);
                    }
                }
            } else {
                mod.log("No bed available to place in shelter.");
            }
            _placedBed = true;
            _phase = ShelterPhase.PLACING_DOOR;
        }

        // Phase: Place door at entrance
        if (_phase == ShelterPhase.PLACING_DOOR && !_placedDoor) {
            if (!mod.getInventoryTracker().hasItem(Items.OAK_DOOR)) {
                // Try to craft a door if we have planks
                if (mod.getInventoryTracker().getItemCount(Items.OAK_PLANKS) >= 6) {
                    setDebugState("Crafting door for shelter...");
                    Task craftDoor = TaskCatalogue.getItemTask("oak_door", 1);
                    if (craftDoor != null) {
                        return craftDoor;
                    }
                } else {
                    mod.log("No door and not enough planks to craft one.");
                    _placedDoor = true;
                    _phase = ShelterPhase.DONE;
                }
            }

            if (mod.getInventoryTracker().hasItem(Items.OAK_DOOR)) {
                BlockPos doorPos = _origin.add(SHELTER_WIDTH / 2, 1, 0);
                if (mod.getWorld().getBlockState(doorPos).isAir()) {
                    setDebugState("Placing door...");
                    return new PlaceBlockTask(doorPos, Blocks.OAK_DOOR);
                }
            }
            _placedDoor = true;
            _phase = ShelterPhase.DONE;
        }

        mod.log("Shelter construction complete!");
        return null;
    }

    /**
     * Find any bed item in inventory.
     */
    private Item findBedItem(AltoClef mod) {
        Item[] beds = {Items.WHITE_BED, Items.RED_BED, Items.BLUE_BED, Items.GREEN_BED,
                Items.YELLOW_BED, Items.BLACK_BED, Items.BROWN_BED, Items.CYAN_BED,
                Items.GRAY_BED, Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED,
                Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED};
        for (Item bed : beds) {
            if (mod.getInventoryTracker().hasItem(bed)) {
                return bed;
            }
        }
        return null;
    }

    /**
     * Map bed item to bed block.
     */
    private Block getBedBlock(Item bedItem) {
        if (bedItem == Items.WHITE_BED) return Blocks.WHITE_BED;
        if (bedItem == Items.RED_BED) return Blocks.RED_BED;
        if (bedItem == Items.BLUE_BED) return Blocks.BLUE_BED;
        if (bedItem == Items.GREEN_BED) return Blocks.GREEN_BED;
        if (bedItem == Items.YELLOW_BED) return Blocks.YELLOW_BED;
        if (bedItem == Items.BLACK_BED) return Blocks.BLACK_BED;
        if (bedItem == Items.BROWN_BED) return Blocks.BROWN_BED;
        if (bedItem == Items.CYAN_BED) return Blocks.CYAN_BED;
        if (bedItem == Items.GRAY_BED) return Blocks.GRAY_BED;
        if (bedItem == Items.LIGHT_BLUE_BED) return Blocks.LIGHT_BLUE_BED;
        if (bedItem == Items.LIGHT_GRAY_BED) return Blocks.LIGHT_GRAY_BED;
        if (bedItem == Items.LIME_BED) return Blocks.LIME_BED;
        if (bedItem == Items.MAGENTA_BED) return Blocks.MAGENTA_BED;
        if (bedItem == Items.ORANGE_BED) return Blocks.ORANGE_BED;
        if (bedItem == Items.PINK_BED) return Blocks.PINK_BED;
        if (bedItem == Items.PURPLE_BED) return Blocks.PURPLE_BED;
        return Blocks.WHITE_BED;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-shelter task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoShelterTask;
    }

    @Override
    protected String toDebugString() {
        if (_phase == ShelterPhase.DONE) {
            return "Auto-shelter complete";
        }
        if (_collectingMaterials) {
            return "Collecting shelter materials (" + (_wallMaterialItem != null ? _wallMaterialItem.getName().getString() : "?") + ")";
        }
        if (_phase == ShelterPhase.PLACING_TORCHES) return "Placing torches inside shelter";
        if (_phase == ShelterPhase.PLACING_BED) return "Placing bed inside shelter";
        if (_phase == ShelterPhase.PLACING_DOOR) return "Placing door on shelter";
        if (_blocksToPlace != null) {
            return "Building shelter (" + _currentBlockIndex + "/" + _blocksToPlace.size() + ")";
        }
        return "Auto-shelter waiting";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _phase == ShelterPhase.DONE;
    }

    /**
     * Generate the list of block positions for a 5x5x4 cobblestone shelter.
     * Leaves a 1x2 opening in the front wall for a door.
     */
    private List<BlockPos> generateShelterBlocks(BlockPos origin) {
        List<BlockPos> blocks = new ArrayList<>();

        for (int y = 0; y < SHELTER_HEIGHT; y++) {
            for (int x = 0; x < SHELTER_WIDTH; x++) {
                for (int z = 0; z < SHELTER_DEPTH; z++) {
                    boolean isWall = x == 0 || x == SHELTER_WIDTH - 1 || z == 0 || z == SHELTER_DEPTH - 1;
                    boolean isRoof = y == SHELTER_HEIGHT - 1;
                    boolean isFloor = y == 0;

                    // Door opening: front wall (z == 0), center column (x == 2), bottom two blocks (y == 0, 1)
                    boolean isDoorOpening = z == 0 && x == SHELTER_WIDTH / 2 && y < 2;

                    if ((isWall || isRoof) && !isFloor && !isDoorOpening) {
                        blocks.add(origin.add(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }
}
