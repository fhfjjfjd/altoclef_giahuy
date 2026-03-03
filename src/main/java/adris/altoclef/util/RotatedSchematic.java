package adris.altoclef.util;

import baritone.api.schematic.ISchematic;
import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRotation;

import java.util.List;

/**
 * Wraps an ISchematic and applies a Y-axis rotation (90°, 180°, 270° clockwise).
 * Rotates both block positions and block states (stairs, logs, etc.).
 */
public class RotatedSchematic implements ISchematic {

    private final ISchematic inner;
    private final BlockRotation rotation;
    private final int newWidthX;
    private final int newLengthZ;

    public RotatedSchematic(ISchematic inner, BlockRotation rotation) {
        this.inner = inner;
        this.rotation = rotation;

        if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
            this.newWidthX = inner.lengthZ();
            this.newLengthZ = inner.widthX();
        } else {
            this.newWidthX = inner.widthX();
            this.newLengthZ = inner.lengthZ();
        }
    }

    /**
     * Create a rotated schematic from a rotation count (0-3).
     * 0 = none, 1 = 90° CW, 2 = 180°, 3 = 270° CW
     */
    public static ISchematic rotate(ISchematic schematic, int rotationSteps) {
        rotationSteps = ((rotationSteps % 4) + 4) % 4;
        if (rotationSteps == 0) return schematic;

        BlockRotation rot;
        switch (rotationSteps) {
            case 1: rot = BlockRotation.CLOCKWISE_90; break;
            case 2: rot = BlockRotation.CLOCKWISE_180; break;
            case 3: rot = BlockRotation.COUNTERCLOCKWISE_90; break;
            default: return schematic;
        }
        return new RotatedSchematic(schematic, rot);
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        int[] orig = mapToOriginal(x, z);
        BlockState state = inner.desiredState(orig[0], y, orig[1], current, approxPlaceable);
        if (state != null && rotation != BlockRotation.NONE) {
            state = state.rotate(rotation);
        }
        return state;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        if (x < 0 || x >= newWidthX || y < 0 || y >= inner.heightY() || z < 0 || z >= newLengthZ) {
            return false;
        }
        int[] orig = mapToOriginal(x, z);
        return inner.inSchematic(orig[0], y, orig[1], currentState);
    }

    @Override
    public int widthX() {
        return newWidthX;
    }

    @Override
    public int heightY() {
        return inner.heightY();
    }

    @Override
    public int lengthZ() {
        return newLengthZ;
    }

    /**
     * Maps rotated (x, z) back to original schematic (origX, origZ).
     */
    private int[] mapToOriginal(int x, int z) {
        int origX, origZ;
        switch (rotation) {
            case CLOCKWISE_90:
                // new(x,z) → orig(z, oldLengthZ-1-x)
                origX = z;
                origZ = inner.lengthZ() - 1 - x;
                break;
            case CLOCKWISE_180:
                origX = inner.widthX() - 1 - x;
                origZ = inner.lengthZ() - 1 - z;
                break;
            case COUNTERCLOCKWISE_90:
                // new(x,z) → orig(oldWidthX-1-z, x)
                origX = inner.widthX() - 1 - z;
                origZ = x;
                break;
            default:
                origX = x;
                origZ = z;
                break;
        }
        return new int[]{origX, origZ};
    }
}
