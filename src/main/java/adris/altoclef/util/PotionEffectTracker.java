package adris.altoclef.util;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;

import java.util.*;

/**
 * Tracks dangerous potion effects and provides threat assessment.
 * Enhanced for witch potion dodging system.
 */
public class PotionEffectTracker {
    
    // Map of dangerous effects to their potion types
    private final Map<StatusEffect, List<Potion>> DANGEROUS_EFFECTS = new HashMap<>();
    
    // Threat level cache
    private final Map<StatusEffect, Integer> THREAT_LEVELS = new HashMap<>();
    
    public PotionEffectTracker() {
        initializeDangerousEffects();
        initializeThreatLevels();
    }
    
    /**
     * Initialize map of dangerous potion effects
     */
    private void initializeDangerousEffects() {
        // Instant damage (most dangerous)
        DANGEROUS_EFFECTS.put(StatusEffects.INSTANT_DAMAGE, Arrays.asList(
            Potions.HARMING,
            Potions.STRONG_HARMING
        ));
        
        // Poison
        DANGEROUS_EFFECTS.put(StatusEffects.POISON, Arrays.asList(
            Potions.POISON,
            Potions.STRONG_POISON,
            Potions.LONG_POISON
        ));
        
        // Weakness
        DANGEROUS_EFFECTS.put(StatusEffects.WEAKNESS, Arrays.asList(
            Potions.WEAKNESS,
            Potions.LONG_WEAKNESS
        ));
        
        // Slowness
        DANGEROUS_EFFECTS.put(StatusEffects.SLOWNESS, Arrays.asList(
            Potions.SLOWNESS,
            Potions.LONG_SLOWNESS
        ));
        
        // Blindness (rare but dangerous in combat)
        DANGEROUS_EFFECTS.put(StatusEffects.BLINDNESS, Arrays.asList(
            Potions.BLINDNESS,
            Potions.LONG_BLINDNESS
        ));
    }
    
    /**
     * Initialize threat levels for each effect type
     */
    private void initializeThreatLevels() {
        THREAT_LEVELS.put(StatusEffects.INSTANT_DAMAGE, 10); // Highest threat
        THREAT_LEVELS.put(StatusEffects.POISON, 8);
        THREAT_LEVELS.put(StatusEffects.WEAKNESS, 5);
        THREAT_LEVELS.put(StatusEffects.SLOWNESS, 6);
        THREAT_LEVELS.put(StatusEffects.BLINDNESS, 7);
    }
    
    /**
     * Check if a potion item contains a dangerous effect
     */
    public boolean isDangerousPotion(ItemStack potion) {
        if (potion.getItem() != Items.POTION && 
            potion.getItem() != Items.SPLASH_POTION &&
            potion.getItem() != Items.LINGERING_POTION) {
            return false;
        }
        
        // Check against known dangerous potions
        for (List<Potion> potions : DANGEROUS_EFFECTS.values()) {
            // In Minecraft 1.17.1, we need to check the potion NBT
            // This is a simplified check - full implementation would read NBT
            if (potion.hasNbt() && potion.getNbt().contains("Potion")) {
                String potionId = potion.getNbt().getString("Potion");
                for (Potion dangerousPotion : potions) {
                    if (potionId.contains(dangerousPotion.toString().toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get the threat level of a specific status effect
     * @param effect The status effect to check
     * @return Threat level (0-10, higher = more dangerous)
     */
    public int getThreatLevel(StatusEffect effect) {
        return THREAT_LEVELS.getOrDefault(effect, 0);
    }
    
    /**
     * Check if a status effect is considered dangerous
     */
    public boolean isDangerousEffect(StatusEffect effect) {
        return DANGEROUS_EFFECTS.containsKey(effect);
    }
    
    /**
     * Get all dangerous effects that are currently active on a player
     */
    public List<StatusEffect> getActiveDangerousEffects(net.minecraft.entity.LivingEntity entity) {
        List<StatusEffect> active = new ArrayList<>();
        for (StatusEffect effect : DANGEROUS_EFFECTS.keySet()) {
            if (entity.hasStatusEffect(effect)) {
                active.add(effect);
            }
        }
        return active;
    }
    
    /**
     * Calculate overall threat score from active effects
     * @param entity The entity to check
     * @return Total threat score (sum of all active effect threat levels)
     */
    public int calculateTotalThreat(net.minecraft.entity.LivingEntity entity) {
        int totalThreat = 0;
        for (StatusEffect effect : getActiveDangerousEffects(entity)) {
            totalThreat += getThreatLevel(effect);
        }
        return totalThreat;
    }
    
    /**
     * Get potions that counter a specific negative effect
     * @param effect The effect to counter
     * @return List of counter potion items
     */
    public List<Item> getCounterPotions(StatusEffect effect) {
        List<Item> counters = new ArrayList<>();
        
        if (effect == StatusEffects.POISON) {
            counters.add(Items.MILK_BUCKET); // Milk removes all effects
            counters.add(Items.HONEY_BOTTLE); // Honey provides some relief
        } else if (effect == StatusEffects.INSTANT_DAMAGE) {
            counters.add(Items.MILK_BUCKET);
            counters.add(Items.GOLDEN_APPLE); // Absorption helps survive
        } else if (effect == StatusEffects.SLOWNESS) {
            counters.add(Items.MILK_BUCKET);
            counters.add(Items.SUGAR); // Speed counter
        } else {
            counters.add(Items.MILK_BUCKET); // Universal counter
        }
        
        return counters;
    }
    
    /**
     * Check if entity has counter items available
     */
    public boolean hasCounterItem(net.minecraft.entity.player.PlayerEntity player, StatusEffect effect) {
        List<Item> counters = getCounterPotions(effect);
        for (Item counter : counters) {
            if (player.getInventory().contains(new ItemStack(counter))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the best counter item for current situation
     */
    public ItemStack getBestCounterItem(net.minecraft.entity.player.PlayerEntity player, List<StatusEffect> activeEffects) {
        // Priority: Milk Bucket > Golden Apple > specific counters
        if (player.getInventory().contains(new ItemStack(Items.MILK_BUCKET))) {
            return new ItemStack(Items.MILK_BUCKET);
        }
        
        if (!activeEffects.isEmpty() && player.getInventory().contains(new ItemStack(Items.GOLDEN_APPLE))) {
            return new ItemStack(Items.GOLDEN_APPLE);
        }
        
        for (StatusEffect effect : activeEffects) {
            for (Item counter : getCounterPotions(effect)) {
                if (player.getInventory().contains(new ItemStack(counter))) {
                    return new ItemStack(counter);
                }
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Get all registered dangerous effects
     */
    public Set<StatusEffect> getDangerousEffects() {
        return Collections.unmodifiableSet(DANGEROUS_EFFECTS.keySet());
    }
    
    /**
     * Check if we should prioritize dodging this potion
     * @param potion The potion to evaluate
     * @param distanceToPlayer Distance from potion to player
     * @return True if should dodge, false otherwise
     */
    public boolean shouldDodge(ItemStack potion, double distanceToPlayer) {
        if (!isDangerousPotion(potion)) {
            return false;
        }
        
        // Always dodge if very close
        if (distanceToPlayer < 3.0) {
            return true;
        }
        
        // Dodge instant damage regardless of distance
        // (simplified - would check NBT in full implementation)
        return distanceToPlayer < 8.0;
    }
}
