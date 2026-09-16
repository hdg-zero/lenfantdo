#!/usr/bin/env bash
# Copyright 2026 L'enfant do Contributors
# SPDX-License-Identifier: MIT

set -euo pipefail

APK_PATH="${1:-app/build/outputs/apk/debug/app-debug.apk}"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at: $APK_PATH"
    echo "Usage: $0 [path-to-apk]"
    exit 1
fi

echo "🔍 Auditing permissions for: $APK_PATH"

AAPT_BIN=""
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
    AAPT_BIN=$(find "$ANDROID_HOME/build-tools" -name "aapt" -type f | sort -V | tail -n 1)
fi

if [ -z "$AAPT_BIN" ] || [ ! -x "$AAPT_BIN" ]; then
    if command -v aapt >/dev/null 2>&1; then
        AAPT_BIN="aapt"
    else
        echo "❌ aapt binary not found in ANDROID_HOME/build-tools or PATH"
        exit 1
    fi
fi

PERMISSIONS=$("$AAPT_BIN" dump badging "$APK_PATH" | grep "uses-permission" || true)

echo "📋 Declared permissions:"
echo "$PERMISSIONS"

FORBIDDEN=("android.permission.INTERNET" "android.permission.ACCESS_NETWORK_STATE" "android.permission.ACCESS_WIFI_STATE" "android.permission.FOREGROUND_SERVICE" "android.permission.WAKE_LOCK")
VIOLATIONS=0

for perm in "${FORBIDDEN[@]}"; do
    if echo "$PERMISSIONS" | grep -Fq "$perm"; then
        echo "❌ CRITICAL SECURITY VIOLATION: Forbidden permission detected -> $perm"
        VIOLATIONS=$((VIOLATIONS + 1))
    fi
done

if [ "$VIOLATIONS" -eq 0 ]; then
    echo "✅ AUDIT PASSED: Zero network or background-leaking permissions detected!"
    exit 0
else
    echo "❌ AUDIT FAILED: $VIOLATIONS forbidden permission(s) found."
    exit 1
fi
