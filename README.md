# AltoClef Huy Edition

A Minecraft bot mod that automatically builds, farms, fights, survives, and completes tasks.

Powered by Baritone. Forked by **Gia Huy**.

> Fabric 1.17.1 | Java 17 | Gradle 7.3.3

---

## New Features (compared to original)

### ⚔️ Advanced Combat
- **Auto-Craft/Repair** — Automatically crafts/repairs weapons and armor when durability is low
- **Witch Potion Dodging** — Dodges harmful potion effects from witches
- **PvP Mode** — Attack players when attacked first (retaliation mode)
- **Advanced Combat Manager** — Shield auto-blocking, critical hits, sprint-reset combos, circle strafing
- **Auto-Equip Weapon** — Automatically equips the best weapon (netherite > diamond > iron, swords + axes ranked by damage)
- **Smart Retreat** — Bot retreats to heal when HP is critically low
- **Combat HUD** — Shows combat state (ENGAGING/BLOCKING/RETREATING) + HP bar

### 🏗️ Advanced Builder
- **Multi-Schematic** — Build multiple files in sequence with `@multi-build <file1.schem> <file2.schem> ...`
- **Auto-Light** — Place torches automatically when building in dark areas
- **Undo Last Build** — Undo the last schematic build with `@build-undo`
- **State Machine Builder** — `SchematicBuildTask` uses a state machine (`BUILDING` → `SOURCING` → `RECOVERING`) instead of messy boolean flags
- **Schematic Rotation** — Rotate schematics 90°/180°/270° before building with `@build file.schem 90`
- **Sourcing Hysteresis** — Bot collects ALL required materials before returning to build
- **Unsupported Block Mapper** — Maps unusual schematic blocks to obtainable items, skips unobtainable blocks
- **Dimension-Aware Building** — Auto-detects Overworld/Nether/End, adjusts height limits, warns about hazards (Nether ceiling, void)
- **Scaffold System** — Auto-builds pillar/bridge scaffolding for elevated (high-altitude) construction
- **Underground Excavation** — Auto-clears underground area before building, seals lava/water hazards on boundaries
- **Build Modes** — `AUTO` (detect from Y), `ELEVATED` (force scaffold), `UNDERGROUND` (force excavation), `SURFACE` (normal)

### 🗺️ Navigation
- **Waypoint System** — `@waypoint save <name>`, `@waypoint go <name>`, `@waypoint remove <name>`, `@waypoint list`
- **Auto-Explore** — Automatically explore new chunks with `@auto-explore`
- **Follow Player** — Follow a specific player with `@follow <player>`

### 🌍 World Interaction
- **Auto-Mine** — Automatically mine ore veins with `@auto-mine <ore> [count]`
- **Auto-Smelt** — Automatically smelt raw materials with `@auto-smelt <item> [count]`
- **Auto-Trade** — Trade with villagers automatically with `@auto-trade <item> [count]`
- **Chest Management** — Automatically sort items into chests with `@chest-manager <mode>`

