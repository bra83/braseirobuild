#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/docs/wave2/SCR015_RUNTIME_CAPTURE_415x915.png}"
python "$ROOT/tools/capture_scr015_playwright.py" "$OUT"
