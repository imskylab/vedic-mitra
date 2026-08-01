#!/usr/bin/env bash
#
# check.sh — run the full local quality gate (mirrors CI).
# Usage: scripts/check.sh
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Spotless (formatting check)"
./gradlew spotlessCheck

echo "==> Detekt (static analysis)"
./gradlew detekt

echo "==> Unit tests"
./gradlew testDebugUnitTest

echo "==> All checks passed."
