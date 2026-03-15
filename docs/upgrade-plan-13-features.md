# Upgrade All 13 Features Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nâng cấp tất cả 13 tính năng với cải tiến mới, HUD cải tiến, và tích hợp tốt hơn

**Architecture:** Mỗi tính năng sẽ được nâng cấp với: (1) Cải tiến logic, (2) HUD messages chi tiết hơn, (3) Integration tests, (4) Cấu hình tùy chọn

**Tech Stack:** Java 17, Minecraft 1.17.1, Fabric API, Baritone, JUnit 5

---

## Chunk 1: Advanced Combat Upgrades

### Task 1: Auto-Craft/Repair Weapons & Armor - Enhanced

**Files:**
- Modify: `src/main/java/adris/altoclef/tasks/misc/CraftDurabilityTask.java`
- Modify: `src/main/java/adris/altoclef/util/CombatManager.java`
- Create: `src/test/java/adris/altoclef/tasks/misc/CraftDurabilityTaskTest.java`

- [ ] **Step 1: Add configurable durability threshold**

```java
// In AltoClef.java settings
@Setting(comment = "Minimum durability % to auto-repair (0-100)")
public static final int MIN_DURABILITY_THRESHOLD = 30;

@Setting(comment = "Auto-craft replacement when durability < 10%")
public static final boolean AUTO_CRAFT_REPLACEMENT = true;
```

- [ ] **Step 2: Enhance CraftDurabilityTask with priority system**

```java
// Priority: Helmet > Chestplate > Leggings > Boots > Weapon
private static final Map<Item, Integer> PRIORITY_MAP = new HashMap<>();
static {
    PRIORITY_MAP.put(Items.NETHERITE_CHESTPLATE, 5);
    PRIORITY_MAP.put(Items.NETHERITE_HELMET, 4);
    // ... etc
}

@Override
protected Task onTick(AltoClef mod) {
    List<ItemStack> damaged = getDamagedItems(mod);
    if (damaged.isEmpty()) return null;
    
    // Sort by priority and durability %
    damaged.sort(Comparator.comparingInt(this::getPriorityScore)
                            .thenComparingInt(this::getDurabilityPercent));
    
    ItemStack toRepair = damaged.get(0);
    // ... repair logic
}
```

- [ ] **Step 3: Add HUD display for repair status**

```java
// In HUDOverlay.java
if (CraftDurabilityTask.isRepairing()) {
    drawText("🔧 Repairing: " + itemName + " (" + durability + "%)", x, y);
}
```

- [ ] **Step 4: Write unit tests**

```java
@Test
void testPrioritizesNetheriteOverDiamond() {
    // Arrange
    ItemStack netheriteSword = new ItemStack(Items.NETHERITE_SWORD);
    netheriteSword.setDamage(50); // Low durability
    ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
    diamondSword.setDamage(10); // Higher durability
    
    // Act
    int netheritePriority = task.getPriorityScore(netheriteSword);
    int diamondPriority = task.getPriorityScore(diamondSword);
    
    // Assert
    assertTrue(netheritePriority > diamondPriority);
}

@Test
void testRepairsWhenBelowThreshold() {
    // Arrange
    ItemStack item = new ItemStack(Items.IRON_PICKAXE);
    item.setDamage(180); // 70% damaged
    
    // Act & Assert
    assertTrue(task.shouldRepair(item, 30)); // 30% threshold
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/adris/altoclef/tasks/misc/CraftDurabilityTask.java
git add src/main/java/adris/altoclef/util/CombatManager.java
git add src/test/java/adris/altoclef/tasks/misc/CraftDurabilityTaskTest.java
git commit -m "feat: enhance auto-repair with priority system and configurable threshold"
```

### Task 2: Witch Potion Dodging - Enhanced

**Files:**
- Modify: `src/main/java/adris/altoclef/util/WitchManager.java`
- Modify: `src/main/java/adris/altoclef/tasks/movement/DodgeProjectilesTask.java`
- Create: `src/main/java/adris/altoclef/util/PotionEffectTracker.java`

