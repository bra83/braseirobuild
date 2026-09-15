#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ACTUAL="${1:?usage: audit_scr015_visual.sh ACTUAL_CAPTURE.png [TOLERANCE]}"
TOL="${2:-16}"
PYTHONPATH="$ROOT/tools" python "$ROOT/tools/visual_diff.py" \
  "$ROOT/docs/wave2/SCR015_REFERENCE_PROPORTIONAL_415x915.png" "$ACTUAL" \
  "$ROOT/docs/wave2/latest_visual_audit" --tolerance "$TOL"
