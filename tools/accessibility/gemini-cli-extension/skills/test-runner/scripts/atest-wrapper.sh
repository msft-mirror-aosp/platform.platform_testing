#!/bin/bash

OUTPUT_FILE=$(mktemp)
trap 'rm -f -- "$OUTPUT_FILE"' EXIT

# Disable as much noise as we can to reduce token spam
# Also the live output crashes Gemini CLI, so we need this wrapper script (b/481736248).
# TODO: b/469782470 - use the upcoming atest MCP server instead
export ANDROID_QUIET_BUILD=true
export SOONG_UI_ANSI_OUTPUT=true
export SOONG_UI_SKIP_ACTION_PROGRESS=true
export GEMINI_CLI=true

echo "Starting atest"
atest "$@" > "$OUTPUT_FILE" 2>&1 &
ATEST_PID=$!

while kill -0 $ATEST_PID 2> /dev/null; do
    # CLI Agent timeouts are not well documented, but some of them (Gemini) timeout at 30 seconds.
    # So we finish a bit before that to be safe.
    sleep 24
    if kill -0 $ATEST_PID 2> /dev/null; then
        echo "Still Running..."
    fi
done

wait $ATEST_PID
EXIT_CODE=$?

cat "$OUTPUT_FILE"

exit $EXIT_CODE
