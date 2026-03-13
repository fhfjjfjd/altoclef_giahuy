package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.SchematicBuildTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Task to build multiple schematics in sequence.
 */
public class MultiSchematicBuildTask extends Task {
    
    private final List<String> _schematicFiles;
    private final BlockPos _startPos;
    private final int _allowedResourceStackCount;
    private int _currentIndex = 0;
    private Task _currentTask = null;
    
    public MultiSchematicBuildTask(List<String> schematicFiles, BlockPos startPos) {
        this(schematicFiles, startPos, 3); // Default allowed resource stack count
    }
    
    public MultiSchematicBuildTask(List<String> schematicFiles, BlockPos startPos, int allowedResourceStackCount) {
        _schematicFiles = new ArrayList<>(schematicFiles); // Make a copy to avoid external modifications
        _startPos = startPos;
        _allowedResourceStackCount = allowedResourceStackCount;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting multi-schematic build task with " + _schematicFiles.size() + " schematics.");
        if (_schematicFiles.isEmpty()) {
            mod.logWarning("No schematics provided for multi-schematic build task.");
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we've completed all schematics, we're done
        if (_currentIndex >= _schematicFiles.size()) {
            return null;
        }
        
        // If we don't have a current task, create one for the next schematic
        if (_currentTask == null || _currentTask.isFinished(mod)) {
            String schematicFile = _schematicFiles.get(_currentIndex);
            mod.log("Starting to build schematic: " + schematicFile + " (" + (_currentIndex + 1) + "/" + _schematicFiles.size() + ")");
            
            // Create a new build task for the current schematic
            _currentTask = new SchematicBuildTask(schematicFile, _startPos, _allowedResourceStackCount);
            _currentIndex++;
        }
        
        // Return the current build task to be executed
        return _currentTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        if (_currentTask != null && !_currentTask.isFinished(mod)) {
            _currentTask.stop(mod, interruptTask);
        }
        mod.log("Multi-schematic build task interrupted at schematic " + _currentIndex + "/" + _schematicFiles.size());
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof MultiSchematicBuildTask task) {
            return task._schematicFiles.equals(_schematicFiles) && 
                   task._startPos.equals(_startPos) && 
                   task._allowedResourceStackCount == _allowedResourceStackCount;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Building " + _schematicFiles.size() + " schematics in sequence, currently at " + _currentIndex + "/" + _schematicFiles.size();
    }
    
    /**
     * Check if all schematics have been built.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        return _currentIndex >= _schematicFiles.size();
    }
}