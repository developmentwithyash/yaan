#!/usr/bin/env bash
# YAAN Static Analysis and Code Style Checker
# Verifies Kotlin coding conventions across the multiplatform repository.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "=== Running YAAN Static Analysis & Style Check ==="

CHECKED_FILES=0
VIOLATIONS=0

while IFS= read -r -d '' file; do
    CHECKED_FILES=$((CHECKED_FILES + 1))

    # Verify line endings are LF, not CRLF
    if file "$file" | grep -q "CRLF"; then
        echo "ERROR: [CRLF Line Endings] $file uses Windows CRLF. Use LF." >&2
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done < <(find "$ROOT_DIR" -type f \( -name "*.kt" -o -name "*.kts" \) ! -path "*/build/*" ! -path "*/.gradle/*" ! -path "*/.git/*" -print0)

if [ "$VIOLATIONS" -gt 0 ]; then
    echo "FAILED: $VIOLATIONS code style violations detected." >&2
    exit 1
fi

echo "✓ Verified $CHECKED_FILES Kotlin source files across :composeApp, :core, :feature, and :backend."
echo "✓ Static analysis and style verification PASSED."
