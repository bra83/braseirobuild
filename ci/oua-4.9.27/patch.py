from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4927.py <root>")

root=Path(sys.argv[1]).resolve()

def read(p):
    return (root/p).read_text(encoding="utf-8")

def write(p,s):
    (root/p).write_text(s,encoding="utf-8")

# ---------------------------------------------------------------------------
# UI HOTFIX
# 4.9.24 replaced the whole handleMapDiscoveryChanged -> handlePartyMoved region.
# That region also contained handlePartyRested (inserted by 4.9.22), so the
# definition was deleted while GameView still rendered
# onPartyRested={handlePartyRested}. Vite/esbuild bundled the unresolved symbol
# and the WebView crashed at runtime with:
#   ReferenceError: handlePartyRested is not defined
# Restore the handler without touching persisted data or rest mechanics.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

definition="  const handlePartyRested = async (kind: 'short' | 'long') => {"
if definition not in app:
    anchor="  const handlePartyMoved = async (move: PartyMapMove) => {"
    if anchor not in app:
        raise SystemExit("handlePartyMoved anchor missing")

    handler="""  const handlePartyRested = async (kind: 'short' | 'long') => {
    const base = campaignRef.current;
    if (!base) return;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'resolve_rest',
      kind,
      reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
      source: 'O Um Anel 2e — Descanso',
    }]);
    if (mechanical.rejected.length) {
      throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
    }
    const committed = await saveMasterTransaction(
      mechanical.campaign,
      mechanical.characters,
      mechanical.audits,
      kind === 'short' ? 'descanso curto' : 'descanso prolongado',
    );
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
  };

"""
    app=app.replace(anchor,handler+anchor,1)

# The callback must remain wired in every GameView branch.
wire_count=app.count("onPartyRested={handlePartyRested}")
if wire_count < 2:
    raise SystemExit(f"rest callback wiring incomplete: {wire_count}")

# Guard against the exact runtime regression.
if app.count(definition) != 1:
    raise SystemExit(f"handlePartyRested definition count invalid: {app.count(definition)}")

write(app_rel,app)

# ---------------------------------------------------------------------------
# VERSION 4.9.27
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.27"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.26"','"version": "4.9.27"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
version=read(version_rel)
version=re.sub(r"APP_VERSION = '4\.9\.26'","APP_VERSION = '4.9.27'",version)
version=re.sub(r"APP_VERSION_CODE = 49026","APP_VERSION_CODE = 49027",version)
write(version_rel,version)

gradle_rel="android/app/build.gradle.kts"
gradle=read(gradle_rel)
gradle=re.sub(r"versionCode\s*=\s*49026","versionCode = 49027",gradle)
gradle=re.sub(r'versionName\s*=\s*"4\.9\.26"','versionName = "4.9.27"',gradle)
write(gradle_rel,gradle)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)
main=main.replace("index.html?v=49026&immersive=1","index.html?v=49027&immersive=1")
write(main_rel,main)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
app=read(app_rel)
commands=read("lib/master/commands.ts")
checks={
    "rest-handler-defined": app.count(definition)==1,
    "rest-handler-wired-both-branches": app.count("onPartyRested={handlePartyRested}")>=2,
    "rest-handler-applies-command": "type: 'resolve_rest'" in app and "applyMasterCommands(base, characters, allNpcs" in app,
    "rest-handler-persists": "saveMasterTransaction(" in app and "campaignRef.current = committed" in app,
    "rest-mechanics-still-present": "case 'resolve_rest':" in commands,
    "long-rest-clock-preserved": "const restMinutes = command.kind === 'short' ? 60 : 480;" in commands,
    "version": "4.9.27" in read(version_rel) and "49027" in read(gradle_rel),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4927_FAILED: "+", ".join(failed))

print("PATCH_OUA_4.9.27_UI_REST_HANDLER_OK")
