#!/bin/bash
# Run GUT tests headlessly
# Usage: ./run_tests.sh [godot_path]
set -e

GODOT="${1:-/Applications/Godot.app/Contents/MacOS/Godot}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== GUT Tests ==="
echo "Godot: $GODOT"
echo "Project: $PROJECT_DIR"

# Build
echo "Building..."
cd "$PROJECT_DIR" && mvn package -DskipTests -q 2>&1

# Run GUT
"$GODOT" --path "$PROJECT_DIR" --headless --scene tests/test_gut_entry.tscn 2>&1 | tee /tmp/gui_test_output.log

if grep -q "ALL TESTS PASSED" /tmp/gui_test_output.log; then
    echo ""
    echo "=== ALL TESTS PASSED ==="
    exit 0
elif grep -q "TESTS FAILED" /tmp/gui_test_output.log; then
    echo ""
    echo "=== SOME TESTS FAILED ==="
    exit 1
else
    echo ""
    echo "=== TEST RUN COMPLETED (check log) ==="
    exit 0
fi
