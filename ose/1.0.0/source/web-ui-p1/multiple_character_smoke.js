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
    if (!/^\[data-(?:add-character|character-select|delete-character|cancel-add-character|retainer-proposal)\]$/.test(selector)) return [];
    const html = this.getElementById('screen').innerHTML;
    const key = selector + ':' + html;
    if (buttonCache.has(key)) return buttonCache.get(key);
    const attr = selector.slice(1,-1);
    const regex = new RegExp('<button\\b[^>]*\\b' + attr + '(?:="([^"]*)")?[^>]*>', 'g');
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
const state = {
  campaign:{profile:'OSE_CLASSIC_FANTASY'},
  session:{phase:'ACTIVE',visibleNarration:'A aventura prossegue.'},
  party:[first,second], selectedCharacterId:'char-second',selectedCharacter:second,
  creation:{drafts:[]}, time:{turns:30,days:0},
  resources:['rations:3','torch:2'],
  inventory:[
    {itemId:'item-only-first',ownerId:'char-first',catalogId:'STARTING_GOLD_GP',kind:'COIN',quantity:1},
    {itemId:'item-only-second',ownerId:'char-second',catalogId:'STARTING_GOLD_GP',kind:'COIN',quantity:1}
  ],
  presentation:{}, loadout:null,
  retainers:[{npcId:'old-hire',name:'Antigo auxiliar',employerCharacterId:'char-first',dailyWageGp:1,treasureHalfShares:1,active:true}],
  retainerCandidates:[{npcId:'local-candidate',name:'Candidato local'}]
};
window.BraseiroReceive(JSON.stringify({type:'ViewState',payload:state}));
const companyHtml = document.getElementById('screen').innerHTML;
assert.match(companyHtml,/ADICIONAR PERSONAGEM/);
assert.match(companyHtml,/Auxiliares de aventura/);
assert.match(companyHtml,/Antigo auxiliar/);
assert.match(companyHtml,/data-retainer-proposal="local-candidate"/);
document.getElementById('retainerDailyWage').value='1';
document.getElementById('retainerTreasureHalves').value='1';
const candidateButtons=document.querySelectorAll('[data-retainer-proposal]');
assert.equal(candidateButtons.length,1);
candidateButtons[0].onclick();
assert.equal(messages.at(-1).type,'PlayerReaction');
assert.equal(messages.at(-1).payload.mechanicalIntent,'RETAINER_RECRUIT:char-second:local-candidate:1:1');

assert.match(companyHtml,/data-character-select="char-second"/);
assert.match(companyHtml,/data-delete-character="char-first"/);
assert.match(companyHtml,/data-delete-character="char-second"/);
assert.match(companyHtml,/character-management active/);

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
console.log('OSE multi-character WebView smoke: PASS');
