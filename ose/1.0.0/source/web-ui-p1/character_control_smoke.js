/* Lightweight DOM/bridge smoke test; uses only the Node.js built-ins provided by Actions. */
'use strict';
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, 'p1.js'), 'utf8');

const elements = new Map();
const messages = [];
const makeElement = (id = '') => ({
  id,
  dataset: {},
  style: {},
  classList: {add(){}, remove(){}, toggle(){}},
  value: '',
  textContent: '',
  innerHTML: '',
  disabled: false,
  addEventListener(){},
  focus(){},
  select(){},
  click(){}
});
let buttonCache = new Map();
const document = {
  body: {dataset:{}},
  getElementById(id) {
    if (!elements.has(id)) elements.set(id, makeElement(id));
    return elements.get(id);
  },
  querySelectorAll(selector) {
    if (!/^\[data-(?:add-character|character-select|delete-character|cancel-add-character|character-mode|curator-approve|curator-change|curator-reject|master-continue|companion-opinions)\]$/.test(selector)) return [];
    const html = this.getElementById('screen').innerHTML;
    const key = selector + ':' + html;
    if (buttonCache.has(key)) return buttonCache.get(key);
    const attr = selector.slice(1,-1);
    const regex = new RegExp('<(?:button|select)\\b[^>]*\\b' + attr + '(?:="([^"]*)")?[^>]*>', 'g');
    const nodes = Array.from(html.matchAll(regex), match => {
      const b = makeElement();
      const dataName = attr.replace(/^data-/, '').replace(/-([a-z])/g, (_, c) => c.toUpperCase());
      b.dataset[dataName] = match[1] || '';
      return b;
    });
    buttonCache.set(key, nodes);
    return nodes;
  },
  querySelector(){return null}
};
const window = {BraseiroBridge: {postMessage(raw){messages.push(JSON.parse(raw));}}};
const context = {
  window, document, URLSearchParams,
  location:{search:'?screen=company',pathname:'/ose'},
  history:{replaceState(){}}, navigator:{},
  confirm(){return true;}, clearTimeout(){}, setTimeout(){}, console
};
vm.runInNewContext(script, context, {filename:'p1.js'});
const character = (id, name) => ({
  characterId:id, name, raceId:'HUMAN', classIds:['FIGHTER'], levelByClass:{FIGHTER:1},
  hpCurrentNumerator:7,hpMaxNumerator:7,hpDenominator:1,armorClassDescending:9,
  attributes:{str:12,int:10,wis:9,dex:13,con:11,cha:8}
});
const first = character('char-first','Primeiro');
const second = character('char-second','Segundo');
first.controlMode='MASTER';
second.controlMode='CURATED';
const state = {
  campaign:{profile:'OSE_CLASSIC_FANTASY',mechanicalStateHash:'UNCHANGED_CAMPAIGN_HASH'},
  session:{phase:'ACTIVE',visibleNarration:'A aventura prossegue.'},
  party:[first,second], selectedCharacterId:'char-second',selectedCharacter:second,
  creation:{drafts:[]}, time:{turns:30,days:0},
  resources:['rations:3','torch:2'],
  inventory:[
    {itemId:'item-only-first',ownerId:'char-first',catalogId:'STARTING_GOLD_GP',kind:'COIN',quantity:1},
    {itemId:'item-only-second',ownerId:'char-second',catalogId:'STARTING_GOLD_GP',kind:'COIN',quantity:1}
  ],
  pendingCharacterActions:[{proposalId:"proposal-curated-1",characterId:"char-second",characterName:"Segundo",text:"Examino a porta",mechanicalIntent:"OBSERVE",reason:"Tenho dúvida sobre o caminho",risk:"O movimento pode nos expor",status:"PENDING"}],
  presentation:{}, loadout:null
};
window.BraseiroReceive(JSON.stringify({type:'ViewState',payload:state}));
const companyHtml = document.getElementById('screen').innerHTML;
assert.match(companyHtml,/ADICIONAR PERSONAGEM/);
assert.match(companyHtml,/data-character-select="char-second"/);
assert.match(companyHtml,/data-delete-character="char-first"/);
assert.match(companyHtml,/data-delete-character="char-second"/);
assert.match(companyHtml,/character-management active/);