- [ ] **Step 1: Create PotionEffectTracker utility**

```java
public class PotionEffectTracker {
    private final Map<StatusEffect, List<Potion>> DANGEROUS_EFFECTS = new HashMap<>();
    
    public PotionEffectTracker() {
        DANGEROUS_EFFECTS.put(StatusEffect.INSTANT_DAMAGE, Arrays.asList(
            Potions.HARMING, Potions.STRONG_HARMING
        ));
        DANGEROUS_EFFECTS.put(StatusEffect.POISON, Arrays.asList(
            Potions.POISON, Potions.STRONG_POISON, Potions.LONG_POISON
        ));
        DANGEROUS_EFFECTS.put(StatusEffect.WEAKNESS, Arrays.asList(
            Potions.WEAKNESS, Potions.LONG_WEAKNESS
        ));
        DANGEROUS_EFFECTS.put(StatusEffect.SLOWNESS, Arrays.asList(
            Potions.SLOWNESS, Potions.LONG_SLOWNESS
        ));
    }
    
    public boolean isDangerousPotion(ItemStack potion) {
        // Check if potion contains dangerous effect
    }
    
    public List<StatusEffect> getActiveThreats(PlayerEntity player) {
        // Return active dangerous effects
    }
}
```

- [ ] **Step 2: Enhance WitchManager with threat detection**

```java
public class WitchManager {
    private final PotionEffectTracker effectTracker = new PotionEffectTracker();
    private final Map<WitchEntity, Integer> witchThreatLevel = new HashMap<>();
    
    public void update() {
        List<WitchEntity> witches = mod.getEntityTracker().getEntities(WitchEntity.class);
        for (WitchEntity witch : witches) {
            int threatLevel = calculateThreatLevel(witch);
            witchThreatLevel.put(witch, threatLevel);
            
            if (threatLevel >= 3) {
                mod.log("⚠️ High threat witch detected at " + witch.getPos());
            }
        }
    }
    
    private int calculateThreatLevel(WitchEntity witch) {
        int level = 0;
        if (witch.isHolding(p -> isDangerousPotion(p))) level += 2;
        if (witch.getPos().distanceTo(mod.getPlayer().getPos()) < 5) level += 1;
        if (mod.getPlayer().getHealth() < 10) level += 1;
        return level;
    }
}
```

- [ ] **Step 3: Improve dodging algorithm**

```java
// In DodgeProjectilesTask.java
@Override
protected Goal getGoal() {
    // Predict projectile trajectory
    Vec3d predictedImpact = predictImpactPoint(thrownPotion);
    
    // Calculate safe zone (perpendicular to trajectory)
    Vec3d safeZone = calculateSafeZone(predictedImpact, 3.0);
    
    // Check if sprint-dodge is possible
    if (canSprintDodge(safeZone)) {
        return new GoalMoveTo(safeZone.x, safeZone.y, safeZone.z);
    }
    
    // Fallback: use water bucket or milk bucket
    if (hasWaterBucket()) {
        return new PlaceWaterGoal(predictedImpact);
    }
    
    return new GoalMoveTo(safeZone.x, safeZone.y, safeZone.z);
}

private Vec3d predictImpact(ThrownPotionEntity potion) {
    Vec3d velocity = potion.getVelocity();
    Vec3d pos = potion.getPos();
    // Calculate parabola trajectory
    double timeToImpact = calculateTimeToImpact(pos, velocity);
    return pos.add(velocity.x * timeToImpact, velocity.y * timeToImpact - 0.5 * 9.8 * timeToImpact * timeToImpact, velocity.z * timeToImpact);
}
```

- [ ] **Step 4: Add HUD for potion threats**

