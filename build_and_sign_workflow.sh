#!/bin/bash
set -e

APP_ID="com.freetime.geoweather"
TAG="${GITHUB_REF_NAME}"
KEYSTORE="./GeoWeather-KeyStore.jks"
KEY_ALIAS="alle"
KEY_PASS="KKKKKK"
OUT_APK="GeoWeather-$TAG.apk"

echo "==> Java Version"
java -version

echo "==> Entferne lokale Tags"
git tag -l | xargs -r git tag -d

echo "==> Hole Tags von GitHub"
git fetch --tags

echo "==> Checkout des Release-Tags: $TAG"
git checkout "$TAG"

echo "==> Baue Release"
./gradlew clean assembleRelease

UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

echo "==> Finde apksigner"
APKSIGNER=$(find /usr/local/lib/android/sdk/build-tools -name apksigner | head -n 1)

if [ -z "$APKSIGNER" ]; then
    echo "FEHLER: apksigner nicht gefunden!"
    exit 1
fi

echo "==> Verwende apksigner: $APKSIGNER"

echo "==> Signiere APK"
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  --out "$OUT_APK" \
  "$UNSIGNED_APK"

echo "==> Prüfe Signatur"
"$APKSIGNER" verify --verbose "$OUT_APK"

echo "==> Fertig!"
echo "Signierte APK: $OUT_APK"