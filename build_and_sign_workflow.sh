#!/bin/bash
set -e

# Variablen
APP_ID="io.github.freetimemaker.geoweather"
TAG="${GITHUB_REF_NAME:-v1.3.6}" # Fallback falls lokal ausgeführt
KEYSTORE="./GeoWeather-KeyStore.jks"
KEY_ALIAS="alle"
KEY_PASS="KKKKKK" # Hinweis: Sollte idealerweise via Secrets kommen
OUT_APK="GeoWeather-$TAG.apk"

echo "==> Java Version"
java -version

# Android APK Pfad (von Gradle generiert)
UNSIGNED_APK="androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk"

# Falls noch nicht gebaut, jetzt bauen (lokaler Support)
if [ ! -f "$UNSIGNED_APK" ]; then
    echo "==> Baue Android Release..."
    ./gradlew :androidApp:assembleRelease
fi

echo "==> Finde apksigner"
APKSIGNER=$(find /usr/local/lib/android/sdk/build-tools -name apksigner | head -n 1)

if [ -z "$APKSIGNER" ]; then
    echo "FEHLER: apksigner nicht gefunden!"
    exit 1
fi

echo "==> Signiere APK: $OUT_APK"
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass pass:"$KEY_PASS" \
  --key-pass pass:"$KEY_PASS" \
  --out "$OUT_APK" \
  "$UNSIGNED_APK"

echo "==> Prüfe Signatur"
"$APKSIGNER" verify --verbose "$OUT_APK"