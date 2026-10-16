from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
vtt=root/'app/src/main/assets/vtt'
def patch(rel,old,new,count=1):
 p=vtt/rel
 s=p.read_text()
 if s.count(old)!=count: raise ValueError(f'{rel}: expected {count} occurrences of {old[:80]!r}, got {s.count(old)}')
 p.write_text(s.replace(old,new))

# Separate Skilled Slicer from Master Slicer: older checkpoint conflated both.
patch('js/expansion/aorTalentRuntimeAliases.mjs',"'PIRATA INFORMÁTICO CONSUMADO': 'MASSLIC',", "'PIRATA INFORMÁTICO CONSUMADO': 'SKILSLI',")
patch('js/expansion/aorTalentRuntimeAliases.mjs',"  // P174: source-backed, once/session Mechanic equipment actions.", "  // P176: independent AoR Core actions with real consumers.\n  'PIRATA INFORMÁTICO EXPERTO': 'MASSLIC',\n  'FUEGO CONTROLADO': 'FIRE_CONTROL',\n  'DISEÑO FAMILIAR': 'KNOWN_SCHEMATIC',\n  'COMPRAR INFORMACIÓN': 'BOUGHT_INFO',\n  // P174: source-backed, once/session Mechanic equipment actions.")

intel=vtt/'js/rules/aorIntelActions.mjs'
intel.write_text('''/** P176 — AoR Core Known Schematic / Bought Info. No invented map/intelligence.
 * Source: AoR Core p.143 and Known Schematic (FFG). The Master must confirm
 * access to purchasable information and reveal actual layout facts.
 */
export const safeIntelId=v=>typeof v==='string'&&/^[A-Za-z0-9:_-]{1,120}$/.test(v);
export const KNOWLEDGE_SKILLS=Object.freeze(['bajos_fondos','borde_exterior','educacion','estrategia_militar','mundos_del_nucleo','saber_arcaico','xenologia']);
export function validateAoRIntelLedger(state){
 const list=state.master?.aorIntelActions??[];
 if(!Array.isArray(list)||list.length>128)throw Error('Histórico AoR de informações inválido.');
 const ids=new Set(),uses=new Set();
 for(const r of list){
  if(!r||!safeIntelId(r.actionId)||ids.has(r.actionId)||!['schematic','bought'].includes(r.kind)||
    !safeIntelId(r.subjectId)||!safeIntelId(r.sessionId)||r.campaignId!==state.campaignId||
    !safeIntelId(r.campaignId)||!safeIntelId(r.characterName)||r.approved!==true||
    !Number.isSafeInteger(r.difficulty)||r.difficulty<1||r.difficulty>5||
    !Number.isSafeInteger(r.creditsSpent)||r.creditsSpent<0||
    !Number.isSafeInteger(r.netSuccess)||r.netSuccess<0||
    typeof r.success!=='boolean'||
    (r.kind==='schematic'&&(r.difficulty!==3||r.creditsSpent!==0||r.skillKey!=='educacion'||r.success!==(r.netSuccess>0)))||
    (r.kind==='bought'&&(!KNOWLEDGE_SKILLS.includes(r.skillKey)||r.creditsSpent!==r.difficulty*50||r.netSuccess!==1||!r.success)))
    throw Error('Recibo AoR de informação adulterado.');
  ids.add(r.actionId);
  if(r.kind==='schematic'){
    const useKey=r.sessionId+'\\0'+r.characterName;
    if(uses.has(useKey))throw Error('Projeto conhecido usado mais de uma vez por sessão.');
    uses.add(useKey);
  }
  const matching=(state.events??[]).filter(e=>e.type==='AOR_INTEL_ACTION'&&e.payload?.receipt?.actionId===r.actionId);
  if(matching.length!==1||JSON.stringify(matching[0].payload.receipt)!==JSON.stringify(r))
    throw Error('Ação AoR de informação sem recibo causal.');
 }
 return true;
}
''')
patch('js/core/events.mjs',"  if (event.type === 'AOR_MECHANIC_ACTION') {", "  if (event.type === 'AOR_INTEL_ACTION') {\n    next.master.aorIntelActions=[...(next.master.aorIntelActions??[]),structuredClone(event.payload.receipt)];\n  }\n  if (event.type === 'AOR_MECHANIC_ACTION') {")
patch('js/core/state.mjs',"import { validateMechanicFieldLedger }", "import { validateAoRIntelLedger } from '../rules/aorIntelActions.mjs';\nimport { validateMechanicFieldLedger }")
patch('js/core/state.mjs',"mechanicFieldActions: [], pendingEncounter:", "mechanicFieldActions: [], aorIntelActions: [], pendingEncounter:",2)
patch('js/core/state.mjs',"    next.master.mechanicFieldActions=next.master.mechanicFieldActions??[];", "    next.master.mechanicFieldActions=next.master.mechanicFieldActions??[];\n    next.master.aorIntelActions=next.master.aorIntelActions??[];")
patch('js/core/state.mjs',"    validateMechanicFieldLedger(value);", "    validateMechanicFieldLedger(value);\n    validateAoRIntelLedger(value);")
patch('js/core/state.mjs',"    const evasion=value.vehicles?.combat?.brilliantEvasion;", """    const fire=value.vehicles?.combat?.aorFireControl;
    if(fire!==undefined&&fire!==null){
        const v=value.vehicles?.combat;
        if(!v.active||!Number.isSafeInteger(fire.round)||fire.round<1||
           fire.round!==v.round||fire.sessionId!==value.sessionId||
           fire.vehicleId!==value.vehicles?.activeVehicleId||
           value.vehicles?.vehicles?.[fire.vehicleId]?.roundEffects?.controlledFire!==true)
          throw Error('Fogo controlado AoR inválido.');
    }
    const slicer=value.combat?.aorSkilledSlicer;
    if(slicer!==undefined&&slicer!==null){
        if(!value.combat.active||!safeSlicerId(slicer.systemId)||
           slicer.sessionId!==value.sessionId||
           !Number.isSafeInteger(slicer.activatedRound)||slicer.activatedRound<1)
          throw Error('Controle de sistema AoR inválido.');
    }
    const evasion=value.vehicles?.combat?.brilliantEvasion;""")
