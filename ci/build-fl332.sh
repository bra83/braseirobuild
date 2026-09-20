#!/usr/bin/env bash
set -euo pipefail

TAG='FORBIDDEN-LANDS-3.3.2-TTS-IMAGE'
APK_NAME='BRASEIRO_FORBIDDEN_LANDS_3.3.2.apk'
SOURCE_NAME='Braseiro_Forbidden_Lands_3.3.2_TTS_IMAGE_PROMPT_PATCH_ANDROID_STUDIO.zip'
SOURCE_SHA='83b99e4ced2f635b1c93d79c0dbb9ef5093545ea1c67cf8af2dc88331ce0061c'
FIRE_API='https://api.firestorage.ai/dev/file'
SHARE_ID='sJUeoArUdSIJ'
FILE_ID='01a0bd4313427307bc87a8337de35d13'

mkdir -p "$GITHUB_WORKSPACE/fl-incoming" "$GITHUB_WORKSPACE/fl-work" "$GITHUB_WORKSPACE/fl-out"

curl -fsSL "$FIRE_API/shares/$SHARE_ID/files?maxResults=1000" -o /tmp/fl-files.json
python3 - <<'PY'
import json
data=json.load(open('/tmp/fl-files.json', encoding='utf-8'))
wanted='01a0bd4313427307bc87a8337de35d13'
if not any(str(x.get('fileId'))==wanted for x in (data.get('files') or [])):
    raise SystemExit('Fonte Forbidden Lands 3.3.2 não encontrada no share temporário')
PY

curl -fsSL -X POST "$FIRE_API/shares/$SHARE_ID/files/$FILE_ID/download" -o /tmp/fl-download.json
URL="$(python3 - <<'PY'
import json
print(json.load(open('/tmp/fl-download.json', encoding='utf-8')).get('downloadUrl') or '')
PY
)"
test -n "$URL"
curl -fL --retry 4 --retry-delay 2 --connect-timeout 20 --max-time 300 "$URL" -o "$GITHUB_WORKSPACE/fl-incoming/$SOURCE_NAME"
echo "$SOURCE_SHA  $GITHUB_WORKSPACE/fl-incoming/$SOURCE_NAME" | sha256sum -c -
unzip -t "$GITHUB_WORKSPACE/fl-incoming/$SOURCE_NAME" >/dev/null
unzip -q "$GITHUB_WORKSPACE/fl-incoming/$SOURCE_NAME" -d "$GITHUB_WORKSPACE/fl-work"

cd "$GITHUB_WORKSPACE/fl-work"
test -f package.json
test -f android/gradlew

npm config set registry https://registry.npmjs.org/
npm config set fetch-retries 5
npm config set fetch-timeout 120000
npm ci --no-audit --no-fund

python3 - <<'PY'
from pathlib import Path
import json,re
p=Path('package.json')
d=json.loads(p.read_text(encoding='utf-8'))
d['version']='3.3.2'
p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
g=Path('android/app/build.gradle')
s=g.read_text(encoding='utf-8')
s=re.sub(r'versionCode\s+\d+','versionCode 303002',s,count=1)
s=re.sub(r'versionName\s+"[^"]+"','versionName "3.3.2"',s,count=1)
g.write_text(s,encoding='utf-8')
PY

npm run lint
npm run verify:start-freeze
npm run verify:rules
npm run android:sync

test -f android/app/src/main/assets/public/index.html
grep -R -q 'Modelo TTS' android/app/src/main/assets/public/assets
grep -R -q 'Prompt visual da cena' android/app/src/main/assets/public/assets

export ANDROID_HOME="${ANDROID_HOME:-/usr/local/lib/android/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0'

cd android
chmod +x ./gradlew
./gradlew :app:assembleDebug --stacktrace --console=plain --no-daemon 2>&1 | tee "$GITHUB_WORKSPACE/fl-out/GRADLE_BUILD.log"
APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' | head -n1)"
test -f "$APK"
cp "$APK" "$GITHUB_WORKSPACE/fl-out/$APK_NAME"

cd "$GITHUB_WORKSPACE"
unzip -t "fl-out/$APK_NAME" >/dev/null
sha256sum "fl-out/$APK_NAME" > "fl-out/$APK_NAME.sha256"
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "fl-out/$APK_NAME" | tee fl-out/APK_BADGING.txt
grep -q "package: name='com.braseiro.forbiddenlands'" fl-out/APK_BADGING.txt
grep -q "versionCode='303002'" fl-out/APK_BADGING.txt
grep -q "versionName='3.3.2'" fl-out/APK_BADGING.txt
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "fl-out/$APK_NAME" | tee fl-out/APK_SIGNATURE.txt

NOTES='Braseiro Forbidden Lands 3.3.2 — TTS e imagens de cena. TTS Gemini com modelo, voz e direção configuráveis; modelo de imagem configurável; geração manual ou automática; prompt visual editável por cena; imagem retornada ao card. Runtime web recompilado e sincronizado com Android antes do APK.'
gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1 || gh release create "$TAG" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" --title 'Braseiro Forbidden Lands 3.3.2 — TTS + Imagem de Cena' --notes "$NOTES"
gh release upload "$TAG" "fl-out/$APK_NAME" "fl-out/$APK_NAME.sha256" fl-out/APK_BADGING.txt fl-out/APK_SIGNATURE.txt fl-out/GRADLE_BUILD.log --repo "$GITHUB_REPOSITORY" --clobber
gh release view "$TAG" --repo "$GITHUB_REPOSITORY" --json url,tagName,name,assets
