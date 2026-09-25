from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4936.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

combat_rel="lib/rules/combat.ts"
combat=read(combat_rel)

combat=req(combat,"position:{x:35,y:50}","position:{x:0.35,y:0.50}","hero default combat position")

combat=req(
    combat,
    "const actor={...heroActor(id,hp.current,hp.max),position:{x:25+(index%4)*13,y:62+Math.floor(index/4)*9}};",
    "const actor={...heroActor(id,hp.current,hp.max),position:{x:clamp(0.25+(index%4)*0.13,0.03,0.97),y:clamp(0.62+Math.floor(index/4)*0.09,0.05,0.95)}};",
    "start combat normalized positions",
)

old_move="""export function moveCombatActor(state:MasterState,actorId:string,x:number,y:number):MasterState{
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===actorId?{...actor,position:{x:Math.max(3,Math.min(97,x)),y:Math.max(5,Math.min(95,y))}}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}"""
new_move="""export function moveCombatActor(state:MasterState,actorId:string,x:number,y:number):MasterState{
  const rawX=Number(x), rawY=Number(y);
  const normalizedX=Number.isFinite(rawX)?(Math.abs(rawX)>1.5?rawX/100:rawX):0.5;
  const normalizedY=Number.isFinite(rawY)?(Math.abs(rawY)>1.5?rawY/100:rawY):0.5;
  const safeX=clamp(normalizedX,0.03,0.97);
  const safeY=clamp(normalizedY,0.05,0.95);
  return {...state,combat:{...state.combat,actors:state.combat.actors.map(actor=>actor.actorId===actorId?{...actor,position:{x:safeX,y:safeY}}:actor),updatedAt:Date.now()},updatedAt:Date.now()};
}"""
combat=req(combat,old_move,new_move,"moveCombatActor normalized clamp")

old_upsert="""export function upsertCombatActor(state:MasterState,actor:CombatActorState):MasterState{
  const list=[...state.combat.actors];const index=list.findIndex(x=>x.actorId===actor.actorId);
  if(index>=0)list[index]={...list[index],...actor,engagedWith:actor.engagedWith||list[index].engagedWith||[],stateNotes:actor.stateNotes||list[index].stateNotes||[]};else list.push({...actor,engagedWith:actor.engagedWith||[],stateNotes:actor.stateNotes||[]});
  return {...state,combat:{...state.combat,actors:list,updatedAt:Date.now()},updatedAt:Date.now()};
}"""
new_upsert="""export function upsertCombatActor(state:MasterState,actor:CombatActorState):MasterState{
  const normalizedPosition=actor.position?{
    x:clamp(Math.abs(Number(actor.position.x))>1.5?Number(actor.position.x)/100:Number(actor.position.x),0.03,0.97),
    y:clamp(Math.abs(Number(actor.position.y))>1.5?Number(actor.position.y)/100:Number(actor.position.y),0.05,0.95),
  }:actor.position;
  const incoming={...actor,position:normalizedPosition};
  const list=[...state.combat.actors];const index=list.findIndex(x=>x.actorId===actor.actorId);
  if(index>=0)list[index]={...list[index],...incoming,engagedWith:incoming.engagedWith||list[index].engagedWith||[],stateNotes:incoming.stateNotes||list[index].stateNotes||[]};else list.push({...incoming,engagedWith:incoming.engagedWith||[],stateNotes:incoming.stateNotes||[]});
  return {...state,combat:{...state.combat,actors:list,updatedAt:Date.now()},updatedAt:Date.now()};
}"""
combat=req(combat,old_upsert,new_upsert,"upsert combat actor position normalization")
write(combat_rel,combat)

state_rel="lib/master/state.ts"
state=read(state_rel)
anchor="""  const director: SessionDirectorState = {
    ...base.sessionDirector,"""
insert="""  const normalizedCombatActors = (incoming.combat?.actors || []).map(actor => {
    if (!actor.position) return actor;
    const rawX = Number(actor.position.x);
    const rawY = Number(actor.position.y);
    const x = Math.max(0.03, Math.min(0.97, Number.isFinite(rawX) ? (Math.abs(rawX) > 1.5 ? rawX / 100 : rawX) : 0.5));
    const y = Math.max(0.05, Math.min(0.95, Number.isFinite(rawY) ? (Math.abs(rawY) > 1.5 ? rawY / 100 : rawY) : 0.5));
    return { ...actor, position: { x, y } };
  });

"""
if "const normalizedCombatActors" not in state:
    state=req(state,anchor,insert+anchor,"saved combat position migration")

old_combat="""    combat: { ...base.combat, ...(incoming.combat || {}), actors: incoming.combat?.actors || [], narrativeBlocks: incoming.combat?.narrativeBlocks || [], rolls: incoming.combat?.rolls || [], turnIndex: Math.max(0, Number(incoming.combat?.turnIndex || 0)), updatedAt: incoming.combat?.updatedAt || Date.now() },"""
new_combat="""    combat: { ...base.combat, ...(incoming.combat || {}), actors: normalizedCombatActors, narrativeBlocks: incoming.combat?.narrativeBlocks || [], rolls: incoming.combat?.rolls || [], turnIndex: Math.max(0, Number(incoming.combat?.turnIndex || 0)), updatedAt: incoming.combat?.updatedAt || Date.now() },"""
state=req(state,old_combat,new_combat,"use migrated combat actors")
write(state_rel,state)

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.36"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.35"','"version": "4.9.36"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.35'","APP_VERSION = '4.9.36'",t)
t=re.sub(r"APP_VERSION_CODE = 49035","APP_VERSION_CODE = 49036",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49035","versionCode = 49036",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.35"','versionName = "4.9.36"',t)
write(gradle_rel,t)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel).replace("index.html?v=49035&immersive=1","index.html?v=49036&immersive=1")
write(main_rel,main)

combat=read(combat_rel); state=read(state_rel); app=read("App.tsx")
checks={
 "combat-default-normalized": "position:{x:0.35,y:0.50}" in combat,
 "combat-start-normalized": "0.25+(index%4)*0.13" in combat and "0.62+Math.floor(index/4)*0.09" in combat,
 "combat-move-normalized-clamp": "safeX=clamp(normalizedX,0.03,0.97)" in combat and "safeY=clamp(normalizedY,0.05,0.95)" in combat,
 "combat-upsert-normalizes": "const normalizedPosition=actor.position?" in combat,
 "saved-position-migration": "const normalizedCombatActors" in state and "actors: normalizedCombatActors" in state,
 "tts-current-architecture-preserved": "kokoroBackendPackages" in read(main_rel) and "Braseiro Kokoro PT-BR · neural" in read(main_rel),
 "music-persistence-preserved": "scene-music-preserved-manual" in app,
 "journey-lock-preserved": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.36" in read(version_rel) and "49036" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4936_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.36_COMBAT_COORDINATE_FIX_OK")