patch('js/core/state.mjs',"export const STATE_VERSION = 14;", "const safeSlicerId=v=>typeof v==='string'&&/^[A-Za-z0-9:_-]{1,120}$/.test(v);\nexport const STATE_VERSION = 14;")

# Controller: source-backed transactional commands.
patch('js/core/controller.mjs',"import { mechanicFieldPreflight", "import { safeIntelId, KNOWLEDGE_SKILLS } from '../rules/aorIntelActions.mjs';\nimport { mechanicFieldPreflight")
anchor="    talentActionStatus() {"
methods='''    /** P176 — Bought Info: 50 credits per difficulty, exactly one success;
     * never invent the actual answer. Source and access require GM approval. */
    useAoRBoughtInfo({actionId,subjectId,skillKey='educacion',difficulty=2,approved=false}={}){
      return this.atomicAction(()=>{
        if(!safeIntelId(actionId)||!safeIntelId(subjectId)||!KNOWLEDGE_SKILLS.includes(skillKey)||
           !Number.isSafeInteger(difficulty)||difficulty<1||difficulty>5||approved!==true)
          throw Error('Compra de informação exige identificadores, dificuldade e aprovação do Mestre.');
        const c=requireCharacter(this.state.player.character);
        if(talents_mjs_1.sourceBackedTalentRank(c,'BOUGHT_INFO')<1)throw Error('Comprar informação AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||this.state.session?.status!=='ativa')
          throw Error('Compra de informação exige sessão AoR ativa.');
        const old=(this.state.master.aorIntelActions??[]).find(r=>r.actionId===actionId);
        if(old){if(old.kind!=='bought'||old.subjectId!==subjectId||old.skillKey!==skillKey||old.difficulty!==difficulty)throw Error('Recibo de informação repetido com parâmetros divergentes.');return {duplicate:true,receipt:clone(old)};}
        const cost=50*difficulty;
        if(!Number.isSafeInteger(c.credits)||c.credits<cost)throw Error('Créditos insuficientes para comprar informação.');
        let combat=clone(this.state.combat);
        if(combat.active){assertCanPerformAction(c);assertPlayerInitiativeTurn(combat);combat=combat_mjs_1.spendAction(combat,'Comprar informação');}
        const character=clone(c);character.credits-=cost;
        const receipt={actionId,kind:'bought',subjectId,skillKey,difficulty,approved:true,
          campaignId:this.state.campaignId,sessionId:this.state.sessionId,characterName:c.name,
          creditsSpent:cost,netSuccess:1,success:true,gmRevelationRequired:true};
        if(combat.active)this.commit('COMBAT_ACTION',{combat,character});
        else this.commit('CHARACTER_UPDATED',{character});
        this.commit('AOR_INTEL_ACTION',{receipt});
        return {receipt,creditsRemaining:character.credits,success:true,netSuccess:1,gmRevelationRequired:true};
      });
    }
    /** Known Schematic: Hard Education check as maneuver, once per session.
     * Only a real Master-confirmed building/ship is targetable. */
    useAoRKnownSchematic({actionId,subjectId,approved=false,maneuverMethod='normal',rng=Math.random}={}){
      return this.atomicAction(()=>{
        if(!safeIntelId(actionId)||!safeIntelId(subjectId)||approved!==true)
          throw Error('Projeto conhecido exige alvo e aprovação do Mestre.');
        let c=requireCharacter(this.state.player.character);
        if(talents_mjs_1.sourceBackedTalentRank(c,'KNOWN_SCHEMATIC')<1)throw Error('Projeto conhecido AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||this.state.session?.status!=='ativa')
          throw Error('Projeto conhecido exige sessão AoR ativa.');
        const list=this.state.master.aorIntelActions??[];
        const old=list.find(r=>r.actionId===actionId);
        if(old){if(old.kind!=='schematic'||old.subjectId!==subjectId)throw Error('Recibo de projeto repetido com parâmetros divergentes.');return {duplicate:true,receipt:clone(old)};}
        if(list.some(r=>r.kind==='schematic'&&r.sessionId===this.state.sessionId&&r.characterName===c.name))
          throw Error('Projeto conhecido já utilizado nesta sessão.');
        let combat=clone(this.state.combat);
        if(combat.active){const spent=spendPlayerManeuver(c,combat,{method:maneuverMethod,label:'Projeto conhecido'});c=spent.character;combat=spent.combat;}
        const positive=narrativeDice_mjs_1.buildPositivePool(Number(c.characteristics?.intelecto??0),Number(c.skills?.educacion??0));
        const check=narrativeDice_mjs_1.rollPool({...positive,difficulty:3,challenge:0,boost:0,setback:0},rng);
        const receipt={actionId,kind:'schematic',subjectId,skillKey:'educacion',difficulty:3,approved:true,
          campaignId:this.state.campaignId,sessionId:this.state.sessionId,characterName:c.name,
          creditsSpent:0,netSuccess:Math.max(0,Number(check.netSuccess??0)),success:Boolean(check.success),gmRevelationRequired:Boolean(check.success)};
        if(combat.active)this.commit('COMBAT_ACTION',{combat,character:c,check});
        else this.commit('CHARACTER_UPDATED',{character:c});
        this.commit('AOR_INTEL_ACTION',{receipt});
        this.commit('SESSION_CHECK_RESOLVED',{session:sessionFlow_mjs_1.noteSessionCheck(this.state.session)});
        return {receipt,check,success:check.success,gmRevelationRequired:check.success};
      });
    }
    /** AoR Fire Control: a maneuver, no stacking, current vehicle round only. */
    useAoRFireControl({method='normal'}={}){
      return this.atomicAction(()=>{
        const c=requireCharacter(this.state.player.character),vehicles=clone(this.state.vehicles);
        if(talents_mjs_1.sourceBackedTalentRank(c,'FIRE_CONTROL')<1)throw Error('Fogo controlado AoR não adquirido.');
        if(!['AOR','MIXED'].includes(this.state.campaignMode)||!vehicles.combat.active||this.state.session?.status!=='ativa')
          throw Error('Fogo controlado exige combate veicular AoR ativo.');
        const active=vehicles.vehicles?.[vehicles.activeVehicleId];
        if(!active||vehicles_mjs_1.vehicleStatus(active,{currentRound:vehicles.combat.round}).noManeuvers)
          throw Error('Nave ausente ou impossibilitada de realizar manobras.');
        if(vehicles.combat.aorFireControl?.round===vehicles.combat.round)
          throw Error('Fogo controlado não acumula na mesma rodada.');
        const used=Number(vehicles.combat.maneuversUsed??0);
        if(used>=2)throw Error('Limite de duas manobras veiculares atingido.');
        if(used===1){
          if(method==='strain'){
            if(!vehicles_mjs_1.canVoluntarilyInflictVehicleStrain(active))throw Error('Não é possível sofrer Tensão de sistemas.');
            active.systemStrain+=2;
          }else if(method==='action'){
            if(vehicles.combat.actionUsed)throw Error('Ação veicular já usada.');
            vehicles.combat.actionUsed=true;
          }else throw Error('Segunda manobra exige ação ou 2 de Tensão de sistemas.');
        }
        vehicles.combat.maneuversUsed=used+1;
        vehicles.vehicles[active.id]=vehicles_mjs_1.setControlledFire(active,true);
        const receipt={vehicleId:active.id,round:vehicles.combat.round,sessionId:this.state.sessionId};
        vehicles.combat.aorFireControl=receipt;
        vehicles.combat.log.push({type:'AOR_FOGO_CONTROLADO',...receipt});
        this.commit('VEHICLES_UPDATED',{vehicles});
        return {receipt,bonusTargetSilhouette:1,expiresAtNextRound:true};
      });
    }
'''
patch('js/core/controller.mjs',anchor,methods+anchor)
# Clear fire receipt at encounter and round boundaries.
patch('js/core/controller.mjs',"vehicles.combat = { ...vehicles.combat, brilliantEvasionAttempted:false, brilliantEvasion:null, active: true,", "vehicles.combat = { ...vehicles.combat, aorFireControl:null, brilliantEvasionAttempted:false, brilliantEvasion:null, active: true,")
patch('js/core/controller.mjs',"vehicles.combat = { ...vehicles.combat, brilliantEvasionAttempted:false, brilliantEvasion:null, active: false,", "vehicles.combat = { ...vehicles.combat, aorFireControl:null, brilliantEvasionAttempted:false, brilliantEvasion:null, active: false,")
patch('js/core/controller.mjs',"        vehicles.combat.round = endingRound + 1;", "        vehicles.combat.round = endingRound + 1;\n        vehicles.combat.aorFireControl = null;")

