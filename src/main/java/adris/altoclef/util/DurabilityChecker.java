package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.item.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for checking item durability and determining when to repair/craft new items.
 */
public class DurabilityChecker {

    private static final double DURABILITY_THRESHOLD_RATIO = 0.1; // 10% of max durability

    /**
     * Checks if the main hand weapon needs repair/replacement based on low durability.
     */
    public static boolean needsWeaponRepair(AltoClef mod) {
        if (mod.getPlayer() == null) return false;
        
        ItemStack mainHandStack = mod.getPlayer().getMainHandStack();
        return needsRepair(mainHandStack);
    }

    /**
     * Checks if any equipped armor pieces need repair/replacement based on low durability.
     */
    public static boolean needsArmorRepair(AltoClef mod) {
        if (mod.getPlayer() == null) return false;

        for (ItemStack armorStack : mod.getPlayer().getArmorItems()) {
            if (needsRepair(armorStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generic method to check if an item needs repair based on durability.
     */
    public static boolean needsRepair(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ToolItem || stack.getItem() instanceof ArmorItem)) return false;

        // Check if durability is below threshold
        int maxDurability = stack.getMaxDamage();
        int currentDurability = stack.getDamage();
        double durabilityRatio = (double) currentDurability / maxDurability;

        return durabilityRatio >= (1.0 - DURABILITY_THRESHOLD_RATIO);
    }

    /**
     * Gets the durability percentage as a value between 0 and 1 (0 = no durability left, 1 = full durability)
     */
    public static double getDurabilityPercentage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0; // No item = no durability concerns
        if (!stack.isDamageable()) return 1.0; // Not damageable = full durability

        int maxDurability = stack.getMaxDamage();
        int currentDurability = stack.getDamage();
        
        return (double) (maxDurability - currentDurability) / maxDurability;
    }

    /**
     * Gets the item type that needs to be crafted based on low durability weapons.
     * Returns the appropriate item type that should be crafted to replace the low durability weapon.
     */
    public static Item getWeaponToCraft(AltoClef mod) {
        if (mod.getPlayer() == null) return null;
        
        ItemStack mainHandStack = mod.getPlayer().getMainHandStack();
        if (needsRepair(mainHandStack)) {
            Item currentItem = mainHandStack.getItem();
            
            // Determine what type of weapon it is and return the appropriate crafting item
            if (currentItem instanceof SwordItem) {
                return getBetterSwordFor(currentItem);
            } else if (currentItem instanceof AxeItem) {
                return getBetterAxeFor(currentItem);
            } else if (currentItem instanceof PickaxeItem) {
                return getBetterPickaxeFor(currentItem);
            } else if (currentItem instanceof ShovelItem) {
                return getBetterShovelFor(currentItem);
            } else if (currentItem instanceof HoeItem) {
                return getBetterHoeFor(currentItem);
            }
        }
        
        return null;
    }

    /**
     * Gets the armor items that need to be crafted based on low durability armor.
     * Returns a list of armor items that should be crafted to replace the low durability armor.
     */
    public static List<Item> getArmorToCraft(AltoClef mod) {
        List<Item> itemsToCraft = new ArrayList<>();
        
        if (mod.getPlayer() == null) return itemsToCraft;

        ItemStack[] armorItems = mod.getPlayer().getArmorItems().toArray(new ItemStack[0]);
        for (ItemStack armorStack : armorItems) {
            if (needsRepair(armorStack)) {
                Item currentItem = armorStack.getItem();
                
                // Determine what type of armor it is and return the appropriate crafting item
                if (currentItem instanceof ArmorItem) {
                    Item betterArmor = getBetterArmorFor(currentItem);
                    if (betterArmor != null) {
                        itemsToCraft.add(betterArmor);
                    }
                }
            }
        }
        
        return itemsToCraft;
    }

    /**
     * Determines what would be a better sword than the current one.
     */
    private static Item getBetterSwordFor(Item currentSword) {
        // If we have wooden sword, upgrade to stone, iron, diamond, netherite in that order
        if (currentSword == Items.WOODEN_SWORD) return Items.STONE_SWORD;
        if (currentSword == Items.STONE_SWORD) return Items.IRON_SWORD;
        if (currentSword == Items.IRON_SWORD) return Items.DIAMOND_SWORD;
        if (currentSword == Items.DIAMOND_SWORD) return Items.NETHERITE_SWORD;
        
        // If we already have the best, return the same to maintain it
        return currentSword;
    }

    /**
     * Determines what would be a better axe than the current one.
     */
    private static Item getBetterAxeFor(Item currentAxe) {
        if (currentAxe == Items.WOODEN_AXE) return Items.STONE_AXE;
        if (currentAxe == Items.STONE_AXE) return Items.IRON_AXE;
        if (currentAxe == Items.IRON_AXE) return Items.DIAMOND_AXE;
        if (currentAxe == Items.DIAMOND_AXE) return Items.NETHERITE_AXE;
        
        return currentAxe;
    }

    /**
     * Determines what would be a better pickaxe than the current one.
     */
    private static Item getBetterPickaxeFor(Item currentPickaxe) {
        if (currentPickaxe == Items.WOODEN_PICKAXE) return Items.STONE_PICKAXE;
        if (currentPickaxe == Items.STONE_PICKAXE) return Items.IRON_PICKAXE;
        if (currentPickaxe == Items.IRON_PICKAXE) return Items.DIAMOND_PICKAXE;
        if (currentPickaxe == Items.DIAMOND_PICKAXE) return Items.NETHERITE_PICKAXE;
        
        return currentPickaxe;
    }

    /**
     * Determines what would be a better shovel than the current one.
     */
    private static Item getBetterShovelFor(Item currentShovel) {
        if (currentShovel == Items.WOODEN_SHOVEL) return Items.STONE_SHOVEL;
        if (currentShovel == Items.STONE_SHOVEL) return Items.IRON_SHOVEL;
        if (currentShovel == Items.IRON_SHOVEL) return Items.DIAMOND_SHOVEL;
        if (currentShovel == Items.DIAMOND_SHOVEL) return Items.NETHERITE_SHOVEL;
        
        return currentShovel;
    }

    /**
     * Determines what would be a better hoe than the current one.
     */
    private static Item getBetterHoeFor(Item currentHoe) {
        if (currentHoe == Items.WOODEN_HOE) return Items.STONE_HOE;
        if (currentHoe == Items.STONE_HOE) return Items.IRON_HOE;
        if (currentHoe == Items.IRON_HOE) return Items.DIAMOND_HOE;
        if (currentHoe == Items.DIAMOND_HOE) return Items.NETHERITE_HOE;
        
        return currentHoe;
    }

    /**
     * Determines what would be better armor than the current one.
     */
    private static Item getBetterArmorFor(Item currentArmor) {
        // Determine armor type and upgrade accordingly
        if (currentArmor == Items.LEATHER_HELMET || currentArmor == Items.LEATHER_CHESTPLATE || 
            currentArmor == Items.LEATHER_LEGGINGS || currentArmor == Items.LEATHER_BOOTS) {
            return Items.IRON_INGOT; // Need iron ingots to craft iron armor
        } else if (currentArmor == Items.CHAINMAIL_HELMET || currentArmor == Items.CHAINMAIL_CHESTPLATE || 
                   currentArmor == Items.CHAINMAIL_LEGGINGS || currentArmor == Items.CHAINMAIL_BOOTS) {
            return Items.IRON_INGOT; // Iron is better than chainmail
        } else if (currentArmor == Items.IRON_HELMET || currentArmor == Items.IRON_CHESTPLATE || 
                   currentArmor == Items.IRON_LEGGINGS || currentArmor == Items.IRON_BOOTS) {
            return Items.DIAMOND; // Need diamonds to craft diamond armor
        } else if (currentArmor == Items.DIAMOND_HELMET || currentArmor == Items.DIAMOND_CHESTPLATE || 
                   currentArmor == Items.DIAMOND_LEGGINGS || currentArmor == Items.DIAMOND_BOOTS) {
            return Items.NETHERITE_INGOT; // Need netherite to upgrade diamond
        } else if (currentArmor == Items.GOLDEN_HELMET || currentArmor == Items.GOLDEN_CHESTPLATE || 
                   currentArmor == Items.GOLDEN_LEGGINGS || currentArmor == Items.GOLDEN_BOOTS) {
            return Items.IRON_INGOT; // Iron is better than gold
        }
        
        // For other cases, return the current item
        return currentArmor;
    }

    /**
     * Determines if we should craft a new weapon based on current weapon durability and available materials.
     */
    public static boolean shouldCraftNewWeapon(AltoClef mod) {
        boolean needsRepair = needsWeaponRepair(mod);
        Item itemToCraft = getWeaponToCraft(mod);
        
        // Check if we have materials to craft the better weapon
        if (needsRepair && itemToCraft != null) {
            // This would need to be expanded to check for actual crafting materials
            // For now, just return true if we need to repair and have a weapon to craft
            return true;
        }
        
        return false;
    }

    /**
     * Determines if we should craft new armor based on current armor durability and available materials.
     */
    public static boolean shouldCraftNewArmor(AltoClef mod) {
        boolean needsRepair = needsArmorRepair(mod);
        List<Item> itemsToCraft = getArmorToCraft(mod);
        
        // Check if we have materials to craft the better armor
        if (needsRepair && !itemsToCraft.isEmpty()) {
            // This would need to be expanded to check for actual crafting materials
            // For now, just return true if we need to repair and have armor to craft
            return true;
        }
        
        return false;
    }
}