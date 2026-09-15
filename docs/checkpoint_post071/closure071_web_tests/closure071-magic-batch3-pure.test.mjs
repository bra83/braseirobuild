import test from 'node:test';
import assert from 'node:assert/strict';
import {
  spellEffectCoverageAudit,
  resolveVisitationClementClime,
  resolveScornFettersOfEarth,
  resolveLikeTheStones,
  resolveCalcifyingScourge,
  resolveAugmentMortalVitality,
  resolveEnfeeblingWaveTarget,
  resolveAeolianAuctoritas,
  resolveTumefaction,
  resolvePhobicStormTargets,
  resolveCallOfTheTombTargets,
} from '../core/wwn-spell-effect-runtime.js';

const phys=(...rolls)=>({rolls,forcedSettle:false,seed:'p',physicsMs:100,visualQuality:'HIGH'});

test('spell effect ledger now has 19 closed resolvers and still covers all 83 spells',()=>{
  const a=spellEffectCoverageAudit(); assert.equal(a.status,'OK'); assert.equal(a.spellCount,83); assert.equal(a.closedResolverCount,19); assert.equal(a.adjudicationCount,64);
});

test('Clement Clime preserves 3 allies per level, hour-per-level duration, mundane immunity and magical half/Physical save negation',()=>{
  const r=resolveVisitationClementClime({casterLevel:2,allyIds:['a','b','c','d','e','f']});
  assert.equal(r.maxVisibleAllies,6); assert.equal(r.durationHours,2); assert.equal(r.magicalElementalDamageMultiplier,0.5); assert.equal(r.physicalSaveNegatesMagicalElementalDamage,true); assert.throws(()=>resolveVisitationClementClime({casterLevel:1,allyIds:['a','b','c','d']}),/exceed/);
});

test('Scorn ally mode grants 3D scene movement and gentle descent without inventing coordinates',()=>{
  const r=resolveScornFettersOfEarth({casterLevel:3,mode:'ALLIES',allyIds:['a','b','c'],magicSkill:2});
  assert.equal(r.casterAlsoAffected,true); assert.equal(r.canMoveInThreeDimensions,true); assert.equal(r.gentleDescentAtEnd,true); assert.equal(r.duration,'SCENE');
});

test('Scorn anti-flight mode requires explicit saves and grounds only failed targets with Magic penalty',()=>{
  const r=resolveScornFettersOfEarth({casterLevel:2,mode:'GROUND_FLYERS',magicSkill:2,flyingTargets:[{targetId:'f1',physicalSaveSucceeded:false},{targetId:'f2',physicalSaveSucceeded:true}]});
  assert.equal(r.resolutions[0].physicalSavePenalty,-2); assert.equal(r.resolutions[0].grounded,true); assert.equal(r.resolutions[0].fallEquivalentDistanceMultiplier,0.5); assert.equal(r.resolutions[1].grounded,false);
});

test('Like the Stones exact elemental modes remain distinct',()=>{
  const stone=resolveLikeTheStones({element:'STONE'}),air=resolveLikeTheStones({element:'AIR'}),fire=resolveLikeTheStones({element:'FIRE'}),water=resolveLikeTheStones({element:'WATER'});
  assert.equal(stone.stone.ignoreDamagePerInstance,3); assert.equal(stone.stone.autoStabilizeAtZero,true); assert.equal(air.air.rangedArmorClassBonus,4); assert.equal(air.air.canFlyAtUsualMove,true); assert.equal(fire.fire.startTurnMeleeDamage,'1d6'); assert.equal(fire.fire.immuneToHeatDamage,true); assert.equal(water.water.canPassMouseSizedAperture,true);
});

test('Calcifying Scourge petrifies on failed save and successful creature save loses Move for settled 1d6 rounds',()=>{
  const fail=resolveCalcifyingScourge({targetKind:'CREATURE',physicalSaveSucceeded:false}); assert.equal(fail.petrified,true); assert.equal(fail.duration,'UNTIL_DISPELLED_OR_CASTER_RELEASE');
  const pass=resolveCalcifyingScourge({targetKind:'CREATURE',physicalSaveSucceeded:true,physicalDice:phys(4)}); assert.equal(pass.petrified,false); assert.equal(pass.moveActionLostRounds,4);
  assert.throws(()=>resolveCalcifyingScourge({targetKind:'CREATURE',physicalSaveSucceeded:true,physicalDice:{rolls:[3],forcedSettle:true}}),/forced physical settle/);
});

