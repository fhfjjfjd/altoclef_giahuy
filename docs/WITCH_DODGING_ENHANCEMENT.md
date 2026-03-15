# Witch Potion Dodging Enhancement

## Overview
Enhanced witch potion dodging system with trajectory prediction and threat assessment.

## Components

### 1. PotionEffectTracker (✅ Complete)
**File:** `src/main/java/adris/altoclef/util/PotionEffectTracker.java`

**Features:**
- Tracks 5 dangerous potion effects:
  - Instant Damage (Threat: 10/10)
  - Poison (Threat: 8/10)
  - Blindness (Threat: 7/10)
  - Slowness (Threat: 6/10)
  - Weakness (Threat: 5/10)

- Methods:
  - `isDangerousPotion(ItemStack)` - Check if potion is dangerous
  - `getThreatLevel(StatusEffect)` - Get threat level (0-10)
  - `calculateTotalThreat(LivingEntity)` - Calculate total threat score
  - `getCounterPotions(StatusEffect)` - Get counter items
  - `getBestCounterItem(PlayerEntity, List<StatusEffect>)` - Get best counter
  - `shouldDodge(ItemStack, double)` - Should dodge decision

### 2. WitchManager Enhancement (🔄 TODO)
**File:** `src/main/java/adris/altoclef/util/WitchManager.java`

**Planned Features:**
- Threat level detection per witch
- Distance-based threat assessment
- Player HP consideration
- Active threat tracking

### 3. DodgeProjectilesTask Enhancement (🔄 TODO)
**File:** `src/main/java/adris/altoclef/tasks/movement/DodgeProjectilesTask.java`

**Planned Features:**
- Trajectory prediction algorithm
- Safe zone calculation
- Sprint-dodge optimization
- Water bucket fallback
- Milk bucket auto-use

## Trajectory Prediction Algorithm

```java
// Predict where potion will land
Vec3d predictImpact(ThrownPotionEntity potion) {
    Vec3d velocity = potion.getVelocity();
    Vec3d pos = potion.getPos();
    
    // Calculate time to impact using projectile motion
    double timeToImpact = calculateTimeToImpact(pos, velocity);
    
    // Predict impact point
    return pos.add(
        velocity.x * timeToImpact,
        velocity.y * timeToImpact - 0.5 * 9.8 * timeToImpact * timeToImpact,
        velocity.z * timeToImpact
    );
}
```

## Safe Zone Calculation

```java
// Calculate perpendicular dodge direction
Vec3d calculateSafeZone(Vec3d impactPoint, double safeDistance) {
    Vec3d toImpact = impactPoint.subtract(playerPos).normalize();
    
    // Get perpendicular vector (dodge left/right)
    Vec3d dodgeDir = new Vec3d(-toImpact.z, 0, toImpact.x);
    
    // Return safe position
    return playerPos.add(dodgeDir.multiply(safeDistance));
}
```

## Threat Assessment

| Threat Level | Effects | Action |
|-------------|---------|--------|
| 8-10 | Instant Damage | **IMMEDIATE DODGE** + Golden Apple |
| 6-7 | Poison, Blindness | **DODGE** + Milk Bucket ready |
| 4-5 | Weakness, Slowness | **EVADE** + Counter potion |
| 0-3 | None/Positive | No action needed |

## Integration Points

1. **CombatManager** - Check for witch threats during combat
2. **HUD Overlay** - Display active potion threats
3. **Auto-Equip** - Auto-equip milk bucket when witch detected
4. **Settings** - Configurable dodge sensitivity

## Testing Checklist

- [ ] Witch throws harming potion → Bot dodges
- [ ] Witch throws poison potion → Bot dodges + drinks milk
- [ ] Multiple witches → Prioritize highest threat
- [ ] Low HP + witch → Retreat + golden apple
- [ ] Has milk bucket → Auto-use when poisoned
- [ ] No dodge path → Place water block

## Next Steps

1. ✅ PotionEffectTracker - Complete
2. 🔄 WitchManager enhancement - In Progress
3. 🔄 DodgeProjectilesTask enhancement - Pending
4. 🔄 HUD integration - Pending
5. 🔄 Integration tests - Pending
