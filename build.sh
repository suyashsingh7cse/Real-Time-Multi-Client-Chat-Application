#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# build.sh — One-command compile script for the Chat Application
# Usage: ./build.sh
# ─────────────────────────────────────────────────────────────
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$ROOT_DIR/src"
OUT_DIR="$ROOT_DIR/out"

echo "================================"
echo "  Building Real-Time Chat App   "
echo "================================"

# Clean previous build
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Collect all .java files
SOURCES=$(find "$SRC_DIR" -name "*.java" | sort)

echo "  Compiling $(echo "$SOURCES" | wc -l | tr -d ' ') source files..."

javac -d "$OUT_DIR" --release 17 $SOURCES

echo "  Build successful → out/"
echo "================================"
echo ""
echo "  Run server:  java -cp out server.ChatServer"
echo "  Run client:  java -cp out client.ChatClient"
echo "================================"
