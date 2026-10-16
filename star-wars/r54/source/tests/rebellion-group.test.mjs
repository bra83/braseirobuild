import test from 'node:test';
import assert from 'node:assert/strict';
import { createRebelGroup,updateBase,setContributionRank,setDestinyReserve,setTextList,addDutyEntry,removeDutyEntry,groupDutyTotal,validateRebelGroup } from '../app/src/main/assets/vtt/js/rules/rebellionGroup.mjs';

test('folha de grupo persiste base, recursos, contatos e reserva sem automatizar progressão',()=>{
 let g=createRebelGroup();
 g=updateBase(g,{name:'Eco',location:'Dantooine',description:'Posto rebelde oculto.'});
 g=setContributionRank(g,3); g=setDestinyReserve(g,{light:2,dark:1});
 g=setTextList(g,'resources',['Célula de inteligência','Hangar']); g=setTextList(g,'belongings',['Ala-X']); g=setTextList(g,'contacts',['Capitã Varo']);
 assert.equal(validateRebelGroup(g),true); assert.equal(g.base.location,'Dantooine'); assert.equal(g.contributionRank,3); assert.deepEqual(g.destiny,{light:2,dark:1});
});

test('Dever do grupo soma somente entradas registradas e pode ser removido',()=>{
 let g=createRebelGroup(); g=addDutyEntry(g,{id:'a',value:10,member:'Nira',type:'Superioridade espacial'}); g=addDutyEntry(g,{id:'b',value:5,member:'RX',type:'Aquisição tecnológica'});
 assert.equal(groupDutyTotal(g),15); g=removeDutyEntry(g,'a'); assert.equal(groupDutyTotal(g),5);
});

test('faixa de contribuição e reserva rejeitam limites inválidos',()=>{
 assert.throws(()=>setContributionRank(createRebelGroup(),11),/entre 0 e 10/i);
 assert.throws(()=>setDestinyReserve(createRebelGroup(),{light:21,dark:0}),/entre 0 e 20/i);
});
