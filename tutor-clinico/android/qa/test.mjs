import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const html=fs.readFileSync(new URL('../app/src/main/assets/index.html',import.meta.url),'utf8');
const script=html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script);
const elems={};
function el(sel){return elems[sel] ||= {innerHTML:'',value:'',remove(){},prepend(){}};}
let saved='', messages=[];
const context=vm.createContext({
 document:{querySelector:el,addEventListener(){},body:{appendChild(){}},createElement(){return {className:'',textContent:'',remove(){}}}},
 window:{scrollTo(){},NativeVault:{loadState(){return saved || JSON.stringify({version:1,cases:[],appointments:[]})},saveState(json){saved=json;return 'OK'},exportBackup(){},importBackup(){},printReport(){}}},
 setTimeout(fn){}, console,Date,Math,JSON,Number,String,Object,Array,Set,Error
});
vm.runInContext(script,context);
const js=q=>vm.runInContext(q,context);
const test=(label,run)=>{try{run();console.log('PASS',label);}catch(e){console.error('FAIL',label,e);process.exitCode=1;}};
test('Carrega estado inicialmente vazio',()=>{js('appUnlock()');assert.equal(js('state.cases.length'),0);});
test('Aceita data real e rejeita 30 de fevereiro',()=>{assert.equal(js("validDate('2026-11-22')"),true);assert.equal(js("validDate('2026-02-30')"),false);});
test('Escapa HTML potencialmente inseguro',()=>{assert.equal(js("esc('<script>')"),'&lt;script&gt;');});
test('Cadastro de caso em quatro etapas',()=>{js("act('newCase');draft.nome='Paciente Fictício';draft.idade='34';act('wizardNext');draft.queixas=['sono'];act('wizardNext');act('wizardNext');act('saveCase')");assert.equal(js('state.cases.length'),1);assert.equal(js("state.cases[0].nome"),'Paciente Fictício');});
test('Salva sessão e preserva versão anterior ao retificar',()=>{js("act('newSession');sessionDraft.texto='S1 inicial';act('saveSession')");assert.equal(js('currentCase().sessoes.length'),1);js("act('editSession',currentCase().sessoes[0].id);sessionDraft.texto='S1 corrigido';act('saveSession')");assert.equal(js('currentCase().sessoes[0].revisoes.length'),1);assert.equal(js('currentCase().sessoes[0].revisoes[0].texto'),'S1 inicial');});
test('Bloqueia agendamento conflitante',()=>{js("act('newAppointment');apptDraft.hora='09:00';apptDraft.duracao='50';act('saveAppointment')");assert.equal(js('state.appointments.length'),1);js("act('newAppointment');apptDraft.hora='09:30';act('saveAppointment')");assert.equal(js('state.appointments.length'),1);});
test('Permite agendamento sem sobreposição',()=>{js("apptDraft.hora='09:50';act('saveAppointment')");assert.equal(js('state.appointments.length'),2);});
test('Normaliza backup legado sem duplicar detalhes de sessões',()=>{const backup=JSON.parse(fs.readFileSync(new URL('./fixtures/backup_ficticio_testado.json',import.meta.url),'utf8'));context.legacy=backup;const result=js('normalizeImported(legacy)');assert.equal(result.cases.length,1);assert.equal(result.cases[0].sessoes.length,2);assert.ok(result.cases[0].sessoes[0].id);assert.equal(result.legacy,true);});
test('Backup local gravado com versão correta',()=>{assert.equal(JSON.parse(saved).version,1);assert.equal(JSON.parse(saved).appointments.length,2);});

test('AndroidKeyStore usa IV gerado pelo provedor (regressão de salvamento)',()=>{
 const java=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/SecureStore.java',import.meta.url),'utf8');
 const save=java.split('synchronized void save(String json)')[1]?.split('static String hashPin')[0];
 assert.ok(save,'Implementação save ausente');
 assert.match(save,/cipher\.init\(Cipher\.ENCRYPT_MODE,\s*deviceKey\(\)\)/);
 assert.match(save,/byte\[\] iv = cipher\.getIV\(\)/);
 assert.doesNotMatch(save,/cipher\.init\(Cipher\.ENCRYPT_MODE,\s*deviceKey\(\),\s*new GCMParameterSpec/);
 assert.match(save,/stream\.write\(iv\);\s*stream\.write\(encrypted\);/);
});

test('ML TF-IDF recupera somente evidências do histórico',()=>{
 const docs=[{date:'2026-11-12',text:'a minha mãe brigou comigo na infância e senti muita raiva'},{date:'2026-11-13',text:'comprei um carro amarelo e fui viajar'}];
 context.ecoDocs=docs;
 const found=js("ecoRank('senti raiva de minha mãe na infância',ecoDocs)");
 assert.equal(found[0].date,'2026-11-12');
 assert.ok(found[0].shared.includes('infancia'));
});
test('Modelo Vosk presente com captura local sem audio salvo',()=>{
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 assert.match(ml,/new Recognizer\(model,16000\.0f\)/);
 assert.match(ml,/new SpeechService\(rec,16000\.0f\)/);
 assert.match(html,/Sonho recorrente/);
 assert.match(html,/data-eco-tag/);
});

test('ECO: aba Gravar visível na navegação, mesmo fora de Casos',()=>{
 assert.match(js('document.querySelector("#bottom").innerHTML'),/data-id="eco"/);
 js("act('nav','eco')");
 assert.equal(js("screen"),"eco");
 assert.match(js("ecoHubScreen()"),/Gravar e marcar/);
});
test('ECO: fluxo direto para gravação e marcações',()=>{
 const cid=js("state.cases[0].id");
 context.cid=cid;
 js("act('ecoOpen',cid)");
 assert.equal(js("screen"),"session");
 assert.match(js("sessionScreen()"),/GRAVAR E TRANSCREVER/);
 assert.match(js("sessionScreen()"),/data-eco-tag/);
});
test('ECO: alterna tema escuro e claro com persistência',()=>{
 js("act('toggleTheme')");
 assert.equal(js("state.uiTheme"),"dark");
 assert.equal(JSON.parse(saved).uiTheme,"dark");
 js("act('toggleTheme')");
 assert.equal(js("state.uiTheme"),"light");
});
test('ECO: nome do aplicativo e ícone de lançamento',()=>{
 const manifest=fs.readFileSync(new URL('../app/src/main/AndroidManifest.xml',import.meta.url),'utf8');
 const drawable=fs.readFileSync(new URL('../app/src/main/res/drawable/ic_app.xml',import.meta.url),'utf8');
 assert.match(manifest,/android:label="ECO"/);
 assert.match(drawable,/#BFFF42/);
 assert.match(html,/data-theme="dark"/);
});

console.log('Teste de comportamento executado sem Android SDK; requer ainda compilação/teste no aparelho.');
