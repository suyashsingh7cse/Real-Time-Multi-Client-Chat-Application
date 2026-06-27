#!/bin/bash
# ============================================================
#  Build Script — Real-Time Multi-Client Chat Application
#  Developer: Suyash
# ============================================================

echo "============================================"
echo "  Building Real-Time Chat Application..."
echo "============================================"

# Clean previous build
rm -rf out/
mkdir -p out/

# Collect all Java sources
SOURCES=$(find src/ -name "*.java")

# Compile
javac -d out/ -sourcepath src/ $SOURCES

if [ $? -eq 0 ]; then
    echo ""
    echo "[SUCCESS] Compilation complete. Classes are in: out/"
    echo ""
    echo "  Run Server : java -cp out server.ChatServer [port]"
    echo "  Run Client : java -cp out client.ChatClient [host] [port]"
    echo ""
else
    echo ""
    echo "[FAILED] Compilation errors. Fix them and try again."
    exit 1
fi