assert.match(companyHtml,/data-character-mode="char-first"/);
assert.match(companyHtml,/data-character-mode="char-second"/);
assert.match(companyHtml,/Mestre \+ minha curadoria/);
assert.match(companyHtml,/data-curator-approve="proposal-curated-1"/);
assert.match(companyHtml,/data-curator-reject="proposal-curated-1"/);
assert.match(companyHtml,/CONTINUAR AÇÕES DO MESTRE/);
assert.match(companyHtml,/O QUE OS COMPANHEIROS ACHAM/);
assert.match(companyHtml,/Tenho dúvida sobre o caminho/);
assert.match(companyHtml,/O movimento pode nos expor/);
assert.match(companyHtml,/data-curator-change="proposal-curated-1"/);
const opinionButton=document.querySelectorAll('[data-companion-opinions]')[0];
assert.ok(opinionButton);
opinionButton.onclick();
assert.equal(messages.at(-1).type,'CompanionOpinionsQuery');
const oldMessageCount=messages.length;
window.BraseiroReceive(JSON.stringify({type:'CompanionOpinionsResult',payload:{
  readOnly:true,mechanicalDelta:false,stateHash:'UNCHANGED_CAMPAIGN_HASH',
  opinions:[{characterId:'char-second',name:'Segundo',stance:'CAUTION',
    perception:'Só vi a porta conhecida',hypothesis:'Talvez esteja trancada',
    motivation:'Quero evitar ferimentos',suggestion:'Escutar antes de entrar',
    risk:'Poderia haver uma armadilha',reason:'Não sabemos o que há atrás da porta'}]
}}));
assert.equal(messages.length,oldMessageCount,'read-only opinion must never issue a mutation command');
assert.match(document.getElementById('screen').innerHTML,/Escutar antes de entrar/);
assert.match(document.getElementById('screen').innerHTML,/Poderia haver uma armadilha/);
const modeSelectors = document.querySelectorAll('[data-character-mode]');
assert.equal(modeSelectors.length,2);
modeSelectors[0].value='CURATED';
modeSelectors[0].onchange();
assert.equal(messages.at(-1).type,'CharacterControlSet');
assert.equal(messages.at(-1).payload.characterId,'char-first');
assert.equal(messages.at(-1).payload.mode,'CURATED');

const approve = document.querySelectorAll('[data-curator-approve]')[0];
approve.onclick();
assert.equal(messages.at(-1).type,'MasterActionReview');
assert.equal(messages.at(-1).payload.proposalId,'proposal-curated-1');
assert.equal(messages.at(-1).payload.approve,true);
const veto = document.querySelectorAll('[data-curator-reject]')[0];
veto.onclick();
assert.equal(messages.at(-1).type,'MasterActionReview');
assert.equal(messages.at(-1).payload.approve,false);

document.querySelectorAll('[data-master-continue]')[0].onclick();
assert.equal(messages.at(-1).type,'MasterActionContinue');

const select = document.querySelectorAll('[data-character-select]');
assert.equal(select.length,2);
select[1].onclick();
assert.equal(messages.at(-1).type,'CharacterSelect');
assert.equal(messages.at(-1).payload.characterId,'char-second');
assert.match(document.getElementById('screen').innerHTML,/data-character-select="char-second"/);

const del = document.querySelectorAll('[data-delete-character]')[0];
del.onclick();
assert.equal(messages.at(-1).type,'CharacterDelete');
assert.equal(messages.at(-1).payload.characterId,'char-first');

window.BraseiroReceive(JSON.stringify({type:'CharacterSelectionResult',payload:{ok:true,viewState:state}}));
const sheet = document.getElementById('screen').innerHTML;
assert.match(sheet,/Segundo/);
assert.match(sheet,/data-photo-item="item-only-second"/);
assert.doesNotMatch(sheet,/data-photo-item="item-only-first"/);
assert.match(sheet,/ADICIONAR PERSONAGEM/);
assert.match(sheet,/EXCLUIR FICHA/);

const add = document.querySelectorAll('[data-add-character]')[0];
assert.ok(add);
add.onclick();
const creation = document.getElementById('screen').innerHTML;
assert.match(creation,/CRIAR PASSO A PASSO/);
assert.match(creation,/CRIAR PERSONAGEM ALEATÓRIO/);
console.log('OSE character-control WebView approval and authority smoke: PASS');
