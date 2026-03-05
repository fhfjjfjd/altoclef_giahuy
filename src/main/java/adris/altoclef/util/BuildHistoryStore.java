package adris.altoclef.util;

import adris.altoclef.Debug;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.format.ISchematicFormat;
import baritone.utils.schematic.SchematicSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

public class BuildHistoryStore {
    private static BlockPos lastStart;
    private static Vec3i lastSize;

    public static void record(String fileName, BlockPos start, int rotationSteps) {
        Vec3i size = loadSize(fileName);
        if (size == null) return;
        if (rotationSteps % 2 != 0) {
            size = new Vec3i(size.getZ(), size.getY(), size.getX());
        }
        lastStart = start;
        lastSize = size;
    }

    public static BlockPos getLastStart() { return lastStart; }
    public static Vec3i getLastSize() { return lastSize; }

    private static Vec3i loadSize(String fileName) {
        try {
            File file = new File("schematics/" + fileName);
            Optional<ISchematicFormat> format = SchematicSystem.INSTANCE.getByFile(file);
            if (!format.isPresent()) return null;
            ISchematic schematic = format.get().parse(new FileInputStream(file));
            return new Vec3i(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        } catch (Exception e) {
            Debug.logWarning("Failed to load schematic size for undo: " + e.getMessage());
            return null;
        }
    }
}
