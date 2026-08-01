#!/usr/bin/env bash
#
# format.sh — auto-format the codebase with Spotless (ktlint under the hood).
# Usage: scripts/format.sh
#
set -euo pipefail

# Resolve repo root regardless of where the script is called from.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Running spotlessApply"
./gradlew spotlessApply "$@"
echo "==> Done. Review and commit the changes."
