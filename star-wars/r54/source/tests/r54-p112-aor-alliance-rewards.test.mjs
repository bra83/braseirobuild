import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createBlankCharacter } from '../app/src/main/assets/vtt/js/rules/character.mjs';

class Repo{constructor(s=createCampaign('P112')){this.state=structuredClone(s)}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function game(){const g=new GameController(new Repo());g.setCampaignMode('AOR');const c=createBlankCharacter('Rebelde');c.derived={soak:2,wounds:{threshold:12,current:0},strain:{threshold:12,current:0},defense:{ranged:0,melee:0}};g.setCharacter(c);const group=g.addGroupDuty({id:'d',value:100,member:'Rebelde',type:'Apoio'});return g}

test('P112: marco de Contribution cria recompensa pendente com raridade máxima 3 + rank',()=>{const g=game();const r=g.resolveContributionMilestone();assert.equal(r.contributionRank,1);assert.equal(r.reward.maxRarity,4);assert.equal(g.state.group.pendingAllianceReward.maxRarity,4);assert.equal(g.groupSummary().dutyTotal,0)});

test('P112: recompensa de equipamento respeita raridade e entra na posse mecânica sem custo',()=>{const g=game();g.resolveContributionMilestone();const before=g.state.player.character.credits;const r=g.claimAllianceReward({kind:'equipment',itemId:'blaster_pistol'});assert.equal(r.asset.catalogId,'blaster_pistol');assert.equal(g.state.player.character.credits,before);assert.ok(g.state.player.character.weapons.some(w=>w.sourceCatalogId==='blaster_pistol'));assert.equal(g.state.group.pendingAllianceReward,undefined);assert.equal(g.state.group.allianceRewardHistory.at(-1).kind,'equipment')});

test('P112: item acima do limite de raridade é rejeitado',()=>{const g=game();g.resolveContributionMilestone();assert.throws(()=>g.claimAllianceReward({kind:'equipment',itemId:'heavy_blaster_pistol'}),/excede o limite/i);assert.ok(g.state.group.pendingAllianceReward)});

test('P112: veículo dentro do limite entra no runtime e registro de ativos',()=>{const g=game();g.resolveContributionMilestone();const r=g.claimAllianceReward({kind:'vehicle',itemId:'x34'});assert.equal(r.asset.catalogId,'x34');assert.ok(g.state.group.ownedAssets.some(a=>a.catalogId==='x34'));assert.ok(Object.values(g.state.vehicles?.vehicles??{}).some(v=>v.profileKey==='x34'||v.name?.includes('X-34')))});

test('P112: ativo estratégico é persistido como recurso da base',()=>{const g=game();g.resolveContributionMilestone();const r=g.claimAllianceReward({kind:'strategic',name:'Célula de Inteligência Bothan'});assert.equal(r.reward.kind,'strategic');assert.ok(g.state.group.resources.some(x=>x.name==='Célula de Inteligência Bothan'))});

test('P112: opções expõem somente ativos dentro do limite do Contribution atual',()=>{const g=game();g.resolveContributionMilestone();const o=g.allianceRewardOptions();assert.equal(o.maxRarity,4);assert.ok(o.equipment.some(x=>x.id==='blaster_pistol'));assert.ok(!o.equipment.some(x=>x.id==='heavy_blaster_pistol'));assert.ok(o.vehicles.some(x=>x.id==='x34'))});
