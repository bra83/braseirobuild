import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { CharacterRosterService } from '../app/src/main/assets/www/core/character-roster-service.js';
import { RuntimeBarbaraProvider } from '../app/src/main/assets/www/core/runtime-narrative-provider.js';

const root=path.resolve(import.meta.dirname,'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');

const jornadaHtml=read('app/src/main/assets/www/jornada.html');
const jornadaJs=read('app/src/main/assets/www/jornada.js');
const fichaJs=read('app/src/main/assets/www/ficha.js');
const configHtml=read('app/src/main/assets/www/configuracoes.html');
for(const needle of ['configuracoes.html','sceneMasterButton'])assert.ok(jornadaHtml.includes(needle),`jornada missing ${needle}`);
for(const needle of ['MESTRE NARRAR AGORA','renderMasterStatus','barbara.generate'])assert.ok(jornadaJs.includes(needle),`jornada.js missing ${needle}`);
for(const needle of ['CRIAR ASSISTIDO','GERAR ALEATÓRIO','EXPORTAR JSON','IMPORTAR JSON','CONFIGURAÇÕES / GEMINI','CharacterRosterService'])assert.ok(fichaJs.includes(needle),`ficha missing ${needle}`);
for(const needle of ['CHAVE API','SALVAR CHAVE E MODELO','SALVAR BACKUP .JSON','SALVAR FICHA .JSON'])assert.ok(configHtml.includes(needle),`config missing ${needle}`);

const p=new MemoryPersistenceAdapter();
const st=makeEmptyCampaignState({campaignId:'flow',displayName:'Flow',masterSeed:'seed-flow'});
p.createCampaign({campaignId:'flow',displayName:'Flow',stateJson:serializeCampaignState(st),masterSeed:'seed-flow',rngAlgorithm:st.rngState.algorithm,initialSnapshotId:'s0'});
const roster=new CharacterRosterService({persistence:p});
const created=roster.generateRandom({campaignId:'flow',characterId:'pc:flow',name:'Fluxo',goal:'Testar integração',classPath:{kind:'FULL',classId:'EXPERT'},setActive:true});
assert.equal(created.status,'OK');
assert.equal(roster.list({campaignId:'flow'}).activeCharacterId,'pc:flow');
const exported=roster.export({campaignId:'flow'});assert.equal(exported.status,'OK');assert.match(exported.json,/BRASEIRO_WWN_CHARACTER/);

const local=new RuntimeBarbaraProvider({native:null,ragSearch:null});
const localNarr=local.generate({mechanicalEvent:{eventId:'scene:x',revision:1,kind:'SCENE_OPENED',scene:{summary:'Ruínas silenciosas sob um céu carregado.'}},state:{campaignId:'x',revision:1}});
assert.equal(localNarr.provider,'LOCAL_SCENE');assert.match(localNarr.text,/Ruínas silenciosas/);
const fakeNative={geminiStatus:()=>JSON.stringify({status:'OK',configured:true,model:'gemini-test'}),geminiGenerateText:()=>JSON.stringify({status:'OK',model:'gemini-test',text:'A névoa se abre diante das ruínas.'})};
const gemini=new RuntimeBarbaraProvider({native:fakeNative,ragSearch:null});
const g=gemini.generate({mechanicalEvent:{eventId:'scene:y',revision:2,kind:'SCENE_OPENED',scene:{summary:'Cena'}},state:{campaignId:'y',revision:2}});
assert.equal(g.provider,'GEMINI_NATIVE');assert.match(g.text,/névoa/);
console.log('POST071_FLOW_INTEGRATION_CHECK PASS');
