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
 assert.match(js("sessionScreen()"),/INICIAR GRAVAÇÃO LOCAL/);
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
 assert.match(js("sessionScreen()"),/Gravar primeiro, transcrever depois/);
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
 js('act("ecoWorkspaceTab","captura")');
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
 assert.match(native,/ml\.importFile\(uri,selectedAsrModel\)/);
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
 assert.match(js('sessionScreen()'),/INICIAR GRAVAÇÃO LOCAL/);
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
 assert.ok(kt.includes('transcribeChunk(false)'));
 assert.match(kt,/WhisperConfig\(language = "pt"/);
 assert.match(kt,/File.createTempFile/);
 assert.match(kt,/finally/);
 assert.match(kt,/it.delete\(\)/);
 assert.match(app,/whisper-android:1.0.0/);
 assert.match(wf,/ggml-base.bin/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/Whisper Base · mais rápido/);
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
 assert.equal(js("ecoWorkspaceTab"),"analise");
 js('act("ecoWorkspaceTab","captura")');
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

test('ECO G1 backup preserva ML, marcações, evidências, extensões, versões e rascunho',()=>{
 const sample={
  version:1,uiTheme:'dark',mlDraft:{caseId:'x',session:{id:'draft-1',mlTranscricao:'fala incompleta'}},
  appointments:[],cases:[{id:'x',nome:'ECO-TESTE',sessoes:[{
   id:'s1',data:'2026-11-23',texto:'Resumo',mlTranscricao:'Sonhei com minha mãe',
   mlRevisada:'Sonhei com minha mãe.',mlTags:['Mãe','Sonho'],
   mlSegments:[{text:'Sonhei com minha mãe',startMs:6000,endMs:9000,speaker:'paciente',origem:'Whisper Small'}],
   mlEvidence:[{id:'e1',label:'mãe',quote:'Sonhei com minha mãe',status:'confirmado'}],
   extraCustom:{keep:true},
   revisoes:[{texto:'Versão original',mlTranscricao:'Texto anterior',mlSegments:[{speaker:'desconhecido'}]}]
  }]}]};
 context.backupComplete=sample;
 const data=js('normalizeImported(backupComplete)');
 assert.equal(data.cases[0].sessoes[0].mlTranscricao,sample.cases[0].sessoes[0].mlTranscricao);
 assert.deepEqual(data.cases[0].sessoes[0].mlSegments,sample.cases[0].sessoes[0].mlSegments);
 assert.deepEqual(data.cases[0].sessoes[0].mlEvidence,sample.cases[0].sessoes[0].mlEvidence);
 assert.deepEqual(data.cases[0].sessoes[0].revisoes,sample.cases[0].sessoes[0].revisoes);
 assert.equal(data.cases[0].sessoes[0].extraCustom.keep,true);
 assert.equal(data.mlDraft.session.mlTranscricao,'fala incompleta');
 assert.equal(data.uiTheme,'dark');
});
test('ECO G1 falha injetada ao salvar nao descarta sessao em edicao',()=>{
 js("act('ecoOpen',state.cases[0].id);sessionDraft.texto='Preservar mesmo com erro'");
 const before=js('currentCase().sessoes.length');
 const normal=context.window.NativeVault.saveState;
 context.window.NativeVault.saveState=()=> 'ERRO FICTÍCIO: sem espaço';
 js("act('ecoSave')");
 assert.equal(js('screen'),'session');
 assert.equal(js('sessionDraft.texto'),'Preservar mesmo com erro');
 assert.equal(js('currentCase().sessoes.length'),before);
 context.window.NativeVault.saveState=normal;
 js("act('ecoSave')");
 assert.equal(js('screen'),'case');
 assert.equal(js('currentCase().sessoes.length'),before+1);
});
test('ECO G4 NLP factual PT com negacao sem criar diagnostico',()=>{
 const e=js("ecoExtractEvidence('Eu sonhei com minha mãe na infância. Não tenho ansiedade. Meu chefe falou comigo no trabalho.','s8')");
 assert.ok(e.some(x=>x.label==='mãe'));
 assert.ok(e.some(x=>x.label==='infância'));
 assert.ok(e.some(x=>x.label==='sonho'));
 assert.ok(e.some(x=>x.label==='chefe'));
 const neg=e.find(x=>x.label==='ansiedade');
 assert.ok(neg);
 assert.equal(neg.polarity,'possível negação');
 assert.equal(e.every(x=>x.status==='sugerido'),true);
 assert.equal(e.every(x=>typeof x.quote==='string'&&x.quote.length>0),true);
 assert.equal(e.every(x=>x.speaker==='desconhecido'),true);
});
test('ECO G5 trechos Whisper conservam posicao temporal e falante desconhecido',()=>{
 let captured='';
 context.window.NativeVault.saveDraft=(json)=>{captured=json;return 'OK'};
 js("act('ecoOpen',state.cases[0].id)");
 js('ecoAudioSource="arquivo";ecoStatus="WHISPER BASE · TRECHO 1"');
 js('window.nativeMlSegmentDetailed(JSON.stringify({text:"Sonhei com minha mãe",startMs:5000,endMs:8800}))');
 assert.equal(js('sessionDraft.mlSegments[0].startMs'),5000);
 assert.equal(js('sessionDraft.mlSegments[0].endMs'),8800);
 assert.equal(js('sessionDraft.mlSegments[0].speaker'),'desconhecido');
 assert.equal(JSON.parse(captured).session.mlSegments[0].startMs,5000);
 const suggestions=js('ecoEvidenceForDraft()');
 assert.ok(suggestions.some(x=>x.label==='mãe'&&x.timeMs===5000));
 context.window.NativeVault.saveDraft=()=> 'ERRO';
 const previous=js('sessionDraft.mlEvidence.length');
 context.evidenceId=suggestions.find(x=>x.label==='mãe').id;
 js("act('ecoAcceptEvidence',evidenceId)");
 assert.equal(js('sessionDraft.mlEvidence.length'),previous);
 context.window.NativeVault.saveDraft=()=> 'OK';
 js("act('ecoAcceptEvidence',evidenceId)");
 assert.equal(js('sessionDraft.mlEvidence.length'),previous+1);
 assert.equal(js('sessionDraft.mlEvidence[0].status'),'confirmado');
});
test('ECO G6 arquivos incrementais AES-GCM e backup inclui rascunho',()=>{
 const store=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/SecureStore.java',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.match(store,/synchronized void saveDraft/);
 assert.match(store,/AES\/GCM\/NoPadding/);
 assert.match(store,/draft\.aes/);
 assert.match(store,/synchronized String readDraft/);
 assert.match(main,/JSONObject journal=new JSONObject\(draft\)/);
 assert.match(main,/export\.put\("mlDraft",journal\)/);
 assert.match(main,/store\.saveDraft/);
});
test('ECO G5 PDF inclui transcricao integral, segmentos e marcacoes',()=>{
 const person=js('currentCase()');
 const pdf=js('pdfForCase(currentCase())');
 assert.match(pdf,/Anexo técnico: transcrição, trechos e evidências/);
 assert.match(pdf,/Transcrição automática sem correção/);
 assert.match(pdf,/não é criptografado/);
});


test('ECO G2 modelo de qualidade Small Q5 e fallback Base em importacao',()=>{
 const kt=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperFileEngine.kt',import.meta.url),'utf8');
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.ok(kt.includes('ggml-small-q5_1.bin'));
 assert.ok(kt.includes('ggml-base.bin'));
 assert.ok(kt.includes('cutAtPause('));
 assert.ok(kt.includes('containsSpeechLikeSignal('));
 assert.ok(ml.includes('fileModelName'));
 assert.ok(main.includes('selectAudioFileWithModel'));
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/Whisper Small Q5/);
 assert.match(js("sessionScreen()"),/Whisper Base/);
});
test('ECO G9 exige referencia documental para audio autorizado',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 js('ecoConsent=true;ecoConsentKind="autorizado";ecoConsentReference=""');
 assert.equal(js('ecoDocumentConsent()'),false);
 js('ecoConsentReference="TERMO-FICTICIO-002"');
 context.window.NativeVault.saveDraft=()=> 'OK';
 assert.equal(js('ecoDocumentConsent()'),true);
 assert.equal(js('sessionDraft.mlConsent.kind'),'autorizado');
 assert.equal(js('sessionDraft.mlConsent.documentReference'),'TERMO-FICTICIO-002');
});


