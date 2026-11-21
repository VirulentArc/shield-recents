#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
. ./build-env.sh
initialize_signing
mkdir -p build/classes dist
rm -rf build/classes
mkdir -p build/classes
"$bt/aapt2" compile --dir app/res -o build/resources.zip
"$bt/aapt2" link -o build/app-unsigned.apk -I "$platform" --manifest app/AndroidManifest.xml --java build/generated build/resources.zip --auto-add-overlay
javac --release 17 -cp "$platform" -d build/classes $(find app/src build/generated -name '*.java')
"$bt/d8" --min-api 35 --lib "$platform" --output build $(find build/classes -name '*.class')
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/app-unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/classes.dex','classes.dex')
PY
"$bt/zipalign" -f 4 build/app-unsigned.apk build/app-aligned.apk
"$bt/apksigner" sign --ks "$keystore" --ks-pass "file:$password_file" --out dist/ShieldRecents.apk build/app-aligned.apk
"$bt/aapt2" compile --dir overlay/res -o build/overlay-resources.zip
"$bt/aapt2" link -o build/overlay-unsigned.apk -I "$platform" --manifest overlay/AndroidManifest.xml build/overlay-resources.zip --auto-add-overlay --no-resource-deduping --no-resource-removal
"$bt/zipalign" -f 4 build/overlay-unsigned.apk build/overlay-aligned.apk
"$bt/apksigner" sign --ks "$keystore" --ks-pass "file:$password_file" --out dist/ShieldRecentsOverlay.apk build/overlay-aligned.apk
"$bt/apksigner" verify dist/ShieldRecents.apk
"$bt/apksigner" verify dist/ShieldRecentsOverlay.apk
"$bt/aapt2" dump resources dist/ShieldRecentsOverlay.apk > build/overlay-resource-table.txt
grep -q '(television) true' build/overlay-resource-table.txt
sha256sum dist/*.apk > dist/SHA256SUMS
