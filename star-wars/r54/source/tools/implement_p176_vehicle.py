from pathlib import Path
root=Path(__file__).resolve().parents[1]
vtt=root/'app/src/main/assets/vtt'
def patch(rel,old,new):
 p=vtt/rel;s=p.read_text()
 if old not in s:raise RuntimeError(f'{rel}: missing {old[:60]}')
 p.write_text(s.replace(old,new,1))
patch('js/expansion/aorTalentRuntimeAliases.mjs',"  'FUEGO CONTROLADO': 'FIRE_CONTROL',", "  'FUEGO CONTROLADO': 'FIRE_CONTROL',\n  'SALIDA TÉRMICA': 'EXHAUST_PORT',\n  'IMPACTO DIRECTO': 'DEAD_TO_RIGHTS',\n  'IMPACTO DIRECTO MEJORADO': 'DEAD_TO_RIGHTS_IMPROVED',")
patch('js/rules/vehicles.mjs',"  int(vehicle.silhouette,'Silhueta',{min:0,max:10});", "  int(vehicle.silhouette,'Silhueta',{min:0,max:10});\n  if(vehicle.massive!==undefined)int(vehicle.massive,'Massive',{min:0,max:10});")
patch('js/rules/vehicles.mjs',"extraSetback=0,currentRound=null}={}){\n  validateVehicle(attacker);", "extraSetback=0,currentRound=null,ignoreMassive=false,deadToRightsBonus=0}={}){\n  validateVehicle(attacker);")
patch('js/rules/vehicles.mjs',"  const damage=hit&&!nonDamage?Number(weapon.damage??0)+check.netSuccess:0;\n  const criticalAvailable=hit && !nonDamage && !ion && Number.isFinite(Number(weapon.crit)) && (check.netAdvantage>=Number(weapon.crit) || check.triumph>0);", """  if(typeof ignoreMassive!=='boolean'||!Number.isSafeInteger(deadToRightsBonus)||deadToRightsBonus<0||deadToRightsBonus>7)
    throw Error('Opções de dano veicular inválidas.');
  const massiveRating=Number(target.massive??0),massiveIgnored=ignoreMassive?massiveRating:0;
  const criticalCost=Number(weapon.crit)+massiveRating-massiveIgnored;
  const damage=hit&&!nonDamage?Number(weapon.damage??0)+check.netSuccess+deadToRightsBonus:0;
  const criticalAvailable=hit && !nonDamage && !ion && Number.isFinite(Number(weapon.crit)) && (check.netAdvantage>=criticalCost || check.triumph>0);""")
patch('js/rules/vehicles.mjs',"criticalAvailable,criticalCost:weapon.crit,difficulty:", "criticalAvailable,criticalCost,massiveRating,massiveIgnored,deadToRightsBonus:hit?deadToRightsBonus:0,difficulty:")
patch('js/core/controller.mjs',"    resolveVehicleAttack({ weaponIndex = 0, facing = 'proa', rng = Math.random } = {}) {", "    resolveVehicleAttack({ weaponIndex = 0, facing = 'proa', useExhaustPort=false, useDeadToRights=false, rng = Math.random } = {}) {")
patch('js/core/controller.mjs',"        const temp=vehicles_mjs_1.vehicleTemporaryEffects(attacker,{currentRound:vehicles.combat.round});", """        if(typeof useExhaustPort!=='boolean'||typeof useDeadToRights!=='boolean')throw Error('Opções de talentos veiculares inválidas.');
        if(useExhaustPort&&talents_mjs_1.sourceBackedTalentRank(character,'EXHAUST_PORT')<1)
          throw Error('Saída térmica AoR não adquirida.');
        if(useDeadToRights&&talents_mjs_1.sourceBackedTalentRank(character,'DEAD_TO_RIGHTS')<1)
          throw Error('Impacto direto AoR não adquirido.');
        const massiveRating=Number(target.massive??0);
        if(useExhaustPort&&massiveRating<1)throw Error('Saída térmica só tem efeito contra alvo com regra Massive.');
        const requiredDestiny=Number(useExhaustPort)+Number(useDeadToRights);
        if(Number(this.state.group?.destiny?.light??0)<requiredDestiny)throw Error('Pontos de Destino luminosos insuficientes.');
        const improved=talents_mjs_1.sourceBackedTalentRank(character,'DEAD_TO_RIGHTS_IMPROVED')>0;
        const deadToRightsBonus=useDeadToRights?(improved?Number(character.characteristics?.agilidad??0):Math.ceil(Number(character.characteristics?.agilidad??0)/2)):0;
        const temp=vehicles_mjs_1.vehicleTemporaryEffects(attacker,{currentRound:vehicles.combat.round});""")
patch('js/core/controller.mjs',"extraBoost:fireDiscipline>0?1:0 });", "extraBoost:fireDiscipline>0?1:0,ignoreMassive:useExhaustPort,deadToRightsBonus });")
patch('js/core/controller.mjs',"        if(fireDiscipline>0){ attacker.roundEffects.fireDiscipline.remainingAttacks=fireDiscipline-1;", """        if(useExhaustPort||(useDeadToRights&&attack.hit)){
            let group=clone(this.state.group);
            if(useExhaustPort)group=destiny_mjs_1.flipDestiny(group,'light');
            if(useDeadToRights&&attack.hit)group=destiny_mjs_1.flipDestiny(group,'light');
            this.commit('GROUP_UPDATED',{group});
        }
        attack.exhaustPortUsed=useExhaustPort;
        attack.deadToRightsUsed=useDeadToRights&&attack.hit;
        if(fireDiscipline>0){ attacker.roundEffects.fireDiscipline.remainingAttacks=fireDiscipline-1;""")
patch('js/core/controller.mjs',"        boughtInfo:rank('BOUGHT_INFO'),knownSchematic:rank('KNOWN_SCHEMATIC'),fireControl:rank('FIRE_CONTROL'),", "        boughtInfo:rank('BOUGHT_INFO'),knownSchematic:rank('KNOWN_SCHEMATIC'),fireControl:rank('FIRE_CONTROL'),\n        exhaustPort:rank('EXHAUST_PORT'),deadToRights:rank('DEAD_TO_RIGHTS'),deadToRightsImproved:rank('DEAD_TO_RIGHTS_IMPROVED'),")
# UI action: no random massive data. If target lacks Massive, engine refuses spending Destiny.
p=vtt/'r46/final-runtime.js';s=p.read_text()
old="  if(s.fireControl&&s.vehicleCombatActive)button('FOGO CONTROLADO · MANOBRA VEICULAR',()=>call('useAoRFireControl',{}));"
new="""  if(s.fireControl&&s.vehicleCombatActive)button('FOGO CONTROLADO · MANOBRA VEICULAR',()=>call('useAoRFireControl',{}));
  if(s.vehicleCombatActive&&(s.exhaustPort||s.deadToRights)){
    const exhaust=s.exhaustPort?check('Usar Saída térmica: 1 Destino, ignora Massive'):null;
    const direct=s.deadToRights?check('Usar Impacto direto: 1 Destino se acertar'):null;
    button('ATAQUE VEICULAR · TALENTOS AoR',()=>call('resolveVehicleAttack',{useExhaustPort:Boolean(exhaust?.checked),useDeadToRights:Boolean(direct?.checked)}));
  }"""
assert old in s;s=s.replace(old,new);p.write_text(s)
print('P176 Massive/Exhaust Port/Dead to Rights integrated')
