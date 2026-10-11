#!/usr/bin/env bash
# Importa ZIP integral U19 numa branch isolada: SEM sobrescrever main.
set -Eeuo pipefail
SOURCE_NAME=BRASEIRO_FORBIDDEN_LANDS_UNIFIED_U19_FULL_SOURCE.zip
EXPECTED_SHA=b2f5a32f76c98f29e977cdad4689b6e50efa72c5a3164d1668f71700f18f8e13
BRANCH=ci/u19-validation
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
ZIP="$(realpath "${1:-$SOURCE_NAME}")"
test -f "$ZIP" || { echo "ERRO: coloque $SOURCE_NAME na raiz do Codespace."; exit 1; }
test "$(basename "$ZIP")" = "$SOURCE_NAME" || { echo 'Nome ZIP inesperado'; exit 1; }
case "$(git remote get-url origin)" in
  *bra83/braseiro-forbidden-lands*) ;;
  *) echo 'ERRO: repositório incorreto'; exit 1 ;;
esac
if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
 echo "ERRO: branch $BRANCH já existe."; exit 1
fi
if git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
 echo 'ERRO: branch remota já existe.'; exit 1
fi
while IFS= read -r row; do
 [ -z "$row" ] && continue
 if [ "$row" != "?? $SOURCE_NAME" ]; then
   echo "ERRO: há outra alteração local: $row"; exit 1
 fi
done < <(git status --porcelain --untracked-files=all)
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp "$ZIP" "$TMP/source.zip"
echo "$EXPECTED_SHA  $TMP/source.zip" | sha256sum -c -
python3 - "$TMP/source.zip" "$TMP/src" <<'PY'
import pathlib, stat, sys, zipfile
source, target = sys.argv[1:]
with zipfile.ZipFile(source) as z:
    assert z.testzip() is None
    assert len(z.infolist()) >= 1485
    for m in z.infolist():
        p = pathlib.PurePosixPath(m.filename)
        assert not p.is_absolute() and '..' not in p.parts and '.git' not in p.parts
        assert not stat.S_ISLNK(m.external_attr >> 16)
    z.extractall(target)
for file in ['CHANGELOG_UNIFIED_U19.md','package-lock.json',
             'scripts/verify-u19-official-canonical-combat.mjs',
             'e2e/run_official_suite.py']:
    assert (pathlib.Path(target)/file).is_file(), file
print('ZIP integral U19: SHA, CRC e arquivos obrigatórios verificados')
PY
mkdir -p "$TMP/src/.github/workflows" "$TMP/src/scripts"
cp .github/workflows/forbidden-lands-public-u19.yml "$TMP/src/.github/workflows/"
cp scripts/import-u19-unified.sh "$TMP/src/scripts/"
git fetch origin main
git switch main
git pull --ff-only origin main
git switch -c "$BRANCH"
rm -f "$ZIP"
rsync -a --delete --exclude='.git/' "$TMP/src/" "$ROOT/"
git add -A
git -c user.name='Braseiro CI' -c user.email='braseiro-ci@users.noreply.github.com' commit -m 'ci: import verified U19 source for Chromium/IndexedDB gates'
git push -u origin "$BRANCH"
echo "Push concluído: https://github.com/bra83/braseiro-forbidden-lands/actions"
