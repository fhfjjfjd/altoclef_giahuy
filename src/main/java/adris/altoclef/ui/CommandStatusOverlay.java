package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.SchematicBuildTask;
import adris.altoclef.tasks.resources.AutoFarmTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CombatManager;
import baritone.process.BuilderProcess;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandStatusOverlay {

    public void render(AltoClef mod, MatrixStack matrixstack) {
        if (mod.getModSettings().shouldShowTaskChain()) {
            List<Task> tasks = Collections.emptyList();
            if (mod.getTaskRunner().getCurrentTaskChain() != null) {
                tasks = mod.getTaskRunner().getCurrentTaskChain().getTasks();
            }

            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            int color = 0xFFFFFFFF;
            float dy = drawTaskChain(renderer, matrixstack, 0, 0, color, 10, tasks);

            // Draw combat info below task chain
            dy = drawCombatInfo(renderer, matrixstack, 0, dy + 4, mod);

            // Draw farm info
            dy = drawFarmInfo(renderer, matrixstack, 0, dy + 4, mod);

            // Draw build info
            dy = drawBuildInfo(renderer, matrixstack, 0, dy + 4, mod);
        }
    }

    private float drawTaskChain(TextRenderer renderer, MatrixStack stack, float dx, float dy, int color, int maxLines, List<Task> tasks) {
        if (tasks.size() == 0) {
            renderer.draw(stack, " (no task running) ", dx, dy, color);
            dy += renderer.fontHeight + 2;
        } else {
            float fontHeight = renderer.fontHeight;

            if (tasks.size() > maxLines) {
                for (int i = 0; i < tasks.size(); ++i) {
                    // Skip over the next tasks
                    if (i == 0 || i > tasks.size() - maxLines) {
                        renderer.draw(stack, tasks.get(i).toString(), dx, dy, color);
                    } else if (i == 1) {
                        renderer.draw(stack, " ... ", dx, dy, color);
                    } else {
                        continue;
                    }
                    dx += 8;
                    dy += fontHeight + 2;
                }
            } else {
                for (Task task : tasks) {
                    renderer.draw(stack, task.toString(), dx, dy, color);
                    dx += 8;
                    dy += fontHeight + 2;
                }
            }
        }
        return dy;
    }

    private float drawCombatInfo(TextRenderer renderer, MatrixStack stack, float dx, float dy, AltoClef mod) {
        try {
            CombatManager combat = mod.getCombatManager();
            CombatManager.CombatState state = combat.getState();
            if (state == CombatManager.CombatState.IDLE) return dy;

            int stateColor;
            switch (state) {
                case ENGAGING:  stateColor = 0xFFFF5555; break;  // red
                case BLOCKING:  stateColor = 0xFF5555FF; break;  // blue
                case RETREATING: stateColor = 0xFFFFAA00; break; // orange
                default: stateColor = 0xFFFFFFFF;
            }

            String info = "[Combat: " + state.name() + "]";
            if (combat.isBlocking()) info += " \u2694 SHIELD";
            renderer.draw(stack, info, dx, dy, stateColor);
            dy += renderer.fontHeight + 2;

            // Show health bar
            float health = mod.getPlayer().getHealth();
            int healthColor = health > 10 ? 0xFFAAFFAA : (health > 6 ? 0xFFFFFF55 : 0xFFFF5555);
            renderer.draw(stack, String.format("  HP: %.0f/20", health), dx, dy, healthColor);
            dy += renderer.fontHeight + 2;
        } catch (Exception e) {
            // Ignore render errors
        }
        return dy;
    }

    private float drawFarmInfo(TextRenderer renderer, MatrixStack stack, float dx, float dy, AltoClef mod) {
        try {
            AutoFarmTask farmTask = findAutoFarmTask(mod);
            if (farmTask == null) return dy;

            AutoFarmTask.FarmState state = farmTask.getFarmState();
            int stateColor;
            switch (state) {
                case HARVESTING:    stateColor = 0xFF55FF55; break; // green
                case REPLANTING:    stateColor = 0xFFAAFFAA; break; // light green
                case BONE_MEALING:  stateColor = 0xFFFFFF55; break; // yellow
                case EXPANDING:
                case TILLING:       stateColor = 0xFFFFAA00; break; // orange
                case GETTING_SEEDS:
                case GETTING_HOE:   stateColor = 0xFF55FFFF; break; // cyan
                case PICKING_UP:    stateColor = 0xFFAAAAFF; break; // light blue
                default:            stateColor = 0xFFAAAAAA; break; // gray
            }

            renderer.draw(stack, "\uD83C\uDF3E [Farm: " + state.name() + "]", dx, dy, stateColor);
            dy += renderer.fontHeight + 2;
        } catch (Exception e) {
            // Ignore render errors
        }
        return dy;
    }

    private AutoFarmTask findAutoFarmTask(AltoClef mod) {
        if (mod.getTaskRunner().getCurrentTaskChain() == null) return null;
        List<Task> tasks = mod.getTaskRunner().getCurrentTaskChain().getTasks();
        for (Task task : tasks) {
            if (task instanceof AutoFarmTask) return (AutoFarmTask) task;
        }
        return null;
    }

    private float drawBuildInfo(TextRenderer renderer, MatrixStack stack, float dx, float dy, AltoClef mod) {
        try {
            BuilderProcess builder = mod.getClientBaritone().getBuilderProcess();
            if (!builder.isActive()) {
                return dy;
            }

            int headerColor = 0xFF55FFFF;  // cyan
            int labelColor = 0xFFFFFF55;    // yellow
            int valueColor = 0xFFAAFFAA;    // light green
            int warnColor = 0xFFFF5555;     // red
            float fontHeight = renderer.fontHeight;

            // Builder status - show state machine state if available
            String status;
            int statusColor;
            SchematicBuildTask buildTask = findSchematicBuildTask(mod);
            if (buildTask != null) {
                SchematicBuildTask.BuildState state = buildTask.getBuildState();
                status = state.name();
                switch (state) {
                    case BUILDING:  statusColor = valueColor; break;
                    case SOURCING:  statusColor = labelColor; break;
                    case RECOVERING: statusColor = warnColor; break;
                    default: statusColor = valueColor;
                }
            } else {
                status = builder.isPaused() ? "PAUSED" : "BUILDING";
                statusColor = builder.isPaused() ? warnColor : valueColor;
            }
            renderer.draw(stack, "[Builder: " + status + "]", dx, dy, statusColor);
            dy += fontHeight + 2;

            // Missing blocks
            Map<BlockState, Integer> missing = builder.getMissing();
            if (missing != null && !missing.isEmpty()) {
                renderer.draw(stack, "Missing blocks:", dx, dy, labelColor);
                dy += fontHeight + 2;

                int count = 0;
                for (Map.Entry<BlockState, Integer> entry : missing.entrySet()) {
                    if (count >= 8) {
                        renderer.draw(stack, "  ... +" + (missing.size() - 8) + " more", dx, dy, valueColor);
                        dy += fontHeight + 2;
                        break;
                    }
                    String blockName = entry.getKey().getBlock().getName().getString();
                    renderer.draw(stack, "  " + blockName + " x" + entry.getValue(), dx, dy, valueColor);
                    dy += fontHeight + 2;
                    count++;
                }
            }
        } catch (Exception e) {
            // Ignore render errors
        }
        return dy;
    }

    private SchematicBuildTask findSchematicBuildTask(AltoClef mod) {
        if (mod.getTaskRunner().getCurrentTaskChain() == null) return null;
        List<Task> tasks = mod.getTaskRunner().getCurrentTaskChain().getTasks();
        for (Task task : tasks) {
            if (task instanceof SchematicBuildTask) {
                return (SchematicBuildTask) task;
            }
        }
        return null;
    }
}
