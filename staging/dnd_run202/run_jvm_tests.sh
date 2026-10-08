#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p /tmp/dnd-run202-classes
javac -encoding UTF-8 -d /tmp/dnd-run202-classes android/app/src/main/java/com/braseiro/dnd5e/*.java tests/com/braseiro/dnd5e/*.java
java -cp /tmp/dnd-run202-classes com.braseiro.dnd5e.Core2024ConcentrationTest
java -cp /tmp/dnd-run202-classes com.braseiro.dnd5e.Core2024ConcentrationBoundaryTest
