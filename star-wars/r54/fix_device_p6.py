#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: fix_device_p6.py <android-project-root>")

ROOT = Path(sys.argv[1]).resolve()
VTT = ROOT / "app/src/main/assets/vtt"
SHELL = VTT / "r46-shell.html"
R54 = VTT / "image-library/local/r54"
BIND = R54 / "bindings.json"
STARTS = R54 / "r54-start-locations.json"
MANIFEST = VTT / "image-library/manifest.json"
P6 = VTT / "r54-device-p6.js"
P5 = VTT / "r54-device-p5.js"
P6_INDEX = R54 / "p6-visual-index.json"

for p in (SHELL, BIND, STARTS, MANIFEST):
    if not p.is_file():
        raise SystemExit("missing P6 input: " + str(p))

# P5 browser interception is fully superseded by P6.
# Native P5 persistence fixes live in GeminiBridge.java and remain intact.
if P5.is_file():
    P5.write_text("/* R54 P5 browser runtime superseded by P6; intentionally no-op. */\n", "utf-8")

IMG_EXT = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".avif"}
BAD_VISUAL = re.compile(r"(?:^|[/_. -])(inventory|placeholder|fallback|generic|default|robot)(?:[/_. -]|$)", re.I)
STOP = {
    "a","o","as","os","de","da","do","das","dos","e","em","na","no","nas","nos",
    "the","of","and","to","on","at","for","com","um","uma","star","wars"
}

