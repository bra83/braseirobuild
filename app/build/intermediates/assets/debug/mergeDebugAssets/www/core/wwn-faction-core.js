import { RNG_ALGORITHM } from './rng-core.js';
import { rollDiceExpression } from './wwn-combat-core.js';

export const WWN_FACTION_SOURCES = Object.freeze({
  turn: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.2.0 The Faction Turn' }),
  movement: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.3.0 Asset Locations and Movement' }),
  checks: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.4.0 Attribute Checks' }),
  tags: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.5.0 Faction Tags' }),
  actions: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.6.0 Faction Turn Actions' }),
  creation: Object.freeze({ sourceId: 'SRC-WWN-SRD-1', locator: '§6.7.0 Creating Factions' }),
});

const HP_BY_ATTRIBUTE = Object.freeze({ 1:1, 2:2, 3:4, 4:6, 5:9, 6:12, 7:16, 8:20 });
const FACTION_XP_COST = Object.freeze({ 2:2, 3:4, 4:6, 5:9, 6:12, 7:16, 8:20 });
const ATTRIBUTE_KEYS = Object.freeze({ FORCE:'force', CUNNING:'cunning', WEALTH:'wealth' });
const MAGIC_RANK = Object.freeze({ NONE:0, LOW:1, MEDIUM:2, HIGH:3 });

function stat(v, label) { if (!Number.isInteger(v) || v < 1 || v > 8) throw new RangeError(`${label} must be 1..8`); return v; }
function money(v, label='treasure') { if (!Number.isInteger(v) || v < 0) throw new RangeError(`${label} must be a non-negative integer`); return v; }
function assetsOf(faction) {
  if (!faction || typeof faction !== 'object') throw new TypeError('faction required');
  if (Array.isArray(faction.assets)) return faction.assets;
  if (faction.assets && typeof faction.assets === 'object') return Object.values(faction.assets);
  return [];
}
function assetId(asset) { return asset?.assetId ?? asset?.id ?? null; }
function normalizeAttribute(attribute) {
  const key=String(attribute??'').toUpperCase();
  if (!(key in ATTRIBUTE_KEYS)) throw new RangeError('asset attribute must be FORCE, CUNNING, or WEALTH');
  return key;
}
function magicRank(value) {
  const key=String(value??'NONE').toUpperCase();
  if (!(key in MAGIC_RANK)) throw new RangeError('magic must be NONE, LOW, MEDIUM, or HIGH');
  return MAGIC_RANK[key];
}

export function factionIncome({ force, cunning, wealth }) {
  stat(force,'Force'); stat(cunning,'Cunning'); stat(wealth,'Wealth');
  return Math.ceil((wealth / 2) + ((force + cunning) / 4));
}

export function factionMaximumHp({ force, cunning, wealth }) {
  stat(force,'Force'); stat(cunning,'Cunning'); stat(wealth,'Wealth');
  return HP_BY_ATTRIBUTE[force] + HP_BY_ATTRIBUTE[cunning] + HP_BY_ATTRIBUTE[wealth];
}

export function factionMovementMiles({ mobile = false, baseMiles = 100 } = {}) {
  if (!Number.isFinite(baseMiles) || baseMiles <= 0) throw new RangeError('baseMiles must be positive');
  return baseMiles * (mobile ? 2 : 1);
}

function rollPoolHighest(rng, count, streamName) {
  if (!Number.isInteger(count) || count < 1) throw new RangeError('dice count must be >=1');
  const dice=[]; for (let i=0;i<count;i+=1) dice.push(rng.die(10, streamName));
  return { dice, highest: Math.max(...dice.map(d=>d.result)) };
}

export function resolveFactionInitiative({ rng, factionId, streamName = null }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  const die = rng.die(8, streamName ?? `faction/${factionId}/initiative`);
  return Object.freeze({ rule:'WWN_FACTION_INITIATIVE_V1', sourceRefs:[WWN_FACTION_SOURCES.turn], factionId, die, total:die.result });
}

export function resolveFactionAttributeCheck({ rng, attackerAttribute, defenderAttribute, attackerDice = 1, defenderDice = 1, streamName = 'faction-check' }) {
  if (!rng || typeof rng.die !== 'function') throw new TypeError('deterministic rng required');
  stat(attackerAttribute,'attackerAttribute'); stat(defenderAttribute,'defenderAttribute');
  const a=rollPoolHighest(rng,attackerDice,`${streamName}/attacker`); const d=rollPoolHighest(rng,defenderDice,`${streamName}/defender`);
  const attackerTotal=a.highest+attackerAttribute; const defenderTotal=d.highest+defenderAttribute;
  return Object.freeze({ rule:'WWN_FACTION_ATTRIBUTE_CHECK_V1', rngAlgorithm:RNG_ALGORITHM, sourceRefs:[WWN_FACTION_SOURCES.checks], attackerDice:a.dice, defenderDice:d.dice, attackerAttribute, defenderAttribute, attackerTotal, defenderTotal, attackerWins:attackerTotal>defenderTotal, defenderWins:attackerTotal<=defenderTotal });
}

