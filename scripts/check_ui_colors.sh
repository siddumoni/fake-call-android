#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="$ROOT/app/src/main/java"
if grep -RInE --include='*.kt' '\bColor\s*\(|\bColor\.[A-Za-z_]+' "$SOURCE" --exclude='Theme.kt'; then
    echo "Direct Compose color values are forbidden outside the central theme." >&2
    exit 1
fi
echo "UI color-token policy passed."
