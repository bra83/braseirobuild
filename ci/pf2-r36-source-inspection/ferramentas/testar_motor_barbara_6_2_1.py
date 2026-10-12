#!/usr/bin/env python3
"""Compatibility smoke gate for the current embedded Motor Barbara.

The filename is historical because older automation invokes it directly.  It no
longer asserts the retired Pathfinder 6.2.1 / Barbara 1.0 contract.  The current
release contract is Pathfinder 8.5.0 + Motor Barbara 1.1.2 in external-host
supervisor mode.
"""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app"
VERSION = (ROOT / "version.properties").read_text("utf-8")
ROOT_GRADLE = (ROOT / "build.gradle.kts").read_text("utf-8")
ANDROID_GRADLE = (ROOT / "android/build.gradle.kts").read_text("utf-8")
APP_GRADLE = (APP / "build.gradle.kts").read_text("utf-8")
SUPERVISOR = (APP / "src/main/java/com/braseiro/pathfinder2e/BarbaraSupervisor.java").read_text("utf-8")
MASTER = (APP / "src/main/java/com/braseiro/pathfinder2e/MasterOrchestrator.java").read_text("utf-8")
ADAPTER = APP / "src/main/python/braseiro_barbara_adapter.py"
MANIFEST_PATH = APP / "src/main/python/BARBARA_EMBEDDED.json"
MANIFEST = json.loads(MANIFEST_PATH.read_text("utf-8"))
PYROOT = APP / "src/main/python/barbara"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


manifest_modules = {m["file"]: m["sha256"] for m in MANIFEST.get("modules", [])}
actual_modules = {p.name: sha256(p) for p in PYROOT.glob("*.py")}
modules_exact = manifest_modules == actual_modules and len(actual_modules) == 60
adapter_text = ADAPTER.read_text("utf-8")
manifest_adapter = MANIFEST.get("adapter", {})
adapter_exact = manifest_adapter.get("sha256") == sha256(ADAPTER)

checks = {
    "versão host 8.5.0": "VERSION_NAME=8.5.0" in VERSION and "VERSION_CODE=80500" in VERSION,
    "schema/backup preservados": "DB_SCHEMA=58" in VERSION and "BACKUP_FORMAT=21" in VERSION,
    "AGP preservado 8.9.1": 'com.android.application") version "8.9.1"' in ROOT_GRADLE and 'com.android.application") version "8.9.1"' in ANDROID_GRADLE,
    "Chaquopy 16.1 na raiz": 'com.chaquo.python") version "16.1.0"' in ROOT_GRADLE,
    "Chaquopy 16.1 no android": 'com.chaquo.python") version "16.1.0"' in ANDROID_GRADLE,
    "plugin Chaquopy aplicado": 'id("com.chaquo.python")' in APP_GRADLE,
    "Python 3.11 fixado": 'version = "3.11"' in APP_GRADLE,
    "minSdk 24": "minSdk = 24" in APP_GRADLE,
    "sem pip/runtime download": "pip {" not in APP_GRADLE and "motor-barbara" not in APP_GRADLE,
    "Barbara 1.1.2 / 60 módulos": MANIFEST.get("motor_version") == "1.1.2" and MANIFEST.get("module_count") == 60,
    "hashes Barbara exatos": modules_exact,
    "hash do adapter exato": adapter_exact,
    "modo supervisor externo": MANIFEST.get("integration_mode") == "embedded_source_no_pip_external_host_supervisor",
    "protocolo supervisor 2": MANIFEST.get("contracts", {}).get("supervisor_protocol") == 2,
    "mecânica permanece no Pathfinder": MANIFEST.get("authority", {}).get("mechanics") == "Pathfinder2eRulesEngine",
    "persistência permanece AppDb": MANIFEST.get("authority", {}).get("persistent_state") == "AppDb",
    "sem segunda base": MANIFEST.get("contracts", {}).get("second_database") is False,
    "sem segunda chamada Gemini": MANIFEST.get("contracts", {}).get("duplicate_gemini_call") is False,
    "canais explícitos": MANIFEST.get("contracts", {}).get("input_channels") == ["PLAYER_ACTION", "GM_HELP"],
    "adapter supervisor 1.1.2": "configure_external_supervisor_json" in adapter_text and '"external_host"' in adapter_text and '"Pathfinder2eRulesEngine"' in adapter_text,
    "supervisor Java existe": "class BarbaraSupervisor" in SUPERVISOR and "AndroidPlatform" in SUPERVISOR,
    "fallback legado preservado": "Mestre legado permanece ativo" in SUPERVISOR and "barbara_unavailable" in SUPERVISOR,
    "MasterOrchestrator integra Barbara": "private final BarbaraSupervisor barbara" in MASTER and 'root.put("barbara",bPlan)' in MASTER,
    "política atual 1.1.2 no prompt": "MOTOR BARBARA 1.1.2 É POLÍTICA OBRIGATÓRIA" in MASTER,
    "descoberta world-first obrigatória": "O jogador NÃO conhece automaticamente" in MASTER and "first_arrival" in MASTER and "changed_return" in MASTER,
    "validação pós-geração": "_barbara_validation" in MASTER and "barbara.validate" in MASTER,
    "descoberta só após aceitação": "barbara.markExperienced" in MASTER and MASTER.find("barbara.markExperienced") > MASTER.find("afterAccepted"),
}

for name, value in checks.items():
    print(("OK  " if value else "FALHA ") + name)
if not all(checks.values()):
    raise SystemExit(1)

# Runtime smoke: uses only the embedded source and writes no __pycache__.
env = dict(os.environ)
env["PYTHONDONTWRITEBYTECODE"] = "1"
env["PYTHONPATH"] = str(APP / "src/main/python")
code = r'''
import json
import barbara
import braseiro_barbara_adapter as a
assert barbara.__version__ == "1.1.2"
h = json.loads(a.health_json())
assert h["ok"]
assert h["system_id"] == "pathfinder_2e"
assert h["mechanics_authority"] == "Pathfinder2eRulesEngine"
assert h["second_database"] is False
assert h["duplicate_gemini_call"] is False
assert h["supervisor_mode"] == "external_host"
sc = h["supervisor_contract"]
assert sc["protocol_version"] == 2
assert sc["authority"]["rules"] == "host"
assert sc["authority"]["atlas"] == "host"
assert sc["datasets"]["atlas"]["owner"] == "host"
assert sc["datasets"]["campaign_pois"]["owner"] == "host"
text = "Eu me levanto rapidamente, pego o chapéu e digo para Adrian: você não vem?"
p = json.loads(a.plan_json(text, input_channel="PLAYER_ACTION"))
assert p["input_classification"]["input_type"] in {"game_action", "dialogue"}
assert p["input_classification"]["world_advances"] is True
assert p["turn_plan"]["world_advances"] is True
g = json.loads(a.plan_json(text, input_channel="GM_HELP"))
assert g["input_classification"]["input_type"] == "meta_command"
assert g["input_classification"]["world_advances"] is False
assert g["turn_plan"]["world_advances"] is False
print("BARBARA_1_1_2_EXTERNAL_HOST_SMOKE_OK")
'''
subprocess.run([sys.executable, "-c", code], check=True, env=env, cwd=ROOT)
print(f"RESULTADO MOTOR BARBARA ATUAL: {len(checks)}/{len(checks)} + smoke Python")
