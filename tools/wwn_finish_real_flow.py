from pathlib import Path

changed=[]

# Jornada: concrete syntax regression around image-removal visibility.
p=Path('app/src/main/assets/www/jornada.js')
s=p.read_text(encoding='utf-8')
bad="if(removeImage)removeImage.classList.toggle('hidden',!imageRef)const observe="
good="if(removeImage)removeImage.classList.toggle('hidden',!imageRef);const observe="
if bad in s:
    s=s.replace(bad,good,1);p.write_text(s,encoding='utf-8');changed.append(str(p))
elif good not in s:
    raise SystemExit('jornada.js: image-removal syntax anchor not found')

# Current canonical bootstrap: the world must exist before the player chooses a hero.
p=Path('app/src/main/assets/www/core/starter-bootstrap.js')
s=p.read_text(encoding='utf-8')
old_make="""export function makeStarterCampaignState({character,pack,campaignId=STARTER_CAMPAIGN_ID,displayName=STARTER_DISPLAY_NAME,masterSeed=STARTER_MASTER_SEED}={}){\n  const c=normalizeCharacter(character);let state=makeEmptyCampaignState({campaignId,displayName,masterSeed});\n  state.characterState=c;state.charactersState={[c.characterId]:clone(c)};\n  state.worldState={...state.worldState,position:clone(STARTER_POSITION),elapsedDays:0};\n  state.partyState=createPartyState({memberIds:[c.characterId],position:state.worldState.position});\n  state=structuredClone(applyStarterContentToState(state,pack));\n  return state;\n}"""
new_make="""export function makeStarterCampaignState({character=null,pack,campaignId=STARTER_CAMPAIGN_ID,displayName=STARTER_DISPLAY_NAME,masterSeed=STARTER_MASTER_SEED}={}){\n  let state=makeEmptyCampaignState({campaignId,displayName,masterSeed});\n  state.worldState={...state.worldState,position:clone(STARTER_POSITION),elapsedDays:0};\n  if(character){\n    const c=normalizeCharacter(character);\n    state.characterState=c;state.charactersState={[c.characterId]:clone(c)};\n    state.partyState=createPartyState({memberIds:[c.characterId],position:state.worldState.position});\n  }\n  state=structuredClone(applyStarterContentToState(state,pack));\n  return state;\n}"""
if old_make in s:
    s=s.replace(old_make,new_make,1);changed.append(str(p))
elif new_make not in s:
    raise SystemExit('starter-bootstrap.js: current makeStarterCampaignState shape not recognized')

old_sig="export function ensureStarterCampaign({persistence,character,pack,campaignId=STARTER_CAMPAIGN_ID}={}){"
new_sig="export function ensureStarterCampaign({persistence,character,pack,campaignId=STARTER_CAMPAIGN_ID,startWithCharacter=false}={}){"
if old_sig in s:
    s=s.replace(old_sig,new_sig,1);changed.append(str(p))
elif new_sig not in s:
    raise SystemExit('starter-bootstrap.js: ensureStarterCampaign signature anchor missing')

old_call="let state=makeStarterCampaignState({character,pack,campaignId});"
new_call="let state=makeStarterCampaignState({character:startWithCharacter?character:null,pack,campaignId});"
if old_call in s:
    s=s.replace(old_call,new_call,1);changed.append(str(p))
elif new_call not in s:
    raise SystemExit('starter-bootstrap.js: starter creation call anchor missing')

p.write_text(s,encoding='utf-8')
print('WWN_FINISH_REAL_FLOW_PATCH=' + ('CHANGED:'+','.join(sorted(set(changed))) if changed else 'NO_CHANGES'))
