#!/usr/bin/env python3
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
GAME=ROOT/'app/src/main/java/com/braseiro/talesvtt/game'
DATA=ROOT/'app/src/main/java/com/braseiro/talesvtt/data'

def text(p): return p.read_text(encoding='utf-8')

tales=text(GAME/'TalesCore.kt')
flood=text(GAME/'FloodCore.kt')
kid_codec=text(DATA/'KidJsonCodec.kt')
flood_codec=text(DATA/'FloodTeenJsonCodec.kt')
transport=text(GAME/'MotorBarbaraTransport.kt')

checks={
 'tales_core_has_no_flood_state': not re.search(r'FloodScar|FloodShame|Friction|flood_teen', tales),
 'flood_core_has_no_luck_state': not re.search(r'\b(?:val|var)\s+luck\b|SpendLuck|luckUsed', flood),
 'flood_core_has_no_pride_state': not re.search(r'\b(?:val|var)\s+pride\b|UsePride|prideUsed', flood),
 'tales_codec_profile_locked': '"tales_kid"' in kid_codec and '"flood_teen"' not in kid_codec,
 'flood_codec_profile_locked': '"flood_teen"' in flood_codec and '"tales_kid"' not in flood_codec,
 'flood_codec_rejects_tales_resources': all(x in flood_codec for x in ['!j.has("luck")','!j.has("pride")']),
 'motor_still_tales_only_until_profile_router': 'Não use Shame, Scars, Friction ou regras de Things from the Flood.' in transport,
 'flood_skill_count_12': flood.count(' to "BODY"')+flood.count(' to "TECH"')+flood.count(' to "HEART"')+flood.count(' to "MIND"') == 12,
 'flood_type_count_10': flood.count('FloodTypeDefinition("') == 10,
}
report={'pass':all(checks.values()),'checks':checks,'note':'Flood core exists as separate mechanical module. Motor Barbara remains intentionally Tales-only until a profile router is installed; this prevents cross-profile contamination.'}
out=ROOT/'docs/PROFILE_FIREWALL_AUDIT.json'
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(0 if report['pass'] else 1)
