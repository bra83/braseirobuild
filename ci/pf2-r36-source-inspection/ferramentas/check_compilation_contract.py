#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
props = dict(line.strip().split("=", 1) for line in (ROOT / "version.properties").read_text("utf-8").splitlines() if "=" in line and not line.lstrip().startswith("#"))
main_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java"
build_path = ROOT / "android/app/build.gradle.kts"
guard_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/DatabaseMigrationGuard.java"
startup_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/AdventureStartupCoordinator.java"
backup_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/BackupContractValidator.java"
map_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/MapPackageManager.java"
dice_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/Dice3DOverlay.java"
physics_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/PhysicalDiceEngine.java"
candle_path = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e/CandleLightOverlayView.java"
errors = []

for p in (main_path, build_path, guard_path, startup_path, backup_path, map_path, dice_path, physics_path, candle_path):
    if not p.is_file(): errors.append(f"arquivo ausente: {p.relative_to(ROOT)}")

if not errors:
    main = main_path.read_text("utf-8")
    build = build_path.read_text("utf-8")
    guard = guard_path.read_text("utf-8")
    startup = startup_path.read_text("utf-8")
    backup = backup_path.read_text("utf-8")
    map_source = map_path.read_text("utf-8")
    dice = dice_path.read_text("utf-8")
    physics = physics_path.read_text("utf-8")
    candle = candle_path.read_text("utf-8")
    compact = re.sub(r"\s+", "", main)

    if props.get("VERSION_NAME") != "8.5.0" or props.get("VERSION_CODE") != "80500": errors.append("version.properties não está em 8.5.0/80500")
    if props.get("DB_SCHEMA") != "58" or props.get("BACKUP_FORMAT") != "21": errors.append("schema/backup não estão em 58/21")
    if 'applicationId = "com.braseiro.pathfinder2e"' not in build: errors.append("applicationId Pathfinder ausente")
    if 'namespace = "com.braseiro.pathfinder2e"' not in build: errors.append("namespace Pathfinder ausente")
    if "APP_VERSION=BuildConfig.VERSION_NAME" not in compact: errors.append("APP_VERSION não deriva de BuildConfig")

    for method in ("onCreate", "beginAdventure", "showCharacterSheetDialog", "applyMasterConsequences"):
        if not re.search(rf"\b{method}\s*\(", main): errors.append(f"método obrigatório ausente: {method}()")

    formatter_defs = re.findall(r"private\s+static\s+String\s+formatLocationTypePt\s*\(\s*String\s+raw\s*\)", main)
    formatter_calls = len(re.findall(r"\bformatLocationTypePt\s*\(", main)) - len(formatter_defs)
    if len(formatter_defs) != 1: errors.append("formatter Atlas formatLocationTypePt deve existir exatamente uma vez")
    if formatter_calls < 1: errors.append("formatter Atlas formatLocationTypePt não está sendo usado")
    if "locationTypePt(" in main: errors.append("símbolo legado locationTypePt ainda está presente")

    for banned in ("DndRulesEngine", "MystaraRulesEngine", "MystaraManualAuthority", "mystaraRules", "lastPlayerAction", "requestMasterTurn("):
        if banned in main: errors.append(f"símbolo legado/compile-risk presente em MainActivity: {banned}")

    if 'new String[]{"D&D","Mystara"}' in compact or 'new String[]{"D&D","Mystara"}' in main.replace(" ", ""):
        errors.append("seletor D&D/Mystara voltou ao Atlas")
    if 'launchAdventure(reset,"")' not in compact: errors.append("início narrativo sem Atlas não está garantido")
    if "atlas_optional" not in startup: errors.append("preflight ainda não declara Atlas opcional")
    if "braseiro_pathfinder2e.db" not in guard or "pre_v58" not in guard or "pf2_v58" not in guard: errors.append("DatabaseMigrationGuard não está no DB PF2e/schema 58")

    if "MAX_SUPPORTED_VERSION=21" not in backup: errors.append("BackupContractValidator não aceita o formato 21 exportado pelo AppDb")
    for stale in ("429f", "450.333f", "39f", "45.033320996790806f"):
        if stale in map_source: errors.append("validação Atlas ainda contém geometria V2 congelada: "+stale)
    if "getOriginX()" not in map_source or "getAlternateColumnOffsetY()" not in map_source: errors.append("Atlas V6 não é validado pelo próprio manifesto")
    if "android.webkit.WebView" in dice or "dice3d_core.js" in dice: errors.append("rolagem ainda depende do WebView/JS antigo")
    for token in ("PhysicalDiceEngine.create", "PhysicalDiceEngine.FIXED_DT", "Dice3DView", "spec.resolve(rolls)"):
        if token not in dice: errors.append("integração nativa de dados ausente: "+token)
    if "public static final double FIXED_DT=1.0/120.0" not in physics.replace(" ","") and "FIXED_DT=1.0/120" not in physics.replace(" ",""): errors.append("física dos dados não preserva passo fixo de 120 Hz")
    if "CandleLightOverlayView" not in main or "shellHost.addView(candleOverlay" not in main: errors.append("luz de vela global não está montada no shell")

if errors:
    print("FALHA NO CONTRATO DE COMPILAÇÃO PATHFINDER 2E")
    for e in errors: print("-", e)
    sys.exit(1)
print(f"OK — contrato Pathfinder 2e {props.get('VERSION_NAME')} ({props.get('VERSION_CODE')}) confirmado.")
