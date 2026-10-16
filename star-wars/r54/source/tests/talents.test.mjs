import test from 'node:test';
import assert from 'node:assert/strict';
import { createEoteCoreCharacter, EOTE_CAREERS } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';
import { SKILLS } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { TALENT_TREES,FIRST_ROW_TALENTS,eligibleTalents,eligibleFirstRowTalents,purchaseTalent,purchaseFirstRowTalent,talentRanks,talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

function charFor(career='smuggler',spec='pilot',xp=500){
  const cdef=EOTE_CAREERS[career], sdef=cdef?.specializations?.[spec];
  assert.ok(cdef&&sdef,`carreira/especialização EotE inválida no teste: ${career}/${spec}`);
  const allCareer=new Set([...cdef.skills,...sdef.skills]);
  const nonCareer=Object.keys(SKILLS).filter(k=>!allCareer.has(k)).slice(0,2);
  const c=createEoteCoreCharacter({
    name:'Nira',speciesKey:'human',careerKey:career,specializationKey:spec,
    careerFreeSkills:cdef.skills.slice(0,4),specializationFreeSkills:sdef.skills.slice(0,2),
    speciesChoiceSkills:nonCareer,motivation:'Sobreviver livre na Orla Exterior.',obligationType:'Dívida',partySize:1
  });
  c.xp.available=xp; return c;
}
function pathTo(tree,targetId){
  const starts=tree.nodes.filter(n=>n.row===1).map(n=>n.id); const adj=new Map(tree.nodes.map(n=>[n.id,[]]));
  for(const [a,b] of tree.edges){adj.get(a).push(b);adj.get(b).push(a);} const q=starts.map(id=>[id]); const seen=new Set(starts);
  while(q.length){const path=q.shift(); const id=path.at(-1); if(id===targetId)return path; for(const n of adj.get(id)){if(!seen.has(n)){seen.add(n);q.push([...path,n]);}}}
  return null;
}

test('catálogo EotE preserva 18 especializações e AoR permanece aditivo',()=>{
 const eoteCareerKeys=['bounty_hunter','colonist','explorer','hired_gun','smuggler','technician'];
 assert.deepEqual(Object.keys(TALENT_TREES).slice(0,6),eoteCareerKeys);
 let specs=0,nodes=0,top=0;
 for(const career of eoteCareerKeys) for(const tree of Object.values(TALENT_TREES[career])){
   specs++; nodes+=tree.nodes.length; top+=tree.nodes.filter(n=>n.row===1).length;
   assert.equal(tree.nodes.length,20); assert.equal(tree.nodes.filter(n=>n.row===1).length,4);
   for(const n of tree.nodes){ assert.equal(n.cost,n.row*5); assert.match(n.source.book,/Edge of the Empire/i); }
   const ids=new Set(tree.nodes.map(n=>n.id)); for(const [a,b] of tree.edges){assert.ok(ids.has(a)); assert.ok(ids.has(b)); assert.notEqual(a,b);}
   const adj=new Map(tree.nodes.map(n=>[n.id,[]])); for(const [a,b] of tree.edges){adj.get(a).push(b);adj.get(b).push(a);} const seen=new Set(tree.nodes.filter(n=>n.row===1).map(n=>n.id)); const q=[...seen]; while(q.length){const id=q.shift(); for(const n of adj.get(id)) if(!seen.has(n)){seen.add(n);q.push(n);}} assert.equal(seen.size,20);
 }
 assert.equal(specs,18); assert.equal(nodes,360); assert.equal(top,72);
 assert.ok(['ace','commander','diplomat','engineer','soldier','spy'].every((career)=>TALENT_TREES[career]));
});

test('API de primeira linha preserva árvores Core e adiciona AoR e Exilado',()=>{
 assert.equal(Object.keys(FIRST_ROW_TALENTS).length,13); let nodes=0;
 for(const [careerKey,career] of Object.entries(FIRST_ROW_TALENTS)) for(const list of Object.values(career)){nodes+=list.length; assert.equal(list.length,4); assert.ok(list.every(t=>t.row===1&&t.cost===5));}
 const coreNodes=Object.entries(FIRST_ROW_TALENTS).filter(([k])=>k!=='universal').reduce((sum,[,career])=>sum+Object.values(career).reduce((n,list)=>n+list.length,0),0);
 assert.equal(coreNodes,144); assert.equal(FIRST_ROW_TALENTS.universal.force_sensitive_exile.length,4); assert.equal(nodes,148);
});

test('somente primeira linha inicia desbloqueada; conexão adquirida libera nó seguinte',()=>{
 let c=charFor(); let list=eligibleTalents(c); assert.equal(list.filter(n=>n.unlocked).length,4); assert.equal(list.filter(n=>n.available).length,4);
 const first=list.find(n=>n.id==='smuggler:pilot:r1c1'); const deeper=list.find(n=>n.id==='smuggler:pilot:r2c1'); assert.equal(deeper.unlocked,false);
 c=purchaseTalent(c,first.id).character; list=eligibleTalents(c); assert.equal(list.find(n=>n.id===deeper.id).unlocked,true); assert.equal(list.find(n=>n.id===deeper.id).available,true);
});

test('talento profundo desconectado não pode ser comprado diretamente',()=>{
 const c=charFor(); assert.throws(()=>purchaseTalent(c,'smuggler:pilot:r5c4'),/ainda não está conectado/i);
});

test('compra desconta o custo da linha e um mesmo espaço só é comprado uma vez',()=>{
 let c=charFor(); const before=c.xp.available; c=purchaseTalent(c,'smuggler:pilot:r1c1').character; assert.equal(c.xp.available,before-5);
 assert.throws(()=>purchaseTalent(c,'smuggler:pilot:r1c1'),/já foi adquirido/i);
 c=purchaseTalent(c,'smuggler:pilot:r2c1').character; assert.equal(c.xp.available,before-15);
});

test('talento repetido em espaços diferentes soma graus',()=>{
 let c=charFor(); c=purchaseTalent(c,'smuggler:pilot:r1c2').character; c=purchaseTalent(c,'smuggler:pilot:r1c1').character; c=purchaseTalent(c,'smuggler:pilot:r2c1').character;
 assert.equal(talentRankByOriginal(c,'SKILLJOCK'),2); assert.equal(talentRanks(c)['Piloto consumado'],2);
});

test('Dedicação exige escolha, respeita máximo 6 e atualiza derivados ligados',()=>{
 const tree=TALENT_TREES.smuggler.pilot, target=tree.nodes.find(n=>n.originalName==='DEDI'); const path=pathTo(tree,target.id); assert.ok(path&&path.length>1);
 let c=charFor(); for(const id of path.slice(0,-1)) c=purchaseTalent(c,id).character;
 assert.throws(()=>purchaseTalent(c,target.id),/exige escolher/i);
 const beforeW=c.derived.wounds.threshold,beforeS=c.derived.soak,beforeF=c.characteristics.fortaleza;
 c=purchaseTalent(c,target.id,{characteristicKey:'fortaleza'}).character;
 assert.equal(c.characteristics.fortaleza,beforeF+1); assert.equal(c.derived.wounds.threshold,beforeW+1); assert.equal(c.derived.soak,beforeS+1);
 let c2=charFor(); c2.characteristics.agilidad=6; for(const id of path.slice(0,-1)) c2=purchaseTalent(c2,id).character;
 assert.throws(()=>purchaseTalent(c2,target.id,{characteristicKey:'agilidad'}),/acima de 6/i);
});

test('Firmeza, Endurecido e Resistente aplicam somente seus efeitos passivos EotE',()=>{
 const cases=[['colonist','doctor','GRIT','strain',1],['hired_gun','marauder','TOUGH','wounds',2],['hired_gun','marauder','ENDUR','soak',1]];
 for(const [career,spec,key,kind,delta] of cases){ let c=charFor(career,spec); const tree=TALENT_TREES[career][spec]; const node=tree.nodes.find(n=>n.originalName===key); assert.ok(node,`${key} precisa existir no caso de teste`); const path=pathTo(tree,node.id); assert.ok(path); for(const id of path.slice(0,-1)) c=purchaseTalent(c,id).character; const before=kind==='soak'?c.derived.soak:c.derived[kind].threshold; c=purchaseTalent(c,node.id).character; const after=kind==='soak'?c.derived.soak:c.derived[kind].threshold; assert.equal(after,before+delta); }
});

test('API compatível de primeira linha continua restrita à linha 1',()=>{
 const c=charFor(); const list=eligibleFirstRowTalents(c); assert.equal(list.length,4); const r=purchaseFirstRowTalent(c,list[0].id); assert.equal(r.character.talentPurchases.length,1); assert.throws(()=>purchaseFirstRowTalent(c,'smuggler:pilot:r2c1'),/primeira linha/i);
});

test('talento de outra especialização não pode ser comprado por ID',()=>{ const c=charFor(); const foreign=FIRST_ROW_TALENTS.technician.slicer[0].id; assert.throws(()=>purchaseTalent(c,foreign),/não pertence/i); });
