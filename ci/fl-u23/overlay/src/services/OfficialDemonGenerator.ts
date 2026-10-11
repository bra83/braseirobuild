import type {EncounterCombatant} from '../types';
/** GM book pp.79–83. Raw D6 faces are retained; no generic monster profile.
 * Ambiguous printed rows are reported, never silently filled or rerolled.
 * Situational abilities/weaknesses are listed for explicit GM adjudication.
 */
export type GeneratedDemonAttack={id:string;kind:'physical'|'fear'|'fire'|'cold';baseDice:number;damage:number;damageType:'slash'|'puncture'|'blunt'|'other';effect?:string};
export type OfficialGeneratedDemon={combatant:EncounterCombatant;attacks:GeneratedDemonAttack[];dice:Array<{label:string;sides:number;value:number}>;situationalRules:string[];sourceGaps:string[]};
export class OfficialDemonGenerator{
 static generate(rng:()=>number=Math.random):OfficialGeneratedDemon{
  const dice:OfficialGeneratedDemon['dice']=[],situationalRules:string[]=[],sourceGaps:string[]=[];
  const d=(label:string,sides=6)=>{const v=rng();if(!Number.isFinite(v)||v<0||v>=1)throw Error('Fonte de dados inválida para o demônio.');const value=1+Math.floor(v*sides);dice.push({label,sides,value});return value;};
  const d66=(label:string)=>10*d(label+':tens')+d(label+':units');
  const forms:Array<[number,number,string,number,number,number]>=[
   [11,24,'humanoid',3,3,0],[25,26,'minotaur',5,3,0],[31,32,'goat',3,4,0],[33,34,'bear',6,3,0],[35,36,'snake',4,4,0],
   [41,42,'undead',3,3,0],[43,44,'spider',5,4,0],[45,46,'serpent',3,4,0],[51,52,'wolf',4,4,0],[53,54,'insect',2,3,0],
   [55,56,'wolfkin',4,4,0],[61,62,'crab',5,2,3],[63,63,'toad',5,2,0],[64,64,'octopus',4,2,0],[65,65,'mist',3,3,-1],[66,66,'shadow',3,3,-1]];
  const formRoll=d66('form');const form=forms.find(([lo,hi])=>formRoll>=lo&&formRoll<=hi);
  if(!form)throw Error('Forma sem linha oficial; confira o D66.');
  let strength=d('strength')+form[3],armor=form[5]<0?0:d('armor')+form[5];
  const sheet:Record<string,unknown>={officialGeneratedDemon:true,sourcePrintedPages:[79,80,81,82,83],form:form[2],formD66:formRoll,standardNpcCombat:true,individualStatisticsGenerated:true,naturalArmor:true};
  if(form[5]<0)sheet.physicalDamageImmune=true;
  if(form[2]==='octopus')situationalRules.push('Running uses a slow action (p.79).');
  const bodies:number[]=[];
  const body=(depth=0)=>{if(depth>12)throw Error('Limite de expansão de habilidades; preserve os dados para adjudicação.');const r=d66('body');
   if(r>=56){body(depth+1);body(depth+1);return;}bodies.push(r);
   if(r<=12)armor+=6;
   else if(r<=14)strength+=d('giant-strength-a')+d('giant-strength-b');
   else if(r<=16)sheet.fireImmune=true;
   else if(r<=22)armor+=2;
   else if(r<=24)sheet.bodyFearDice=d('mummified-fear')+5;
   else if(r<=26)situationalRules.push('Fits through tiny openings (p.80).');
   else if(r<=32)sheet.manipulationBonus=Number(sheet.manipulationBonus||0)+2;
   else if(r<=34){sheet.touchDiseaseVirulence=d('touch-disease')+5;situationalRules.push('Infectious touch: disease check after actual contact (p.80).');}
   else if(r<=36)armor+=2;
   else if(r<=43){sheet.canFly=true;sheet.movement=3;}
   else if(r<=45)sheet.bodyFearDice=d('bright-fear')+5;
   else if(r===46)armor+=3;
   else if(r<=53)sheet.incomingAttackModifier=-3;
   else if(r===54)sheet.extraInitiative='keep_best';
   else if(r===55)sheet.extraInitiative='keep_worst';
  };body();sheet.bodyD66=bodies;
  const attacks:GeneratedDemonAttack[]=[];
  const attack=(depth=0)=>{if(depth>12||attacks.length>32)throw Error('Limite de expansão dos ataques do demônio.');const r=d66('attack');
   if(r>=61){const count=r===66?4:3;for(let i=0;i<count;i++)attack(depth+1);return;}
   if(r>=56){sourceGaps.push(`Attack D66 ${r}: printed table has no row (p.81).`);return;}
   if(r===26){sourceGaps.push('Attack D66 26 overlaps horns/tentacles (p.80); GM must resolve from the printed table.');return;}
   let a:GeneratedDemonAttack;
   if(r<=15||r<=22){const base=d('attack-base')+(r<=15?4:5),damageDie=d('attack-damage');a={id:r<=15?'claws':'fangs',kind:'physical',baseDice:base,damage:damageDie<=2?1:damageDie<=5?2:3,damageType:r<=15?'slash':'puncture'};}
   else if(r<=25){const base=d('attack-base')+6,damageDie=d('attack-damage');a={id:'horns',kind:'physical',baseDice:base,damage:damageDie<=4?2:3,damageType:'puncture'};}
   else if(r<=32){const base=d('attack-base')+4,damageDie=d('attack-damage');if(damageDie>2)sourceGaps.push(`Tentacle damage D6 ${damageDie}: printed 3–2 interval is invalid (p.80).`);a={id:'tentacles',kind:'physical',baseDice:base,damage:1,damageType:'blunt',effect:'grapple'};}
   else if(r<=35){sourceGaps.push('Fire attack weapon damage is not explicit in the table (p.81); GM source adjudication required.');a={id:'fire',kind:'fire',baseDice:d('attack-base')+6,damage:1,damageType:'other',effect:'burning'};}
   else if(r<=41){sourceGaps.push('Cold attack weapon damage is not explicit in the table (p.81); GM source adjudication required.');a={id:'cold',kind:'cold',baseDice:d('attack-base')+5,damage:1,damageType:'other',effect:'cold'};}
   else if(r<=45)a={id:'roar',kind:'fear',baseDice:d('fear-dice',3)+6,damage:0,damageType:'other'};
   else if(r<=51)a={id:'death-gaze',kind:'fear',baseDice:d('fear-dice')+5,damage:0,damageType:'other'};
   else {const base=d('attack-base')+5,weaponDie=d('heavy-weapon');const names=['long-sword','greatsword','warhammer','morning-star','two-handed-axe','trident'];const damage=[2,3,3,2,3,2];a={id:names[weaponDie-1],kind:'physical',baseDice:base,damage:damage[weaponDie-1],damageType:weaponDie===3||weaponDie===4?'blunt':weaponDie===6?'puncture':'slash'};}
   attacks.push({...a,id:`${attacks.length}:${a.id}`});
  };attack();attack();
  const specials:number[]=[];
  const special=(depth=0)=>{if(depth>12)throw Error('Limite de expansão de habilidades especiais.');const r=d66('special');if(r>=65){special(depth+1);special(depth+1);return;}specials.push(r);
   if(r<=14)sheet.immuneToWeapons=true;
   else if(r<=22)sheet.fireImmune=true;
   else if(r<=25)sheet.coldImmune=true;
   else if(r<=32){sheet.extraInitiative='act_twice';situationalRules.push('Acts at both initiative cards (p.81).');}
   else if(r<=34)sheet.physicalDamageImmune=true;
   else if(r<=36)situationalRules.push('Touch can use Puppeteer at power 3 (p.81); resolve actual spell separately.');
   else if(r<=43){sheet.regenerationDieSides=3;situationalRules.push('Recover D3 Strength each round (p.81).');}
   else if(r<=45){sheet.touchParalysisPotency=d('paralysis-potency')+5;situationalRules.push('Actual touch applies paralysis poison (p.82).');}
   else if(r<=51){sheet.touchLethalPotency=d('lethal-potency')+5;situationalRules.push('Actual touch applies lethal poison (p.82).');}
   else if(r<=54)situationalRules.push('Shapeshifting retains a telltale detail (p.82).');
   else if(r<=56)situationalRules.push('Can pass through solid matter (p.82).');
   else if(r<=62)situationalRules.push('Can float up to 10 metres (p.82).');
   else situationalRules.push('May teleport within long range every other round (p.82).');
  };special();sheet.specialD66=specials;
  const weakness=d66('weakness');sheet.weaknessD66=weakness;
  const weaknesses:Array<[number,number,string]>=[[11,13,'water'],[14,15,'fire'],[16,21,'cold'],[22,24,'light'],[25,31,'silver'],[32,33,'gold'],[34,35,'children'],[36,41,'elves'],[42,43,'dwarves'],[44,52,'holy_symbols'],[53,56,'music'],[61,63,'earth'],[64,66,'wood']];
  sheet.weakness=weaknesses.find(([lo,hi])=>weakness>=lo&&weakness<=hi)?.[2];situationalRules.push(`Weakness ${String(sheet.weakness)}: GM confirms actual exposure and resolves the published effect (pp.82–83).`);
  const skills:Record<string,number>={};for(const name of ['Scout','Stealth','Movement','Lore','Insight','Manipulation'])skills[name]=d('skill:'+name)-1;
  skills.Manipulation+=Number(sheet.manipulationBonus||0);
  sheet.skills=skills;sheet.attributes={strength:{current:strength,max:strength},agility:{current:form[4],max:form[4]},wits:{current:3,max:3},empathy:{current:3,max:3}};
  sheet.armor=armor;sheet.generatedAttacks=attacks;sheet.situationalRules=situationalRules;sheet.generatedDice=dice;sheet.sourceGaps=sourceGaps;
  const combatant:EncounterCombatant={id:'monster:grelf:'+crypto.randomUUID(),entityId:'official-generated-grelf',name:'Grelf — demônio gerado',kind:'monster',side:'enemy',initiative:d('initiative',10),currentStrength:strength,maxStrength:strength,armor,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',notes:'Generated from GM book pp.79–83; situational rules require explicit GM confirmation, not automatic fictitious effects.',quickSheet:sheet};
  return{combatant,attacks,dice,situationalRules,sourceGaps};
 }
}
