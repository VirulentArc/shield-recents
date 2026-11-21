#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Shared setup, sourced after changing to the repository directory.
if [ -z "${JAVA_HOME:-}" ] && [ -d "$PWD/../tools/jdk-21.0.13.1+2" ]; then
    export JAVA_HOME="$PWD/../tools/jdk-21.0.13.1+2"
fi
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
bt="${ANDROID_BUILD_TOOLS_DIR:-}"
platform="${ANDROID_JAR:-}"
keystore="${SHIELD_RECENTS_KEYSTORE:-$PWD/build/signing.p12}"
password_file="${SHIELD_RECENTS_PASSWORD_FILE:-$PWD/build/signing.pass}"
if [ -z "$bt" ]; then
    if [ -n "$sdk" ]; then bt="$sdk/build-tools/${ANDROID_BUILD_TOOLS_VERSION:-35.0.0}";
    else bt="$PWD/../tools/sdk-build/android-15"; fi
fi
if [ -z "$platform" ]; then
    if [ -n "$sdk" ]; then platform="$sdk/platforms/android-35/android.jar";
    else platform="$PWD/../tools/sdk-platform/android-35/android.jar"; fi
fi
for binary in java javac keytool python3; do
    command -v "$binary" >/dev/null || { echo "Missing tool: $binary" >&2; exit 1; }
done
[ -x "$bt/aapt2" ] && [ -x "$bt/d8" ] && [ -f "$platform" ] || {
    echo 'Set ANDROID_SDK_ROOT to an SDK with platform android-35 and build-tools 35.0.0.' >&2
    echo 'Alternatively set ANDROID_BUILD_TOOLS_DIR and ANDROID_JAR explicitly.' >&2
    exit 1
}
initialize_signing() {
    mkdir -p build
    if [ ! -f "$keystore" ]; then
        mkdir -p "$(dirname "$keystore")" "$(dirname "$password_file")"
        if [ ! -f "$password_file" ]; then
            python3 -c 'import secrets; print(secrets.token_urlsafe(32))' > "$password_file"
        fi
        chmod 600 "$password_file"
        keytool -genkeypair -keystore "$keystore" -storepass:file "$password_file" -alias recents -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Local Shield Recents Development'
        chmod 600 "$keystore"
    fi
    [ -f "$password_file" ] || {
        echo 'Signing key exists; provide its password through SHIELD_RECENTS_PASSWORD_FILE.' >&2
        exit 1
    }
}
