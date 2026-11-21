#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
. ./build-env.sh
initialize_signing
mkdir -p build/fixture/classes
"$bt/aapt2" link -o build/fixture/unsigned.apk -I "$platform" --manifest test/AndroidManifest.xml
javac --release 17 -cp "$platform" -d build/fixture/classes $(find test/src -name '*.java')
"$bt/d8" --min-api 35 --lib "$platform" --output build/fixture $(find build/fixture/classes -name '*.class')
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/fixture/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/fixture/classes.dex','classes.dex')
PY
"$bt/zipalign" -f 4 build/fixture/unsigned.apk build/fixture/aligned.apk
"$bt/apksigner" sign --ks "$keystore" --ks-pass "file:$password_file" --out build/fixture/fixture.apk build/fixture/aligned.apk