```java
// In HUDOverlay.java
if (WitchManager.hasActiveThreats()) {
    drawText("⚠️ Witch Threat: " + threatLevel + " witches nearby", x, y, 0xFF0000);
    List<StatusEffect> effects = effectTracker.getActiveThreats(player);
    for (int i = 0; i < effects.size(); i++) {
        drawText("  • " + effects.get(i).getName(), x + 10, y + 10 + i * 10);
    }
}
```

- [ ] **Step 5: Write tests**

```java
@Test
void testPredictsPotionTrajectory() {
    // Arrange
    ThrownPotionEntity potion = mock(ThrownPotionEntity.class);
    when(potion.getVelocity()).thenReturn(new Vec3d(0, -0.5, 1));
    when(potion.getPos()).thenReturn(new Vec3d(0, 10, 0));
    
    // Act
    Vec3d impact = dodgeTask.predictImpactPoint(potion);
    
    // Assert
    assertEquals(0, impact.x, 0.1);
    assertTrue(impact.y < 10); // Should be lower
    assertTrue(impact.z > 0); // Should be forward
}

@Test
void testIdentifiesDangerousPotions() {
    // Arrange
    ItemStack harmingPotion = new ItemStack(Items.POTION);
    PotionApplier.setEffect(harmingPotion, StatusEffects.INSTANT_DAMAGE);
    
    // Act & Assert
    assertTrue(tracker.isDangerousPotion(harmingPotion));
    assertFalse(tracker.isDangerousPotion(new ItemStack(Items.HEALING_POTION)));
}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/adris/altoclef/util/PotionEffectTracker.java
git add src/main/java/adris/altoclef/util/WitchManager.java
git add src/main/java/adris/altoclef/tasks/movement/DodgeProjectilesTask.java
git commit -m "feat: enhance witch potion dodging with trajectory prediction and threat detection"
```

### Task 3: PvP Mode - Enhanced

**Files:**
- Modify: `src/main/java/adris/altoclef/util/PvPManager.java`
- Modify: `src/main/java/adris/altoclef/util/KillAura.java`
- Create: `src/main/java/adris/altoclef/tasks/combat/RetaliateTask.java`

- [ ] **Step 1: Add PvP mode configuration**

```java
// In AltoClef.java
@Setting(comment = "PvP Mode: OFF, DEFENSIVE (retaliate), AGGRESSIVE (attack on sight)")
public static final String PVP_MODE = "DEFENSIVE";

@Setting(comment = "Ignore players in creative/spectator mode")
public static final boolean IGNORE_NON_SURVIVAL = true;

@Setting(comment = "Auto-eat food when HP < 50% during PvP")
public static final boolean AUTO_EAT_DURING_PVP = true;
```

- [ ] **Step 2: Create RetaliateTask**

```java
public class RetaliateTask extends Task {
    private PlayerEntity _target;
    private final TimerGame _cooldown = new TimerGame(5);
    private int _lastDamageTaken = 0;
    
    @Override
    protected Task onTick(AltoClef mod) {
        // Check if we were recently damaged
        int currentDamage = mod.getPlayer().getTotalDamageTaken();
        if (currentDamage > _lastDamageTaken) {
            // We took damage - find attacker
            PlayerEntity attacker = findAttacker(mod);
            if (attacker != null && isValidTarget(attacker)) {
                _target = attacker;
                _cooldown.reset();
            }
        }
        _lastDamageTaken = currentDamage;
        
        if (_target == null || !_target.isAlive()) {
            return null;
        }
        
        if (_cooldown.elapsed()) {
            _target = null; // Cooldown expired
            return null;
        }
        
        // Engage target
        return new KillPlayerTask(_target);
    }
    
    private PlayerEntity findAttacker(AltoClef mod) {
        // Find player who damaged us recently
        List<PlayerEntity> players = mod.getEntityTracker().getEntities(PlayerEntity.class);
        for (PlayerEntity player : players) {
            if (player.getAttackCooldownProgress(0) > 0.8) {
                return player;
            }
        }
        return null;
    }
}
```

