#!/usr/bin/env bash
set -euo pipefail

# Refreshed from the user's Google Drive build bridge on 2026-09-17.
# These signed object URLs are temporary, but bypass the expired Firestorage shares.
DIRECT_URLS=(
'https://sdmntprbrazilsouth.oaiusercontent.com/files/00000000-2620-820e-96a3-d0f390be45f6/raw?se=2026-09-18T02%3A07%3A21Z&sp=r&sv=2026-02-06&sr=b&scid=d6f98b72-f570-56f8-bcc8-9ee7aa0a3b4d&skoid=4a431cd2-eafd-49c7-90be-bbb4d45e696d&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2026-09-17T18%3A15%3A50Z&ske=2026-09-18T18%3A15%3A50Z&sks=b&skv=2026-02-06&sig=5a5KHAzZmAZL1hDPnd1IureQoCcfWNaiELI764k6D7k%3D'
'https://sdmntprbrazilsouth.oaiusercontent.com/files/00000000-3e64-820e-8c13-d7ad4d69a135/raw?se=2026-09-18T02%3A07%3A38Z&sp=r&sv=2026-02-06&sr=b&scid=25a7fda6-0b4c-56eb-9c62-36bf8ed47592&skoid=4a431cd2-eafd-49c7-90be-bbb4d45e696d&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2026-09-17T18%3A15%3A50Z&ske=2026-09-18T18%3A15%3A50Z&sks=b&skv=2026-02-06&sig=yseYjqM66O/xVxXIUHYAKtf%2BzjMzsDIuvx3Awp6dSP4%3D'
'https://sdmntprbrazilsouth.oaiusercontent.com/files/00000000-6324-820e-9459-d14db3798e36/raw?se=2026-09-18T02%3A07%3A46Z&sp=r&sv=2026-02-06&sr=b&scid=52a7b66c-3ed9-5992-af2f-fb0139a410f0&skoid=4a431cd2-eafd-49c7-90be-bbb4d45e696d&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2026-09-17T18%3A11%3A32Z&ske=2026-09-18T18%3A11%3A32Z&sks=b&skv=2026-02-06&sig=V3hRA%2BRH8g6Qy4cBvth6pulzv%2BtTJs4rcFCy4u1AC3s%3D'
'https://sdmntprbrazilsouth.oaiusercontent.com/files/00000000-43f0-820e-9944-1e895d975e38/raw?se=2026-09-18T02%3A07%3A55Z&sp=r&sv=2026-02-06&sr=b&scid=851f6b5e-68d0-56d7-a1b4-0da1852fbd40&skoid=4a431cd2-eafd-49c7-90be-bbb4d45e696d&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2026-09-17T18%3A25%3A42Z&ske=2026-09-18T18%3A25%3A42Z&sks=b&skv=2026-02-06&sig=1vtveVT0Y6%2Bn%2BaJushx6i11AekGdYHBeyFQFBcc8rpM%3D'
'https://sdmntprbrazilsouth.oaiusercontent.com/files/00000000-c19c-820e-9a1e-e6069a28ccb0/raw?se=2026-09-18T02%3A07%3A59Z&sp=r&sv=2026-02-06&sr=b&scid=dbbf1bf1-fa53-5321-b1df-7cd105cc474a&skoid=4a431cd2-eafd-49c7-90be-bbb4d45e696d&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2026-09-17T18%3A09%3A27Z&ske=2026-09-18T18%3A09%3A27Z&sks=b&skv=2026-02-06&sig=HNLttfgsyFDlyBoUyBhkNVl69hanL0HO55mOC9VNNW4%3D'
)
NAMES=(dnd103.part.00 dnd103.part.01 dnd103.part.02 dnd103.part.03 dnd103.part.04)
SOURCE_SHA256='91ae5ea70b0e309b6c381b77fb63497e43fecd6eda2dd54974f49b2be04a06d5'

mkdir -p incoming work out
for i in 0 1 2 3 4; do
  echo "DOWNLOAD_PART $i"
  curl -fL --retry 3 --retry-all-errors --connect-timeout 20 --max-time 180 \
    -o "incoming/${NAMES[$i]}" "${DIRECT_URLS[$i]}"
  size=$(stat -c%s "incoming/${NAMES[$i]}")
  echo "PART_OK $i $size"
  test "$size" -gt 1000000
done

cat incoming/dnd103.part.* > incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip
echo "$SOURCE_SHA256  incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip" | sha256sum -c -
unzip -q incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip -d work

MAIN="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java' | head -n1)"
SRD="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/Srd51BestiaryBatchEngine.java' | head -n1)"
ORCH="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/MasterOrchestrator.java' | head -n1)"
test -f "$MAIN" && test -f "$SRD" && test -f "$ORCH"

python3 ci/dnd-run103/fix2_compile.py "$MAIN" "$SRD" "$ORCH"

export ANDROID_HOME="${ANDROID_HOME:-/usr/local/lib/android/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' >/dev/null

WRAPPER="$(find work -type f -path '*/android/gradlew' | head -n1)"
ROOT="$(dirname "$WRAPPER")"
chmod +x "$ROOT/gradlew"
cd "$ROOT"
./gradlew clean :app:assembleDebug --stacktrace --console=plain --no-daemon

APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' | head -n1)"
test -f "$APK"
cp "$APK" "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX5_DEBUG.apk"
sha256sum "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX5_DEBUG.apk" > "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX5_DEBUG.apk.sha256"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX5_DEBUG.apk" | tee "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
grep -q "package: name='com.braseiro.dnd5e'" "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
unzip -t "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX5_DEBUG.apk" >/dev/null
printf '%s\n' 'RUN103 FIX5 compile-only; source recovered from Google Drive bridge; maps/assets/gameplay unchanged.' > "$GITHUB_WORKSPACE/out/BUILD_FIX.txt"