export function resolveFactionAttack({ rng, attackerAttribute, defenderAttribute, attackDamage, counterDamage = null, attackerDice = 1, defenderDice = 1, streamName = 'faction-attack' }) {
  const check=resolveFactionAttributeCheck({rng,attackerAttribute,defenderAttribute,attackerDice,defenderDice,streamName:`${streamName}/check`});
  if(check.attackerWins){
    const roll=rollDiceExpression({rng,expression:attackDamage,streamName:`${streamName}/damage`});
    return Object.freeze({rule:'WWN_FACTION_ATTACK_V1',sourceRefs:[WWN_FACTION_SOURCES.actions],check,outcome:'ATTACK_SUCCESS',damageToDefender:Math.max(0,roll.total),damageToAttacker:0,damageRoll:roll});
  }
  if(counterDamage){
    const roll=rollDiceExpression({rng,expression:counterDamage,streamName:`${streamName}/counter`});
    return Object.freeze({rule:'WWN_FACTION_ATTACK_V1',sourceRefs:[WWN_FACTION_SOURCES.actions],check,outcome:'ATTACK_FAILED_COUNTERATTACK',damageToDefender:0,damageToAttacker:Math.max(0,roll.total),damageRoll:roll});
  }
  return Object.freeze({rule:'WWN_FACTION_ATTACK_V1',sourceRefs:[WWN_FACTION_SOURCES.actions],check,outcome:'ATTACK_FAILED_NO_COUNTER',damageToDefender:0,damageToAttacker:0,damageRoll:null});
}

export function repairAssetAmount({ relevantAttribute, applicationNumber = 1 }) {
  stat(relevantAttribute,'relevantAttribute');
  if (!Number.isInteger(applicationNumber) || applicationNumber < 1) throw new RangeError('applicationNumber must be >=1');
  return Object.freeze({sourceRefs:[WWN_FACTION_SOURCES.actions],treasureCost:applicationNumber,hpRepaired:Math.ceil(relevantAttribute/2)});
}

export function repairFactionAmount({ force, cunning, wealth }) {
  stat(force,'Force'); stat(cunning,'Cunning'); stat(wealth,'Wealth');
  const values=[force,cunning,wealth].sort((a,b)=>a-b);
  return Object.freeze({sourceRefs:[WWN_FACTION_SOURCES.actions],treasureCost:1,hpRepaired:Math.ceil((values[0]+values[2])/2)});
}

export function factionAssetCounts(faction) {
  const counts={FORCE:0,CUNNING:0,WEALTH:0};
  for(const asset of assetsOf(faction)){
    if(!asset || asset.destroyed) continue;
    const key=normalizeAttribute(asset.attribute);
    counts[key]+=1;
  }
  return Object.freeze(counts);
}

export function inspectFactionUpkeep(faction) {
  stat(faction.force,'Force'); stat(faction.cunning,'Cunning'); stat(faction.wealth,'Wealth');
  const treasure=money(faction.treasure??0);
  const assets=assetsOf(faction).filter(a=>a&&!a.destroyed);
  const explicitAssetUpkeep=[];
  let explicitAssetUpkeepTotal=0;
  for(const asset of assets){
    const upkeep=asset.upkeepTreasure??0;
    money(upkeep,`upkeep:${assetId(asset)??'asset'}`);
    if(upkeep>0){explicitAssetUpkeep.push({assetId:assetId(asset),treasure:upkeep});explicitAssetUpkeepTotal+=upkeep;}
  }
  const counts=factionAssetCounts(faction);
  const excessByAttribute={
    FORCE:Math.max(0,counts.FORCE-faction.force),
    CUNNING:Math.max(0,counts.CUNNING-faction.cunning),
    WEALTH:Math.max(0,counts.WEALTH-faction.wealth),
  };
  const excessAssetUpkeep=excessByAttribute.FORCE+excessByAttribute.CUNNING+excessByAttribute.WEALTH;
  const totalTreasureDue=explicitAssetUpkeepTotal+excessAssetUpkeep;
  return Object.freeze({
    rule:'WWN_FACTION_UPKEEP_INSPECTION_V1',sourceRefs:[WWN_FACTION_SOURCES.turn,WWN_FACTION_SOURCES.actions],
    treasure,explicitAssetUpkeep:Object.freeze(explicitAssetUpkeep),explicitAssetUpkeepTotal,
    assetCounts:counts,excessByAttribute:Object.freeze(excessByAttribute),excessAssetUpkeep,totalTreasureDue,
    canPayAll:treasure>=totalTreasureDue,
    unpaidResolutionRequired:treasure<totalTreasureDue,
  });
}

