# AltoClef Huy Edition

A Minecraft bot mod that automatically builds, survives, and completes tasks.

Powered by Baritone. Forked by **Gia Huy**.

> Fabric 1.17.1 | Java 17 | Gradle 7.3.3

---

## New Features (compared to original)

- **State Machine Builder** — `SchematicBuildTask` uses a state machine (`BUILDING` → `SOURCING` → `RECOVERING`) instead of messy boolean flags
- **Sourcing Hysteresis** — Bot collects ALL required materials before returning to build, no more collect 1 block → place → collect again loop
- **Dimension-Aware Avoidance** — Avoidance system recognizes dimensions (Overworld/Nether/End), won't block breaking in the wrong dimension
- **Fall Damage Prevention** — Bot automatically crouches when standing near edges with ≥ 4 block drops
- **Unsupported Block Mapper** — Automatically maps unusual schematic blocks (water, redstone_wire, crops...) to obtainable items, skips unobtainable blocks
- **Global Stuck Watchdog** — If the bot doesn't move for 30 seconds, automatically resets the task chain
- **HUD Overlay** — Displays builder state (BUILDING/SOURCING/RECOVERING) + list of missing blocks
- **`@info` Command** — View commands, schematic files, builder status and task chain

---

## Installation

### Requirements
- Minecraft 1.17.1 (Fabric)
- Java 17+
- Fabric Loader 0.11.6+

### How to install
1. Download the JAR from [Releases](https://github.com/fhfjjfjddg/altoclef_huy/releases) or build from source
2. Copy the JAR into `.minecraft/mods/`
3. Launch Minecraft with Fabric Loader

### Build from source
```bash
git clone https://github.com/fhfjjfjddg/altoclef_huy.git
cd altoclef_huy
chmod +x gradlew
./gradlew build
# JAR output: build/libs/altoclef-*.jar
```

---

## Usage

| Command | Description |
|---------|-------------|
| `@build <file.schem>` | Start building from a schematic file |
| `@info` | View bot status, available commands, schematic files |
| `@stop` | Stop the current task |
| `@coords` | Display current coordinates and dimension |

### Schematics
- Place `.schem` files in the `schematics/` folder inside `.minecraft/`
- The bot will automatically gather materials and build

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
│   ├── SchematicBuildTask.java   # ⭐ Core builder (state machine)
│   └── RandomRadiusGoalTask.java # Recovery movement
├── tasksystem/
│   ├── TaskRunner.java           # ⭐ Tick loop + watchdog
│   └── TaskChain.java            # Task chain system
├── chains/
│   ├── WorldSurvivalChain.java   # ⭐ Survival + fall protection
│   └── MLGBucketFallChain.java   # MLG water bucket
├── ui/
│   └── CommandStatusOverlay.java # ⭐ HUD overlay
└── util/
    ├── Dimension.java            # Enum + current()
    ├── CubeBounds.java           # Bounding box + dimension
    └── SchematicBlockMapper.java # ⭐ Block → Item mapper
```

---

## Credits

- [Original Alto Clef](https://github.com/gaucho-matrero/altoclef) by gaucho-matrero
- [Meloweh's Fork](https://github.com/Meloweh/altoclef) — schematic builder
- **Gia Huy** — state machine, dimension-aware avoidance, fall protection, block mapper, watchdog

## License

CC0-1.0
