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
 assert.match(js("ecoHubScreen()"),/Uma sessão. Quatro etapas/);
});
test('ECO: fluxo direto para gravação e marcações',()=>{
 const cid=js("state.cases[0].id");
 context.cid=cid;
 js("act('ecoOpen',cid)");
 assert.equal(js("screen"),"session");
 assert.match(js("sessionScreen()"),/GRAVAR AO VIVO/);
 js("act('ecoWorkspaceTab','marcar')");
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


test('ECO v0.4 usa dashboard de escuta e atalhos reais',()=>{
 js("act('nav','home')");
 assert.match(js("homeScreen()"),/INICIAR SESSÃO/);
 assert.match(js("homeScreen()"),/Memória|Últimos registros/);
 js("act('nav','eco')");
 assert.match(js("ecoHubScreen()"),/Selecionar caso/);
});
test('ECO v0.4 marcação, revisão e atlas sem áudio',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 assert.match(js("sessionScreen()"),/Transcrição de áudio/);
 js("act('ecoWorkspaceTab','marcar')");
 assert.match(js("sessionScreen()"),/data-eco-tag/);
 js("act('ecoWorkspaceTab','revisar')");
 assert.match(js("sessionScreen()"),/data-field="mlRevisada"/);
 js("act('ecoWorkspaceTab','ecos')");
 assert.match(js("sessionScreen()"),/TF-IDF/);
 js("act('nav','atlas')");
 assert.match(js("ecoAtlasScreen()"),/Histórico das sessões/);
});
test('ECO v0.4 não inventa ecos quando não há evidência',()=>{
 const p=js("ecoRank('nuvem azul jardim',[{date:'2026-11-10',text:'desenho vermelho fumaça madeira'}])");
 assert.equal(p.length,0);
});


test('ECO v0.5 mostra importação de áudio no fluxo da sessão',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 js("act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/IMPORTAR E TRANSCREVER ÁUDIO/);
 assert.match(js("sessionScreen()"),/MP3, WAV, M4A ou FLAC/);
});
test('ECO v0.5 requer confirmação antes de selecionar arquivo',()=>{
 context.window.NativeVault.selectAudioFile=()=>{context.filePickerCalled=true};
 context.filePickerCalled=false;
 js("ecoConsent=false;act('ecoImportFile')");
 assert.equal(context.filePickerCalled,false);
 js("ecoConsent=true;act('ecoImportFile')");
 assert.equal(context.filePickerCalled,true);
 assert.equal(js("ecoAudioSource"),"arquivo");
 js('window.nativeMlState("IMPORTAÇÃO CANCELADA")');
});
test('ECO v0.5 reconhece importação como operação ocupada',()=>{
 js('window.nativeMlState("PROCESSANDO ÁUDIO · 28 s")');
 assert.equal(js("ecoIsCapturing()"),true);
 js('window.nativeMlState("CONCLUÍDO · REVISE A TRANSCRIÇÃO")');
 assert.equal(js("ecoIsCapturing()"),false);
});
test('ECO v0.5 arquivo SAF stream / decodificação local / sem upload',()=>{
 const native=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const decoding=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/AudioFileTranscriber.java',import.meta.url),'utf8');
 assert.match(native,/Intent\.ACTION_OPEN_DOCUMENT/);
 assert.match(native,/intent\.setType\("audio\/\*"\)/);
 assert.match(native,/ml\.importFile\(uri\)/);
 assert.match(native,/AUDIO_FILE_CODE/);
 assert.match(ml,/WhisperFileEngine\.transcribe/);
 assert.match(decoding,/MediaExtractor/);
 assert.match(decoding,/MediaCodec/);
 assert.match(decoding,/new Recognizer\(model, TARGET_RATE\)/);
 assert.match(decoding,/recognizer\.acceptWaveForm\(pcm, size\)/);
 assert.match(decoding,/openInputStream\(uri\)/);
 assert.doesNotMatch(decoding,/new FileOutputStream|createTempFile|File\.createTempFile|upload|HttpURLConnection/);
});


test('Fluxo ECO permite começar sem sintomas, idade, intensidade ou tempo da queixa',()=>{
 const count=js('state.cases.length');
 js("act('nav','eco');act('ecoQuickTest')");
 assert.equal(js('state.cases.length'),count+1);
 assert.equal(js('screen'),'session');
 assert.equal(js('sessionDraft.data!==""'),true);
 assert.equal(js('currentCase().queixas.length'),0);
 assert.equal(js('currentCase().intensidade'),'');
 assert.equal(js('currentCase().desde'),'');
 assert.match(js('sessionScreen()'),/GRAVAR AO VIVO/);
});
test('Cadastro de caso não exige seleção prévia de temas',()=>{
 js("act('newCase');draft.nome='ECO-CASE-TEST';act('wizardNext')");
 assert.equal(js('wizardStep'),1);
 assert.equal(js('validateWizard()'),true);
 assert.equal(js('draft.queixas.length'),0);
});
test('Cadastro rápido abre sessão após apenas um código',()=>{
 js("act('newCase');draft.nome='PAC-004';act('ecoSaveCaseStart')");
 assert.equal(js('screen'),'session');
 assert.equal(js('currentCase().nome'),'PAC-004');
 assert.equal(js('currentCase().queixas.length'),0);
 assert.equal(js('ecoWorkspaceTab'),'captura');
});

test('ECO Whisper Base para arquivos sem API externa',()=>{
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const kt=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperFileEngine.kt',import.meta.url),'utf8');
 const app=fs.readFileSync(new URL('../app/build.gradle',import.meta.url),'utf8');
 const wf=fs.readFileSync(new URL('../../../.github/workflows/tutor-clinico-android.yml',import.meta.url),'utf8');
 assert.match(ml,/WhisperFileEngine.transcribe/);
 assert.match(kt,/Whisper.transcribe/);
 assert.match(kt,/AudioFileTranscriber.streamPcm/);
 assert.match(kt,/SEGMENT_SECONDS = 25/);
 assert.match(kt,/transcribeChunk\(\)/);
 assert.match(kt,/WhisperConfig\(language = "pt"/);
 assert.match(kt,/File.createTempFile/);
 assert.match(kt,/finally/);
 assert.match(kt,/it.delete\(\)/);
 assert.match(app,/whisper-android:1.0.0/);
 assert.match(wf,/ggml-base.bin/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/Whisper Base offline/);
});

test('ECO 0.6.1 não trata Whisper em andamento como microfone parado',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 js("act('ecoWorkspaceTab','captura')");
 context.window.NativeVault.startMlCapture=()=>{context.microphoneStarted=true};
 context.microphoneStarted=false;
 js('ecoConsent=true;window.nativeMlState("WHISPER BASE · TRANSCRIBINDO 0m50s · TRECHO 3")');
 assert.equal(js("ecoIsCapturing()"),true);
 js("act('ecoStart')");
 assert.equal(context.microphoneStarted,false);
 js('window.nativeMlState("CONCLUÍDO · WHISPER BASE · REVISE A TRANSCRIÇÃO")');
 assert.equal(js("ecoIsCapturing()"),false);
 assert.match(js("sessionScreen()"),/IMPORTAR E TRANSCREVER ÁUDIO/);
});
test('ECO 0.6.1 lê áudio por URI em trechos, sem enviar gravação inteira ao Whisper',()=>{
 const dec=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/AudioFileTranscriber.java',import.meta.url),'utf8');
 const kt=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperFileEngine.kt',import.meta.url),'utf8');
 assert.ok(dec.includes('void streamPcm('));
 assert.ok(dec.includes('interface PcmListener'));
 assert.ok(kt.includes('ByteArrayOutputStream(SEGMENT_SIZE'));
 assert.ok(kt.includes('SAMPLE_RATE = 16000'));
 assert.ok(kt.includes('SEGMENT_SECONDS = 25'));
 assert.ok(kt.includes('File.createTempFile(CACHE_PREFIX, ".wav"'));
 assert.ok(kt.includes('wav.delete()'));
 assert.ok(kt.includes('AudioFileTranscriber.streamPcm'));
 assert.ok(!kt.includes('context.contentResolver.openInputStream(uri)?.use'));
});
console.log('Teste de comportamento executado sem Android SDK; requer ainda compilação/teste no aparelho.');
