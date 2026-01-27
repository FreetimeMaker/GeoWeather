#!/bin/bash
set -e

### CONFIG ###
APP_ID="com.freetime.geoweather"
TAG="v1.1.3"   # <-- HIER deine Version eintragen
KEYSTORE="$HOME/AndroidStudioProjects/GeoWeather/GeoWeather-KeyStore.jks"
KEY_ALIAS="alle"
KEY_PASS="KKKKKK"
OUT_APK="GeoWeather-$TAG.apk"
################

echo "==> Hole Tags von GitHub"
git fetch --tags

echo "==> Checkout des Release-Tags: $TAG"
git checkout "$TAG"

echo "==> Sauberer Build"
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
