package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.io.IOException;
import java.util.function.Predicate;

public class CubeBounds {
    final BlockPos low;
    final BlockPos high;
    final Dimension dimension;
    final Predicate<BlockPos> predicate;

    public CubeBounds(final BlockPos startPos, final int sizeX, final int sizeY, final int sizeZ) {
        this(startPos, sizeX, sizeY, sizeZ, Dimension.OVERWORLD);
    }

    public CubeBounds(final BlockPos startPos, final int sizeX, final int sizeY, final int sizeZ, final Dimension dimension) {
        this.low = new BlockPos(startPos.getX(), startPos.getY(), startPos.getZ());
        this.high = new BlockPos(low.getX() + sizeX, low.getY() + sizeY, low.getZ() + sizeZ);
        this.dimension = dimension;

        this.predicate = (BlockPos e) ->
            low.getX() <= e.getX() &&
            low.getY() <= e.getY() &&
            low.getZ() <= e.getZ() &&
            e.getX() <= high.getX() &&
            e.getY() <= high.getY() &&
            e.getZ() <= high.getZ();
    }

    public boolean inside(final int x, final int y, final int z) {
        return low.getX() <= x && low.getY() <= y && low.getZ() <= z && high.getX() >= x && high.getY() >= y && high.getZ() >= z;
    }

    public boolean inside(final Vec3i vec) {
        return inside(vec.getX(), vec.getY(), vec.getZ());
    }

    public BlockPos getLow() {
        return this.low;
    }

    public BlockPos getHigh() {
        return this.high;
    }

    public Dimension getDimension() {
        return this.dimension;
    }

    public Predicate<BlockPos> getPredicate() {
        return this.predicate;
    }

    @Override
    public String toString() {
        return "CubeBounds[" + dimension + " (" + low.getX() + "," + low.getY() + "," + low.getZ() + ")-(" + high.getX() + "," + high.getY() + "," + high.getZ() + ")]";
    }
}
