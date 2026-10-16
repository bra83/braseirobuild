import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';

class Repo {
  constructor(){this.state=createCampaign('SWE02 T’rex carryover');this.saves=0;}
  load(){return structuredClone(this.state);}
  save(state){this.state=structuredClone(state);this.saves++;}
}
function fixture(playerCount){
  const repo=new Repo(),g=new GameController(repo);
  g.loadStarterCharacter('cael');g.startSession({playerCount,rng:()=>.5});
  for(let i=1;i<playerCount;i++){const c=structuredClone(g.state.player.character);c.name=`PC ${i+1}`;g.registerPartyCharacter({character:c,id:`test-pc-${i+1}`});}
  g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.5});
  for(const choice of ['dock-ryloth','help-new-meen','travel-lylek-den'])g.advancePublishedAdventure(choice);
  return {repo,g};
}
function ready(g){g.chooseSWE02OldLylekCrew({crewPcIds:g.state.player.roster.entries.map(x=>x.id)});g.chooseSWE02OldLylekRoute({slowDown:true});g.approachSWE02OldLylekOnFoot();g.recordSWE02OldLylekRuse({recognizedByHunters:false,oskaraPresent:false});g.chooseSWE02OldLylekTactics({approach:'frontal',frontalOutcome:'fighting_retreat',lylekBranch:'disturb_on_advantage'});}
function reopenThree(repo,assertState){let last;for(let i=0;i<3;i++){last=new GameController(repo);assertState(last.state);}return last;}

test('T’rex p.24 profile matches the official Adventure Book block',()=>{
  const p=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.trex;
  assert.equal(p.kind,'nemesis');
  assert.deepEqual(p.characteristics,{fortaleza:4,agilidade:3,intelecto:2,astucia:3,voluntad:3,presencia:2});
  assert.deepEqual(p.skills,{pelea:1,frialdad:1,disciplina:1,mecanica:3,negociacao:1,percepcao:2,pilotar_veiculo_planetario:1,armas_distancia_ligeras:2,vigilancia:1});
  assert.equal(p.soak,5);assert.equal(p.woundsThreshold,16);assert.equal(p.strainThreshold,11);
  assert.deepEqual(p.weapons.map(w=>[w.name,w.skill,w.damage,w.crit]),[
    ['Heavy Blaster Pistol','armas_distancia_ligeras',7,3],['Trandoshan Claws','pelea',5,3]
  ]);
  assert.equal(p.source.productCode,'SWE01');assert.equal(p.source.printedPage,24);
});

test('T’rex carryover is explicit, persists on reopen, and replaces one existing hunter slot with only the correct starting wounds',()=>{
  const cases=[
    {history:'escaped',size:2,present:true,preWounds:0},
    {history:'defeated_unconfirmed_dead',size:3,present:true,preWounds:10},
    {history:'confirmed_dead',size:1,present:false,preWounds:0},
    {history:'confirmed_dead',size:2,present:false,preWounds:0},
    {history:'confirmed_dead',size:4,present:false,preWounds:0}
  ];
  for(const c of cases){
    const {repo,g}=fixture(c.size);
    assert.throws(()=>g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5}),/resolva rota/);
    const receipt=g.selectSWE02TrexCarryover({history:c.history});
    assert.equal(receipt.status,'selected');assert.equal(receipt.present,c.present);assert.equal(receipt.preWounds,c.preWounds);
    let reloaded=reopenThree(repo,s=>{
      const carry=s.world.publishedAdventure.oldLylek.trexCarryover;
      assert.equal(carry.status,'selected');assert.equal(carry.history,c.history);assert.equal(carry.present,c.present);assert.equal(carry.preWounds,c.preWounds);
    });
    ready(reloaded);
    reloaded.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
    const enemies=reloaded.state.combat.adversaries;
    assert.equal(enemies.length,c.size,`PC count ${c.size}, history ${c.history}`);
    assert.equal(enemies.filter(x=>x.profileKey==='laoth_gand_bounty_hunter').length,1);
    const trex=enemies.filter(x=>x.profileKey==='trex');
    assert.equal(trex.length,c.present&&c.size>1?1:0);
    if(trex.length){assert.equal(trex[0].wounds.current,c.preWounds);assert.equal(trex[0].wounds.threshold,16);}
    assert.equal(enemies.filter(x=>x.profileKey==='laoth_bounty_hunter').length,c.size-1-trex.length);
    for(const other of enemies.filter(x=>x.profileKey!=='trex'))assert.equal(other.wounds.current,0,'existing hunters receive no T’rex pre-wounds');
    reloaded=reopenThree(repo,s=>{
      const persisted=s.combat.adversaries.filter(x=>x.profileKey==='trex');
      assert.equal(persisted.length,c.present&&c.size>1?1:0);
      if(persisted.length)assert.equal(persisted[0].wounds.current,c.preWounds);
    });
  }
});

test('living T’rex stays pending for a solo or unknown-size party rather than removing the required Gand or adding an extra hunter',()=>{
  const {repo,g}=fixture(1);g.selectSWE02TrexCarryover({history:'escaped'});ready(g);
  assert.throws(()=>g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5}),/composição fica PENDENTE/);
  assert.equal(repo.state.world.publishedAdventure.oldLylek.trexCarryover.present,true);
  assert.equal(repo.state.combat.active,false);
  const world=structuredClone(g.state.world);delete world.publishedAdventure.flags.confirmedPartySize;
  const spec=getPublishedAdventureAdapter('long-arm-of-the-hutt').encounterSpec(world,'lylek-den-bounty-hunters');
  assert.equal(spec.requiresPartySizeConfirmation,true);assert.equal(spec.requiresTrexSlotReview,true);
  assert.equal(spec.adversaries.length,1);assert.equal(spec.adversaries[0].profileKey,'laoth_gand_bounty_hunter');
});

test('invalid and duplicate history selections do not rewrite the source history',()=>{
  const {g}=fixture(2);
  assert.throws(()=>g.selectSWE02TrexCarryover({history:'maybe'}),/resultado válido/);
  g.selectSWE02TrexCarryover({history:'confirmed_dead'});
  assert.throws(()=>g.selectSWE02TrexCarryover({history:'escaped'}),/resultado válido/);
  assert.equal(g.state.world.publishedAdventure.oldLylek.trexCarryover.history,'confirmed_dead');
});

test('Android UI bridge exposes explicit T’rex history persistence',async()=>{
  const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
  assert.equal(typeof BraseiroUiBridge.prototype.selectSWE02TrexCarryover,'function');
});
