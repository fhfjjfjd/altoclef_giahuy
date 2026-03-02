package adris.altoclef.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;

import java.util.HashMap;
import java.util.Map;

public class SchematicBlockMapper {
    private static final Map<Block, Item> BLOCK_TO_ITEM = new HashMap<>();

    static {
        // Fluids
        BLOCK_TO_ITEM.put(Blocks.WATER, Items.WATER_BUCKET);
        BLOCK_TO_ITEM.put(Blocks.LAVA, Items.LAVA_BUCKET);

        // Redstone
        BLOCK_TO_ITEM.put(Blocks.REDSTONE_WIRE, Items.REDSTONE);

        // Torches (wall variants)
        BLOCK_TO_ITEM.put(Blocks.WALL_TORCH, Items.TORCH);
        BLOCK_TO_ITEM.put(Blocks.SOUL_WALL_TORCH, Items.SOUL_TORCH);
        BLOCK_TO_ITEM.put(Blocks.REDSTONE_WALL_TORCH, Items.REDSTONE_TORCH);

        // Crops
        BLOCK_TO_ITEM.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.CARROTS, Items.CARROT);
        BLOCK_TO_ITEM.put(Blocks.POTATOES, Items.POTATO);
        BLOCK_TO_ITEM.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.MELON_STEM, Items.MELON_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.PUMPKIN_STEM, Items.PUMPKIN_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.ATTACHED_MELON_STEM, Items.MELON_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.ATTACHED_PUMPKIN_STEM, Items.PUMPKIN_SEEDS);
        BLOCK_TO_ITEM.put(Blocks.SWEET_BERRY_BUSH, Items.SWEET_BERRIES);
        BLOCK_TO_ITEM.put(Blocks.COCOA, Items.COCOA_BEANS);

        // Tripwire
        BLOCK_TO_ITEM.put(Blocks.TRIPWIRE, Items.STRING);

        // Blocks that are auto-generated (skip, cannot be placed directly)
        BLOCK_TO_ITEM.put(Blocks.PISTON_HEAD, null);
        BLOCK_TO_ITEM.put(Blocks.MOVING_PISTON, null);
        BLOCK_TO_ITEM.put(Blocks.FIRE, null);
        BLOCK_TO_ITEM.put(Blocks.SOUL_FIRE, null);
        BLOCK_TO_ITEM.put(Blocks.NETHER_PORTAL, null);
        BLOCK_TO_ITEM.put(Blocks.END_PORTAL, null);
        BLOCK_TO_ITEM.put(Blocks.END_GATEWAY, null);
        BLOCK_TO_ITEM.put(Blocks.FROSTED_ICE, null);
        BLOCK_TO_ITEM.put(Blocks.BUBBLE_COLUMN, null);
    }

    /**
     * Map a schematic BlockState to the Item needed to obtain/place it.
     *
     * @return the obtainable Item, or null if the block should be SKIPPED
     */
    public static Item getObtainableItem(BlockState state) {
        Block block = state.getBlock();
        if (BLOCK_TO_ITEM.containsKey(block)) {
            return BLOCK_TO_ITEM.get(block);
        }
        Item defaultItem = block.asItem();
        if (defaultItem == Items.AIR) {
            return null;
        }
        return defaultItem;
    }
}
