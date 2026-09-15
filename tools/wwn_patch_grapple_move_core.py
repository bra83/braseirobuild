from pathlib import Path

p=Path('app/src/main/assets/www/core/wwn-special-combat-core.js')
s=p.read_text()
old="""export function grappleMoveOutcome({attackerOpposedTotal,defenderOpposedTotal,attackerIsPc=false,defenderIsPc=false,mode='MOVE'}){
  const winner=opposedWinner({aTotal:int(attackerOpposedTotal,'attackerOpposedTotal'),bTotal:int(defenderOpposedTotal,'defenderOpposedTotal'),aIsPc:Boolean(attackerIsPc),bIsPc:Boolean(defenderIsPc)});
  const success=winner==='A';const normalized=String(mode).toUpperCase();if(!['MOVE','THROW'].includes(normalized))throw new RangeError('mode must be MOVE or THROW');
  return Object.freeze({rule:'WWN_GRAPPLE_MOVE_V1',success,targetEscapes:!success,actionCost:'MAIN',moveTargetFeet:success&&normalized==='MOVE'?10:0,throwTargetFeet:success&&normalized==='THROW'?5:0,targetProne:success&&normalized==='THROW',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}"""
new="""export function grappleMoveOutcome({attackerOpposedTotal,defenderOpposedTotal,attackerIsPc=false,defenderIsPc=false,mode='MOVE'}){
  const attacker=int(attackerOpposedTotal,'attackerOpposedTotal'),defender=int(defenderOpposedTotal,'defenderOpposedTotal');
  const winner=attacker>defender?'A':defender>attacker?'B':'TIE';
  const success=attacker>defender;const normalized=String(mode).toUpperCase();if(!['MOVE','THROW'].includes(normalized))throw new RangeError('mode must be MOVE or THROW');
  return Object.freeze({rule:'WWN_GRAPPLE_MOVE_V1',success,winner,targetEscapes:!success,actionCost:'MAIN',moveTargetFeet:success&&normalized==='MOVE'?10:0,throwTargetFeet:success&&normalized==='THROW'?5:0,targetProne:success&&normalized==='THROW',sourceRefs:[WWN_SPECIAL_COMBAT_SOURCES.maneuvers]});
}"""
if new not in s:
    if old not in s:raise SystemExit('grappleMoveOutcome marker missing')
    s=s.replace(old,new,1)
p.write_text(s)
print('WWN grapple move tie rule patched')
