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

echo "==> Prüfe, ob Release existiert"

API_URL="https://api.github.com/repos/${GITHUB_REPOSITORY}/releases/tags/${TAG}"
RELEASE_JSON=$(curl -s -H "Authorization: Bearer ${GITHUB_TOKEN}" "$API_URL")

RELEASE_ID=$(echo "$RELEASE_JSON" | jq -r '.id')

if [ "$RELEASE_ID" = "null" ]; then
    echo "Kein Release für Tag $TAG gefunden – APK wird NICHT hochgeladen."
    echo "==> Fertig!"
    exit 0
fi

echo "Release gefunden (ID: $RELEASE_ID)"

echo "==> Prüfe, ob Asset bereits existiert"

ASSET_ID=$(echo "$RELEASE_JSON" | jq -r '.assets[] | select(.name=="'"$OUT_APK"'") | .id')

if [ -n "$ASSET_ID" ] && [ "$ASSET_ID" != "null" ]; then
    echo "Alte APK gefunden (Asset ID: $ASSET_ID) – lösche sie"
    curl -s -X DELETE \
      -H "Authorization: Bearer ${GITHUB_TOKEN}" \
      "https://api.github.com/repos/${GITHUB_REPOSITORY}/releases/assets/${ASSET_ID}"
fi

echo "==> Lade neue APK hoch"

UPLOAD_URL="https://uploads.github.com/repos/${GITHUB_REPOSITORY}/releases/${RELEASE_ID}/assets?name=${OUT_APK}"

curl -s \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @"${OUT_APK}" \
  "$UPLOAD_URL"

echo "==> Upload abgeschlossen!"
echo "Signierte APK: $OUT_APK"
echo "==> Fertig!"
