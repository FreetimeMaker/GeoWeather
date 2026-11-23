#!/usr/bin/env bash
# Run only inside a disposable F-Droid buildserver container: --on-server removes sudo.
set -euo pipefail

exec > >(tee /output/build.log) 2>&1
source /etc/profile.d/bsenv.sh

export fdroidserver=/opt/geoweather-fdroidserver
# Reviewed upstream tooling; update deliberately rather than following master at build time.
FDROID_SERVER_REVISION=fc2c86473e66a44fc641f403c338a8b2cccfad88
git init "$fdroidserver"
git -C "$fdroidserver" remote add origin https://gitlab.com/fdroid/fdroidserver.git
git -C "$fdroidserver" fetch --depth 1 origin "$FDROID_SERVER_REVISION"
git -C "$fdroidserver" checkout --detach FETCH_HEAD
export PATH="$fdroidserver:$PATH"
export PYTHONPATH="$fdroidserver:$fdroidserver/examples"
export PYTHONUNBUFFERED=true
export ANDROID_HOME=/opt/android-sdk
export GRADLE_USER_HOME=/home/vagrant/.gradle

apt-get update
apt-get install -y --no-install-recommends openjdk-21-jdk-headless
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
sdkmanager "platform-tools" "build-tools;35.0.0"

mkdir -p /work/metadata /work/build /work/logs /work/repo /work/tmp /work/unsigned
cd /work

# Test the checked-out commit (including PR merge commits), not the latest published release.
# No Binaries field: pre-release CI cannot compare against an already published APK.
python3 - <<'PY'
import json
from pathlib import Path
import re
import subprocess

source = Path('/checkout/app/build.gradle.kts').read_text()
version = re.search(r'versionName\s*=\s*"([^"]+)"', source).group(1)
code = int(re.search(r'versionCode\s*=\s*(\d+)', source).group(1))
commit = subprocess.check_output(
    ['git', '-c', 'safe.directory=/checkout', '-C', '/checkout', 'rev-parse', 'HEAD'],
    text=True,
).strip()
metadata = {
    'License': 'Apache-2.0',
    'RepoType': 'git',
    'Repo': '/checkout',
    'Builds': [{
        'versionName': version,
        'versionCode': code,
        'commit': commit,
        'subdir': 'app',
        'gradle': ['yes'],
    }],
}
Path('metadata/com.freetime.geoweather.yml').write_text(json.dumps(metadata, indent=2) + '\n')
PY

# Preserve diagnostics even if the source scanner or build rejects this commit.
collect_results() {
    for path in metadata logs repo/status tmp; do
        if [ -e "/work/$path" ]; then
            cp -r "/work/$path" /output/
        fi
    done
}
trap collect_results EXIT

chown -R vagrant:vagrant /work "$fdroidserver"
run_as_builder() {
    # sudo's secure_path overrides PATH even with --preserve-env.
    sudo --user vagrant env \
        HOME=/home/vagrant \
        PATH="$PATH" \
        PYTHONPATH="$PYTHONPATH" \
        PYTHONUNBUFFERED="$PYTHONUNBUFFERED" \
        JAVA_HOME="$JAVA_HOME" \
        ANDROID_HOME="$ANDROID_HOME" \
        ANDROID_SDK_ROOT="$ANDROID_HOME" \
        GRADLE_USER_HOME="$GRADLE_USER_HOME" \
        "$@"
}

# Local clones validate the source git directory separately from its working tree.
run_as_builder git config --global --add safe.directory /checkout
run_as_builder git config --global --add safe.directory /checkout/.git
run_as_builder /usr/bin/python3 "$fdroidserver/fdroid" \
    scanner --verbose --refresh --exit-code com.freetime.geoweather
run_as_builder /usr/bin/python3 "$fdroidserver/fdroid" \
    build --verbose --on-server --stop --test --refresh-scanner --scan-binary --no-tarball \
    com.freetime.geoweather
