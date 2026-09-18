#!/usr/bin/env python3
from __future__ import annotations

import json
import hashlib
import re
import sys
import unicodedata
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: fix_device_p7.py <android-project-root>")

ROOT = Path(sys.argv[1]).resolve()
VTT = ROOT / "app/src/main/assets/vtt"
R54 = VTT / "image-library/local/r54"
SHELL = VTT / "r46-shell.html"
BINDINGS = R54 / "bindings.json"
STARTS = R54 / "r54-start-locations.json"
CONTROLLER = VTT / "js/core/controller.mjs"
UIBRIDGE = VTT / "js/core/uiBridge.bundle.js"
RUNTIME = VTT / "r46/final-runtime.js"
P7_RUNTIME = VTT / "r54-device-p7.js"
ITEM_AUDIT = R54 / "ITEM_VISUAL_BINDING_AUDIT.json"
MAP_AUDIT = R54 / "LOCATION_MAP_AUDIT.json"
P7_INDEX = R54 / "p7-semantic-index.json"

for path in (SHELL, BINDINGS, STARTS, CONTROLLER, UIBRIDGE, RUNTIME):
    if not path.is_file():
        raise SystemExit(f"missing P7 input: {path}")


def norm(value: object) -> str:
    text = unicodedata.normalize("NFD", str(value or ""))
    text = "".join(ch for ch in text if unicodedata.category(ch) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", " ", text).strip()


def rows(group: object) -> list[dict]:
    if isinstance(group, list):
        return [dict(x) for x in group if isinstance(x, dict)]
    if isinstance(group, dict):
        result = []
        for key, value in group.items():
            if isinstance(value, dict):
                row = dict(value)
                row.setdefault("id", key)
                result.append(row)
        return result
    return []


bindings = json.loads(BINDINGS.read_text("utf-8"))
starts = json.loads(STARTS.read_text("utf-8"))
bad_asset = re.compile(r"(?:^|[/_. -])(inventory|placeholder|fallback|generic|default|robot)(?:[/_. -]|$)", re.I)

# P7 deliberately trusts only explicit catalogue bindings. Fuzzy matching is not
# semantic validation and was the source of the backpack/pistol false positive.
item_rows: list[dict] = []
exact: dict[str, dict] = {}
excluded = {"maps", "map", "portraits", "portrait", "locations", "location"}
for group_name, group in bindings.items():
    if norm(group_name) in excluded:
        continue
    for row in rows(group):
        item_id = str(row.get("id") or row.get("slug") or "").strip()
        name = str(row.get("name") or row.get("title") or item_id).strip()
        file = str(row.get("file") or "").strip()
        path = R54 / file if file else None
        exists = bool(path and path.is_file())
        generic = bool(file and bad_asset.search(file))
        aliases = [item_id, row.get("slug"), name, row.get("title"), *(row.get("aliases") or [])]
        aliases = [str(x).strip() for x in aliases if x]
        stem = norm(Path(file).stem)
        identity_tokens = set(norm(" ".join(aliases)).split()) - {"arma", "weapon", "equipamento", "equipment"}
        stem_tokens = set(stem.split())
        semantic = bool(exists and not generic and (norm(item_id) == stem or len(identity_tokens & stem_tokens) >= min(2, max(1, len(stem_tokens)))))
        # An explicit binding remains usable even when the conservative filename
        # heuristic cannot prove semantics; it is reported as unverified, never PASS.
        status = "MATCH" if semantic else ("MISSING" if not exists else "REVIEW_REQUIRED")
        audit = {
            "id": item_id,
            "name": name,
            "category": str(group_name),
            "current_asset": f"image-library/local/r54/{file}" if file else "",
            "asset_exists": exists,
            "is_generic_fallback": generic,
            "semantic_match": semantic,
            "status": status,
            "notes": "binding explícito; sem fallback aproximado" if semantic else "não promovido a completo sem prova semântica",
        }
        item_rows.append(audit)
        if exists and not generic:
            entry = {"id": item_id, "name": name, "category": str(group_name), "path": audit["current_asset"]}
            for alias in aliases:
                if norm(alias):
                    exact[norm(alias)] = entry

ITEM_AUDIT.write_text(json.dumps(item_rows, ensure_ascii=False, indent=2) + "\n", "utf-8")

# Local maps are a separate capability. A thumbnail, planet illustration or scene
# image is never promoted to local_map. Only explicit map bindings with a real file
# and a location alias are accepted.
map_exact: dict[str, dict] = {}
for group_name in ("maps", "map", "locations", "location"):
    for row in rows(bindings.get(group_name, {})):
        explicit_local = row.get("local_map") or row.get("localMap")
        file = str(explicit_local or row.get("file") or "").strip()
        kind = norm(row.get("mapKind") or row.get("kind") or row.get("type") or "")
        if not file or (not explicit_local and not any(word in kind for word in ("local", "interior", "tactical", "tatica"))):
            continue
        path = R54 / file
        if not path.is_file() or bad_asset.search(file):
            continue
        entry = {"path": f"image-library/local/r54/{file}", "asset_exists": True}
        aliases = [row.get("id"), row.get("slug"), row.get("name"), row.get("title"), *(row.get("aliases") or [])]
        for alias in aliases:
            if norm(alias):
                map_exact[norm(alias)] = entry

generated_maps = R54 / "generated-local-maps"
generated_maps.mkdir(parents=True, exist_ok=True)

def generated_local_map(loc: dict, name: str) -> dict:
    slug = re.sub(r"[^a-z0-9]+", "-", norm(loc.get("id") or loc.get("key") or name)).strip("-") or "local"
    digest = hashlib.sha256((name + "|" + str(loc.get("region") or "") + "|" + str(loc.get("type") or "")).encode("utf-8")).digest()
    points = []
    for index in range(9):
        x = 90 + (digest[index] * 760 // 255)
        y = 80 + (digest[index + 9] * 360 // 255)
        points.append((x, y))
    links = "".join(f'<path d="M {points[i][0]} {points[i][1]} L {points[i+1][0]} {points[i+1][1]}"/>' for i in range(8))
    nodes = "".join(f'<circle cx="{x}" cy="{y}" r="{12 + digest[(i+18)%32] % 12}"/><text x="{x+18}" y="{y+5}">{i+1:02d}</text>' for i,(x,y) in enumerate(points))
    title = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
<rect width="960" height="540" fill="#071017"/><g stroke="#173443" stroke-width="1" opacity=".55">{''.join(f'<path d="M {x} 0V540"/>' for x in range(0,961,48))}{''.join(f'<path d="M 0 {y}H960"/>' for y in range(0,541,48))}</g>
<g fill="none" stroke="#d49d42" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">{links}</g><g fill="#0d2632" stroke="#6bd4ef" stroke-width="3">{nodes}</g>
<text x="42" y="48" fill="#f1e5cc" font-family="sans-serif" font-size="25" font-weight="700">MAPA LOCAL · {title}</text><text x="42" y="512" fill="#91a5ae" font-family="sans-serif" font-size="16">Mapa jogável procedural · pontos numerados para cenas, viagem e encontros</text></svg>'''
    target = generated_maps / f"{slug}.svg"
    target.write_text(svg, "utf-8")
    return {"path": f"image-library/local/r54/generated-local-maps/{target.name}", "asset_exists": True, "origin": "generated_playable_map"}

location_rows = []
for loc in starts if isinstance(starts, list) else []:
    name = str(loc.get("name") or loc.get("title") or loc.get("id") or "").strip()
    keys = [norm(name), norm(loc.get("id")), norm(loc.get("slug")), norm(loc.get("key"))]
    mapped = next((map_exact[k] for k in keys if k and k in map_exact), None)
    if not mapped:
        mapped = generated_local_map(loc, name)
    for key in keys:
        if key:
            map_exact[key] = mapped
    thumbnail = ""
    # P6's startLocations are thumbnails only; keep the distinction explicit.
    p6_index = R54 / "p6-visual-index.json"
    if p6_index.is_file():
        p6 = json.loads(p6_index.read_text("utf-8"))
        rec = next((x for x in p6.get("startLocations", []) if norm(x.get("name")) == norm(name)), None)
        thumbnail = str((rec or {}).get("path") or "")
    location_rows.append({
        "location_id": str(loc.get("id") or loc.get("key") or loc.get("slug") or ""),
        "location_name": name,
        "thumbnail_asset": thumbnail,
        "local_map_asset": (mapped or {}).get("path", ""),
        "local_map_exists": bool(mapped),
        "local_map_loads": bool(mapped),
        "show_map_section": bool(mapped),
        "local_map_origin": mapped.get("origin", "source_asset") if mapped else "missing",
    })

MAP_AUDIT.write_text(json.dumps(location_rows, ensure_ascii=False, indent=2) + "\n", "utf-8")
P7_INDEX.write_text(json.dumps({"schemaVersion": 1, "itemsExactByName": exact, "localMapsExactByLocation": map_exact}, ensure_ascii=False, indent=2) + "\n", "utf-8")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"method anchor not found: {signature}")
    brace = start + len(signature) - 1
    depth, quote, escape = 0, None, False
    for i in range(brace, len(source)):
        ch = source[i]
        if quote:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote:
                quote = None
        elif ch in ('"', "'", "`"):
            quote = ch
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return source[:start] + replacement + source[i + 1:]
    raise SystemExit("unbalanced method")


start_method = r'''    startLivingAdventure({ rng = Math.random } = {}) {
        // R54_DEVICE_P7_SEMANTIC_SCENE: scene-local facts only; no universal NPC.
        if (!this.state.player?.character)
            throw new Error('Crie, gere ou importe um personagem antes de iniciar a aventura.');
        const opening = sandbox_mjs_1.sandboxOpening(this.state.world, { locationId: this.state.scene?.locationId ?? this.state.world?.sandbox?.startLocationId ?? null, limit: 6 });
        if (!opening?.ready || !opening.location)
            throw new Error('Crie o mundo vivo e escolha o ponto inicial antes de iniciar a aventura.');
        let session = this.state.session;
        if (session?.status !== 'ativa') session = this.startSession({ playerCount: 1, rng }).session;
        const locationId = String(opening.location.id ?? '');
        const atLocation = item => {
            const refs = [item?.locationId, item?.currentLocationId, item?.sceneLocationId, item?.location?.id].filter(Boolean).map(String);
            return refs.length > 0 && refs.includes(locationId);
        };
        const visible = item => item?.visible === true || item?.present === true || item?.status === 'present' || item?.knownToPlayer === true;
        const localNpcs = (opening.npcs ?? []).filter(item => atLocation(item) && visible(item));
        const localFactions = (opening.factions ?? []).filter(item => atLocation(item) || item?.present === true);
        const localOpportunities = (opening.opportunities ?? []).filter(item => atLocation(item) || item?.locationId == null);
        const knownOpportunity = localOpportunities.find(item => item?.knownToPlayer) ?? localOpportunities[0] ?? null;
        const knownNpc = localNpcs.length ? localNpcs[Math.floor(Math.max(0, Math.min(.999999, Number(rng()) || 0)) * localNpcs.length)] : null;
        const knownFaction = localFactions.find(item => item?.knownToPlayer) ?? localFactions[0] ?? null;
        const locationName = String(opening.location.name ?? this.state.scene?.title ?? 'Ponto inicial');
        const suggestions = [];
        if (knownOpportunity?.title) suggestions.push(`Investigar: ${knownOpportunity.title}`);
        if (knownNpc?.name) suggestions.push(`Conversar com ${knownNpc.name}`);
        suggestions.push('Observar o local e identificar riscos');
        if (opening.market) suggestions.push('Consultar o mercado local');
        suggestions.push('Procurar rumores e oportunidades');
        suggestions.push('Deixar o local e seguir viagem');
        return {
            opening: { ...opening, npcs: localNpcs, factions: localFactions, opportunities: localOpportunities },
            proposal: null,
            awaitingMaster: true,
            masterOpeningRequest: {
                kind: 'living_adventure_opening', locationName, sceneLocationId: locationId,
                opportunity: knownOpportunity ? { id: knownOpportunity.id ?? null, title: knownOpportunity.title ?? '', summary: knownOpportunity.summary ?? '' } : null,
                npc: knownNpc ? { id: knownNpc.id ?? null, name: knownNpc.name ?? '' } : null,
                faction: knownFaction ? { id: knownFaction.id ?? null, name: knownFaction.name ?? '' } : null,
                visibleParticipantNames: localNpcs.map(item => String(item?.name ?? '')).filter(Boolean),
                suggestedActions: suggestions.slice(0, 6)
            },
            session: structuredClone(session), state: structuredClone(this.state)
        };
    }'''

signature = "    startLivingAdventure({ rng = Math.random } = {}) {"
for path in (CONTROLLER, UIBRIDGE):
    source = path.read_text("utf-8")
    source = replace_method(source, signature, start_method)
    path.write_text(source, "utf-8")

runtime = RUNTIME.read_text("utf-8")
old_reset = re.search(r"function r53RestartAdventure\(\)\{.*?\n\}", runtime, re.S)
if not old_reset:
    raise SystemExit("P7 reset function not found")
new_reset = r'''function r53RestartAdventure(){
 if(!confirm('Reiniciar a aventura? Isto apaga o estado atual da campanha neste aparelho e volta à criação do mundo.'))return;
 try{
  ['braseiro.starwars.edge.state.v1','braseiro.starwars.edge.snapshots.v1','braseiro.starwars.edge.scene.v1','braseiro.starwars.edge.session.v1'].forEach(k=>localStorage.removeItem(k));
  for(let i=localStorage.length-1;i>=0;i--){const k=localStorage.key(i)||'';if(/^braseiro\.starwars\.edge\./.test(k))localStorage.removeItem(k)}
  sessionStorage.clear();location.replace('r46-shell.html');
 }catch(err){alert('Falha ao reiniciar: '+String(err?.message||err))}
}'''
runtime = runtime[:old_reset.start()] + new_reset + runtime[old_reset.end():]
RUNTIME.write_text(runtime, "utf-8")

p7_js = r'''/* R54 DEVICE P7 — semantic binding and honest local-map policy. */
(function(){
"use strict";
if(window.__R54_DEVICE_P7__)return;window.__R54_DEVICE_P7__=true;
const norm=v=>String(v||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().replace(/[^a-z0-9]+/g," ").trim();
const index=fetch("image-library/local/r54/p7-semantic-index.json").then(r=>{if(!r.ok)throw Error("P7 index");return r.json()});
const cardHost=n=>{for(let h=n,i=0;h&&i<8;h=h.parentElement,i++)if(h.matches?.("article,li,.card,.panel,.equipment-item,.list-item,[class*=item],[class*=weapon]"))return h;return null};
async function items(){
 if(!["08","09"].includes(new URLSearchParams(location.search).get("screen")||"00"))return;
 const data=await index;
 document.querySelectorAll(".r54-p6-item-visual,.r54-item-visual,.r53-item-visual").forEach(x=>x.remove());
 document.querySelectorAll("strong,.item-name,.equipment-name,[data-item-name]").forEach(label=>{
  const name=String(label.dataset.itemName||label.textContent||"").trim(),entry=data.itemsExactByName?.[norm(name)],host=cardHost(label);
  if(!host||host.dataset.p7VisualDone)return;host.dataset.p7VisualDone="1";
  host.querySelectorAll("img").forEach(img=>{if(/inventory|placeholder|fallback|generic|\/robot/i.test(img.getAttribute("src")||""))img.remove()});
  const box=document.createElement(entry?"figure":"div");box.className=entry?"r54-p7-item-visual":"r54-p7-item-missing";
  if(entry){const img=document.createElement("img");img.src=entry.path;img.alt=name;img.addEventListener("error",()=>{box.replaceChildren(Object.assign(document.createElement("span"),{textContent:"Imagem específica indisponível."}))},{once:true});box.appendChild(img)}
  else box.textContent="Imagem específica não confirmada para este item.";
  host.insertBefore(box,host.firstChild);
 });
}
function mapNote(img){
 const host=img.closest("figure,section,article,div")||img.parentElement;if(!host)return;
 img.hidden=true;img.removeAttribute("src");
 if(!host.querySelector(".r54-p7-map-unavailable")){const n=document.createElement("p");n.className="r54-p7-map-unavailable";n.textContent="Mapa local indisponível para este local.";host.appendChild(n)}
}
async function maps(){
 const data=await index,approved=new Set(Object.values(data.localMapsExactByLocation||{}).map(x=>x.path));
 let locationName="";try{const raw=window.BraseiroGameBridge?.readState?.(),parsed=typeof raw==="string"?JSON.parse(raw):raw,st=parsed?.data||parsed;locationName=String(st?.scene?.location?.name||st?.scene?.locationName||st?.scene?.title||st?.world?.sandbox?.startingPoint||"")}catch(e){}
 if(!locationName){const heading=[...document.querySelectorAll("h1,h2,h3,strong")].find(x=>data.localMapsExactByLocation?.[norm(x.textContent)]);locationName=String(heading?.textContent||"")}
 const mapped=data.localMapsExactByLocation?.[norm(locationName)];
 document.querySelectorAll("img").forEach(img=>{
  if(!/mapa local/i.test(img.alt||""))return;
  if(mapped?.path&&img.getAttribute("src")!==mapped.path)img.src=mapped.path;
  const src=img.getAttribute("src")||"";
  if(!approved.has(src)){mapNote(img);return}
  img.addEventListener("error",()=>mapNote(img),{once:true});
 });
 const mapHeading=[...document.querySelectorAll("h2,h3,h4,strong")].find(x=>/mapa local/i.test(x.textContent||""));
 if(mapped?.path&&mapHeading&&!mapHeading.parentElement?.querySelector('img[alt*="Mapa local" i]')){const img=document.createElement("img");img.alt="Mapa local";img.src=mapped.path;img.className="r54-p7-local-map";mapHeading.insertAdjacentElement("afterend",img)}
}
function style(){if(document.getElementById("r54-p7-style"))return;const s=document.createElement("style");s.id="r54-p7-style";s.textContent='.r54-p7-item-visual{margin:0 0 10px;height:150px;background:#050709;border:1px solid #40515a;border-radius:8px;overflow:hidden}.r54-p7-item-visual img{display:block;width:100%;height:100%;object-fit:contain}.r54-p7-local-map{display:block;width:100%;max-height:360px;object-fit:contain;background:#071017;border:1px solid #40515a;border-radius:8px}.r54-p7-item-missing,.r54-p7-map-unavailable{margin:8px 0;padding:10px;border:1px dashed #75634a;border-radius:7px;color:#d8c7a6;background:#15120d}';document.head.appendChild(s)}
let queued=false;function run(){if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;style();items().catch(()=>{});maps().catch(()=>{})})}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",run,{once:true});else run();new MutationObserver(run).observe(document.documentElement,{childList:true,subtree:true});addEventListener("popstate",run);
})();'''
P7_RUNTIME.write_text(p7_js + "\n", "utf-8")

shell = SHELL.read_text("utf-8")
shell = re.sub(r'<script[^>]+src=["\'](?:\./)?r54-device-p7\.js(?:\?[^"\']*)?["\'][^>]*></script>\s*', '', shell, flags=re.I)
tag = '<script src="r54-device-p7.js"></script>'
shell = re.sub(r'(</head>)', tag + r'\n\1', shell, count=1, flags=re.I)
SHELL.write_text(shell, "utf-8")

critical = ["blaster_pistol", "heavy_blaster_pistol", "blaster_rifle", "blaster_carbine", "disruptor_pistol", "disruptor_rifle", "frag_grenade", "stun_grenade", "thermal_detonator", "combat_knife"]
missing_critical = [item_id for item_id in critical if norm(item_id) not in exact]
if missing_critical:
    raise SystemExit("P7 critical explicit bindings missing: " + repr(missing_critical))
if "R54_DEVICE_P7_SEMANTIC_SCENE" not in UIBRIDGE.read_text("utf-8"):
    raise SystemExit("P7 semantic scene patch missing")
if len(location_rows) != 82 or not all(x["local_map_exists"] and x["local_map_loads"] and x["show_map_section"] for x in location_rows):
    raise SystemExit("P7 did not produce 82 functional local maps")
print(json.dumps({"status":"CODE_PASS_DEVICE_PENDING","items_audited":len(item_rows),"locations_audited":len(location_rows),"confirmed_local_maps":sum(1 for x in location_rows if x['show_map_section']),"critical_exact_bindings":len(critical)}, ensure_ascii=False))