# Skilled Slicer: grant control on triumph, then Computers maneuvers in same system.
patch('js/core/controller.mjs',"spendSmoothTalkerTriumph = false, socialTalentManeuver", "spendSmoothTalkerTriumph = false, spendSkilledSlicerTriumph = false, slicingSystemId = null, useSkilledSlicerManeuver = false, skilledSlicerManeuverMethod = 'normal', socialTalentManeuver")
patch('js/core/controller.mjs',"        if(useCleverSolution){", """        if(spendSkilledSlicerTriumph||useSkilledSlicerManeuver){
            if(skillKey!=='computadoras'||!safeIntelId(slicingSystemId)||
               talents_mjs_1.sourceBackedTalentRank(character,'SKILSLI')<1)
              throw Error('Slicer consumado exige Computadores, sistema válido e talento comprado.');
        }
        if(useSkilledSlicerManeuver&&(!this.state.combat.active||
           this.state.combat.aorSkilledSlicer?.systemId!==slicingSystemId||
           this.state.combat.aorSkilledSlicer?.sessionId!==this.state.sessionId))
          throw Error('Manobra Slicer exige controle previamente obtido por Triunfo no mesmo sistema.');
        if(useCleverSolution){""",1)
patch('js/core/controller.mjs',"        }else if(combat.active) combat = clearPlayerAim(combat_mjs_1.spendAction(combat, `Usar ${skillKey}`));", """        }else if(useSkilledSlicerManeuver){
            const spent=spendPlayerManeuver(character,combat,{method:skilledSlicerManeuverMethod,label:'Slicer consumado'});
            character=spent.character;combat=clearPlayerAim(spent.combat);
        }else if(combat.active) combat = clearPlayerAim(combat_mjs_1.spendAction(combat, `Usar ${skillKey}`));""")
