from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_finalize.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf-8")

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.47"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.46"','"version": "4.9.47"',t,count=2)
    lock.write_text(t,encoding="utf-8")

rel="lib/version.ts"
t=read(rel)
t=re.sub(r"APP_VERSION = '4\.9\.46'","APP_VERSION = '4.9.47'",t)
t=re.sub(r"APP_VERSION_CODE = 49046","APP_VERSION_CODE = 49047",t)
write(rel,t)

rel="android/app/build.gradle.kts"
t=read(rel)
t=re.sub(r"versionCode\s*=\s*49046","versionCode = 49047",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.46"','versionName = "4.9.47"',t)
write(rel,t)

rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
t=read(rel).replace("index.html?v=49046&immersive=1","index.html?v=49047&immersive=1")
write(rel,t)

app=read("App.tsx")
dice=read("android/app/src/main/java/com/braseiro/oumanel/OneRingDice3DOverlay.java")
db=read("lib/db.ts")
main=read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt")
checks={
  "version-4947":"4.9.47" in read("lib/version.ts") and "49047" in read("android/app/build.gradle.kts"),
  "db-preserved":"const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
  "tts-preserved":"Braseiro Kokoro PT-BR · neural" in main,
  "journey-native-roll":"rollOneRingFromObservation" in app and "preparedRoll.successResults?.length" in app,
  "journey-event-native-roll":"'Evento de Jornada'" in app and "eventPhysical?.feat_results" in app,
  "explicit-roll-button":'button("ROLAR")' in dice and 'Dados prontos. Toque em ROLAR.' in dice,
  "no-auto-start-on-show":"setOnShowListener" in dice and "setOnShowListener(x->{Window ww=dialog.getWindow();if(ww!=null)ww.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);});" in dice,
  "roll-starts-physics":'rollButton.setOnClickListener' in dice and 'diceView.postOnAnimation(ticker)' in dice,
  "no-data-clear":"indexedDB.deleteDatabase" not in (app+"\n"+db) and "clearApplicationUserData" not in (app+"\n"+main),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed: raise SystemExit("PATCH_4947_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.47_EXPLICIT_JOURNEY_DICE_OK")