### 🌾 Farming
- **Auto-Farm System** — Fully automatic crop farming with `@auto-farm <crop> <count>`
- Supports 12 crop types: wheat, carrot, potato, beetroot, sugar_cane, melon, pumpkin, cactus, bamboo, sweet_berries, nether_wart, cocoa_beans
- **Farmland Expansion** — Auto-builds 9x9 farm plots around water sources (up to 4 plots), auto-tills dirt
- **Bone Meal** — Auto-uses bone meal on immature crops to speed up growth
- **Smart Harvesting** — Sugar cane/cactus/bamboo: only breaks upper blocks (preserves base). Sweet berries: right-click harvest (doesn't destroy bush)
- **Nether Wart** — Harvests at age 3, replants on soul sand
- **Auto Seed Management** — Picks up dropped seeds, acquires new seeds when needed
- **Farm HUD** — Shows farm state (HARVESTING/REPLANTING/BONE_MEALING/EXPANDING/TILLING/SEARCHING)

### 🛡️ Survival
- **Fall Damage Prevention** — Auto-crouches near edges with ≥ 4 block drops
- **Dimension-Aware System** — Height limits, ceiling detection, scaffold block preferences per dimension (Overworld/Nether/End)
- **Auto-Equip Tool** — Automatically equips the best tool when mining

### 🔧 System
- **Global Stuck Watchdog** — Resets task chain if no movement for 30 seconds
- **HUD Overlay** — Builder state + missing blocks + combat status + farm state + HP
- **Categorized Help** — `@help` shows commands grouped by category
- **`@info` Command** — View commands, schematics, farm crops, build status

---

## Installation

### Requirements
- Minecraft 1.17.1 (Fabric)
- Java 17+
- Fabric Loader 0.11.6+

### How to install
1. Download the JAR from [Releases](https://github.com/fhfjjfjd/altoclef_giahuy/releases) or build from source
2. Copy the JAR into `.minecraft/mods/`
3. Launch Minecraft with Fabric Loader

### Build from source
```bash
git clone https://github.com/fhfjjfjd/altoclef_giahuy.git
cd altoclef_giahuy
```

**Linux / macOS / Termux:**
```bash
./build.sh
```

**Windows:**
```
build.bat
```

The JAR will be in `build/libs/`.

---

## Commands

### 🏗️ Building
| Command | Description |
|---------|-------------|
| `@build <file.schem>` | Build a schematic at player position |
| `@build <file.schem> <90/180/270>` | Build with clockwise rotation |
| `@build <file.schem> [rotation] elevated` | Build with auto-scaffolding (high altitude) |
| `@build <file.schem> [rotation] underground` | Build with auto-excavation (underground) |
| `@build <file.schem> [rotation] auto` | Auto-detect build mode from position |
| `@multi-build <file1.schem> <file2.schem> ...` | Build multiple schematics in sequence |
| `@auto-light-build <file.schem>` | Build with automatic torch placement |
| `@build-undo` | Undo the last schematic build |
| `@autofill <materials...>` | Fill a target chest with specified materials |

### 🌾 Farming
| Command | Description |
|---------|-------------|
| `@auto-farm <crop> [count]` | Auto-farm a crop (default: 100) |
| `@food <count>` | Collect food (hunts animals, harvests crops) |

**Supported crops:**
| Type | Crops |
|------|-------|
| Farmland | `wheat` `carrot` `potato` `beetroot` |
| Ground | `sugar_cane` `cactus` `bamboo` `sweet_berries` `melon` `pumpkin` |
| Nether | `nether_wart` |
| Tree | `cocoa_beans` |

### 🗺️ Navigation
| Command | Description |
|---------|-------------|
| `@goto <x> <y> <z>` | Go to coordinates |
| `@follow <player>` | Follow a player |
| `@roundtrip <x> <y> <z>` | Travel to coordinates and back |
| `@coords` | Show current coordinates and dimension |
| `@locate_structure <name>` | Find a structure |
| `@waypoint save <name>` | Save current position as waypoint |
| `@waypoint go <name>` | Navigate to saved waypoint |
| `@waypoint remove <name>` | Delete a waypoint |
| `@waypoint list` | List all waypoints |
| `@auto-explore` | Automatically explore new chunks |

### ⛏️ Resources & Mining
| Command | Description |
|---------|-------------|
| `@get <item> [count]` | Obtain any item |
| `@list` | List all obtainable items |
| `@give <player> <item> [count]` | Give item to a player |
| `@auto-mine <ore> [count]` | Automatically mine ore veins |
| `@auto-smelt <item> [count]` | Automatically smelt raw materials |

### ⚔️ Combat
| Command | Description |
|---------|-------------|
| `@punk <player>` | Attack a player |
| `@gamer` | Auto speedrun (kill Ender Dragon) |
| `@auto-trade <item> [count]` | Auto-trade with villagers |

### 📦 Chest Management
| Command | Description |
|---------|-------------|
| `@chest-manager <mode>` | Automatically sort items into chests |

### 🔧 System
| Command | Description |
|---------|-------------|
| `@help` | Show all commands grouped by category |
| `@info` | Show commands, schematics, farm crops, build status |
| `@status` | Show current task status |
| `@inventory` | Show inventory contents |
| `@stop` | Stop the current task |
| `@reload_settings` | Reload settings from file |
| `@gamma <value>` | Set brightness level |

---

## Schematics

- Place `.schem` files in the `schematics/` folder inside `.minecraft/`
- The bot will automatically gather materials and build
- Rotation is clockwise around the Y axis (looking down)
- The block mapper handles unusual blocks (water, redstone_wire, crops → obtainable items)

---

## Auto-Equip

- **Mining** — Automatically equips the best tool (pickaxe/axe/shovel) for the target block
- **Combat** — Equips the best weapon available (netherite > diamond > iron, swords + axes ranked by damage)
- **Shield** — Auto-raises shield between attack cooldowns (if shield in offhand)

---

## Original Features

- Obtain 400+ items from a fresh survival world
- Dodge mob projectiles and force field mobs away
- Collect + smelt food from animals, hay, & crops
- Receive commands via chat whispers (Butler System)
- Beat the entire game autonomously (no user input)
- Config file reloadable via command

---

## Project Structure

```
src/main/java/adris/altoclef/
├── AltoClef.java                 # Main mod class
├── TaskCatalogue.java            # Item → task registry
├── tasks/
│   ├── SchematicBuildTask.java   # ⭐ Core builder (state machine + rotation)
│   ├── DimensionAwareBuildTask.java # ⭐ Dimension-aware build (scaffold/excavate)
│   ├── construction/
│   │   ├── ScaffoldTask.java        # ⭐ Auto-scaffold for elevated building
│   │   ├── ExcavateAreaTask.java    # ⭐ Underground excavation with hazard seal
│   │   ├── PlaceBlockTask.java      # Block placement logic
│   │   └── ClearRegionTask.java     # Region clearing
│   └── resources/
│       └── AutoFarmTask.java     # ⭐ Auto-farm system (12 crop types)
├── commands/
│   ├── BuildCommand.java         # @build with rotation
│   ├── AutoFarmCommand.java      # @auto-farm command
│   ├── HelpCommand.java          # ⭐ Categorized help
│   └── InfoCommand.java          # @info with farm crops
├── tasksystem/
│   ├── TaskRunner.java           # ⭐ Tick loop + watchdog
│   └── TaskChain.java            # Task chain system
├── chains/
│   ├── MobDefenseChain.java      # ⭐ Combat + retreat logic
│   ├── WorldSurvivalChain.java   # ⭐ Survival + fall protection
│   └── MLGBucketFallChain.java   # MLG water bucket
├── ui/
│   └── CommandStatusOverlay.java # ⭐ HUD overlay (build + combat + farm)
└── util/
    ├── CombatManager.java        # ⭐ Advanced combat (shield/crit/strafe)
    ├── AutoToolEquip.java        # ⭐ Auto-equip best tool/weapon
    ├── KillAura.java             # ⭐ Combat aura + crit integration
    ├── RotatedSchematic.java     # ⭐ Schematic rotation wrapper
    ├── Dimension.java            # Enum + current()
    ├── CubeBounds.java           # Bounding box + dimension
    └── SchematicBlockMapper.java # ⭐ Block → Item mapper
```

---

## Credits

- [Original Alto Clef](https://github.com/gaucho-matrero/altoclef) by gaucho-matrero
- [Meloweh's Fork](https://github.com/Meloweh/altoclef) — schematic builder
- **Gia Huy** — state machine, rotation, auto-equip, auto-farm, advanced combat, dimension-aware building, scaffold, excavation, fall protection, block mapper, watchdog, HUD overlay

## License

CC0-1.0
