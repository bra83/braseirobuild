from pathlib import Path
root=Path('app/src/main/assets/vtt')
def patch(path, old,new):
 p=root/path
 t=p.read_text()
 if old not in t:raise Exception('MISSING '+str(p)+' '+old[:120])
 p.write_text(t.replace(old,new,1))

patch(Path('js/adventure/jewelOfYavin.mjs'),"import {validateJewelResidenceWingResponse", "import {validateJewelPursuitTravel,jewelPursuitTravelStatus,jewelPursuitGroupEncounter} from './jewelPursuitTravel.mjs';\nimport {validateJewelResidenceWingResponse")
patch(Path('js/adventure/jewelOfYavin.mjs'),"    residenceWingPending: clone(pendingJewelResidenceWingChecks(p)),", "    residenceWingPending: clone(pendingJewelResidenceWingChecks(p)),\n    pursuitTravel: clone(jewelPursuitTravelStatus(world)),")
patch(Path('js/adventure/jewelOfYavin.mjs'),"  // P220: separately prepared property stages", "  // P226: GM-confirmed physically present groups after a real location-travel check.\n  for(const response of validateJewelPursuitTravel(p).arrivals){\n    const encounter=jewelPursuitGroupEncounter(p,response.encounterId);\n    if(encounter)return encounter;\n  }\n  // P220: separately prepared property stages")
patch(Path('js/adventure/jewelOfYavin.mjs'),"  const residence=jewelResidenceWingEncounter(world?.publishedAdventure,id);", "  const pursuit=jewelPursuitGroupEncounter(world?.publishedAdventure,id);\n  if(pursuit&&current?.id===id&&current.available)return pursuit;\n  const residence=jewelResidenceWingEncounter(world?.publishedAdventure,id);")
patch(Path('js/core/controller.mjs'),"import {prepareJewelResidenceWingResponse}", "import {recordJewelPursuitTravelCheck,prepareJewelPursuitGroup} from '../adventure/jewelPursuitTravel.mjs';\nimport {prepareJewelResidenceWingResponse}")
patch(Path('js/core/controller.mjs'),"    /** P225: failed patrol -> confirmed physical Wing Guard -> actual initiative. */", """    /** P226: actual Hard check on each physical change of location, no NPC spawn. */
    resolveJewelPursuitTravelCheck(details={}){
        return this.atomicAction(()=>{
            if(!['engano','sigilo','callejeo'].includes(details.skillKey))throw Error('Perícia não publicada.');
            if(details.gmNewLocationConfirmed!==true)throw Error('Mestre precisa confirmar chegada ao novo local.');
            const check=this.resolveSkillCheck({skillKey:details.skillKey,difficulty:3,rng:details.rng??Math.random});
            const out=recordJewelPursuitTravelCheck(this.state.world,this.state.combat,this.state.player.character,{...details,
                check:{pool:clone(check.pool),faces:clone(check.faces),success:check.success,netSuccess:check.netSuccess,
                    netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair}});
            this.commit('WORLD_UPDATED',{world:out.world});
            return {check,adjudication:out.adjudication,status:out.status,state:clone(this.state)};
        });
    }
    /** P226: only physically confirmed distinct factions enter real combat. */
    beginJewelPursuitGroup(details={}){
        return this.atomicAction(()=>{
            const prepared=prepareJewelPursuitGroup(this.state.world,this.state.combat,details);
            this.commit('WORLD_UPDATED',{world:prepared.world});
            const started=this.beginPublishedAdventureEncounter(prepared.encounterId,
                {adventureId:'jewel-of-yavin',rng:details.rng??Math.random});
            if(!this.state.combat?.active||this.state.combat.publishedAdventureContext?.encounterId!==prepared.encounterId)
                throw Error('Grupo perseguindo não iniciou iniciativa real.');
            return {adjudication:prepared.adjudication,encounter:started.encounter,combat:clone(this.state.combat),state:clone(this.state)};
        });
    }
    /** P225: failed patrol -> confirmed physical Wing Guard -> actual initiative. */""")
patch(Path('js/core/uiBridge.mjs'),"    /** P225: Wing Guard physically confirmed after a failed residence patrol. */", """    /** P226: travel pursuit roll and sequential GM-confirmed physical factions. */
    resolveJewelPursuitTravelCheck(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelPursuitTravelCheck(details??{})));}
        catch(error){return fail('JEWEL_PURSUIT_TRAVEL_CHECK_FAILED',messageOf(error));}
    }
    beginJewelPursuitGroup(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelPursuitGroup(details??{})));}
        catch(error){return fail('JEWEL_PURSUIT_GROUP_FAILED',messageOf(error));}
    }
    /** P225: Wing Guard physically confirmed after a failed residence patrol. */""")
patch(Path('js/core/state.mjs'),"import {validateJewelResidenceWingResponse}", "import {validateJewelPursuitTravel} from '../adventure/jewelPursuitTravel.mjs';\nimport {validateJewelResidenceWingResponse}")
p=root/'js/core/state.mjs';t=p.read_text();t=t.replace('validateJewelResidenceWingResponse(next.world.publishedAdventure,next.world);','validateJewelResidenceWingResponse(next.world.publishedAdventure,next.world);validateJewelPursuitTravel(next.world.publishedAdventure,next.world);');t=t.replace('validateJewelResidenceWingResponse(value.world.publishedAdventure,value.world);','validateJewelResidenceWingResponse(value.world.publishedAdventure,value.world);validateJewelPursuitTravel(value.world.publishedAdventure,value.world);');p.write_text(t)
patch(Path('js/adventure/jewelOfYavinOperations.mjs'),"'JEWEL_RESIDENCE_WING_RESPONSE']);", "'JEWEL_RESIDENCE_WING_RESPONSE','JEWEL_PURSUIT_TRAVEL_CHECK','JEWEL_PURSUIT_GROUP_ARRIVAL']);")
