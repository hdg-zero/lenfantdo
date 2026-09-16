#!/bin/bash -ex
#
# Copyright 2023 Miklos Vajna
#
# SPDX-License-Identifier: MIT
#

#
# This script runs all the tests for CI purposes.
#

mkdir -p app/keystore/
if [ -n "$KEYSTORE" ]; then
    echo "$KEYSTORE" | base64 -d > app/keystore/plees_keystore.jks
fi

./gradlew lintDebug
./gradlew test
./gradlew assembleRelease

# Run connectedAndroidTest only if a device or emulator is connected
if [ -n "${ANDROID_SERIAL:-}" ] || adb get-state 2>/dev/null | grep -q "device"; then
    ./gradlew connectedAndroidTest
fi

tools/license-check.sh

# Collect the release APK and audit offline permissions
if [ -f app/build/outputs/apk/release/app-release.apk ]; then
    ./tools/audit-release-apk.sh app/build/outputs/apk/release/app-release.apk
    mkdir -p dist
    cp app/build/outputs/apk/release/app-release.apk dist/
fi

for apk in app/build/outputs/apk/*/release/app-*-release.apk; do
    if [ -e "$apk" ]; then
        mkdir -p dist
        cp "$apk" dist/
    fi
done

# vim:set shiftwidth=4 softtabstop=4 expandtab:
