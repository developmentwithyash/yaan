#!/usr/bin/env bash
# YAAN Automated APK & Desktop JAR Binary Size + Dependency Report
# Measures build artifact sizes, appends metrics to $GITHUB_STEP_SUMMARY,
# and fails the build if any binary exceeds its configured size budget.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

APK_PATH="${APK_PATH:-$ROOT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk}"
if [[ -z "${JAR_PATH:-}" ]]; then
    JAR_CANDIDATE="$(find "$ROOT_DIR/composeApp/build/compose/jars" -maxdepth 1 -name '*.jar' 2>/dev/null | head -n 1 || true)"
    JAR_PATH="${JAR_CANDIDATE:-$ROOT_DIR/composeApp/build/compose/jars/composeApp-desktop.jar}"
fi

# Default budgets: 100 MiB for debug APK, 120 MiB for Desktop Uber JAR
APK_BUDGET_BYTES="${APK_BUDGET_BYTES:-104857600}"
JAR_BUDGET_BYTES="${JAR_BUDGET_BYTES:-125829120}"

file_size_bytes() {
    local target="$1"
    if stat -f%z "$target" >/dev/null 2>&1; then
        stat -f%z "$target"
    else
        stat -c%s "$target"
    fi
}

format_mib() {
    local bytes="$1"
    awk -v b="$bytes" 'BEGIN { printf "%.2f MiB", b / (1024 * 1024) }'
}

VIOLATIONS=0
REPORT_LINES=()
REPORT_LINES+=("### YAAN Binary Size & Dependency Report")
REPORT_LINES+=("")
REPORT_LINES+=("| Artifact | Size (Bytes) | Size (MiB) | Budget (MiB) | Status |")
REPORT_LINES+=("| :--- | ---: | ---: | ---: | :---: |")

check_artifact() {
    local label="$1"
    local path="$2"
    local budget="$3"

    if [[ ! -f "$path" ]]; then
        REPORT_LINES+=("| \`$label\` | 0 | 0.00 MiB | $(format_mib "$budget") | MISSING |")
        echo "ERROR: Missing artifact for $label at $path" >&2
        VIOLATIONS=$((VIOLATIONS + 1))
        return
    fi

    local size
    size="$(file_size_bytes "$path")"
    local size_mib
    size_mib="$(format_mib "$size")"
    local budget_mib
    budget_mib="$(format_mib "$budget")"

    if (( size > budget )); then
        REPORT_LINES+=("| \`$label\` | $size | $size_mib | $budget_mib | EXCEEDED |")
        echo "ERROR: $label ($size bytes / $size_mib) exceeds budget ($budget bytes / $budget_mib)" >&2
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        REPORT_LINES+=("| \`$label\` | $size | $size_mib | $budget_mib | PASS |")
    fi
}

check_artifact "composeApp-debug.apk" "$APK_PATH" "$APK_BUDGET_BYTES"
check_artifact "composeApp-desktop.jar" "$JAR_PATH" "$JAR_BUDGET_BYTES"

# Count declared project/library dependencies in composeApp/build.gradle.kts
GRADLE_FILE="$ROOT_DIR/composeApp/build.gradle.kts"
if [[ -f "$GRADLE_FILE" ]]; then
    DEP_COUNT="$(grep -cE '^\s*(implementation|api)\(' "$GRADLE_FILE" || true)"
    REPORT_LINES+=("")
    REPORT_LINES+=("- Declared dependencies in \`composeApp/build.gradle.kts\`: **$DEP_COUNT**")
fi

REPORT_CONTENT="$(printf '%s\n' "${REPORT_LINES[@]}")"
echo "$REPORT_CONTENT"

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    printf '%s\n' "$REPORT_CONTENT" >> "$GITHUB_STEP_SUMMARY"
fi

if (( VIOLATIONS > 0 )); then
    exit 1
fi