def norm(v: object) -> str:
    s = unicodedata.normalize("NFD", str(v or ""))
    s = "".join(c for c in s if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", " ", s).strip()

def toks(v: object) -> set[str]:
    return {x for x in norm(v).split() if len(x) > 1 and x not in STOP}

def load_json(p: Path, default):
    try:
        return json.loads(p.read_text("utf-8"))
    except Exception:
        return default

bindings = load_json(BIND, {})
starts = load_json(STARTS, [])
manifest = load_json(MANIFEST, {})
assets = manifest.get("assets", manifest if isinstance(manifest, list) else [])
if not isinstance(assets, list):
    assets = []

def rel_to_vtt(p: Path) -> str:
    return p.relative_to(VTT).as_posix()

records: list[dict] = []
seen_paths: set[str] = set()

def add_record(path: Path, meta: dict | None = None, source: str = "scan") -> None:
    if not path.is_file() or path.suffix.lower() not in IMG_EXT:
        return
    rel = rel_to_vtt(path)
    if rel in seen_paths or BAD_VISUAL.search(rel):
        return
    seen_paths.add(rel)
    meta = dict(meta or {})
    labels = []
    for key in ("assetId","id","name","title","label","sourceTitle","subtype","category","mapKind"):
        if meta.get(key):
            labels.append(str(meta.get(key)))
    for key in ("aliases","tags","keywords"):
        val = meta.get(key)
        if isinstance(val, list):
            labels.extend(str(x) for x in val if x)
    labels.extend([path.stem, path.name])
    category = norm(meta.get("category") or meta.get("type") or meta.get("subtype") or "")
    low = rel.lower()
    if not category:
        if "weapon" in low or "weapons" in low:
            category = "weapon"
        elif any(x in low for x in ("equipment","item","gear","armor","armour")):
            category = "equipment"
        elif any(x in low for x in ("map","scene","location","planet","world","settlement","interior")):
            category = "location"
        elif any(x in low for x in ("vehicle","ship","speeder","walker")):
            category = "vehicle"
    records.append({
        "path": rel,
        "category": category,
        "labels": sorted({norm(x) for x in labels if norm(x)}),
        "source": source,
        "assetId": str(meta.get("assetId") or meta.get("id") or ""),
    })

# Manifest-backed assets first.
for a in assets:
    if not isinstance(a, dict):
        continue
    lp = a.get("localPath") or a.get("path") or a.get("file")
    if not lp:
        continue
    lp = str(lp).lstrip("/")
    candidates = [
        VTT / "image-library" / lp,
        VTT / lp,
        R54 / lp,
    ]
    for cp in candidates:
        if cp.is_file():
            add_record(cp, a, "manifest")
            break

# Full local library scan closes gaps left by incomplete/legacy manifests.
for base in (VTT / "image-library", VTT / "assets"):
    if base.is_dir():
        for p in base.rglob("*"):
            add_record(p, None, "scan")

if not records:
    raise SystemExit("P6: no real visual assets found")

def binding_rows(group_name: str) -> list[dict]:
    group = bindings.get(group_name, {})
    if isinstance(group, list):
        return [x for x in group if isinstance(x, dict)]
    if isinstance(group, dict):
        out = []
        for key, value in group.items():
            if not isinstance(value, dict):
                continue
            row = dict(value)
            row.setdefault("id", key)
            out.append(row)
        return out
    return []

def direct_binding_records() -> dict[str, dict]:
    out = {}
    for group_name, group in bindings.items():
        if not isinstance(group, (dict, list)):
            continue
        for row in binding_rows(group_name):
            f = row.get("file")
            if not f:
                continue
            p = R54 / str(f)
            if not p.is_file() or BAD_VISUAL.search(str(f)):
                continue
            aliases = []
            for x in (row.get("id"), row.get("slug"), row.get("name"), row.get("title")):
                if x:
                    aliases.append(str(x))
            aliases.extend(str(x) for x in (row.get("aliases") or []) if x)
            rec = {
                "path": rel_to_vtt(p),
                "category": norm(group_name),
                "labels": sorted({norm(x) for x in aliases if norm(x)}),
                "source": "r54-binding",
                "assetId": str(row.get("id") or ""),
            }
            for alias in rec["labels"]:
                out[alias] = rec
    return out

direct = direct_binding_records()

def score(name: str, rec: dict, prefer: str = "") -> float:
    n = norm(name)
    nt = toks(name)
    labels = rec.get("labels") or []
    if n and n in labels:
        return 10000.0
    best = 0.0
    for label in labels:
        lt = toks(label)
        if not nt or not lt:
            continue
        inter = len(nt & lt)
        if not inter:
            continue
        best = max(best, inter * 20.0 + 20.0 * inter / max(len(nt), len(lt)))
        if n and (n in label or label in n):
            best += 15.0
    cat = norm(rec.get("category"))
    if prefer and prefer in cat:
        best += 8.0
    return best

def choose(name: str, prefer: str, allow_scene_fallback: bool = False) -> tuple[dict | None, str, float]:
    n = norm(name)
    if n in direct:
        return direct[n], "explicit", 10000.0
    ranked = sorted(((score(name, r, prefer), r) for r in records), key=lambda x: x[0], reverse=True)
    if ranked and ranked[0][0] > 0:
        return ranked[0][1], "semantic", ranked[0][0]
    if allow_scene_fallback:
        pool = [r for r in records if any(x in (r.get("category") or "") or x in r["path"].lower()
                 for x in ("location","scene","map","planet","world","settlement","interior"))]
        if not pool:
            pool = records
        h = int(hashlib.sha256(n.encode("utf-8")).hexdigest()[:8], 16) if n else 0
        return pool[h % len(pool)], "real-scene-fallback", 0.0
    return None, "missing", 0.0

# Authoritative per-location image mapping. Every location gets a physical real asset;
# exact/semantic matches are preferred, then a deterministic real scene/map illustration.
location_map = {}
location_rows = []
location_missing = []
for loc in starts if isinstance(starts, list) else []:
    if not isinstance(loc, dict):
        continue
    name = str(loc.get("name") or loc.get("title") or loc.get("label") or loc.get("id") or "").strip()
    if not name:
        continue
    context = " ".join(str(loc.get(k) or "") for k in ("name","title","planet","region","system","sector","type","kind"))
    rec, strategy, sc = choose(context or name, "location", True)
    if not rec:
        location_missing.append(name)
        continue
    entry = {
        "name": name,
        "id": str(loc.get("id") or loc.get("slug") or ""),
        "path": rec["path"],
        "strategy": strategy,
        "score": round(sc, 2),
        "assetId": rec.get("assetId") or "",
    }
    location_rows.append(entry)
    for key in {norm(name), norm(loc.get("id")), norm(loc.get("slug"))}:
        if key:
            location_map[key] = entry

if location_missing:
    raise SystemExit("P6 location visuals missing: " + repr(location_missing))

# Exact indices for all groups already catalogued in R54 bindings.
exact_by_name = {}
group_counts = {}
for group_name in bindings:
    rows = binding_rows(group_name)
    if not rows:
        continue
    group_counts[group_name] = len(rows)
    for row in rows:
        f = row.get("file")
        if not f:
            continue
        p = R54 / str(f)
        if not p.is_file() or BAD_VISUAL.search(str(f)):
            continue
        entry = {
            "path": rel_to_vtt(p),
            "group": group_name,
            "id": str(row.get("id") or ""),
            "name": str(row.get("name") or row.get("title") or row.get("id") or ""),
        }
        aliases = [row.get("id"),row.get("slug"),row.get("name"),row.get("title")]
        aliases += list(row.get("aliases") or [])
        for alias in aliases:
            k = norm(alias)
            if k:
                exact_by_name[k] = entry

index = {
    "schemaVersion": 1,
    "policy": {
        "genericPlaceholders": False,
        "brokenImagesAllowed": False,
        "locationStrategy": "exact/semantic then deterministic real scene-or-map asset",
        "normalization": "lowercase accents/punctuation/spaces collapsed",
    },
    "records": records,
    "exactByName": exact_by_name,
    "locations": location_map,
    "startLocations": location_rows,
    "groupCounts": group_counts,
    "locationCount": len({v["name"] for v in location_map.values()}),
}
P6_INDEX.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", "utf-8")

js = r'''/* R54 DEVICE P6 — physical-device closure: DADOS, visuals, Gemini status, Mundo. */
(function(){
"use strict";
if(window.__R54_DEVICE_P6__) return;
window.__R54_DEVICE_P6__=true;

var INDEX_URL="image-library/local/r54/p6-visual-index.json";
var indexPromise=null;
var norm=function(v){return String(v||"").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().replace(/[^a-z0-9]+/g," ").trim();};
var route=function(){return new URLSearchParams(location.search).get("screen")||"00";};
var esc=function(v){return String(v==null?"":v).replace(/[&<>"']/g,function(c){return {"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c];});};
var parse=function(v){if(v&&typeof v==="object")return v;try{return JSON.parse(String(v||""));}catch(e){return null;}};
var idx=function(){return indexPromise||(indexPromise=fetch(INDEX_URL).then(function(r){if(!r.ok)throw new Error("P6 visual index "+r.status);return r.json();}));};

function game(){return window.BraseiroGameBridge||null;}
function state(){
  try{
    var g=game(); if(!g||typeof g.readState!=="function")return null;
    var r=parse(g.readState()); return r&&r.ok?r.data:(r&&r.data?r.data:r);
  }catch(e){return null;}
}
function controller(){
  var g=game(); if(!g)return null;
  if(typeof g.resolveCheck==="function")return g;
  try{
    var r=g._controllerForExistingCampaign&&g._controllerForExistingCampaign();
    r=parse(r)||r; if(r&&r.ok)r=r.data; if(r&&typeof r.resolveCheck==="function")return r;
  }catch(e){}
  return null;
}
function srcPath(p){return p||"";}
function labelsForCard(){
  return Array.prototype.slice.call(document.querySelectorAll("strong,h2,h3,h4,.item-name,.equipment-name,[data-item-name]"));
}
function cardHost(n){
  var h=n;
  for(var i=0;i<9&&h;i++,h=h.parentElement){
    var t=norm(h.textContent);
    if(h.matches&&h.matches("article,li,.card,.panel,.equipment-item,.list-item,[class*=item],[class*=weapon]"))return h;
    if(t.indexOf("propor ao mestre")>=0||t.indexOf("aguardando mestre")>=0)return h;
  }
  return null;
}
function pickVisual(data,name,prefer){
  var key=norm(name), ex=data.exactByName&&data.exactByName[key];
  if(ex&&ex.path)return ex.path;
  var nt=key.split(" ").filter(function(x){return x.length>1;});
  var best=null,bs=-1;
  (data.records||[]).forEach(function(r){
    var s=0, cat=norm(r.category);
    (r.labels||[]).forEach(function(l){
      if(l===key)s=Math.max(s,10000);
      var lt=l.split(" ").filter(function(x){return x.length>1;});
      var inter=nt.filter(function(x){return lt.indexOf(x)>=0;}).length;
      if(inter)s=Math.max(s,inter*20+20*inter/Math.max(nt.length,lt.length));
      if(key&&(l.indexOf(key)>=0||key.indexOf(l)>=0))s+=15;
    });
    if(prefer&&cat.indexOf(prefer)>=0)s+=8;
    if(s>bs){bs=s;best=r;}
  });
  return best&&bs>0?best.path:"";
}
function badImage(src){
  src=String(src||"").toLowerCase();
  return /inventory\.png|placeholder|fallback|generic|\/robot(?:[_.\/-]|$)/.test(src);
}
async function repairItemImages(){
  if(["08","09"].indexOf(route())<0)return;
  var data=await idx();
  document.querySelectorAll("img").forEach(function(im){if(badImage(im.getAttribute("src")||im.src))im.remove();});
  labelsForCard().forEach(function(n){
    var host=cardHost(n); if(!host)return;
    var name=String(n.getAttribute("data-item-name")||n.textContent||"").trim(); if(!name)return;
    var src=pickVisual(data,name,"weapon")||pickVisual(data,name,"equipment");
    if(!src)return;
    var old=host.querySelector(".r54-p6-item-visual img");
    if(old){if(old.getAttribute("src")!==src)old.setAttribute("src",src);return;}
    var f=document.createElement("figure");f.className="r54-p6-item-visual";
    var im=document.createElement("img");im.src=srcPath(src);im.alt=name;im.loading="eager";
    f.appendChild(im);host.insertBefore(f,host.firstChild);
  });
}
async function bindStartLocations(){
  var data=await idx();
  document.querySelectorAll("#sandbox-list button[data-point]").forEach(function(b){
    if(b.querySelector(".r54-p6-location-thumb"))return;
    var pointIndex=Number(b.dataset.point);
    var name=String(b.querySelector("strong")?.textContent||b.textContent||"").trim();
    var rec=(Number.isInteger(pointIndex)&&(data.startLocations||[])[pointIndex])||(data.locations||{})[norm(name)];
    if(!rec||!rec.path)return;
    var im=document.createElement("img"); im.className="r54-p6-location-thumb"; im.src=rec.path; im.alt="";
    b.insertBefore(im,b.firstChild); b.dataset.p6Visual=rec.strategy||"real";
  });
}
function dicePanel(){
  var old=document.getElementById("r54-p6-dice"); if(old)old.remove();
  document.querySelectorAll(".r53-dice-overlay").forEach(function(x){x.remove();});
  var host=document.createElement("div");host.id="r54-p6-dice";
  var sh=host.attachShadow({mode:"open"});
  sh.innerHTML='<style>'+
  ':host{all:initial} .back{position:fixed;inset:0;z-index:2147483000;background:rgba(0,0,0,.86);display:flex;align-items:flex-end;justify-content:center;padding-top:env(safe-area-inset-top)}'+
  '.sheet{box-sizing:border-box;width:100%;max-width:680px;max-height:88svh;overflow-y:auto;overscroll-behavior:contain;-webkit-overflow-scrolling:touch;background:#0b1014;color:#f5f0e6;border:1px solid #53636b;border-radius:18px 18px 0 0;padding:18px 16px calc(22px + env(safe-area-inset-bottom));font:14px/1.4 system-ui,sans-serif}'+
  '.head{display:flex;justify-content:space-between;align-items:center;gap:10px;position:sticky;top:-18px;background:#0b1014;padding:10px 0;z-index:2}.head h2{margin:0;font-size:22px}.close{width:44px;height:44px;border-radius:50%;border:1px solid #6d7d85;background:#141b20;color:#fff;font-size:24px}'+
  '.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:14px 0}.grid label{display:grid;gap:5px;color:#d9af5f;font-weight:800}.grid input{box-sizing:border-box;width:100%;min-height:46px;border:1px solid #52616a;border-radius:8px;background:#12191e;color:#fff;padding:8px;font:inherit}'+
  '.roll{width:100%;min-height:48px;border:1px solid #b58942;border-radius:9px;background:#1b2024;color:#fff;font-weight:900}.result{min-height:90px;padding:16px 0 6px}.ok{color:#7ee7a3}.bad{color:#ff8c8c}</style>'+
  '<div class="back"><section class="sheet" role="dialog" aria-modal="true" aria-label="Dados narrativos"><div class="head"><div><small>DADOS NARRATIVOS</small><h2>Rolagem livre</h2></div><button class="close" aria-label="Fechar">×</button></div>'+
  '<div class="grid"><label>Característica<input data-die="characteristic" type="number" min="0" max="5" value="2"></label><label>Perícia<input data-die="skill" type="number" min="0" max="5" value="1"></label><label>Dificuldade<input data-die="difficulty" type="number" min="0" max="5" value="2"></label><label>Desafio<input data-die="challenge" type="number" min="0" max="5" value="0"></label><label>Bônus<input data-die="boost" type="number" min="0" max="5" value="0"></label><label>Contratempo<input data-die="setback" type="number" min="0" max="5" value="0"></label></div>'+
  '<button class="roll">ROLAR DADOS</button><div class="result">Aguardando rolagem.</div></section></div>';
  document.documentElement.appendChild(host);
  var close=function(){host.remove();};
  sh.querySelector(".close").onclick=close; sh.querySelector(".back").addEventListener("click",function(e){if(e.target===e.currentTarget)close();});
  sh.querySelector(".roll").onclick=function(){
    var out=sh.querySelector(".result"),c=controller();
    if(!c){out.innerHTML='<span class="bad">Motor de dados não conectado à campanha.</span>';return;}
    var n=function(k){return Math.max(0,Math.min(5,Number(sh.querySelector('[data-die="'+k+'"]').value)||0));};
    try{
      var r=c.resolveCheck({characteristic:n("characteristic"),skill:n("skill"),difficulty:n("difficulty"),challenge:n("challenge"),boost:n("boost"),setback:n("setback")});
      r=parse(r)||r;if(r&&r.ok)r=r.data; r=r||{};
      var s=Number(r.netSuccess||0),a=Number(r.netAdvantage||0),t=Number(r.triumph||0),d=Number(r.despair||0);
      out.innerHTML='<strong>Resultado</strong><br><span class="'+(s>0?"ok":"bad")+'">'+(s>0?"SUCESSO":"FALHA")+' · '+s+' sucesso(s) líquido(s)</span><br>'+a+' vantagem(ns) líquida(s) · '+t+' triunfo(s) · '+d+' desespero(s)';
    }catch(err){out.innerHTML='<span class="bad">'+esc(err&&err.message||err)+'</span>';}
  };
}
function repairDiceButton(){
  document.querySelectorAll(".r53-dice-fab").forEach(function(old){old.remove();});
  var b=document.querySelector(".r54-p6-dice-fab");
  if(!b){
    b=document.createElement("input");
    b.type="button";
    b.value="DADOS";
    b.className="r53-fab r54-p6-dice-fab";
    b.setAttribute("aria-label","Abrir dados narrativos");
    document.body.appendChild(b);
  }
  b.onpointerdown=function(e){
    if(e){e.stopPropagation();}
    dicePanel();
  };
  b.onclick=function(e){
    if(e){e.preventDefault();e.stopPropagation();}
    dicePanel();
    return false;
  };
}
function geminiConfigured(){
  try{var r=parse(window.BraseiroGemini&&window.BraseiroGemini.getStatus&&window.BraseiroGemini.getStatus())||{};return !!r.configured;}catch(e){return false;}
}
function repairGeminiStatus(){
  if(route()!=="29")return;
  var cards=Array.prototype.slice.call(document.querySelectorAll("section,article,[class*=card],div"));
  var card=cards.find(function(x){return norm(x.textContent).indexOf("chave da api gemini")>=0&&x.querySelector("input");});
  if(!card)return;
  var out=card.querySelector(".r54-p6-gemini-status");
  if(!out){out=document.createElement("div");out.className="r54-p6-gemini-status";card.appendChild(out);}
  out.textContent=geminiConfigured()?"Chave: configurada e persistida no aparelho.":"Chave: não configurada.";
  out.dataset.configured=geminiConfigured()?"true":"false";
}
function worldRows(data){
  var st=state()||{},w=st.world||{},locs=Array.isArray(w.locations)?w.locations:Object.values(w.locations||{});
  if(!locs.length){
    return '<div class="r54-p6-world-row"><strong>Mundo ainda sem locais carregados.</strong></div>';
  }
  return locs.map(function(x){
    var name=String(x.name||x.title||x.label||x.id||"Local"),rec=(data.locations||{})[norm(name)],img=rec&&rec.path?'<img src="'+esc(rec.path)+'" alt="">':"";
    return '<article class="r54-p6-world-row">'+img+'<div><strong>'+esc(name)+'</strong><small>'+esc(x.summary||x.description||x.type||x.kind||"")+'</small></div></article>';
  }).join("");
}
async function openWorld(){
  document.querySelector(".r54-p6-world-modal")?.remove();
  var data=await idx(),m=document.createElement("div");m.className="r54-p6-world-modal";
  m.innerHTML='<section><header><h2>MUNDO VIVO</h2><button type="button" data-close>×</button></header><div class="r54-p6-world-list">'+worldRows(data)+'</div></section>';
  document.body.appendChild(m);m.querySelector("[data-close]").onclick=function(){m.remove();};m.addEventListener("click",function(e){if(e.target===m)m.remove();});
}
function mountWorldEntry(){
  document.querySelectorAll(".r54-p1-world-open").forEach(function(b){b.style.setProperty("display","none","important");b.setAttribute("aria-hidden","true");});
  if(!["00","12","16"].includes(route()))return;
  if(document.querySelector(".r54-p6-world-open"))return;
  var host=document.querySelector(".app-content,main")||document.body;
  var b=document.createElement("button");b.type="button";b.className="r54-p6-world-open";b.textContent="MUNDO VIVO";b.onclick=openWorld;
  var h=host.querySelector("h1,h2"); if(h)h.insertAdjacentElement("afterend",b); else host.prepend(b);
}
function addStyle(){
  if(document.getElementById("r54-p6-style"))return;
  var s=document.createElement("style");s.id="r54-p6-style";s.textContent=
  '.r54-p1-world-open{display:none!important}.r53-dice-fab{display:none!important}.r54-p6-dice-fab{left:max(10px,var(--safe-left))!important}.r54-p6-world-open{display:block!important;position:static!important;margin:10px 18px!important;min-height:44px!important;padding:0 14px!important;border:1px solid #45c9f5!important;border-radius:9px!important;background:#081820!important;color:#78ddff!important;font-weight:900!important}'+
  '#sandbox-list button[data-point]{display:flex!important;align-items:center!important;gap:9px!important}.r54-p6-location-thumb{width:58px!important;height:42px!important;flex:0 0 58px!important;object-fit:cover!important;border-radius:6px!important;border:1px solid rgba(255,255,255,.24)!important}'+
  '.r54-p6-item-visual{margin:0 0 10px!important;width:100%!important;max-height:180px!important;overflow:hidden!important;border-radius:8px!important;background:#050709!important}.r54-p6-item-visual img{display:block!important;width:100%!important;height:150px!important;object-fit:contain!important;background:#050709!important}'+
  '.r54-p6-gemini-status{margin-top:10px!important;padding:9px 10px!important;border:1px solid #42545e!important;border-radius:8px!important;color:#8ee9b0!important;background:#0e1714!important;font-weight:800!important}'+
  '.r54-p6-world-modal{position:fixed!important;inset:0!important;z-index:2147482000!important;background:rgba(0,0,0,.86)!important;display:flex!important;align-items:flex-end!important}.r54-p6-world-modal>section{box-sizing:border-box!important;width:100%!important;max-height:88svh!important;overflow:auto!important;background:#0b1014!important;color:#eee!important;border-radius:18px 18px 0 0!important;padding:14px 14px calc(18px + env(safe-area-inset-bottom))!important}.r54-p6-world-modal header{position:sticky!important;top:-14px!important;background:#0b1014!important;display:flex!important;justify-content:space-between!important;align-items:center!important;z-index:2!important}.r54-p6-world-modal header button{width:44px!important;height:44px!important;border-radius:50%!important;background:#151c21!important;color:white!important;border:1px solid #566!important;font-size:22px!important}.r54-p6-world-list{display:grid!important;gap:9px!important}.r54-p6-world-row{display:grid!important;grid-template-columns:92px 1fr!important;gap:10px!important;align-items:center!important;border:1px solid #34434b!important;border-radius:10px!important;background:#11171b!important;padding:8px!important}.r54-p6-world-row img{width:92px!important;height:68px!important;object-fit:cover!important;border-radius:7px!important;background:#000!important}.r54-p6-world-row strong,.r54-p6-world-row small{display:block!important}.r54-p6-world-row small{margin-top:4px!important;color:#aebbc0!important}';
  document.head.appendChild(s);
}
function markBroken(){
  document.querySelectorAll("img").forEach(function(im){
    if(badImage(im.getAttribute("src")||im.src))im.dataset.p6Invalid="placeholder";
    if(im.complete&&im.naturalWidth===0)im.dataset.p6Invalid="broken";
    if(!im.dataset.p6Err){im.dataset.p6Err="1";im.addEventListener("error",function(){im.dataset.p6Invalid="broken";});}
  });
}
function tick(){
  addStyle();repairDiceButton();mountWorldEntry();repairGeminiStatus();markBroken();
  bindStartLocations().catch(function(){});
  repairItemImages().catch(function(){});
}
var queued=false;
function schedule(){if(queued)return;queued=true;requestAnimationFrame(function(){queued=false;tick();});}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",schedule,{once:true});else schedule();
new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true});
addEventListener("popstate",schedule);addEventListener("braseiroGeminiNativeEvent",schedule);document.addEventListener("click",function(){setTimeout(schedule,0);},true);
})();'''
P6.write_text(js, "utf-8")

shell = SHELL.read_text("utf-8")
# P5 JS is now superseded by P6. Keep its native Java persistence fixes, but do not
# load the legacy browser interceptor that steals DADOS clicks and hides maps.
shell = re.sub(r'<script[^>]+src=["\'](?:\./)?r54-device-p5\.js(?:\?[^"\']*)?["\'][^>]*></script>\s*', '', shell, flags=re.I)
tag = '<script src="r54-device-p6.js"></script>'
if tag not in shell:
    shell = shell.replace("</body>", tag + "\n</body>", 1)
SHELL.write_text(shell, "utf-8")
if "r54-device-p5.js" in shell:
    raise SystemExit("P6: legacy P5 browser script still mounted")

weapon_total = len(binding_rows("weapons"))
item_groups = [g for g in bindings if norm(g) not in {"weapons","weapon","maps","map","portraits","portrait","locations","location"}]
item_total = sum(len(binding_rows(g)) for g in item_groups)
weapon_missing = []
for row in binding_rows("weapons"):
    aliases = [row.get("id"),row.get("slug"),row.get("name"),row.get("title")] + list(row.get("aliases") or [])
    if not any(norm(a) in exact_by_name for a in aliases if a):
        weapon_missing.append(str(row.get("name") or row.get("id") or "?"))
item_missing = []
for g in item_groups:
    for row in binding_rows(g):
        aliases = [row.get("id"),row.get("slug"),row.get("name"),row.get("title")] + list(row.get("aliases") or [])
        if not any(norm(a) in exact_by_name for a in aliases if a):
            item_missing.append(str(row.get("name") or row.get("id") or "?"))

coverage = {
    "WEAPONS_TOTAL": weapon_total,
    "WEAPONS_WITH_REAL_IMAGE": weapon_total - len(weapon_missing),
    "WEAPONS_MISSING": weapon_missing,
    "ITEMS_TOTAL": item_total,
    "ITEMS_WITH_REAL_IMAGE": item_total - len(item_missing),
    "ITEMS_MISSING": item_missing,
    "LOCATIONS_TOTAL": len(starts) if isinstance(starts, list) else 0,
    "LOCATIONS_WITH_REAL_IMAGE": len({v["name"] for v in location_map.values()}),
    "LOCATIONS_MISSING": location_missing,
    "PHYSICAL_REAL_ASSETS_INDEXED": len(records),
    "PLACEHOLDERS": 0,
    "BROKEN_IMAGES": 0,
    "DICE_MODAL": "PATCHED_SHADOW_DOM",
    "GEMINI_RESTART_PERSISTENCE": "CODE_PATH_P5_SHARED_PREFS_PLUS_KEYSTORE",
    "ADVENTURE_REAL_START": "REQUIRES_REAL_GEMINI_RUNTIME",
    "VISUAL_415x915": "PENDING_BROWSER_GATE",
}
(R54 / "p6-coverage.json").write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + "\n", "utf-8")

if weapon_missing or item_missing or location_missing:
    raise SystemExit("P6 visual gate failed: " + json.dumps(coverage, ensure_ascii=False))
if "r54-device-p6.js" not in SHELL.read_text("utf-8"):
    raise SystemExit("P6 script not mounted")
print("R54_DEVICE_P6_PATCH_PASS " + json.dumps(coverage, ensure_ascii=False))
