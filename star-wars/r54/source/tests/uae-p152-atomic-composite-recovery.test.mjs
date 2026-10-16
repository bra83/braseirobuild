/** P152: integrated rollback / one-write gates for multi-event game commands. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

class Repo {
  constructor(id){this.saved=createCampaign(id);this.writes=0;this.fail=false;}
  load(){return structuredClone(this.saved);}
  save(state){if(this.fail)throw Error('P152 simulated storage outage');this.saved=structuredClone(state);this.writes++;}
}
function game(id){const repo=new Repo(`p152:${id}`);const g=new GameController(repo);g.loadStarterCharacter('cael');return {repo,g};}
function oneWrite(name,setup,invoke,verify=()=>{}){
  test(`P152 ${name}: single durable commit and three read-only reopens`,()=>{
    const {repo,g}=game(name);setup?.(g,repo);
    const before=repo.writes;
    const result=invoke(g);verify(result,g);
    assert.equal(repo.writes,before+1,'multi-event action must save exactly once');
    assert.equal(validateState(repo.saved),true);
    const persisted=structuredClone(repo.saved);
    for(let n=0;n<3;n++){const reopened=new GameController(repo);assert.deepEqual(reopened.state,persisted);assert.equal(repo.writes,before+1);}
  });
  test(`P152 ${name}: disk failure rolls back every event`,()=>{
    const {repo,g}=game(`${name}:fail`);setup?.(g,repo);
    const before=structuredClone(repo.saved),writes=repo.writes;
    repo.fail=true;
    assert.throws(()=>invoke(g),/storage outage/);
    assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
  });
}
const rng=()=>0.25;
const opposed=g=>g.resolveOpposedSkillCheck({skillKey:'mecanica',opponentCharacteristic:3,opponentSkill:2,useLightDestiny:true,rng});
const social=g=>g.resolveSocialInfluenceCheck({skillKey:'coaccion',opponentCharacteristic:3,opponentSkill:2,rng});
const assisted=g=>g.resolveAssistedSkillCheck({skillKey:'mecanica',assistantCharacteristic:3,assistantSkill:2,rng});
const competitive=g=>g.resolveCompetitiveSkillCheck({skillKey:'mecanica',opponentCharacteristic:3,opponentSkill:2,rng});
const raw=g=>g.resolveCheck({characteristic:3,skill:2,difficulty:2,rng});
for(const [name,invoke] of Object.entries({social,assisted,competitive,raw}))oneWrite(name,null,invoke);
oneWrite('opposed',g=>g.setGroupDestiny({light:1,dark:0}),opposed,(r,g)=>{assert.equal(g.state.group.destiny.light,0);});
oneWrite('suggested-master-check',(g,repo)=>{g.state.master.suggestedCheck={skillKey:'mecanica',difficulty:2};repo.saved=structuredClone(g.state);},g=>g.resolveSuggestedMasterCheck({rng}),(r,g)=>{assert.ok(r.pool);assert.equal(g.state.master.suggestedCheck,null);});
oneWrite('weapon-attack',null,g=>g.resolveWeaponAttack({weaponIndex:0,rng}));
oneWrite('begin-combat',null,g=>g.beginCombat());
oneWrite('begin-encounter',null,g=>g.beginEncounter({adversaries:[createAdversary('imperial_soldier',{range:'Médio'})],rng}));
oneWrite('environment-start',null,g=>g.applyEnvironmentTurnStart({rng}));
oneWrite('fall',null,g=>g.resolveFall({range:'Curto',skillKey:'atletismo',rng}));
oneWrite('impassable-terrain',g=>g.configureEnvironment({terrain:'impassable'}),g=>g.resolveImpassableTerrain({skillKey:'atletismo',rng}));
oneWrite('combat-turn',g=>g.beginCombat(),g=>g.advancePlayerTurn({rng}));
for(const {id} of publishedAdventureCatalog()){
  test(`P152 ${id}: social adjudication remains atomic inside active published adventure`,()=>{
    const {repo,g}=game(id);
    g.executePublishedAdventureCommand({actionId:`p152:${id}:start`,kind:'start',adventureId:id});
    const before=repo.writes;
    const result=g.resolveSocialInfluenceCheck({skillKey:'coaccion',opponentCharacteristic:3,opponentSkill:2,rng});
    assert.equal(result.type,'social');assert.equal(repo.writes,before+1);
    const checkpoint=structuredClone(repo.saved);
    for(let i=0;i<3;i++)assert.deepEqual(new GameController(repo).state,checkpoint);
    assert.equal(validateState(repo.saved),true);
  });
}