test('ECO G3 captura: AudioRecord em uma thread, grava WAV, encerra e depois transcreve',()=>{
 const rec=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/SessionRecorder.java',import.meta.url),'utf8');
 const engine=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.match(rec,/AudioRecord\.READ_NON_BLOCKING/);
 assert.match(rec,/new Thread\(this::capture/);
 assert.match(rec,/recording\.set\(false\)/);
 assert.match(rec,/writeHeader\(wav,bytes\)/);
 assert.match(rec,/events\.ready\(file,bytes/);
 assert.match(rec,/File\.createTempFile\(PREFIX, "\.wav"/);
 assert.match(engine,/new SessionRecorder/);
 assert.match(engine,/beginFile\(\)/);
 assert.match(engine,/pauseTranscription\(\)/);
 assert.match(engine,/resumeTranscription\(\)/);
 assert.match(engine,/discardRemainingAudio\(\)/);
 assert.match(main,/ml\.onHostPause\(\)/);
 const pausedLifecycle=main.slice(main.indexOf("onPause()"),main.indexOf("onResume()",main.indexOf("onPause()")));
 assert.doesNotMatch(pausedLifecycle,/ml\.stop\(\)/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 const view=js("sessionScreen()");
 assert.match(view,/INICIAR GRAVAÇÃO LOCAL/);
 assert.match(view,/Gravar primeiro, transcrever depois/);
});
test('ECO G3 live mic engine selection passes to Android bridge and blocks parallel imports',()=>{
 const cid=js('state.cases[0].id');
 js('act("ecoOpen",cid);ecoConsent=true;ecoConsentKind="simulacao";ecoConsentReference="";ecoStatus="PARADO";ecoChosenMic="whisper-base"');
 context.window.NativeVault.saveDraft=()=> 'OK';
 context.window.NativeVault.startMlCaptureWithEngine=(consent,engine)=>{context.liveChoice={consent,engine};};
 assert.equal(js("ecoIsCapturing()"),false);
 js('act("ecoStart")');
 assert.ok(context.liveChoice,"Native bridge não chamado: "+JSON.stringify({status:js("ecoStatus"),consent:js("ecoConsent"),consentKind:js("ecoConsentKind"),reference:js("ecoConsentReference")}));
 assert.deepEqual(context.liveChoice,{consent:true,engine:'whisper-base'});
 assert.equal(js("ecoIsCapturing()"),true);
 js('window.nativeMlState("FINALIZANDO MICROFONE · TRANSCRIÇÃO DE TRECHOS PENDENTES")');
 assert.equal(js("ecoIsCapturing()"),true);
 js('window.nativeMlState("CONCLUÍDO · MICROFONE WHISPER BASE · REVISE O TEXTO")');
 assert.equal(js("ecoIsCapturing()"),false);
});


test('ECO G4 revisão invalida evidência que não corresponde mais à fonte',()=>{
 const session={
   id:'s-review',mlTranscricao:'Eu sonhei com minha mãe',mlRevisada:'',
   mlEvidence:[{id:'ev1',status:'confirmado',label:'mãe',category:'Pessoa',quote:'Eu sonhei com minha mãe'}]
 };
 context.sampleEvidenceSession=session;
 assert.equal(js('ecoEvidenceCurrent(sampleEvidenceSession,sampleEvidenceSession.mlEvidence[0])'),true);
 session.mlRevisada='Eu sonhei com meu pai';
 assert.equal(js('ecoEvidenceCurrent(sampleEvidenceSession,sampleEvidenceSession.mlEvidence[0])'),false);
 assert.equal(js('ecoLongitudinalEvidence([sampleEvidenceSession]).length'),0);
 const before=js('ecoExtractEvidence("Sonhei com minha mãe", "s").find(x=>x.label==="mãe").id');
 const after=js('ecoExtractEvidence("Sonhei com minha avó", "s").find(x=>x.label==="avó").id');
 assert.notEqual(before,after);
});
test('ECO G4 Atlas identifica recorrência confirmada sem inferir causalidade',()=>{
 const mk=(id,data,text)=>({id,data,mlTranscricao:text,mlEvidence:[{
  id:'e'+id,category:'Pessoa',label:'mãe',status:'confirmado',quote:'minha mãe',polarity:'menção'
 }]});
 context.sample1=mk('one','2026-10-01','Ontem falei com minha mãe.');
 context.sample2=mk('two','2026-10-08','Lembrei da minha mãe.');
 context.sample3=mk('three','2026-10-15','Não saí de casa.');
 const rec=js('ecoLongitudinalEvidence([sample1,sample2,sample3])');
 assert.equal(rec.length,1);
 assert.equal(rec[0].sessions,2);
 assert.equal(rec[0].label,'mãe');
});


test('ECO G2 taxa WER calcula exato, omissao, insercao, negacao e acentos',()=>{
 const t=js('ecoCalculateWer("Minha mãe estava em casa.","Minha mãe estava em casa.")');
 assert.equal(t.distance,0);assert.equal(t.wer,0);
 const neg=js('ecoCalculateWer("Eu não tenho medo","Eu tenho medo")');
 assert.equal(neg.distance,1);assert.equal(neg.referenceWords,4);
 assert.equal(neg.wer,.25);
 const ins=js('ecoCalculateWer("Eu sonhei", "Eu sonhei de novo")');
 assert.equal(ins.distance,2);assert.equal(ins.wer,1);
 assert.throws(()=>js('ecoCalculateWer("","teste")'),/referência verificada/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','revisar')");
 assert.match(js('sessionScreen()'),/Teste objetivo da precisão/);
 assert.match(js('sessionScreen()'),/ecoBenchmark/);
});


test('ECO captação e importação bloqueiam ações enquanto motores realmente processam',()=>{
 for(const busy of [
  'GRAVANDO · WHISPER BASE · TRANSCRIÇÃO EM TRECHOS',
  'CAPTURANDO · VOSK PT-BR',
  'WHISPER SMALL Q5 · DECODIFICANDO ÁUDIO',
  'FINALIZANDO MICROFONE · PROCESSANDO ÚLTIMOS TRECHOS'
 ]){
   context.busyStatus=busy;
   js('window.nativeMlState(busyStatus)');
   assert.equal(js('ecoIsCapturing()'),true,"Estado deveria ser ocupado: "+busy);
 }
 js('window.nativeMlState("CONCLUÍDO · MICROFONE WHISPER BASE · REVISE O TEXTO")');
 assert.equal(js('ecoIsCapturing()'),false);
});


test('ECO não permite mudar de caso/importar outro arquivo durante áudio pausado',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 js('ecoConsent=true;ecoStatus="PAUSADO · 25s PROCESSADOS"');
 const before=js('screen');
 js('act("nav","atlas")');
 assert.equal(js('screen'),before);
 context.filePickerCalled=false;
 context.window.NativeVault.selectAudioFileWithModel=()=>{context.filePickerCalled=true};
 js('act("ecoImportFile")');
 assert.equal(context.filePickerCalled,false);
 js('window.nativeMlState("GRAVAÇÃO FINALIZADA · INICIANDO WHISPER")');
 assert.equal(js('ecoIsCapturing()'),true);
});
test('ECO gravação e transcrição têm pausas e conclusão parcial distintas',()=>{
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const whisper=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperFileEngine.kt',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.match(ml,/pauseRequested=true/);
 assert.match(ml,/resumeFromMs=processedMs/);
 assert.match(ml,/if\(pauseRequested && wanted/);
 assert.match(ml,/if\(recordingRequested && recorder!=null\)/);
 assert.match(ml,/file\.delete\(\)/);
 assert.match(whisper,/resumeFromMs: Long/);
 assert.match(whisper,/fun progress\(processedMs: Long\)/);
 assert.match(main,/pauseMlTranscription/);
 assert.match(main,/resumeMlTranscription/);
 assert.match(main,/discardMlAudio/);
 js('act("ecoOpen",state.cases[0].id)');
 context.window.NativeVault.pauseMlTranscription=()=>{context.pauseCount=(context.pauseCount||0)+1;};
 context.window.NativeVault.resumeMlTranscription=()=>{context.resumeCount=(context.resumeCount||0)+1;};
 context.window.NativeVault.discardMlAudio=()=>{context.discardCount=(context.discardCount||0)+1;};
 js('window.nativeMlState("WHISPER SMALL Q5 · TRANSCRIBINDO 0m25s · TRECHO 1")');
 assert.match(js('sessionScreen()'),/PAUSAR TRANSCRIÇÃO E REVISAR/);
 js('act("ecoPause")');assert.equal(context.pauseCount,1);
 js('window.nativeMlState("PAUSADO · 25s PROCESSADOS · REVISE OU RETOME")');
 assert.match(js('sessionScreen()'),/RETOMAR DO MINUTO INTERROMPIDO/);
 js('act("ecoResume")');assert.equal(context.resumeCount,1);
 js('act("ecoDiscard")');assert.equal(context.discardCount,1);
});
test('ECO linha do tempo 00:00 e navegação por minuto',()=>{
 js('act("ecoOpen",state.cases[0].id);act("ecoWorkspaceTab","revisar")');
 js('sessionDraft.mlSegments=[{text:"Eu sonhei com minha mãe.",startMs:7000,endMs:10000,speaker:"paciente",origem:"Whisper"},{text:"Falei com meu chefe.",startMs:93000,endMs:96000,speaker:"paciente",origem:"Whisper"}]');
 assert.equal(js('ecoTimecode(93000)'),'01:33');
 assert.equal(js('ecoTimecode(null)'),'sem horário');
 assert.equal(js('ecoTimelineRows(sessionDraft,"chefe").length'),1);
 assert.match(js('ecoTimeline(sessionDraft)'),/01:33/);
 assert.match(js('ecoTimeline(sessionDraft)'),/ecoFocusSegment/);
});
test('ECO NLP factual eventos extrai verbo, referencia temporal, familia e negacao, sem diagnostico',()=>{
 context.structured={
  id:"s-audit",mlTranscricao:"Ontem sonhei com minha mãe. Eu não falei com meu pai. Meu chefe disse que esperava.",
  mlSegments:[{text:"Ontem sonhei com minha mãe.",startMs:18000,speaker:"paciente"},
              {text:"Eu não falei com meu pai.",startMs:24000,speaker:"paciente"},
              {text:"Meu chefe disse que esperava.",startMs:41000,speaker:"paciente"}]
 };
 const events=js('ecoStructuredEvents(structured)');
 assert.ok(events.some(x=>x.kind==='Sonho relatado'&&x.atMs===18000));
 assert.ok(events.some(x=>x.kind==='Comunicação relatada'&&x.quote.includes('meu pai')));
 assert.ok(events.some(x=>x.kind==='Espera relatada'&&x.quote.includes('Meu chefe')));
 assert.equal(events.every(x=>x.status==='sugerido'),true);
 assert.equal(events.every(x=>!!x.quote),true);
 assert.equal(events.every(x=>!('diagnosis' in x)),true);
 assert.ok(js('ecoEventCurrent({...structured,mlEvents:[]},{status:"confirmado",quote:"Ontem sonhei com minha mãe."})'));
});


test('ECO Android background não grava escondido e preserva áudio para retomada',()=>{
 const java=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.match(java,/deferRecognitionUntilResume=true/);
 assert.match(java,/onHostPause\(\)/);
 assert.match(java,/else if\(importing && !pauseRequested && !backgroundAllowed\)/);
 assert.match(java,/TranscriptionService\.begin/);
 assert.match(java,/PAUSADO · GRAVAÇÃO LOCAL FINALIZADA/);
 assert.match(java,/deferRecognitionUntilResume=false/);
 assert.match(main,/audioCleanedThisProcess\.compareAndSet\(false,true\)/);
 assert.match(main,/ml\.onHostPause\(\)/);
});


test('ECO pós-transcrição abre análise e expõe revisão e marcação claramente',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 js('sessionDraft.mlTranscricao="Sonhei com minha mãe e meu irmão."');
 js('sessionDraft.mlSegments=[{text:"Sonhei com minha mãe e meu irmão.",startMs:55000,endMs:60000,speaker:"paciente"}]');
 js('window.nativeMlState("CONCLUÍDO · TRANSCRIÇÃO DISPONÍVEL NA LINHA DO TEMPO")');
 assert.equal(js("ecoWorkspaceTab"),"analise");
 const screenContent=js("sessionScreen()");
 assert.match(screenContent,/Análise da sessão/);
 assert.match(screenContent,/Nuvem de palavras/);
 assert.match(screenContent,/Corrigir transcrição/);
 assert.match(screenContent,/Confirmar marcações/);
 assert.match(screenContent,/Histórico/);
});
test('ECO nuvem de palavras elimina preposições, pronomes e conectivos',()=>{
 assert.equal(js('ecoNlpStop.has("minha")'),true);
 assert.equal(js('ecoNlpStop.has("porque")'),true);
 assert.equal(js('ecoNlpStop.has("sonho")'),false);
 context.stopwordsSample={mlTranscricao:"Eu e minha mãe na casa. Minha mãe no sonho.",
 mlSegments:[{text:"Eu e minha mãe na casa. Minha mãe no sonho.",startMs:0,endMs:8000}]};
 const rows=js('ecoWordRows(stopwordsSample)');
 assert.ok(!rows.some(r=>["minha","com","para","que","porque"].includes(r.word)));
 assert.equal(rows.find(r=>r.word==="mae").count,2);
});
test('ECO relatório lexical conta mãe e sonho com minuto rastreável',()=>{
 context.caseAnalysisFixture={
  mlTranscricao:'Eu sonhei com minha mãe. Minha mãe contou outro sonho.',
  mlSegments:[{text:'Eu sonhei com minha mãe.',startMs:12000,endMs:15000,speaker:'paciente'},
              {text:'Minha mãe contou outro sonho.',startMs:93000,endMs:97000,speaker:'paciente'}]
 };
 const terms=js("ecoWordRows(caseAnalysisFixture)");
 assert.equal(terms.find(x=>x.word==="mae").count,2);
 assert.equal(terms.find(x=>x.word==="sonho").count,2);
 assert.equal(terms.find(x=>x.word==="mae").locations[1].atMs,93000);
 assert.equal(js("ecoTimecode(93000)"),"01:33");
 const html=js("ecoAnalyticsPanel(caseAnalysisFixture)");
 assert.match(html,/Nuvem de palavras/);
 assert.match(html,/Contagem e ocorrências por minuto/);
 assert.match(html,/Negações explícitas/);
 assert.match(html,/Sinal acústico/);
});
test('ECO identifica negações literais sem alegar negação psicológica',()=>{
 context.signalSample={mlSegments:[{text:'Eu não sentia medo. Nunca disse isso...',startMs:31000,speaker:'paciente'}]};
 const signals=js('ecoNlpSignals(signalSample)');
 assert.equal(signals.negations.length,2);
 assert.ok(signals.negations.some(x=>x.word.toLowerCase()==="nunca"));
 assert.equal(signals.negations[0].atMs,31000);
 assert.equal(signals.interruptions.length,1);
 assert.match(js('ecoAnalyticsPanel(signalSample)'),/Não significa negação psicológica/);
});
test('ECO medidas acústicas registradas no rascunho cifrado, sem inferir emoção',()=>{
 js("act('ecoOpen',state.cases[0].id)");
 context.window.NativeVault.saveDraft=()=> "OK";
 js('window.nativeMlAcoustic(JSON.stringify({atMs:5000,durationMs:5000,rmsDbFs:-30.1,nearSilenceRatio:0.42,f0EstimateHz:150}))');
 assert.equal(js('sessionDraft.mlAcoustic.length'),1);
 assert.equal(js('sessionDraft.mlAcoustic[0].atMs'),5000);
 js('window.nativeMlAcoustic(JSON.stringify({atMs:5000,durationMs:5000,rmsDbFs:-28.3,nearSilenceRatio:0.22}))');
 assert.equal(js('sessionDraft.mlAcoustic.length'),1);
 const html=js("ecoAnalyticsPanel(sessionDraft)");
 assert.match(html,/não análise emocional|Nenhuma transcrição disponível/);
});
test('ECO caso oferece análises reais, sem menus genéricos de investigar e técnicas',()=>{
 const html=js("caseScreen()");
 assert.match(html,/Análises/);
 assert.doesNotMatch(html,/data-id="investigar"/);
 assert.doesNotMatch(html,/data-id="tecnicas"/);
 const caseId=js('state.cases[0].id');
 js('act("openCase",state.cases[0].id);act("caseTab","analisar")');
 assert.match(js('caseScreen()'),/Registros para análise|Termos recorrentes neste caso/);
});
test('ECO edição do cadastro salva identificação sem quatro etapas',()=>{
 const caseId=js('state.cases[0].id');
 js('act("openCase",state.cases[0].id);act("editCase")');
 assert.match(js('wizardScreen()'),/Salvar identificação/);
 js("draft.nome='ECO-EDITADO-TESTE';act('ecoSaveCaseIdentity')");
 assert.equal(js('state.cases[0].nome'),'ECO-EDITADO-TESTE');
 assert.equal(js('screen'),'case');
});
test('ECO registra transcrição sem exigir nota clínica inventada',()=>{
 js('window.nativeMlState("PARADO · TEXTO PARCIAL PRESERVADO")');
 js('act("ecoOpen",state.cases[0].id)');
 js("sessionDraft.mlTranscricao='Eu estava na escada com meu irmão.';sessionDraft.texto=''");
 const count=js('state.cases[0].sessoes.length');
 js('act("ecoSave")');
 assert.equal(js('state.cases[0].sessoes.length'),count+1);
 assert.match(js('state.cases[0].sessoes[state.cases[0].sessoes.length-1].texto'),/sem síntese profissional/);
});
test('ECO rotação mantém atividade e recuperação do último rascunho',()=>{
 const manifest=fs.readFileSync(new URL('../app/src/main/AndroidManifest.xml',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 const html=fs.readFileSync(new URL('../app/src/main/assets/index.html',import.meta.url),'utf8');
 assert.match(manifest,/configChanges="orientation\|screenSize/);
 assert.match(main,/onConfigurationChanged\(Configuration newConfig\)/);
 assert.match(html,/RASCUNHO RESTAURADO/);
 assert.match(main,/audioCleanedThisProcess\.compareAndSet\(false,true\)/);
});

console.log('Teste de comportamento executado sem Android SDK; requer ainda compilação/teste no aparelho.');

test('ECO 0.11 marca repetições sem reescrever transcrição',()=>{assert.equal(js('ecoReviewFlags("as pagas que pagas em contraditórias")'),true);assert.equal(js('ecoReviewFlags("quero falar sobre meu trabalho")'),false);});
test('ECO 0.11 falantes anônimos não viram papéis sem confirmação',()=>{assert.equal(js('ecoSpeakerLabel({speaker:"desconhecido",cluster:"falante1"})'),'Falante 1 (sugerido)');assert.equal(js('ecoSpeakerLabel({speaker:"paciente",cluster:"falante1"})'),'paciente');});

test('ECO 0.11 análise mostra nuvem antes do motor semântico, falantes e linha do tempo',()=>{js("act('ecoOpen',state.cases[0].id)");js('sessionDraft.mlTranscricao="Sonhei com minha mãe."');js('window.nativeMlState("CONCLUÍDO · TESTE")');const h=js('sessionScreen()');assert.match(h,/id="ecoCloudPanel"/);assert.ok(h.indexOf('id="ecoCloudPanel"')<h.indexOf('Semântica neural'));assert.match(h,/Identificação dos falantes/);assert.match(h,/Transcrição reconhecida/);});

test('ECO 0.12 atribui dois grupos em lote e preserva correções',()=>{
 js('window.nativeMlState("PARADO");act("ecoOpen",state.cases[0].id)');
 js('sessionDraft.mlSegments=[{text:"a",cluster:"falante1",speaker:"desconhecido"},{text:"b",cluster:"falante2",speaker:"desconhecido"},{text:"c",cluster:"falante1",speaker:"terapeuta",speakerManual:true},{text:"d",speaker:"desconhecido"}]');
 assert.equal(js('ecoAssignBoth("paciente")'),true);
 assert.equal(js('sessionDraft.mlSegments.map(s=>s.speaker).join(",")'),'paciente,terapeuta,terapeuta,desconhecido');
 assert.equal(js('ecoAssignBoth("terapeuta")'),true);
 assert.equal(js('sessionDraft.mlSegments.map(s=>s.speaker).join(",")'),'terapeuta,paciente,terapeuta,desconhecido');
 assert.equal(js('ecoCorrectSpeaker(0,"paciente")'),true);
 assert.equal(js('ecoAssignBoth("terapeuta")'),true);
 assert.equal(js('sessionDraft.mlSegments[0].speaker'),'paciente');
 assert.equal(js('ecoCorrectSpeaker(0,"grupo")'),true);
 assert.equal(js('sessionDraft.mlSegments[0].speaker'),'terapeuta');
 assert.equal(js('ecoSpeakerRows(sessionDraft.mlSegments,"pendentes").length'),1);
 assert.equal(js('state.mlDraft.session.mlSpeakerRoles.falante2'),'paciente');
});
test('ECO 0.12 revisão acessa trecho 801 e filtra por falante',()=>{
 js('sessionDraft.mlSegments=Array.from({length:850},(_,i)=>({text:"fala "+i,speaker:i%2?"paciente":"terapeuta",cluster:i%2?"falante1":"falante2",startMs:i*1000}))');
 js('ecoSpeakerContext=openedId+"|"+sessionDraft.id;ecoSpeakerPage=22;ecoSpeakerFilter="todos";ecoSpeakerSearch=""');
 assert.match(js('ecoSegmentReview()'),/ecoSegment-800/);
 assert.equal(js('ecoSpeakerRows(sessionDraft.mlSegments,"paciente").length'),425);
 assert.equal(js('ecoSpeakerRows(sessionDraft.mlSegments,"todos","fala 849").length'),1);
});
test('ECO 0.12 falha de persistência restaura papéis individuais e coletivos',()=>{
 js('sessionDraft.mlSegments=[{text:"a",cluster:"falante1",speaker:"desconhecido"},{text:"b",cluster:"falante2",speaker:"desconhecido"}];sessionDraft.mlSpeakerRoles={};window.NativeVault.saveDraft=()=>"ERRO"');
 assert.equal(js('ecoAssignBoth("paciente")'),false);
 assert.equal(js('sessionDraft.mlSegments[0].speaker'),'desconhecido');
 assert.equal(js('Object.keys(sessionDraft.mlSpeakerRoles).length'),0);
 assert.equal(js('ecoCorrectSpeaker(0,"paciente")'),false);
 assert.equal(js('sessionDraft.mlSegments[0].speaker'),'desconhecido');
 js('delete window.NativeVault.saveDraft');
});

test('ECO 0.13 seleção TAGARELA nas duas entradas',()=>{js('window.nativeMlState("PARADO");act("ecoOpen",state.cases[0].id)');const h=js('sessionScreen()');assert.match(h,/data-eco-mic-engine><option value="tagarela"/);assert.match(h,/data-eco-asr-model><option value="tagarela"/);});
test('ECO 0.13 correção individual preserva posição e falante',()=>{js('sessionDraft.mlSegments=[{text:"fala",speaker:"desconhecido"}]');assert.equal(js('ecoCorrectSpeaker(0,"paciente")'),true);assert.equal(js('sessionDraft.mlSegments[0].speaker'),'paciente');assert.equal(js('state.mlDraft.session.mlSegments[0].speaker'),'paciente');});

test('ECO 0.13 retificação arquivada salva em definitivo e não replica transcrição por clique',()=>{js('window.nativeMlState("PARADO");act("ecoOpen",state.cases[0].id);sessionDraft.mlTranscricao="Fala fictícia.";sessionDraft.mlSegments=[{text:"Fala fictícia.",startMs:1000,endMs:2000,speaker:"desconhecido"}];sessionDraft.texto="Teste de retificação";act("ecoSave");');js('var archivedTestId=state.cases[0].sessoes.at(-1).id;act("editSession",archivedTestId);');assert.equal(js('ecoCorrectSpeaker(0,"paciente")'),true);assert.equal(js('currentCase().sessoes.find(s=>s.id===archivedTestId).mlSegments[0].speaker'),'paciente');assert.equal(js('currentCase().sessoes.find(s=>s.id===archivedTestId).revisoes.at(-1).tipo'),'falantes');assert.equal(js('currentCase().sessoes.find(s=>s.id===archivedTestId).revisoes.at(-1).mlTranscricao'),undefined);js('act("ecoBackCase");act("editSession",archivedTestId)');assert.equal(js('sessionDraft.mlSegments[0].speaker'),'paciente');});

test('ECO neural: progresso, cancelamento e resultado tardio não travam interface',()=>{
 js('window.nativeMlState("PARADO");act("ecoOpen",state.cases[0].id);sessionDraft.mlTranscricao="Fala fictícia. Outra fala.";ecoWorkspaceTab="analise";window.NativeVault.analyzeSemantic=()=>"OK";window.NativeVault.cancelSemantic=()=>"OK";ecoNeuralStart()');
 assert.equal(js('ecoNeuralRun.pending'),true);
 js('window.nativeSemanticProgress(JSON.stringify({id:ecoNeuralRun.id,done:1,total:2,stage:"Analisando trechos",elapsedMs:500}))');
 assert.equal(js('ecoNeuralRun.done'),1);
 assert.match(js('ecoNeuralPanel()'),/1 de 2 trechos/);
 js('ecoNeuralCancel();window.nativeSemanticResult(JSON.stringify({id:ecoNeuralRun.id,hits:[],pairs:[]}))');
 assert.equal(js('ecoNeuralRun.pending'),false);
 assert.match(js('ecoNeuralRun.error'),/cancelada/);
 js('ecoNeuralStart();window.nativeSemanticUnlocked()');
 assert.equal(js('ecoNeuralRun.pending'),false);
 assert.match(js('ecoNeuralRun.error'),/bloqueio/);
 js('window.NativeVault.analyzeSemantic=()=>"Motor ocupado";ecoNeuralStart()');
 assert.equal(js('ecoNeuralRun.pending'),false);assert.equal(js('ecoNeuralRun.error'),'Motor ocupado');
 js('window.NativeVault.analyzeSemantic=()=>"OK";ecoNeuralStart();ecoNeuralRun.lastProgress=Date.now()-181000;ecoNeuralWatch(ecoNeuralRun.id)');
 assert.match(js('ecoNeuralRun.error'),/sem progresso/);
 js('ecoNeuralStart();window.nativeSemanticResult(JSON.stringify({id:ecoNeuralRun.id,model:"test",count:2,dimensions:512,hits:[],pairs:[]}))');
 assert.equal(js('ecoNeuralRun.pending'),false);assert.equal(js('ecoNeuralRun.result.count'),2);
 js('delete window.NativeVault.analyzeSemantic;delete window.NativeVault.cancelSemantic;ecoNeuralRun=null');
});
