from pathlib import Path
import json, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch.py <project-root>")

root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

map_file = "components/EriadorMap.tsx"
s = read(map_file)

old_current = """  const currentPoiPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    const locationKey = normalizePoiAssetKey(currentLocation);
    const asset = resolvePoiAsset(immersiveAssets, locationKey) || resolvePoiAssetsForHex(immersiveAssets, discovery.currentHexId)[0] || null;
    if (!asset) return null;
    return {
      asset,
      left: camera.x + hex.centerX * camera.scale,
      top: camera.y + hex.centerY * camera.scale,
    };
  }, [immersive, discovery?.currentHexId, byId, immersiveAssets, currentLocation, camera.x, camera.y, camera.scale]);
"""

new_current = """  const currentPoiPresentation = useMemo(() => {
    if (!immersive || !discovery?.currentHexId) return null;
    const hex = byId.get(discovery.currentHexId);
    if (!hex) return null;
    const locationKey = normalizePoiAssetKey(currentLocation);
    const asset = resolvePoiAsset(immersiveAssets, locationKey) || resolvePoiAssetsForHex(immersiveAssets, discovery.currentHexId)[0] || null;
    if (!asset) return null;
    return { asset };
  }, [immersive, discovery?.currentHexId, byId, immersiveAssets, currentLocation]);
"""

if old_current not in s:
    raise SystemExit("currentPoiPresentation anchor missing")
s = s.replace(old_current, new_current, 1)

old_render = """        {immersive && currentPoiPresentation && (!selectedPoiPresentation || selected?.id !== discovery?.currentHexId) && (
          <img
            src={currentPoiPresentation.asset}
            alt="Ilustração do local atual"
            draggable={false}
            className="pointer-events-none absolute z-[12] max-h-[30vh] w-[min(48vw,250px)] select-none object-contain opacity-[0.82]"
            style={{
              left: currentPoiPresentation.left,
              top: currentPoiPresentation.top,
              transform: 'translate(-50%, -72%)',
              mixBlendMode: 'multiply',
              filter: 'sepia(.18) saturate(.72) contrast(1.02)',
            }}
          />
        )}
"""

new_render = """        {immersive && currentPoiPresentation && (!selectedPoiPresentation || selected?.id !== discovery?.currentHexId) && (
          <img
            src={currentPoiPresentation.asset}
            alt="Ilustração do local atual"
            draggable={false}
            style={{
              pointerEvents: 'none',
              position: 'absolute',
              zIndex: 12,
              right: '-5%',
              top: '15%',
              width: 'min(76vw, 340px)',
              height: '38vh',
              objectFit: 'contain',
              objectPosition: 'right center',
              opacity: .48,
              mixBlendMode: 'multiply',
              filter: 'sepia(.14) saturate(.76) contrast(.96)',
              WebkitMaskImage: 'radial-gradient(ellipse at center, #000 44%, rgba(0,0,0,.9) 57%, transparent 80%)',
              maskImage: 'radial-gradient(ellipse at center, #000 44%, rgba(0,0,0,.9) 57%, transparent 80%)',
            }}
          />
        )}
"""

if old_render not in s:
    raise SystemExit("current POI illustration render anchor missing")
s = s.replace(old_render, new_render, 1)
write(map_file, s)

pkg = root / "package.json"
data = json.loads(pkg.read_text(encoding="utf-8"))
data["version"] = "4.9.7"
pkg.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lock = root / "package-lock.json"
if lock.exists():
    t = lock.read_text(encoding="utf-8")
    t = t.replace('"version": "4.9.5"', '"version": "4.9.7"')
    lock.write_text(t, encoding="utf-8")

for rel in ["lib/version.ts", "android/app/build.gradle.kts", "android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"]:
    p = root / rel
    t = p.read_text(encoding="utf-8")
    t = t.replace("4.9.5", "4.9.7").replace("49005", "49007")
    p.write_text(t, encoding="utf-8")

(root / "CHANGELOG_OUA_4.9.7_VISUAL_POI_OVERLAY.md").write_text(
"""# O Um Anel 4.9.7 — correção visual de ilustração contextual

- parte da 4.9.5 atual, sem recriar o projeto;
- preserva o Atlas funcional, pan/zoom, seleção de hex, Companhia e rotas;
- preserva a UI 415×915 e Motor Barbara 1.1.3;
- mantém a associação local/hex -> asset já existente;
- move a ilustração do local atual para um overlay fixo no canto direito;
- usa opacidade 0.48, máscara radial e tratamento sépia discreto;
- a ilustração não recebe eventos de toque e não bloqueia mapa, cards ou navegação;
- a ilustração do POI selecionado continua vinculada ao hex selecionado.
""",
encoding="utf-8")

final = read(map_file)
assert "right: '-5%'" in final
assert "opacity: .48" in final
assert "maskImage: 'radial-gradient" in final
assert "currentPoiPresentation.asset" in final
assert "selectedPoiPresentation.asset" in final
assert "PartyCluster" in final
print("PATCH_OUA_4.9.7_VISUAL_OK")
