package adris.altoclef.tasks.misc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CraftDurabilityTask priority system and durability calculations
 */
public class CraftDurabilityTaskTest {

    @Test
    void testNetheriteHasHigherPriorityThanDiamond() {
        // Arrange & Act
        int netheriteSwordPriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_SWORD);
        int diamondSwordPriority = CraftDurabilityTask.getPriorityScore(Items.DIAMOND_SWORD);
        
        // Assert
        assertTrue(netheriteSwordPriority > diamondSwordPriority, 
            "Netherite sword (60) should have higher priority than diamond sword (50)");
    }

    @Test
    void testDiamondHasHigherPriorityThanIron() {
        // Arrange & Act
        int diamondPickaxePriority = CraftDurabilityTask.getPriorityScore(Items.DIAMOND_PICKAXE);
        int ironPickaxePriority = CraftDurabilityTask.getPriorityScore(Items.IRON_PICKAXE);
        
        // Assert
        assertTrue(diamondPickaxePriority > ironPickaxePriority,
            "Diamond pickaxe (48) should have higher priority than iron pickaxe (38)");
    }

    @Test
    void testChestplateHasHigherPriorityThanBoots() {
        // Arrange & Act
        int netheriteChestplatePriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_CHESTPLATE);
        int netheriteBootsPriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_BOOTS);
        
        // Assert
        assertTrue(netheriteChestplatePriority > netheriteBootsPriority,
            "Netherite chestplate (55) should have higher priority than netherite boots (48)");
    }

    @Test
    void testSwordHasHighestPriority() {
        // Arrange
        int swordPriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_SWORD);
        int pickaxePriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_PICKAXE);
        int chestplatePriority = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_CHESTPLATE);
        
        // Assert
        assertTrue(swordPriority > pickaxePriority, "Sword should have higher priority than pickaxe");
        assertTrue(swordPriority > chestplatePriority, "Sword should have highest priority");
    }

    @Test
    void testDurabilityPercentCalculation() {
        // Arrange
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int maxDamage = sword.getMaxDamage(); // 1561 for diamond sword
        
        // Act - Damage the sword by 50%
        sword.setDamage(maxDamage / 2);
        int durabilityPercent = CraftDurabilityTask.getDurabilityPercent(sword);
        
        // Assert
        assertEquals(50, durabilityPercent, 1, "Durability should be approximately 50%");
    }

    @Test
    void testDurabilityPercentAtFullDurability() {
        // Arrange
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        
        // Act
        int durabilityPercent = CraftDurabilityTask.getDurabilityPercent(sword);
        
        // Assert
        assertEquals(100, durabilityPercent, "New item should have 100% durability");
    }

    @Test
    void testDurabilityPercentAtZeroDurability() {
        // Arrange
        ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
        sword.setDamage(sword.getMaxDamage()); // Completely damaged
        
        // Act
        int durabilityPercent = CraftDurabilityTask.getDurabilityPercent(sword);
        
        // Assert
        assertEquals(0, durabilityPercent, "Broken item should have 0% durability");
    }

    @Test
    void testNonDamageableItemReturns100Percent() {
        // Arrange
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        
        // Act
        int durabilityPercent = CraftDurabilityTask.getDurabilityPercent(cobblestone);
        
        // Assert
        assertEquals(100, durabilityPercent, "Non-damageable items should return 100%");
    }

    @Test
    void testPriorityOrderForArmor() {
        // Test full armor set priority order
        int[] priorities = {
            CraftDurabilityTask.getPriorityScore(Items.NETHERITE_HELMET),      // 50
            CraftDurabilityTask.getPriorityScore(Items.NETHERITE_CHESTPLATE),  // 55
            CraftDurabilityTask.getPriorityScore(Items.NETHERITE_LEGGINGS),    // 52
            CraftDurabilityTask.getPriorityScore(Items.NETHERITE_BOOTS)        // 48
        };
        
        // Chestplate should be highest priority
        assertTrue(priorities[1] > priorities[0], "Chestplate > Helmet");
        assertTrue(priorities[1] > priorities[2], "Chestplate > Leggings");
        assertTrue(priorities[1] > priorities[3], "Chestplate > Boots");
    }

    @Test
    void testUnknownItemHasLowPriority() {
        // Arrange & Act - Use a common item not in the priority map
        int stickPriority = CraftDurabilityTask.getPriorityScore(Items.STICK);
        
        // Assert - Default priority is 10
        assertEquals(10, stickPriority, "Unknown items should have default priority of 10");
    }

    @Test
    void testToolPriorityOrder() {
        // Verify tool priority order: Sword > Pickaxe > Axe > Shovel (for same material)
        int netheriteSword = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_SWORD);      // 60
        int netheritePickaxe = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_PICKAXE);  // 58
        int netheriteAxe = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_AXE);          // 56
        int netheriteShovel = CraftDurabilityTask.getPriorityScore(Items.NETHERITE_SHOVEL);    // 54
        
        assertTrue(netheriteSword > netheritePickaxe, "Sword > Pickaxe");
        assertTrue(netheritePickaxe > netheriteAxe, "Pickaxe > Axe");
        assertTrue(netheriteAxe > netheriteShovel, "Axe > Shovel");
    }
}
