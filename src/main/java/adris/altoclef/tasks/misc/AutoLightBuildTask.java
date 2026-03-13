package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.SchematicBuildTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Task that builds a schematic and automatically places torches in dark areas.
 */
public class AutoLightBuildTask extends Task {
    
    private final String _schematicFileName;
    private final BlockPos _startPos;
    private final int _allowedResourceStackCount;
    private final int _rotationSteps;
    
    private SchematicBuildTask _buildTask;
    private final TimerGame _torchCheckTimer = new TimerGame(2); // Check every 2 seconds
    private boolean _buildingComplete = false;
    
    public AutoLightBuildTask(String schematicFileName, BlockPos startPos) {
        this(schematicFileName, startPos, 3, 0);
    }
    
    public AutoLightBuildTask(String schematicFileName, BlockPos startPos, int allowedResourceStackCount) {
        this(schematicFileName, startPos, allowedResourceStackCount, 0);
    }
    
    public AutoLightBuildTask(String schematicFileName, BlockPos startPos, int allowedResourceStackCount, int rotationSteps) {
        _schematicFileName = schematicFileName;
        _startPos = startPos;
        _allowedResourceStackCount = allowedResourceStackCount;
        _rotationSteps = rotationSteps;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-light build for schematic: " + _schematicFileName);
        _buildTask = new SchematicBuildTask(_schematicFileName, _startPos, _allowedResourceStackCount, _rotationSteps);
        _torchCheckTimer.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // First, run the main build task
        if (!_buildingComplete) {
            Task buildSubtask = _buildTask.onTick(mod);
            
            if (_buildTask.isFinished(mod)) {
                _buildingComplete = true;
                mod.log("Building complete, now checking for dark areas to light up...");
            } else {
                // Return the build subtask if we're still building
                return buildSubtask;
            }
        }
        
        // After building is complete, check for dark areas and place torches
        if (_torchCheckTimer.elapsed()) {
            _torchCheckTimer.reset();
            
            BlockPos torchPos = findDarkAreaNeedingLight(mod);
            if (torchPos != null) {
                // Check if we have torches
                if (mod.getInventoryTracker().getItemCount(Items.TORCH) > 0) {
                    mod.log("Found dark area at " + torchPos.toShortString() + ", placing torch...");
                    return new PlaceBlockTask(torchPos, Blocks.TORCH);
                } else {
                    mod.log("Need torches to light up dark area, collecting...");
                    return TaskCatalogue.getItemTask(Items.TORCH, 16); // Collect a stack of torches
                }
            } else {
                mod.log("No dark areas needing light found, auto-light task complete.");
                return null; // No more dark areas to light, we're done
            }
        }
        
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        if (_buildTask != null && !_buildTask.isFinished(mod)) {
            _buildTask.stop(mod, interruptTask);
        }
        mod.log("Auto-light build task interrupted.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoLightBuildTask task) {
            return task._schematicFileName.equals(_schematicFileName) && 
                   task._startPos.equals(_startPos) && 
                   task._allowedResourceStackCount == _allowedResourceStackCount && 
                   task._rotationSteps == _rotationSteps;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (!_buildingComplete) {
            return "Building schematic with auto-light: " + _schematicFileName;
        } else {
            return "Checking and lighting dark areas after building";
        }
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return _buildingComplete && findDarkAreaNeedingLight(mod) == null;
    }
    
    /**
     * Find a dark area near the built structure that needs lighting.
     */
    private BlockPos findDarkAreaNeedingLight(AltoClef mod) {
        // This is a simplified implementation - in a real implementation, you'd want to be more 
        // sophisticated about finding the built structure boundaries and checking light levels
        
        // For now, we'll just check around the start position in a small radius
        for (int x = -10; x <= 10; x++) {
            for (int y = -5; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = _startPos.add(x, y, z);
                    
                    // Check if the position is air and has low light level
                    if (mod.getWorld().getBlockState(pos).isAir() && 
                        mod.getWorld().getLightLevel(pos) < 8) {
                        // Check if adjacent blocks are solid (walls/floors/ceilings that might cast shadows)
                        if (isNearSolidBlocks(mod, pos)) {
                            return pos; // Found a dark area that might need lighting
                        }
                    }
                }
            }
        }
        
        return null; // No dark areas needing light found
    }
    
    /**
     * Check if a position is near solid blocks that might make it dark.
     */
    private boolean isNearSolidBlocks(AltoClef mod, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.offset(direction);
            Block block = mod.getWorld().getBlockState(adjacent).getBlock();
            
            // If the adjacent block is solid, this might be a corner/enclosed space that needs lighting
            if (block.getDefaultState().isOpaque()) {
                return true;
            }
        }
        return false;
    }
}