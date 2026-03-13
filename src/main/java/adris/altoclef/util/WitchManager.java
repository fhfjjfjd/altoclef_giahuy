package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Manages witch encounters and helps dodge their potion attacks.
 */
public class WitchManager {

    private static final double WITCH_CASTING_DISTANCE = 12.0; // Distance where witches can cast potions
    private static final double SAFE_DISTANCE = 16.0; // Distance to try to maintain from witches
    private static final double POTION_ESCAPE_RADIUS = 8.0; // Radius to escape from thrown potions

    /**
     * Checks if there are witches nearby that are casting potions and take evasive action.
     */
    public static void tick(AltoClef mod) {
        // Check for nearby witches and their casting state
        List<Entity> witches = mod.getEntityTracker().getTrackedEntities(WitchEntity.class);
        
        for (Entity entity : witches) {
            if (entity instanceof WitchEntity witch && entity.isAlive()) {
                if (isWitchCastingPotion(witch)) {
                    // Witch is casting a potion, take evasive action
                    mod.log("Witch is casting potion, taking evasive action!");
                    takeEvasiveAction(mod, witch);
                    return; // Only handle one witch at a time
                }
            }
        }

        // Also check for nearby potion entities that are incoming
        List<Entity> potions = mod.getEntityTracker().getTrackedEntities(PotionEntity.class);
        for (Entity entity : potions) {
            if (entity instanceof PotionEntity potion && entity.isAlive()) {
                if (isPotionTargetingPlayer(mod, potion)) {
                    mod.log("Incoming potion detected, dodging!");
                    dodgePotion(mod, potion);
                    return; // Only handle one potion at a time
                }
            }
        }
    }

    /**
     * Determines if a witch is currently casting a potion.
     */
    private static boolean isWitchCastingPotion(WitchEntity witch) {
        // Check if the witch is in an active attack or casting state
        // In Minecraft, witches start to glow when they're about to throw a potion
        return witch.getMainHandStack().isOf(Items.POTION) && 
               witch.getMainHandStack().getCount() > 0 && 
               witch.getHealth() > 0 && 
               witch.isAttacking();
    }

    /**
     * Determines if a potion is targeting the player.
     */
    private static boolean isPotionTargetingPlayer(AltoClef mod, PotionEntity potion) {
        // Check if the potion is close to the player and moving in their direction
        Vec3d playerPos = mod.getPlayer().getPos();
        Vec3d potionPos = potion.getPos();
        Vec3d potionVelocity = potion.getVelocity();
        
        double distanceToPlayer = playerPos.distanceTo(potionPos);
        if (distanceToPlayer > POTION_ESCAPE_RADIUS) {
            return false;
        }
        
        // Check if it's moving toward the player
        Vec3d playerToPotion = potionPos.subtract(playerPos);
        double dotProduct = playerToPotion.dotProduct(potionVelocity.normalize());
        
        // If the potion is moving toward the player (negative dot product when normalized)
        return dotProduct < 0;
    }

    /**
     * Take evasive action when a witch is casting a potion.
     */
    private static void takeEvasiveAction(AltoClef mod, WitchEntity witch) {
        Vec3d witchPos = witch.getPos();
        Vec3d playerPos = mod.getPlayer().getPos();
        
        // Move away from the witch in the opposite direction
        Vec3d escapeVector = playerPos.subtract(witchPos).normalize();
        Vec3d targetPos = playerPos.add(escapeVector.multiply(SAFE_DISTANCE * 0.5));
        
        // For now, we'll just log that we need to move
        // In a real implementation, this would trigger a movement task
        mod.log("Moving away from witch at: " + targetPos.toString());
    }

    /**
     * Dodge an incoming potion.
     */
    private static void dodgePotion(AltoClef mod, PotionEntity potion) {
        Vec3d playerPos = mod.getPlayer().getPos();
        Vec3d potionPos = potion.getPos();
        Vec3d potionVelocity = potion.getVelocity();
        
        // Move perpendicular to the potion's trajectory to dodge it
        Vec3d potionDirection = potionVelocity.normalize();
        Vec3d perpendicular = new Vec3d(-potionDirection.z, 0, potionDirection.x).normalize();
        
        // Move to one side of the trajectory
        Vec3d dodgePos = playerPos.add(perpendicular.multiply(3.0));
        
        // For now, we'll just log that we need to move
        // In a real implementation, this would trigger a movement task
        mod.log("Dodging incoming potion by moving to: " + dodgePos.toString());
    }

    /**
     * Check if there are witches nearby that might pose a threat.
     */
    public static boolean areWitchesNearby(AltoClef mod) {
        List<Entity> witches = mod.getEntityTracker().getTrackedEntities(WitchEntity.class);
        Vec3d playerPos = mod.getPlayer().getPos();
        
        for (Entity entity : witches) {
            if (entity instanceof WitchEntity witch && entity.isAlive()) {
                if (playerPos.distanceTo(entity.getPos()) < WITCH_CASTING_DISTANCE) {
                    return true;
                }
            }
        }
        return false;
    }
}