#!/usr/bin/env bash
# AltoClef Huy Edition - Build Script (Linux/macOS/Termux)

set -e

echo "=================================="
echo "  AltoClef Huy Edition - Builder"
echo "=================================="
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install Java 17+."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "[INFO] Java version: $JAVA_VER"
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    echo "[WARN] Java 17+ is required. Current: $JAVA_VER"
    exit 1
fi

# Grant permission
chmod +x gradlew

# Clean & build
echo "[INFO] Building project..."
./gradlew build

# Show output
echo ""
echo "=================================="
echo "  Build complete!"
echo "=================================="
echo ""
JAR=$(ls -1 build/libs/*.jar 2>/dev/null | grep -v sources | head -1)
if [ -n "$JAR" ]; then
    echo "[OUTPUT] $JAR"
    echo "[SIZE]   $(du -h "$JAR" | cut -f1)"
else
    echo "[WARN] No JAR found in build/libs/"
fi
echo ""
echo "Copy the JAR to .minecraft/mods/ to use."
