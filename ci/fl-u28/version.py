import json,os,pathlib
root=pathlib.Path(os.environ["FL_ROOT"])/"project"
pkg=root/"package.json"
lock=root/"package-lock.json"
gradle=root/"android/app/build.gradle"
p=json.loads(pkg.read_text(encoding="utf-8"))
l=json.loads(lock.read_text(encoding="utf-8"))
assert p["version"] == l["version"] == l["packages"][""]["version"] == "3.3.24", "Unrecognized U28 base version"
p["version"]=l["version"]=l["packages"][""]["version"]="3.3.25"
pkg.write_text(json.dumps(p,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock.write_text(json.dumps(l,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
s=gradle.read_text(encoding="utf-8")
assert s.count("versionCode 303924")==1 and s.count('versionName "3.3.24"')==1, "Version anchors changed"
gradle.write_text(s.replace("versionCode 303924","versionCode 303925").replace('versionName "3.3.24"','versionName "3.3.25"'),encoding="utf-8")
source=root/"public/fl-build-source.json"
source.write_text(json.dumps({"sourceCommit":os.environ["SOURCE_SHA"],"sourceRepo":"bra83/braseiro-forbidden-lands","ci82Gate":"https://github.com/bra83/braseiro-forbidden-lands/actions/runs/38178048976","androidVersion":"3.3.25","versionCode":303925},indent=2)+"\n",encoding="utf-8")
print("VERSION_AND_SOURCE_PIN_PASS",os.environ["SOURCE_SHA"])
