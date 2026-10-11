import hashlib,json,os,pathlib,zipfile
root=pathlib.Path(os.environ["FL_ROOT"])
project=root/"project"
out=root/"release"
apk=out/"BRASEIRO_FORBIDDEN_LANDS_3.3.25_U28_INTEGRATED.apk"
with zipfile.ZipFile(apk) as z:
    proof=json.loads(z.read("assets/public/fl-build-source.json"))
    assert proof["sourceCommit"]==os.environ["SOURCE_SHA"],"APK has wrong U28 source revision"
    assert proof["versionCode"]==303925 and proof["androidVersion"]=="3.3.25","Wrong version marker"
    marker=json.loads(z.read("assets/public/braseiro-release-marker.json"))
    assert marker["version"]=="3.3.25" and marker["generatedBy"]=="npm run android:sync","Stale web build"
    count=0
    for p in (project/"dist").rglob("*"):
        if not p.is_file():continue
        name="assets/public/"+p.relative_to(project/"dist").as_posix()
        assert z.read(name)==p.read_bytes(),"APK asset differs from built source: "+name
        count+=1
    assert count>=2,"No web assets validated"
sha=hashlib.sha256(apk.read_bytes()).hexdigest()
(out/(apk.name+".sha256")).write_text(sha+"  "+apk.name+"\n",encoding="ascii")
report={"status":"PASS","sourceCommit":os.environ["SOURCE_SHA"],"sourceGateRun":38178048976,"certifiedConsequences":82,"versionName":"3.3.25","versionCode":303925,"appId":"com.braseiro.forbiddenlands","signerSha1":os.environ["EXPECTED_SIGNER"],"apkSha256":sha,"apkBytes":apk.stat().st_size,"webAssetsCompared":count,"androidPhysicalDevice":"NOT_CERTIFIED","externalTts":"NOT_CERTIFIED"}
(out/"BUILD_PROOF_U28.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("U28_APK_BINARY_SOURCE_PROOF_PASS",json.dumps(report))
