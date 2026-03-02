package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;

public enum Dimension {
    OVERWORLD,
    NETHER,
    END;

    public static Dimension current() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return OVERWORLD;
        if (mc.world.getDimension().isUltrawarm()) return NETHER;
        if (mc.world.getDimension().isNatural()) return OVERWORLD;
        return END;
    }
}