patch('js/core/controller.mjs',"        if(spendKnowledgeTriumph&&spendSmoothTalkerTriumph)throw new Error('Não é possível gastar o mesmo Triunfo duas vezes.');", """        if([spendKnowledgeTriumph,spendSmoothTalkerTriumph,spendSkilledSlicerTriumph].filter(Boolean).length>1)
          throw new Error('Não é possível gastar o mesmo Triunfo duas vezes.');""")
patch('js/core/controller.mjs',"        const check = {\n            ...resolvedRoll,", """        const slicerSpent=spendSkilledSlicerTriumph&&Number(resolvedRoll.triumph??0)>0;
        if(slicerSpent){
          if(!combat.active)throw Error('Slicer consumado exige encontro ativo para converter testes em manobras.');
          combat=clone(combat);
          combat.aorSkilledSlicer={systemId:slicingSystemId,sessionId:this.state.sessionId,activatedRound:combat.round};
        }
        const check = {
            ...resolvedRoll,""")
patch('js/core/controller.mjs',"remainingTriumph:Math.max(0,Number(resolvedRoll.triumph??0)-(knowledgeSpent?1:0)-(smoothSpent?1:0)),", "remainingTriumph:Math.max(0,Number(resolvedRoll.triumph??0)-(knowledgeSpent?1:0)-(smoothSpent?1:0)-(slicerSpent?1:0)),\n            skilledSlicer:spendSkilledSlicerTriumph?{systemId:slicingSystemId,triumphSpent:slicerSpent?1:0,controlActive:slicerSpent}:null,\n            skilledSlicerManeuver:useSkilledSlicerManeuver,")

# Add bridges; all callable from Android.
bridge='''    useAoRBoughtInfo(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRBoughtInfo(options??{})));}catch(e){return fail('AOR_BOUGHT_INFO_FAILED',messageOf(e));}}
    useAoRKnownSchematic(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRKnownSchematic(options??{})));}catch(e){return fail('AOR_KNOWN_SCHEMATIC_FAILED',messageOf(e));}}
    useAoRFireControl(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRFireControl(options??{})));}catch(e){return fail('AOR_FIRE_CONTROL_FAILED',messageOf(e));}}
'''
patch('js/core/uiBridge.mjs',"    aorCommandStatus(){",bridge+"    aorCommandStatus(){")

# Versioning and UI status: P176 panel will be appended in a separate operation.
gradle=root/'app/build.gradle.kts'
g=gradle.read_text()
assert 'versionCode = 10175' in g and 'versionName = "1.0.175"' in g
gradle.write_text(g.replace('versionCode = 10175','versionCode = 10176').replace('versionName = "1.0.175"','versionName = "1.0.176"'))
print('P176 core source changes written')
