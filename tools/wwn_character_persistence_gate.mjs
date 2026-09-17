import assert from 'node:assert/strict';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { CharacterCreationService } from '../app/src/main/assets/www/core/character-creation-service.js';

function seedCampaign(persistence,campaignId){
  const state=makeEmptyCampaignState({campaignId,displayName:'Character persistence gate',masterSeed:'wwn-character-persistence-gate'});
  const receipt=persistence.createCampaign({campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:`snapshot:${campaignId}:0`});
  assert.equal(receipt.status,'OK');
}
function assertPersisted(persistence,campaignId,characterId){
  const loaded=persistence.loadCampaign(campaignId,{fresh:true});
  assert.equal(loaded.status,'OK');
  const state=JSON.parse(loaded.stateJson);
  assert.ok(state.charactersState?.[characterId],`missing roster character ${characterId}`);
  assert.equal(state.characterState?.characterId,characterId,'generated character is not active');
  assert.ok(Object.values(state.partyState?.groups??{}).some(g=>(g.memberIds??[]).includes(characterId)),'generated character missing from party');
  return state;
}

{
  const persistence=new MemoryPersistenceAdapter();
  const campaignId='gate:normal';
  const characterId='pc:normal';
  seedCampaign(persistence,campaignId);
  const service=new CharacterCreationService({persistence});
  const result=service.generateRandom({campaignId,characterId,name:'Persistente',goal:'Continuar existindo',setActive:true});
  assert.equal(result.status,'OK');
  assert.equal(result.verified,true);
  const persisted=assertPersisted(persistence,campaignId,characterId);
  assert.equal(result.projectedState.revision,persisted.revision);
}

class ConflictOncePersistence extends MemoryPersistenceAdapter{
  constructor(){super();this.injectConflict=true;}
  commit(request){
    if(this.injectConflict&&request.eventKind==='RANDOM_CHARACTER_CREATED'){
      this.injectConflict=false;
      const loaded=super.loadCampaign(request.campaignId);
      assert.equal(loaded.status,'OK');
      const interleaved=JSON.parse(loaded.stateJson);
      interleaved.revision=loaded.revision+1;
      const side=super.commit({campaignId:request.campaignId,expectedRevision:loaded.revision,eventId:`gate:interleave:r${interleaved.revision}`,eventKind:'GATE_INTERLEAVE',eventPayloadJson:'{}',newStateJson:JSON.stringify(interleaved),rngCountersJson:'{}'});
      assert.equal(side.status,'OK');
    }
    return super.commit(request);
  }
}

{
  const persistence=new ConflictOncePersistence();
  const campaignId='gate:conflict';
  const characterId='pc:conflict';
  seedCampaign(persistence,campaignId);
  const service=new CharacterCreationService({persistence});
  const result=service.generateRandom({campaignId,characterId,name:'Conflito Seguro',goal:'Sobreviver ao conflito de revisão',setActive:true});
  assert.equal(result.status,'OK');
  assert.equal(result.verified,true);
  assertPersisted(persistence,campaignId,characterId);
  const loaded=persistence.loadCampaign(campaignId);
  assert.equal(loaded.revision,2,'retry should commit after one interleaved revision');
}

console.log('WWN_CHARACTER_PERSISTENCE_GATE=PASS');
