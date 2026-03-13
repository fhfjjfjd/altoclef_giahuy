package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages PvP combat functionality - retaliates against players that attack us first.
 */
public class PvPManager {
    
    // Track recent attackers and when they attacked
    private final Map<String, Long> _recentAttackers = new HashMap<>();
    private static final long ATTACKER_TIMEOUT_MS = 30000; // 30 seconds before forgetting about an attacker
    
    // Whether PvP mode is enabled
    private boolean _pvpEnabled = false;
    
    // Last time we were attacked
    private long _lastAttackTime = 0;
    
    public void tick(AltoClef mod) {
        if (!_pvpEnabled) {
            return;
        }
        
        // Clean up old attackers
        long currentTime = System.currentTimeMillis();
        _recentAttackers.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > ATTACKER_TIMEOUT_MS);
        
        // Check if we have a recent attacker and try to initiate combat
        String recentAttacker = getMostRecentAttacker();
        if (recentAttacker != null) {
            // Look for the attacking player in the entity tracker
            PlayerEntity attacker = mod.getEntityTracker().getPlayerEntity(recentAttacker);
            if (attacker != null && attacker.isAlive()) {
                // For now, just log that we should start combat
                // In a real implementation, this would start a combat task against the player
                mod.log("Attempting to retaliate against attacker: " + recentAttacker);
                
                // Here we would run a task to attack the player
                // mod.runUserTask(new AttackPlayerTask(attacker.getName().getString()));
            } else {
                // Attacker is no longer in range or died, remove from tracking
                _recentAttackers.remove(recentAttacker);
            }
        }
    }
    
    /**
     * Called when the player takes damage from another player
     */
    public void onPlayerAttack(AltoClef mod, PlayerEntity attacker) {
        if (!_pvpEnabled || attacker == null) {
            return;
        }
        
        String attackerName = attacker.getName().getString();
        long currentTime = System.currentTimeMillis();
        
        // Record this attacker
        _recentAttackers.put(attackerName, currentTime);
        _lastAttackTime = currentTime;
        
        mod.log("Player " + attackerName + " attacked us! Retaliating...");
        
        // In a real implementation, this would start a combat task against the attacker
        // For now, we just log that we should retaliate
    }
    
    /**
     * Enable or disable PvP mode
     */
    public void setPvPEnabled(boolean enabled) {
        _pvpEnabled = enabled;
        if (!enabled) {
            _recentAttackers.clear();
        }
    }
    
    /**
     * Check if PvP mode is enabled
     */
    public boolean isPvPEnabled() {
        return _pvpEnabled;
    }
    
    /**
     * Get the most recent attacker
     */
    public String getMostRecentAttacker() {
        if (_recentAttackers.isEmpty()) {
            return null;
        }
        
        // Find the most recent attacker
        String mostRecentAttacker = null;
        long mostRecentTime = 0;
        
        for (Map.Entry<String, Long> entry : _recentAttackers.entrySet()) {
            if (entry.getValue() > mostRecentTime) {
                mostRecentTime = entry.getValue();
                mostRecentAttacker = entry.getKey();
            }
        }
        
        return mostRecentAttacker;
    }
    
    /**
     * Check if we were recently attacked by a player
     */
    public boolean wasRecentlyAttacked() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - _lastAttackTime) < ATTACKER_TIMEOUT_MS;
    }
    
    /**
     * Get the list of recent attackers
     */
    public Map<String, Long> getRecentAttackers() {
        return new HashMap<>(_recentAttackers);
    }
}