test('Augment Mortal Vitality is a scene buff with Magic-skill Physical save bonus, auto stabilization, one injury heal, and +1 Strain',()=>{
  const r=resolveAugmentMortalVitality({magicSkill:3}); assert.equal(r.physicalSaveBonus,3); assert.equal(r.autoStabilizeWhenMortallyWounded,true); assert.equal(r.instantHealOneNonMortalInjuryOnce,true); assert.equal(r.systemStrainGained,1);
});

test('Enfeebling Wave has exact save-dependent duration but identical half-move/worse-of-two penalties',()=>{
  const fail=resolveEnfeeblingWaveTarget({living:true,physicalSaveSucceeded:false}); const pass=resolveEnfeeblingWaveTarget({living:true,physicalSaveSucceeded:true});
  assert.equal(fail.duration,'SCENE'); assert.equal(pass.duration,'NEXT_TURN'); assert.equal(fail.moveMultiplier,0.5); assert.equal(pass.attackRollsTwiceTakeWorse,true); assert.equal(pass.damageRollsTwiceTakeWorse,true);
});

test('Aeolian gust only harms failed man-size targets and requires one settled d6 per failed target',()=>{
  const r=resolveAeolianAuctoritas({casterLevel:4,mode:'GUST',targets:[{targetId:'a',manSizeOrSmaller:true,physicalSaveSucceeded:false},{targetId:'b',manSizeOrSmaller:true,physicalSaveSucceeded:true},{targetId:'c',manSizeOrSmaller:false,physicalSaveSucceeded:false}],physicalDiceByTarget:{a:phys(5)}});
  assert.deepEqual(r.resolutions[0],{targetId:'a',affected:true,physicalSaveSucceeded:false,pushFeet:30,loseNextRoundMainAction:true,damage:5}); assert.equal(r.resolutions[1].affected,false); assert.equal(r.resolutions[2].reason,'TOO_LARGE');
});

test('Aeolian weather mode is quarter-mile, hour per level, seasonal-only, and forbids extreme weather',()=>{
  const r=resolveAeolianAuctoritas({casterLevel:5,mode:'WEATHER'}); assert.equal(r.radiusMiles,0.25); assert.equal(r.durationHours,5); assert.equal(r.seasonallyPossibleClimateOnly,true); assert.equal(r.extremeWeatherForbidden,true);
});

test('Tumefaction applies 2 damage per caster level once per round and lower-HD failed saves become indefinite',()=>{
  const low=resolveTumefaction({casterLevel:5,targetHitDice:4,living:true}); const peer=resolveTumefaction({casterLevel:5,targetHitDice:5,living:true}); assert.equal(low.damagePerVigorousAction,10); assert.equal(low.damageMaxTimesPerRound,1); assert.equal(low.failedSaveDuration,'INDEFINITE'); assert.equal(peer.failedSaveDuration,'SCENE');
});

test('Phobic Storm uses explicit Morale outcome, ignores fearless/Morale 12, and never kills on successful check damage',()=>{
  const r=resolvePhobicStormTargets({casterLevel:4,targets:[{targetId:'a',morale:8,moraleCheckSucceeded:false},{targetId:'b',morale:8,moraleCheckSucceeded:true},{targetId:'c',morale:12,moraleCheckSucceeded:false},{targetId:'d',morale:7,fearless:true,moraleCheckSucceeded:false}]});
  assert.equal(r.resolutions[0].flees,true); assert.equal(r.resolutions[0].damage,0); assert.equal(r.resolutions[1].damage,4); assert.equal(r.resolutions[1].zeroHpInsteadBecomesOneAndFlees,true); assert.equal(r.resolutions[2].immune,true); assert.equal(r.resolutions[3].immune,true);
});

test('Call of the Tomb successful save lasts one round; failure lasts caster level rounds and keeps exact auto-hit/max-damage contract',()=>{
  const r=resolveCallOfTheTombTargets({casterLevel:5,currentRound:3,targets:[{targetId:'a',physicalSaveSucceeded:true},{targetId:'b',physicalSaveSucceeded:false}]}); assert.equal(r.resolutions[0].endsAfterRound,3); assert.equal(r.resolutions[1].endsAfterRound,7); assert.equal(r.resolutions[1].attacksAutomaticallyHit,true); assert.equal(r.resolutions[1].maximizeAllDamageDice,true); assert.equal(r.resolutions[1].negateSpecialDefenses,true);
});
