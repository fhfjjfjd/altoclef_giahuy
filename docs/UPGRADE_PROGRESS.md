# 13 Features Upgrade Progress

## ✅ Completed

### Settings (All 13 Features)
- Added combat settings: durability threshold, PvP mode, auto-eat during PvP
- Added build settings: auto-light, undo history limit
- Added navigation settings: auto-explore render distance
- File: `Settings.java` - 120+ lines added

### Documentation
- Created upgrade plan: `docs/upgrade-plan-13-features.md`
- Updated README with all 13 features list
- Progress tracker: `docs/UPGRADE_PROGRESS.md`

### Chunk 1: Advanced Combat - Auto-Craft/Repair ✅
- [x] Settings added
- [x] Priority system (Netherite > Diamond > Iron)
  - Sword: 60/50/40/30/20
  - Pickaxe: 58/48/38
  - Axe: 56/46/36
  - Shovel: 54/44/34
  - Chestplate: 55/45/35
  - Helmet: 50/40/30
  - Leggings: 52/42/32
  - Boots: 48/38/28
- [x] Utility methods:
  - `getPriorityScore(Item)` - Get item priority
  - `getDurabilityPercent(ItemStack)` - Calculate durability %
  - `shouldRepair(ItemStack, AltoClef)` - Check if needs repair
  - `getDamagedItems(AltoClef)` - Get all damaged items
- [x] Unit tests (14 tests covering all scenarios)
  - Priority ordering tests
  - Durability calculation tests
  - Edge case tests
- [x] Workflow: https://github.com/fhfjjfjd/altoclef_giahuy/actions/runs/23107343357

---

## 🔄 In Progress

### Chunk 1: Advanced Combat Upgrades

#### 1. Auto-Craft/Repair Weapons & Armor
- [x] Settings added
- [ ] Priority system (Netherite > Diamond > Iron)
- [ ] HUD display for repair status
- [ ] Unit tests

#### 2. Witch Potion Dodging  
- [ ] PotionEffectTracker utility
- [ ] Threat level detection
- [ ] Trajectory prediction algorithm
- [ ] HUD for potion threats

#### 3. PvP Mode
- [x] Settings added (OFF/DEFENSIVE/AGGRESSIVE)
- [ ] RetaliateTask implementation
- [ ] KillAura combo system
- [ ] Circle strafing
- [ ] Auto-eat during PvP
- [ ] PvP HUD

---

## 📋 TODO

### Chunk 2: Advanced Builder Upgrades

#### 4. Multi-Schematic Building
- [ ] MultiSchematicBuildTask enhancement
- [ ] Queue management
- [ ] Progress tracking

#### 5. Auto-Light Placement
- [ ] Light level detection during build
- [ ] Auto-torch placement algorithm
- [ ] Fuel management

#### 6. Undo Last Build
- [ ] BuildHistory enhancement
- [ ] Block-by-block undo
- [ ] Multi-level undo stack

### Chunk 3: Navigation Upgrades

#### 7. Waypoint System
- [ ] WaypointManager enhancement
- [ ] Category support
- [ ] Share/import/export

#### 8. Auto-Explore
- [ ] Chunk exploration algorithm
- [ ] Memory of explored areas
- [ ] Efficient pathing

#### 9. Follow Player
- [ ] FollowPlayerTask enhancement
- [ ] Dynamic distance keeping
- [ ] Obstacle avoidance

### Chunk 4: World Interaction Upgrades

#### 10. Auto-Mine
- [ ] Ore vein detection
- [ ] Efficient mining pattern
- [ ] Cave safety

#### 11. Auto-Smelt
- [ ] Fuel management
- [ ] Batch smelting
- [ ] Output collection

#### 12. Auto-Trade
- [ ] Villager trade optimizer
- [ ] Emerald management
- [ ] Trade locking

#### 13. Chest Management
- [ ] Item categorization
- [ ] Auto-sort algorithm
- [ ] Priority rules

---

## Next Steps

1. **Implement Combat Upgrades** (Priority: HIGH)
   - Complete Auto-Craft/Repair with priority system
   - Finish Witch Potion Dodging with trajectory prediction
   - Add PvP RetaliateTask with combo system

2. **Write Tests** (Required for each feature)
   - CraftDurabilityTaskTest
   - PotionEffectTrackerTest  
   - RetaliateTaskTest

3. **Update HUD Overlay** (All features need visual feedback)

4. **Integration Testing** (Test features working together)

---

## Commands Reference

```bash
# Build and test
./gradlew build
./gradlew test

# Run specific test
./gradlew test --tests "CraftDurabilityTaskTest"

# Commit frequently
git add <files>
git commit -m "feat: <specific feature>"
```