export function factionAttributeXpCost(targetRating) {
  if(!Number.isInteger(targetRating)||targetRating<2||targetRating>8) throw new RangeError('targetRating must be 2..8');
  return Object.freeze({sourceRefs:[WWN_FACTION_SOURCES.creation],targetRating,xpCost:FACTION_XP_COST[targetRating]});
}

export function createAssetEligibility({ faction, asset, locationHasOwnBase }) {
  if(!asset||typeof asset!=='object')return Object.freeze({eligible:false,status:'MISSING_ASSET_DEFINITION'});
  if(!locationHasOwnBase)return Object.freeze({eligible:false,status:'BASE_OF_INFLUENCE_REQUIRED'});
  const attr=normalizeAttribute(asset.attribute); const statKey=ATTRIBUTE_KEYS[attr];
  const minAttribute=asset.minimumAttribute??asset.rating??1; stat(minAttribute,'minimumAttribute');
  const current=stat(faction[statKey],statKey); const cost=money(asset.costTreasure??asset.cost??0,'asset cost');
  const requiredMagic=String(asset.minimumMagic??'NONE').toUpperCase(); const factionMagic=String(faction.magic??'NONE').toUpperCase();
  if(current<minAttribute)return Object.freeze({eligible:false,status:'ATTRIBUTE_TOO_LOW',attribute:attr,current,required:minAttribute});
  if(magicRank(factionMagic)<magicRank(requiredMagic))return Object.freeze({eligible:false,status:'MAGIC_TOO_LOW',current:factionMagic,required:requiredMagic});
  if(money(faction.treasure??0)<cost)return Object.freeze({eligible:false,status:'INSUFFICIENT_TREASURE',cost});
  return Object.freeze({eligible:true,status:'OK',attribute:attr,cost,sourceRefs:[WWN_FACTION_SOURCES.actions]});
}

export function hideAssetEligibility({ faction, asset, rivalBasePresent }) {
  stat(faction.cunning,'Cunning');
  if(faction.cunning<3)return Object.freeze({eligible:false,status:'CUNNING_3_REQUIRED'});
  if(!asset)return Object.freeze({eligible:false,status:'ASSET_REQUIRED'});
  if(rivalBasePresent)return Object.freeze({eligible:false,status:'RIVAL_BASE_BLOCKS_HIDE'});
  if(money(faction.treasure??0)<2)return Object.freeze({eligible:false,status:'INSUFFICIENT_TREASURE',cost:2});
  return Object.freeze({eligible:true,status:'OK',cost:2,sourceRefs:[WWN_FACTION_SOURCES.actions]});
}

export function sellAssetReturn(asset) {
  if(!asset||typeof asset!=='object')throw new TypeError('asset required');
  const cost=money(asset.costTreasure??asset.cost??0,'asset cost');
  const maxHp=asset.maxHp??asset.hpMax??asset.hp;
  const hp=asset.hp;
  const damaged=Number.isFinite(maxHp)&&Number.isFinite(hp)?hp<maxHp:Boolean(asset.damaged);
  return Object.freeze({sourceRefs:[WWN_FACTION_SOURCES.actions],damaged,treasureGained:damaged?0:Math.floor(cost/2)});
}

export function expandInfluenceCost(baseHp) {
  if(!Number.isInteger(baseHp)||baseHp<1)throw new RangeError('baseHp must be a positive integer');
  return Object.freeze({sourceRefs:[WWN_FACTION_SOURCES.actions],baseHp,treasureCost:baseHp});
}

export function factionSpecialResolutionRequirements(faction) {
  const required=[];
  for(const asset of assetsOf(faction)){
    const specials=Array.isArray(asset?.startTurnSpecials)?asset.startTurnSpecials:[];
    for(const special of specials){
      if(!special||special.optional===true)continue;
      required.push(Object.freeze({assetId:assetId(asset),specialId:special.specialId??special.id??null,sourceRef:special.sourceRef??null}));
    }
  }
  return Object.freeze(required);
}
