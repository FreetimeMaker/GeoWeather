#!/bin/bash
set -e

### CONFIG ###
APP_ID="com.freetime.geoweather"
TAG="${GITHUB_REF_NAME}"   # automatisch vom CI gesetzt
KEYSTORE="release.keystore"
KEY_ALIAS="${KEY_ALIAS}"
KEY_PASS="${KEY_PASS}"
OUT_APK="GeoWeather-$TAG.apk"
################

echo "==> Java Version"
java -version

echo "==> Checkout des Release-Tags: $TAG"
git fetch --tags
git checkout "$TAG"

echo "==> Baue Release"
./gradlew clean assembleRelease

UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

if [ ! -f "$UNSIGNED_APK" ]; then
    echo "FEHLER: Unsigned APK nicht gefunden!"
    exit 1
fi

echo "==> Signiere APK"
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  --out "$OUT_APK" \
  "$UNSIGNED_APK"

echo "==> Prüfe Signatur"
apksigner verify --verbose "$OUT_APK"

echo "==> Fertig!"
echo "Signierte APK: $OUT_APK"
