# AltoClef Huy Edition

Minecraft bot mod tự động xây dựng, sinh tồn và hoàn thành nhiệm vụ.

Powered by Baritone. Fork bởi **Gia Huy**.

> Fabric 1.17.1 | Java 17 | Gradle 7.3.3

---

## Tính năng mới (so với bản gốc)

- **State Machine Builder** — `SchematicBuildTask` dùng state machine (`BUILDING` → `SOURCING` → `RECOVERING`) thay vì boolean flags rối rắm
- **Sourcing Hysteresis** — Bot thu thập TẤT CẢ vật liệu cần thiết trước khi quay lại xây, không còn loop collect 1 block → đặt → collect lại
- **Dimension-Aware Avoidance** — Hệ thống avoidance nhận biết chiều không gian (Overworld/Nether/End), không block breaking sai dimension
- **Fall Damage Prevention** — Bot tự động crouch khi đứng gần vực ≥ 4 block
- **Unsupported Block Mapper** — Tự động map block lạ trong schematic (water, redstone_wire, crops...) sang item lấy được, skip block không thể lấy
- **Global Stuck Watchdog** — Nếu bot không di chuyển trong 30 giây, tự động reset task chain
- **HUD Overlay** — Hiển thị trạng thái builder (BUILDING/SOURCING/RECOVERING) + danh sách block còn thiếu
- **`@info` Command** — Xem lệnh, file schematic, trạng thái builder và task chain

---

## Cài đặt

### Yêu cầu
- Minecraft 1.17.1 (Fabric)
- Java 17+
- Fabric Loader 0.11.6+

### Cách cài
1. Tải file JAR từ [Releases](https://github.com/fhfjjfjddg/altoclef_huy/releases) hoặc build từ source
2. Copy JAR vào thư mục `.minecraft/mods/`
3. Chạy Minecraft với Fabric Loader

### Build từ source
```bash
git clone https://github.com/fhfjjfjddg/altoclef_huy.git
cd altoclef_huy
chmod +x gradlew
./gradlew build
# JAR output: build/libs/altoclef-*.jar
```

---

## Cách sử dụng

| Lệnh | Mô tả |
|-------|--------|
| `@build <file.schem>` | Bắt đầu xây từ file schematic |
| `@info` | Xem trạng thái bot, lệnh khả dụng, file schematic |
| `@stop` | Dừng task hiện tại |
| `@coords` | Hiển thị tọa độ và dimension hiện tại |

### Schematic
- Đặt file `.schem` vào thư mục `schematics/` trong `.minecraft/`
- Bot sẽ tự động thu thập vật liệu và xây dựng

---

## Tính năng gốc

- Thu thập 400+ loại item từ world survival mới
- Né đạn mob, force field mob
- Thu thập + nấu thức ăn từ động vật, cây trồng
- Nhận lệnh qua chat whisper (Butler System)
- Tự động đánh Minecraft từ đầu đến cuối
- File config có thể reload qua lệnh

---

## Cấu trúc dự án

```
src/main/java/adris/altoclef/
├── AltoClef.java                 # Main mod class
├── TaskCatalogue.java            # Registry item → task
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

## Credit

- [Alto Clef gốc](https://github.com/gaucho-matrero/altoclef) bởi gaucho-matrero
- [Fork Meloweh](https://github.com/Meloweh/altoclef) — schematic builder
- **Gia Huy** — state machine, dimension-aware, fall protection, block mapper, watchdog

## License

CC0-1.0
