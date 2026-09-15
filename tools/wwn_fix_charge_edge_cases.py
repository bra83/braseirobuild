from pathlib import Path

p=Path('app/src/main/assets/www/core/combat-service.js')
s=p.read_text()
old="charge=inspectCharge({from:originalFrom,to:destination,economy,normalMoveFeet:magicMove.moveFeet});if(charge.status!=='OK')return Object.freeze({status:`${charge.status}_NO_MUTATION`,revision:loaded.revision,charge});attackFrom=destination;actionCostKind='MOVE_AND_MAIN';tacticalHitModifier+=charge.hitModifier;"
new="charge=inspectCharge({from:originalFrom,to:destination,economy,normalMoveFeet:magicMove.moveFeet});if(charge.status!=='OK')return Object.freeze({status:`${charge.status}_NO_MUTATION`,revision:loaded.revision,charge});const chargeLeaving=(combat.enemyIds??[]).filter(id=>(state.npcState?.[id]?.hp??0)>0&&state.npcState?.[id]?.weaponContract?.melee!==false&&combat.tactical.actors?.[id]&&distanceFeet(originalFrom,combat.tactical.actors[id])<=5+1e-9&&distanceFeet(destination,combat.tactical.actors[id])>5+1e-9);if(chargeLeaving.length)return Object.freeze({status:'CHARGE_FREE_ATTACK_REQUIRED_NO_MUTATION',revision:loaded.revision,leavingEnemyIds:Object.freeze(chargeLeaving),charge});attackFrom=destination;actionCostKind='MOVE_AND_MAIN';tacticalHitModifier+=charge.hitModifier;"
if new not in s:
    if old not in s:raise SystemExit('charge inspection marker missing')
    s=s.replace(old,new,1)
old="const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_ATTACK',suffix:`attack:${actorId}:${npcId}`,payload:"
new="const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:charge?'PLAYER_CHARGE':'PLAYER_ATTACK',suffix:charge?`charge:${actorId}:${npcId}`:`attack:${actorId}:${npcId}`,payload:"
newer="eventKind:charge?'PLAYER_CHARGE':swarmPlan?'PLAYER_SWARM_ATTACK':'PLAYER_ATTACK'"
if new not in s and newer not in s:
    if old not in s:raise SystemExit('attack event marker missing')
    s=s.replace(old,new,1)
p.write_text(s)
print('WWN charge edge cases patched')