- [ ] **Step 3: Enhance KillAura with combo system**

```java
public class KillAura {
    private int _comboCount = 0;
    private long _lastHitTime = 0;
    
    public void onAttack(LivingEntity target) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - _lastHitTime < 500) {
            _comboCount++;
        } else {
            _comboCount = 1;
        }
        _lastHitTime = currentTime;
        
        // Execute combo move
        if (_comboCount == 3) {
            // Critical hit combo
            mod.getController().jump();
        }
        
        // Circle strafe
        circleStrafe(target);
    }
    
    private void circleStrafe(LivingEntity target) {
        Vec3d playerPos = mod.getPlayer().getPos();
        Vec3d targetPos = target.getPos();
        
        // Calculate perpendicular vector
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        Vec3d strafeDir = new Vec3d(-direction.z, 0, direction.x);
        
        // Move perpendicular to target
        Goal goal = new GoalMoveTo(
            playerPos.x + strafeDir.x * 3,
            playerPos.y,
            playerPos.z + strafeDir.z * 3
        );
        mod.getBaritone().getCustomGoalProcess().setGoal(goal);
    }
}
```

- [ ] **Step 4: Add auto-eat during PvP**

```java
// In CombatManager.java
public void tick() {
    if (PVP_MODE.equals("OFF")) return;
    
    PlayerEntity player = mod.getPlayer();
    if (player.getHealth() < 10 && AUTO_EAT_DURING_PVP) {
        // Find best food in inventory
        ItemStack bestFood = findBestFood(mod);
        if (bestFood != null) {
            mod.getSlotHandler().forceEquipItem(bestFood.getItem());
            mod.getController().interactItem(player, Hand.MAIN_HAND);
        }
    }
}

private ItemStack findBestFood(AltoClef mod) {
    return mod.getInventoryTracker().getInventory().stream()
        .filter(stack -> stack.getItem().isFood())
        .max(Comparator.comparingInt(stack -> stack.getItem().getFoodComponent().getHunger()))
        .orElse(null);
}
```

- [ ] **Step 5: Add PvP HUD**

```java
// In HUDOverlay.java
if (PvPManager.isPvPActive()) {
    drawText("⚔️ PvP Mode: " + PVP_MODE, x, y, 0xFF0000);
    if (PvPManager.hasTarget()) {
        PlayerEntity target = PvPManager.getTarget();
        drawText("  Target: " + target.getName().getString() + 
                 " (HP: " + (int)target.getHealth() + "/" + (int)target.getMaxHealth() + ")",
                 x + 10, y + 10);
        drawText("  Combo: " + KillAura.getComboCount() + " hits", x + 10, y + 20);
    }
}
```

- [ ] **Step 6: Write tests**

```java
@Test
void testRetaliatesWhenDamaged() {
    // Arrange
    PlayerEntity attacker = mock(PlayerEntity.class);
    when(attacker.getAttackCooldownProgress(0)).thenReturn(0.9f);
    when(attacker.isAlive()).thenReturn(true);
    
    // Act
    Task task = retaliateTask.onTick(mod);
    
    // Assert
    assertNotNull(task);
    assertTrue(task instanceof KillPlayerTask);
}

@Test
void testIgnoresCreativePlayers() {
    // Arrange
    PlayerEntity creativePlayer = mock(PlayerEntity.class);
    when(creativePlayer.isCreative()).thenReturn(true);
    
    // Act & Assert
    assertFalse(PvPManager.isValidTarget(creativePlayer));
}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/adris/altoclef/tasks/combat/RetaliateTask.java
git add src/main/java/adris/altoclef/util/PvPManager.java
git add src/main/java/adris/altoclef/util/KillAura.java
git commit -m "feat: enhance PvP mode with combo system, circle strafing, and auto-eat"
```

---

## Chunk 2: Advanced Builder Upgrades

(Tiếp tục với các tasks cho Builder, Navigation, và World Interaction...)
