from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    s = p.read_text(encoding="utf-8")
    if new in s:
        return False
    if old not in s:
        raise SystemExit(f"{label}: expected pattern not found")
    p.write_text(s.replace(old, new, 1), encoding="utf-8")
    return True


changed = []

# 1) Jornada: fix the concrete syntax regression introduced around the image-removal button.
p = Path("app/src/main/assets/www/jornada.js")
s = p.read_text(encoding="utf-8")
bad = "if(removeImage)removeImage.classList.toggle('hidden',!imageRef)const observe="
good = "if(removeImage)removeImage.classList.toggle('hidden',!imageRef);const observe="
if bad in s:
    s = s.replace(bad, good, 1)
    p.write_text(s, encoding="utf-8")
    changed.append(str(p))
elif good not in s:
    raise SystemExit("jornada.js: image-removal syntax anchor not found")

# 2) Starter bootstrap: a new campaign must exist before a hero is chosen.
p = Path("app/src/main/assets/www/core/starter-bootstrap.js")
s = p.read_text(encoding="utf-8")
old_make = """export function makeStarterCampaignState({character,pack,campaignId=DEFAULT_STARTER_CAMPAIGN_ID,masterSeed=DEFAULT_STARTER_SEED}={}){\n  if(!character)throw new Error('Starter character required');\n  if(!pack)throw new Error('Starter pack required');\n  const normalized=normalizeCharacterForRuntime(character);\n  let state=makeEmptyCampaignState({campaignId,displayName:pack.campaignName,masterSeed});\n  state.characterState=normalized;\n  state.charactersState={[normalized.characterId]:clone(normalized)};\n  state.partyState=createPartyState({members:[normalized.characterId],position:clone(pack.startingSituation?.position??null)});\n  state.worldState.position=clone(pack.startingSituation?.position??null);\n  state.worldState.expedition.supplies={foodUnits:7,waterUnits:7,firewoodUnits:2};\n  state=applyStarterContent(state,compileStarterPack(pack));\n  const opening=prepareCampaignOpening(state,{campaignId,nextRevision:0});\n  if(opening.status==='OK')state=opening.state;\n  return state;\n}"""
new_make = """export function makeStarterCampaignState({character=null,pack,campaignId=DEFAULT_STARTER_CAMPAIGN_ID,masterSeed=DEFAULT_STARTER_SEED}={}){\n  if(!pack)throw new Error('Starter pack required');\n  let state=makeEmptyCampaignState({campaignId,displayName:pack.campaignName,masterSeed});\n  if(character){\n    const normalized=normalizeCharacterForRuntime(character);\n    state.characterState=normalized;\n    state.charactersState={[normalized.characterId]:clone(normalized)};\n    state.partyState=createPartyState({members:[normalized.characterId],position:clone(pack.startingSituation?.position??null)});\n  }\n  state.worldState.position=clone(pack.startingSituation?.position??null);\n  state.worldState.expedition.supplies={foodUnits:7,waterUnits:7,firewoodUnits:2};\n  state=applyStarterContent(state,compileStarterPack(pack));\n  const opening=prepareCampaignOpening(state,{campaignId,nextRevision:0});\n  if(opening.status==='OK')state=opening.state;\n  return state;\n}"""
if old_make in s:
    s = s.replace(old_make, new_make, 1)
    changed.append(str(p))
elif new_make not in s:
    raise SystemExit("starter-bootstrap.js: starter state function shape changed unexpectedly")

old_ensure_sig = "export function ensureStarterCampaign({persistence,character,pack,campaignId=DEFAULT_STARTER_CAMPAIGN_ID}={}){"
new_ensure_sig = "export function ensureStarterCampaign({persistence,character,pack,campaignId=DEFAULT_STARTER_CAMPAIGN_ID,startWithCharacter=false}={}){"
if old_ensure_sig in s:
    s = s.replace(old_ensure_sig, new_ensure_sig, 1)
    changed.append(str(p))
elif new_ensure_sig not in s:
    raise SystemExit("starter-bootstrap.js: ensureStarterCampaign signature anchor missing")

old_call = "const state=makeStarterCampaignState({character,pack,campaignId});"
new_call = "const state=makeStarterCampaignState({character:startWithCharacter?character:null,pack,campaignId});"
if old_call in s:
    s = s.replace(old_call, new_call, 1)
    changed.append(str(p))
elif new_call not in s:
    raise SystemExit("starter-bootstrap.js: starter creation call anchor missing")

p.write_text(s, encoding="utf-8")

# 3) Prevent the UI hotfix from masking a real empty-state regression: it may add shortcuts,
# but the canonical sheet already renders SEM PERSONAGEM itself.
# No source rewrite is needed here; the runtime gate asserts this contract.

print("WWN_FINISH_REAL_FLOW_PATCH=" + ("CHANGED:" + ",".join(sorted(set(changed))) if changed else "NO_CHANGES"))
