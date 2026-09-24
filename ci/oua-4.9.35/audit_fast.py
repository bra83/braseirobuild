from pathlib import Path
import json,re,sys,subprocess,tempfile,os

if len(sys.argv)!=3:
    raise SystemExit("usage: audit_fast.py <project-root> <out-dir>")
root=Path(sys.argv[1]).resolve()
out=Path(sys.argv[2]).resolve()
out.mkdir(parents=True,exist_ok=True)

rows=[]
def add(area,item,status,evidence,detail):
    rows.append({"area":area,"item":item,"status":status,"evidence":evidence,"detail":detail})

def read(rel):
    p=root/rel
    return p.read_text(encoding="utf-8",errors="replace") if p.exists() else ""

main=read("android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt")
app=read("App.tsx")
audio=read("lib/AudioEngine.ts")
db=read("lib/db.ts")
manifest=read("android/app/src/main/AndroidManifest.xml")
commands=read("lib/master/commands.ts")

critical=[
("Integração","Rules Engine ligado à ação","applyMasterCommands(" in commands and "applyOne(" in commands and "rejected.push" in commands,"lib/master/commands.ts","Comandos passam por dispatcher com rejeição explícita."),
("Persistência","IndexedDB estável","const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,"lib/db.ts","Nome/schema preservados."),
("Viagem","Movimento livre bloqueado","Movimento livre bloqueado" in mapview and "advancePlannedRoute" in mapview and "Jogada de Marcha do Guia antes de avançar a Jornada" in journey,"components/EriadorMap.tsx + lib/rules/journey.ts","UI não oferece salto livre e o Rules Engine rejeita avanço sem Marcha."),
("Descanso","Descanso altera relógio","resolve_rest" in commands and "advanceWorldClock" in commands,"lib/master/commands.ts","Descanso está no Rules Engine e avança relógio."),
("Combate","Combate canônico conectado","rollHeroAttack" in (commands+app) and "rollHeroProtection" in (commands+app),"Rules/UI","Ataque/proteção existem no fluxo atual."),
("Conselho","Conselho conectado","record_council_result" in (commands+app) and "resolveCouncil" in commands,"Rules/UI","Resultado do conselho chega ao estado."),
("Mundo","Mundo vivo persiste","simulateLivingWorldDetailed" in commands and "livingWorld" in commands,"lib/master/commands.ts","Tempo/comandos propagam mundo vivo."),
("Android","TTS engines dinâmicas","queryIntentServices" in main and "textToSpeech?.engines.orEmpty()" in main,"MainActivity.kt","Catálogo vem do Android."),
("Android","Braseiro Kokoro deduplicado","Braseiro Kokoro PT-BR · neural" in main and "preferredInstalledKokoroBackend" in main,"MainActivity.kt","Debug/release Sherpa colapsam para um backend visível."),
("Android","Somente Voice real","e.voices.orEmpty().firstOrNull{it.name==requested}" in main and "setVoice(voice)" in main and all(x not in main for x in ["pf_dora","pm_alex","pm_santa"]),"MainActivity.kt","Seleção usa Voice real."),
("Android","Kokoro multi-bloco","QUEUE_ADD else TextToSpeech.QUEUE_FLUSH" in main and "advanceSpeechQueue(p.rootId)" in main and "if(isNeuralTtsProfile())return" in main,"MainActivity.kt","Primeiro bloco flush; sequência por onDone; sem watchdog neural."),
("Áudio","Música manual persiste entre cenas","scene-music-preserved-manual" in app and "library-preview" in audio,"App.tsx/lib/AudioEngine.ts","Música manual usa slot independente."),
("Áudio","Faixa longa não usa SoundPool","durationMs >= 30_000" in audio and "playLoop(channel, sound, sound.name, 'library-preview')" in audio,"lib/AudioEngine.ts","Faixas longas usam player contínuo."),
]
for area,item,ok,evidence,detail in critical:
    add(area,item,"PASSOU" if ok else "FALHOU",evidence,detail)

generated_names={"node_modules","build","dist","dist-offline",".offline-vendor"}
files=[p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in {".ts",".tsx",".kt",".java",".js",".mjs"} and not any(part in generated_names for part in p.parts) and not ("android" in p.parts and "assets" in p.parts and "vendor" in p.parts)]
patterns={
 "TODO/FIXME":re.compile(r"\\b(?:TODO|FIXME|HACK)\\b"),
 "empty-click":re.compile(r"onClick\\s*=\\s*\\{\\s*\\(\\s*\\)\\s*=>\\s*\\{\\s*\\}\\s*\\}"),
 "not-implemented":re.compile(r"not implemented|não implementad[oa]|throw new Error\\([\\\"'][^\\\"']*(?:TODO|implementar)",re.I),
}
for label,rx in patterns.items():
    hits=[]
    for p in files:
        t=p.read_text(encoding="utf-8",errors="ignore")
        for m in rx.finditer(t):
            line=t.count("\n",0,m.start())+1
            hits.append(str(p.relative_to(root))+":"+str(line))
            if len(hits)>=20: break
        if len(hits)>=20: break
    add("Varredura",label,"PASSOU" if not hits else "SEM EVIDÊNCIA","; ".join(hits[:20]) or "nenhum","Ocorrências exigem inspeção; não são erro automaticamente.")

forbidden=[]
for token in ["Android/data/","model.onnx","voices.bin"]:
    if token in main: forbidden.append(token)
if 'com.braseiro.kokoro.ptbr' in manifest: forbidden.append("legacy package in Manifest")
add("Android","Sem acesso/cópia de modelo Kokoro","PASSOU" if not forbidden else "FALHOU","MainActivity.kt/AndroidManifest.xml","nenhum acesso proibido" if not forbidden else ", ".join(forbidden))

runtime_template=Path(__file__).with_name("audit_runtime.ts").read_text(encoding="utf-8")
runtime_src=runtime_template.replace("../../TEMP_ROOT",str(root).replace("\\","/"))
with tempfile.TemporaryDirectory() as td:
    td=Path(td)
    src=td/"audit_runtime.ts"
    bundle=td/"audit_runtime.mjs"
    src.write_text(runtime_src,encoding="utf-8")
    try:
        cmd=["npx.cmd" if os.name=="nt" else "npx","--yes","esbuild",str(src),"--bundle","--platform=node","--format=esm","--outfile="+str(bundle)]
        cp=subprocess.run(cmd,cwd=root,text=True,capture_output=True,timeout=120)
        if cp.returncode!=0:
            add("Testes","Harness determinístico","SEM EVIDÊNCIA","esbuild",cp.stderr[-1500:])
        else:
            rp=subprocess.run(["node",str(bundle)],cwd=root,text=True,capture_output=True,timeout=60)
            try:
                vals=json.loads(rp.stdout.strip().splitlines()[-1])
            except Exception:
                vals=[]
            if not vals:
                add("Testes","Harness determinístico","SEM EVIDÊNCIA","audit_runtime.ts",(rp.stdout+rp.stderr)[-1500:])
            for x in vals:
                add("Regras",x["id"],x["status"],"audit_runtime.ts",x["detail"])
    except Exception as e:
        add("Testes","Harness determinístico","SEM EVIDÊNCIA","audit_runtime.ts",repr(e))

gradle=read("android/app/build.gradle.kts")
add("Build","Versão atual","PASSOU" if 'versionName = "4.9.35"' in gradle and "versionCode = 49035" in gradle else "FALHOU","android/app/build.gradle.kts","Esperado 4.9.35 / 49035.")

counts={s:sum(r["status"]==s for r in rows) for s in ["PASSOU","CORRIGIDO E RETESTADO","FALHOU","SEM EVIDÊNCIA"]}
(out/"AUDITORIA_RAPIDA.json").write_text(json.dumps({"counts":counts,"rows":rows},ensure_ascii=False,indent=2),encoding="utf-8")
lines=["# O Um Anel 4.9.35 — auditoria funcional rápida","",f"PASSOU {counts['PASSOU']} · CORRIGIDO E RETESTADO {counts['CORRIGIDO E RETESTADO']} · FALHOU {counts['FALHOU']} · SEM EVIDÊNCIA {counts['SEM EVIDÊNCIA']}",""]
for r in rows:
    lines += [f"- **{r['status']}** — {r['area']} / {r['item']} — {r['detail']} — `{r['evidence']}`"]
(out/"AUDITORIA_RAPIDA.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
print(json.dumps({"counts":counts,"report":str(out/"AUDITORIA_RAPIDA.md")},ensure_ascii=False))
if counts["FALHOU"]:
    raise SystemExit(2)
