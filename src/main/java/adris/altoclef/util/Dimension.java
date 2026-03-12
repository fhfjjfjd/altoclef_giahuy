package adris.altoclef.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;

public enum Dimension {
    OVERWORLD(0, 256, false, Blocks.COBBLESTONE, Blocks.DIRT),
    NETHER(0, 128, true, Blocks.COBBLESTONE, Blocks.NETHERRACK),
    END(0, 256, false, Blocks.COBBLESTONE, Blocks.END_STONE);

    private final int minY;
    private final int maxY;
    private final boolean hasCeiling;
    private final Block[] scaffoldBlocks;

    Dimension(int minY, int maxY, boolean hasCeiling, Block... scaffoldBlocks) {
        this.minY = minY;
        this.maxY = maxY;
        this.hasCeiling = hasCeiling;
        this.scaffoldBlocks = scaffoldBlocks;
    }

    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public boolean hasCeiling() { return hasCeiling; }
    public Block[] getScaffoldBlocks() { return scaffoldBlocks; }

    /** Check if a Y coordinate is valid for building in this dimension. */
    public boolean isValidBuildY(int y) {
        return y >= minY && y < maxY;
    }

    /** Check if building at a Y level is considered "elevated" (above ground, needs scaffolding). */
    public boolean isElevated(int y, int groundY) {
        return y > groundY + 3;
    }

    /** Check if building at a Y level is considered "underground" (needs excavation). */
    public boolean isUnderground(int y, int surfaceY) {
        return y < surfaceY - 1;
    }

    public static Dimension current() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return OVERWORLD;
        if (mc.world.getDimension().isUltrawarm()) return NETHER;
        if (mc.world.getDimension().isNatural()) return OVERWORLD;
        return END;
    }
}
