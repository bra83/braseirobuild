import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const html=fs.readFileSync(new URL('../app/src/main/assets/index.html',import.meta.url),'utf8');
const script=html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script);
const elems={};
function el(sel){return elems[sel] ||= {innerHTML:'',value:'',children:[],remove(){},prepend(){},querySelector(s){return elems[s]||null;},querySelectorAll(){return [];}};}
let saved='', messages=[];
const context=vm.createContext({
 document:{querySelector:el,getElementById(id){return el('#'+id);},querySelectorAll(){return [];},addEventListener(){},body:{appendChild(){}},createElement(){return {className:'',textContent:'',remove(){}}}},
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
 js('ecoActiveJobId="job-test-g5"');
 js('window.nativeMlSegmentDetailed(JSON.stringify({id:"wrong",jobId:"wrong-job",caseId:openedId,sessionId:sessionDraft.id,text:"Outro job",startMs:100,endMs:200}))');
 js('window.nativeMlSegmentDetailed(JSON.stringify({id:"wrong",jobId:ecoActiveJobId,caseId:"other-case",sessionId:sessionDraft.id,text:"Outro paciente",startMs:100,endMs:200}))');
 js('window.nativeMlSegmentDetailed(JSON.stringify({id:"wrong",jobId:ecoActiveJobId,caseId:openedId,sessionId:"other-session",text:"Outra sessão",startMs:100,endMs:200}))');
 assert.equal(js('sessionDraft.mlSegments.length'),0);
 js('window.nativeMlSegmentDetailed(JSON.stringify({id:"job-test-g5_5000_8800",jobId:ecoActiveJobId,caseId:openedId,sessionId:sessionDraft.id,text:"Sonhei com minha mãe",startMs:5000,endMs:8800}))');
 js('window.nativeMlSegmentDetailed(JSON.stringify({id:"job-test-g5_5000_8800",jobId:ecoActiveJobId,caseId:openedId,sessionId:sessionDraft.id,text:"Sonhei com minha mãe",startMs:5000,endMs:8800}))');
 assert.equal(js('sessionDraft.mlSegments.length'),1);
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
 assert.match(html,/não análise emocional/);
 assert.match(html,/dBFS médio/);
 assert.match(html,/-28\.3/);
 assert.match(html,/gravação inteira/);
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
 js('window.nativeMlState("PARADO");act("ecoOpen",state.cases[0].id);sessionDraft.mlTranscricao="Fala fictícia. Outra fala.";ecoWorkspaceTab="analise";ecoAnalysisScope="todos";window.NativeVault.analyzeSemantic=()=>"OK";window.NativeVault.cancelSemantic=()=>"OK";ecoNeuralStart()');
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

test('ECO 0.14 paciente exclui terapeuta e indefinido mesmo quando existe revisão global',()=>{
 context.scopeFixture={mlSegments:[{text:"Tenho medo de perder meu trabalho.",speaker:"paciente",startMs:0},{text:"Conte sobre sua família.",speaker:"terapeuta",startMs:2000},{text:"Família e trabalho.",speaker:"desconhecido",startMs:3000}],mlRevisada:"Texto revisado de ambos."};
 assert.equal(js('ecoAnalysisRows(scopeFixture,"paciente").length'),1);
 assert.equal(js('ecoAnalysisRows(scopeFixture,"paciente")[0].index'),0);
 assert.equal(js('ecoAnalysisRows(scopeFixture,"terapeuta")[0].index'),1);
 assert.equal(js('ecoAnalysisRows(scopeFixture,"desconhecido")[0].index'),2);
 assert.equal(js('ecoAnalysisRows(scopeFixture,"revisado")[0].speaker'),'desconhecido');
 assert.equal(js('ecoAnalysisRows({mlTranscricao:"Texto sem falante."},"paciente").length'),0);
 assert.ok(!js('ecoWordRows(scopeFixture,"paciente")').some(x=>x.word==="familia"));
 assert.ok(js('ecoWordRows(scopeFixture,"terapeuta")').some(x=>x.word==="familia"));
 js('ecoAnalysisScope="paciente";sessionDraft.mlSegments=scopeFixture.mlSegments;sessionDraft.mlRevisada=scopeFixture.mlRevisada');
 assert.equal(js('ecoNeuralRows().length'),1);
 assert.equal(js('ecoNeuralRows()[0].speaker'),'paciente');
});

test('ECO acústica independe de falas do paciente sem misturar o discurso do terapeuta',()=>{context.acousticFilterFixture={mlSegments:[{text:"Pergunta sobre trabalho",speaker:"terapeuta"}],mlAcoustic:[{atMs:0,rmsDbFs:-21,nearSilenceRatio:0.2}]};js('ecoAnalysisScope="paciente"');const h=js('ecoAnalyticsPanel(acousticFilterFixture)');assert.match(h,/Nenhum termo disponível neste filtro/);assert.match(h,/dBFS médio/);assert.match(h,/-21\.0/);assert.doesNotMatch(h,/Pergunta sobre trabalho/);assert.doesNotMatch(h,/data-id="trabalho"/);});

test('ECO 0.15 Painel Hoje exibe badge ativo do TAGARELA e agenda',()=>{
 js("act('nav','home')");
 const h=js("homeScreen()");
 assert.match(h,/Motor Neural Ativo: TAGARELA PT-BR INT8/);
 assert.match(h,/Hoje · Agenda & Próximo Atendimento/);
 assert.match(h,/Acesso rápido/);
});

test('ECO 0.15 Atlas de Sonhos e Dicionário com Freud e Jung verificáveis',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','sonhos')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Atlas de Sonhos do Paciente/);
 assert.match(h,/Dicionário Teórico de Símbolos Oníricos/);
 assert.match(h,/Sigmund Freud/);
 assert.match(h,/Carl G\. Jung/);
 assert.match(h,/Animais e Forças Instintuais/);
 assert.match(h,/Casas, Andares e Cômodos/);
 assert.match(h,/Parentes e Figuras Familiares/);
 assert.match(h,/Morte e Renascimento/);
 assert.match(h,/Queda e Desamparo/);
 assert.match(h,/Espelhos e Reflexos/);
 assert.match(h,/Dentes e Vulnerabilidade Corporal/);
});

test('ECO 0.15 12 Eixos de Investigação Psicanalítica e Diário de Hipóteses',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','investigar')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Eixos de Investigação Psicanalítica/);
 assert.match(h,/1\. Repetição e Compulsão/);
 assert.match(h,/2\. Ambivalência Afetiva/);
 assert.match(h,/3\. Divergências e Deslocamentos/);
 assert.match(h,/4\. Lacunas e Silêncios/);
 assert.match(h,/5\. Associação Livre e Deriva/);
 assert.match(h,/6\. Família de Origem e Transgeracional/);
 assert.match(h,/7\. Transferência e Suposição de Saber/);
 assert.match(h,/8\. Defesas e Resistência ao Saber/);
 assert.match(h,/9\. Discurso e Significantes-Mestre/);
 assert.match(h,/10\. Sonhos, Fantasias e Imaginário/);
 assert.match(h,/11\. Desejos e Demandas/);
 assert.match(h,/12\. Angústias e Afetos Corporais/);
 assert.match(h,/Diário de Hipóteses Clínicas/);
 assert.match(h,/Evidências favoráveis/);
 assert.match(h,/Evidências contrárias/);
});

test('ECO 0.15 Biblioteca de Técnicas por Escola com Freud, Lacan, Jung, Winnicott, Klein e Bion',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','tecnicas')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Sigmund Freud \(Psicanálise Clássica\)/);
 assert.match(h,/Jacques Lacan \(Retorno a Freud\)/);
 assert.match(h,/Carl G\. Jung \(Psicologia Analítica\)/);
 assert.match(h,/Donald W\. Winnicott \(Relações Objetais\)/);
 assert.match(h,/Melanie Klein &amp; Wilfred Bion|Melanie Klein & Wilfred Bion/);
 assert.match(h,/Associação Livre/);
 assert.match(h,/Pontuação e Escansão/);
 assert.match(h,/Imaginação Ativa/);
 assert.match(h,/Sustentação \(Holding\)/);
 assert.match(h,/Função Alfa e Continência/);
 assert.match(h,/Registro de Intervenções Clínicas/);
});

test('ECO 0.15 Genograma familiar interativo e Linha do Tempo Biográfica',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','genograma')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Genograma Familiar Interativo/);
 assert.match(h,/Linha do Tempo Biográfica/);
 assert.match(h,/Tipo de vínculo relatado/);
 assert.match(h,/Adicionar ao genograma/);
});

test('ECO 0.15 Diário Privado do Terapeuta segregado de relatórios externos',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','diario')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Diário Privado do Terapeuta/);
 assert.match(h,/Contratransferência/);
 assert.match(h,/segregado do prontuário do paciente e nunca é incluído em relatórios/);
});

test('ECO 0.15 Área de Segurança Clínica Manual sem escore automático de risco',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','seguranca')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Área de Segurança Clínica Manual/);
 assert.match(h,/Protocolo conduzido exclusivamente por julgamento humano/);
 assert.match(h,/não realiza triagem algorítmica nem calcula escore automático de risco/);
 assert.match(h,/Plano de segurança pactuado com o paciente/);
});

test('ECO 0.15 Emissão de Documentos Clínicos Formatados A4 com cabeçalho e salvaguardas',()=>{
 js("act('nav','atlas');act('ecoAtlasTab','documentos')");
 const h=js("ecoAtlasScreen()");
 assert.match(h,/Emissão e Impressão de Documentos Clínicos A4/);
 assert.match(h,/Ficha de Acompanhamento Completa/);
 assert.match(h,/Resumo de Acompanhamento/);
 assert.match(h,/Encaminhamento Clínico/);
 assert.match(h,/Síntese para Supervisão/);
 assert.match(h,/Imprimir \/ Salvar PDF/);
 const docHtml=js("ecoFormatClinicalDoc(currentCase(),'ficha',ecoDocSections)");
 assert.match(docHtml,/SISTEMA DE REGISTRO CLÍNICO CONFIDENCIAL/);
 assert.match(docHtml,/Lei Geral de Proteção de Dados/);
 assert.match(docHtml,/Identificação/);
});

test('ECO 0.15 Caso exibe enquadre contratual e anamnese completa em 10 categorias',()=>{
 js("act('openCase',state.cases[0].id);act('caseTab','sintese')");
 const sinteseHtml=js("caseScreen()");
 assert.match(sinteseHtml,/Enquadre Terapêutico e Contrato \(ECO-105\)/);
 assert.match(sinteseHtml,/Frequência acordada/);

 js("act('caseTab','anamnese')");
 const anamneseHtml=js("caseScreen()");
 assert.match(anamneseHtml,/Anamnese Clínica em 10 Categorias Estruturadas \(ECO-014\)/);
 assert.match(anamneseHtml,/1\. Identificação e Dados Sociodemográficos/);
 assert.match(anamneseHtml,/2\. Queixa Principal e Expectativas/);
 assert.match(anamneseHtml,/3\. Infância, Desenvolvimento e Adolescência/);
 assert.match(anamneseHtml,/4\. Família de Origem e Dinâmica Geracional/);
 assert.match(anamneseHtml,/5\. Relações Interpessoais e Vínculos Significativos/);
 assert.match(anamneseHtml,/6\. Sexualidade, Afetividade e Intimidade Pertinente/);
 assert.match(anamneseHtml,/7\. Trabalho, Vida Profissional e Rotina/);
 assert.match(anamneseHtml,/8\. Saúde Geral e Medicações Relatadas/);
 assert.match(anamneseHtml,/9\. Sono, Hábitos, Ritmos e Sintomas Corporais/);
 assert.match(anamneseHtml,/10\. Perdas, Lutos, Recursos Pessoais e Rede de Apoio/);
});

test('ECO 0.16 A16/A17/A18: salvamento com resolução de dataset, multi-caso e retorno de persistência', () => {
  js(`(() => {
    const caseA = state.cases[0];
    const caseB = { ...makeCase(), id: 'case-test-b', nome: 'Caso B Teste' };
    state.cases.push(caseB);

    const elNote = document.getElementById('axis_note_transferencia');
    elNote.value = 'Transferência positiva observada';
    const elStatus = document.getElementById('axis_status_transferencia');
    elStatus.value = 'em curso';
    const elPri = document.getElementById('axis_priority_transferencia');
    elPri.value = 'alta';

    const mockButton = {
      dataset: {
        act: 'ecoSaveAxis',
        axis: 'transferencia',
        case: 'case-test-b'
      }
    };
    act('ecoSaveAxis', 'transferencia', mockButton);
  })()`);

  const updatedB = js("state.cases.find(c => c.id === 'case-test-b')");
  assert.ok(updatedB.investigacaoAxes?.transferencia, 'Eixo de investigação salvo no caso B');
  assert.equal(updatedB.investigacaoAxes.transferencia.nota, 'Transferência positiva observada');
  assert.equal(updatedB.investigacaoAxes.transferencia.status, 'em curso');
  assert.equal(updatedB.investigacaoAxes.transferencia.prioridade, 'alta');

  const updatedA = js("state.cases[0]");
  assert.equal(updatedA.investigacaoAxes?.transferencia, undefined, 'Caso A não foi afetado');
});

test('ECO 0.16 A19: Genograma SVG com gerações G1-G4 e vínculos relacionais interativos', () => {
  js(`(() => {
    const targetC = state.cases[0];
    targetC.genograma = {
      membros: [
        { id: 'm1', nome: 'Ana', parentesco: 'Mãe', geracao: 'G2 (Pais/Tios)', sexo: 'F', vivo: 'sim' },
        { id: 'm2', nome: 'Carlos', parentesco: 'Pai', geracao: 'G2 (Pais/Tios)', sexo: 'M', vivo: 'não' },
        { id: 'm3', nome: 'Paciente', parentesco: 'Paciente titular', geracao: 'G3 (Paciente/Irmãos)', sexo: 'F', vivo: 'sim' }
      ],
      relacoes: [
        { id: 'r1', deId: 'm1', paraId: 'm2', tipo: 'casamento' },
        { id: 'r2', deId: 'm1', paraId: 'm3', tipo: 'pais_filho' }
      ]
    };
  })()`);

  const html = js("ecoRenderGenogram(state.cases[0])");
  assert.match(html, /<svg class="eco-genogram-svg"/);
  assert.match(html, /G1: Avós/);
  assert.match(html, /G2: Pais e Tios/);
  assert.match(html, /G3: Sujeito \/ Parceiros \/ Irmãos/);
  assert.match(html, /G4: Filhos e Descendentes/);
  assert.match(html, /stroke="#38bdf8"/, 'Aresta de casamento desenhada');
  assert.match(html, /stroke="#ef4444"/, 'Traço de falecido para Carlos');
  assert.match(html, /Adicionar vínculo entre familiares/);
});

test('ECO 0.16 A20/A30: Emissão de documentos com pseudonimização real em supervisão, confirmação explícita e filtros', () => {
  js(`(() => {
    const targetC = state.cases[0];
    targetC.nome = 'Maria Madalena';
    targetC.idade = '34';
    targetC.obs = 'Observação sigilosa do caso';
    targetC.sessoes = [
      { id: 's1', data: '2026-11-20', texto: 'A paciente Maria Madalena relatou conflito com a mãe Ana.' },
      { id: 's2', data: '2026-11-23', texto: 'Maria Madalena trouxe um sonho com mar revolto.' }
    ];
  })()`);

  // Test supervision mode (pseudonymized)
  const supHtml = js("ecoFormatClinicalDoc(state.cases[0], 'supervisao', ecoDocSections, { confirmed: false, profNome: 'Dra. Silva', profRegistro: 'CRP 06/12345' })");
  assert.match(supHtml, /Síntese para Supervisão Clínica \(Pseudonimizada\)/);
  assert.match(supHtml, /Faixa adulta \(Pseudonimizada\)/);
  assert.match(supHtml, /\[PACIENTE\]/);
  assert.doesNotMatch(supHtml, /Maria Madalena/);
  assert.match(supHtml, /MINUTA NÃO CONFIRMADA/);

  // Test confirmed document
  const confHtml = js("ecoFormatClinicalDoc(state.cases[0], 'ficha', ecoDocSections, { confirmed: true, profNome: 'Dra. Silva', profRegistro: 'CRP 06/12345' })");
  assert.match(confHtml, /Documento revisado e liberado pelo profissional responsável/);
  assert.match(confHtml, /Dra\. Silva/);
  assert.match(confHtml, /CRP 06\/12345/);
});

test('ECO 0.16 A21: Edição e persistência das 10 categorias de anamnese e enquadre', () => {
  js(`(() => {
    const targetC = state.cases[0];
    document.getElementById('ana_nomeSocial').value = 'M. R.';
    document.getElementById('ana_queixaDetalhada').value = 'Ansiedade em situações de exposição social';
    document.getElementById('ana_nascimentoPrimeirosAnos').value = 'Sem intercorrências no parto';

    act('ecoSaveAnamnese', targetC.id, { dataset: { case: targetC.id } });

    document.getElementById('enq_frequencia').value = 'Bisemanal (50 min)';
    document.getElementById('enq_duracao').value = '50 min';
    document.getElementById('enq_modalidade').value = 'Online';
    document.getElementById('enq_honorarios').value = 'Recibo quinzenal';
    document.getElementById('enq_limites').value = 'Contato restrito a emergências';
    document.getElementById('enq_ferias').value = 'Recesso em janeiro';
    document.getElementById('enq_expectativas').value = 'Elaboração de conflitos subjetivos';

    act('ecoSaveEnquadre', targetC.id, { dataset: { case: targetC.id } });
  })()`);

  const savedCase = js("state.cases[0]");
  assert.equal(savedCase.anamnese?.nomeSocial, 'M. R.');
  assert.equal(savedCase.anamnese?.queixaDetalhada, 'Ansiedade em situações de exposição social');
  assert.equal(savedCase.anamnese?.nascimentoPrimeirosAnos, 'Sem intercorrências no parto');

  assert.equal(savedCase.enquadre?.frequencia, 'Bisemanal (50 min)');
  assert.equal(savedCase.enquadre?.modalidade, 'Online');
  assert.equal(savedCase.enquadre?.honorarios, 'Recibo quinzenal');
});

test('ECO 0.16 A25: Diário do terapeuta segregado com persistência no SecureStore/NativeVault', () => {
  js(`(() => {
    const targetC = state.cases[0];
    let vaultStored = null;
    window.NativeVault.savePrivateDiary = (caseId, json) => { vaultStored = { caseId, json }; return "OK"; };
    window.NativeVault.loadPrivateDiary = (caseId) => vaultStored ? vaultStored.json : "[]";

    document.getElementById('diaryData').value = '2026-11-24';
    document.getElementById('diaryCategoria').value = 'Contratransferência / Afeto suscitado';
    document.getElementById('diaryTexto').value = 'Sensação de angústia contratransferencial diante do silêncio do paciente.';

    act('ecoAddDiaryNote', targetC.id, { dataset: { case: targetC.id } });
  })()`);

  const vaultJson = js("window.NativeVault.loadPrivateDiary(state.cases[0].id)");
  assert.match(vaultJson, /angústia contratransferencial/);

  // Verify backup export does not leak diarioNotas by default
  const exportedJson = js("JSON.stringify({ ...state, cases: state.cases.map(cs => { const cp = { ...cs }; delete cp.diarioNotas; return cp; }) })");
  assert.doesNotMatch(exportedJson, /angústia contratransferencial/, 'Diário confidencial não vaza no backup exportado');
});

test('ECO 0.16 A27: Diferenciação epistemológica do espelho (Lacan 1949 vs Jung)', () => {
  const mirror = js("ecoSymbolDictionary.find(s => s.titulo.includes('Espelho'))");
  assert.ok(mirror, 'Verbete do Espelho presente no dicionário');
  assert.match(mirror.freud, /Inquietante|Narcisismo/);
  assert.match(mirror.lacan, /Estádio do Espelho/);
  assert.match(mirror.lacan, /1949/);
  assert.match(mirror.lacan, /imaginári[ao]/i);
  assert.match(mirror.jung, /Sombra/);
  assert.notEqual(mirror.lacan, mirror.jung, 'Lacan e Jung claramente diferenciados');
});

test('ECO 0.16 A29: Painel Hoje exclui consultas com horários já passados no dia atual', () => {
  const today = js("datestr()");
  const currentClock = js("nowHHMM()");

  // Set an appointment guaranteed to be in the past (00:00) if clock > 00:00, or yesterday
  const pastTime = '00:00';
  const futureTime = '23:59';

  js(`
    state.appointments = [
      { id: 'apt-past', caseId: state.cases[0].id, data: '${today}', hora: '${pastTime}', duracao: 50, status: 'Agendado' },
      { id: 'apt-future', caseId: state.cases[0].id, data: '${today}', hora: '${futureTime}', duracao: 50, status: 'Agendado' }
    ];
  `);

  const landingHtml = js("homeScreen()");
  if (currentClock > pastTime) {
    assert.doesNotMatch(landingHtml, /às 00:00/, 'Consulta com hora 00:00 passada hoje não é listada como próxima');
    assert.match(landingHtml, /às 23:59/, 'Consulta futura às 23:59 é listada como próxima');
  }
});

test('ECO 0.16 Atendimento Manual: Prontuário sem gravação, microfone ou arquivo de áudio', () => {
  const initialCount = js("(state.cases[0].sessoes || []).length");

  js(`(() => {
    const targetC = state.cases[0];
    document.getElementById('manualSessData').value = '2026-11-24';
    document.getElementById('manualSessHora').value = '15:30';
    document.getElementById('manualSessTexto').value = 'Atendimento clínico presencial conduzido sem gravação de áudio.';
    document.getElementById('manualSessInterv').value = 'Interpretação da demanda transferencial.';
    document.getElementById('manualSessPlano').value = 'Retomar questão profissional no próximo encontro.';

    act('saveManualSession', targetC.id, { dataset: { case: targetC.id } });
  })()`);

  const newCount = js("state.cases[0].sessoes.length");
  assert.equal(newCount, initialCount + 1, 'Nova sessão manual adicionada');
  const manualSession = js("state.cases[0].sessoes[state.cases[0].sessoes.length - 1]");
  assert.equal(manualSession.origem, 'manual');
  assert.equal(manualSession.texto, 'Atendimento clínico presencial conduzido sem gravação de áudio.');
  assert.equal(manualSession.intervencoes, 'Interpretação da demanda transferencial.');
});

test('ECO 0.16 Grade de 12 módulos no prontuário do paciente', () => {
  js("act('openCase', state.cases[0].id); act('caseTab', 'sintese')");
  const html = js("caseScreen()");
  assert.match(html, /eco-module-grid/);
  assert.match(html, /Sessões Clínicas/);
  assert.match(html, /Atend\. Manual/);
  assert.match(html, /Análise & Ecos/);
  assert.match(html, /Anamnese/);
  assert.match(html, /Enquadre/);
  assert.match(html, /Investigação/);
  assert.match(html, /Técnicas/);
  assert.match(html, /Sonhos/);
  assert.match(html, /Genograma/);
  assert.match(html, /Cronologia/);
  assert.match(html, /Documentos/);
  assert.match(html, /Diário Privado/);
  assert.match(html, /Segurança/);
});

test('ECO 0.17 A20/A30: Auditoria independente 8 sondagens de documentos e enquadre', () => {
  js(`(() => {
    state.cases.push({
      id: 'probe_test_case',
      nome: 'Maria Madalena',
      idade: '12',
      enquadre: { limites: 'Ligar para Maria Madalena no telefone 99999-1234' },
      anamnese: { queixaDetalhada: 'QUEIXA NOVA ANAMNESE', nomeSocial: 'Nome social' },
      sessoes: [{ data: '2026-11-24', texto: 'Maria Madalena mora na Rua das Flores, 123, CPF 123.456.789-00.' }],
      genograma: { membros: [{ nome: 'Ana (mãe)', parentesco: 'Mãe' }] },
      obs: 'Ana (mãe) está presente.'
    });
  })()`);

  const docHtml = js("ecoFormatClinicalDoc(state.cases.find(x => x.id === 'probe_test_case'), 'supervisao', {identificacao:true, enquadre:true, anamnese:true, sessoes:true, plano:true}, {confirmed:false})");

  // Probe 1: Nome mascarado no enquadre
  assert.doesNotMatch(docHtml, /Ligar para Maria Madalena/, 'Nome cru não deve vazar em limites de enquadre');
  assert.match(docHtml, /Ligar para \[PACIENTE\]/, 'Paciente pseudonimizado');

  // Probe 2: Menor rotulado faixa infantojuvenil
  assert.doesNotMatch(docHtml, /Faixa adulta/, 'Menor de 18 não deve ser rotulado como adulto');
  assert.match(docHtml, /Faixa infantojuvenil/, 'Rotulagem etária correta');

  // Probe 3: Endereço e CPF mascarados
  assert.doesNotMatch(docHtml, /Rua das Flores, 123/, 'Endereço removido');
  assert.doesNotMatch(docHtml, /123\.456\.789-00/, 'CPF removido');
  assert.match(docHtml, /\[ENDEREÇO REMOVIDO\]/);
  assert.match(docHtml, /\[CPF REMOVIDO\]/);

  // Probe 4: Familiar com parênteses mascarado
  assert.doesNotMatch(docHtml, /Ana \(mãe\)/, 'Familiar com parênteses removido');
  assert.match(docHtml, /\[Mãe\]/, 'Parentesco mantido de forma segura');

  // Probe 5: queixaDetalhada presente
  assert.match(docHtml, /QUEIXA NOVA ANAMNESE/, 'Queixa detalhada da anamnese deve constar no documento');

  // Probe 6: Bloqueio de impressão sem confirmação
  let printedCount = 0;
  context.window.NativeVault.printReport = () => { printedCount++; };
  js("act('ecoPrintDoc', 'probe_test_case', {dataset:{case:'probe_test_case'}})");
  assert.equal(printedCount, 0, 'Minuta não confirmada não pode ser impressa');

  // Probe 7: Rejeição de caso inexistente sem fallback para primeiro paciente
  js("act('ecoPrintDoc', 'caso_inexistente_999', {dataset:{case:'caso_inexistente_999'}})");
  assert.equal(printedCount, 0, 'Caso inexistente não pode imprimir outro paciente por fallback');

  // Probe 8: Falha em gravação dedicada não duplica diário no cofre principal
  context.window.NativeVault.savePrivateDiary = () => 'ERRO: disco cheio';
  js(`(() => {
    document.getElementById('diaryTexto').value = 'REFLEXAO_PRIVADA_CONFIDENCIAL';
    act('ecoAddDiaryNote', 'probe_test_case', {dataset:{case:'probe_test_case'}});
  })()`);
  const probeCase = js("state.cases.find(x => x.id === 'probe_test_case')");
  assert.equal(probeCase.diarioNotas, undefined, 'Diário privado jamais deve residir dentro do caso do paciente');
});

test('ECO 0.17 A28: Container #ecoSymbolResults preserva foco e teclado móvel', () => {
  js("act('openCase', state.cases[0].id); act('caseTab', 'sonhos')");
  const html = js("caseScreen()");
  assert.match(html, /id=\"ecoSymbolResults\"/, 'Container de resultados oníricos presente no DOM');
});

test('ECO 0.17 Arquitetura: Banco de dados relacional cifrado e áudio efêmero', () => {
  const dbHelper = fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/EcoDatabaseHelper.java', import.meta.url), 'utf8');
  const store = fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/SecureStore.java', import.meta.url), 'utf8');
  const recorder = fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/SessionRecorder.java', import.meta.url), 'utf8');

  assert.match(dbHelper, /CREATE (?:TABLE|VIEW) IF NOT EXISTS pacientes/);
  assert.match(dbHelper, /CREATE TABLE IF NOT EXISTS sessoes/);
  assert.match(dbHelper, /CREATE TABLE IF NOT EXISTS segmentos/);
  assert.match(dbHelper, /CREATE TABLE IF NOT EXISTS diario_privado/);
  assert.match(dbHelper, /CREATE TABLE IF NOT EXISTS processamento_jobs/);
  assert.match(dbHelper, /commitSegment/);
  assert.match(dbHelper, /confirmSpeaker/);

  assert.match(store, /EcoDatabaseHelper/);
  assert.match(store, /dbHelper\.savePrivateDiary/);
  assert.match(store, /dbHelper\.readPrivateDiary/);

  assert.match(recorder, /AES\/(?:GCM|CTR)\/NoPadding/);
  assert.match(recorder, /Arrays\.fill\(ephemeralKey/);
});


// Behavioral test: two private diary saves preserve both records; deletion is scoped.
test('ECO 0.23 diário: append, delete and failure are scoped and durable',()=>{
 const vault=context.window.NativeVault;
 let notes=[{id:'existing',texto:'Primeira nota'}];
 vault.loadPrivateDiary=()=>JSON.stringify(notes);
 vault.savePrivateDiary=(caseId,raw)=>{notes=JSON.parse(raw);return 'OK'};
 js("ecoStatus='PARADO'; openedId=state.cases[0].id; screen='case'");
 js("document.getElementById('diaryTexto').value='Segunda nota'; act('ecoAddDiaryNote',openedId,{dataset:{case:openedId}})");
 assert.equal(notes.length,2);assert.equal(notes[0].id,'existing');
 js("act('ecoDeleteDiaryNote','existing',{dataset:{case:openedId,id:'existing'}})");
 assert.equal(notes.length,1);assert.equal(notes[0].texto,'Segunda nota');
 vault.savePrivateDiary=()=> 'ERRO: disco cheio';
 js("document.getElementById('diaryTexto').value='Terceira nota'; act('ecoAddDiaryNote',openedId,{dataset:{case:openedId}})");
 assert.equal(notes.length,1);assert.equal(js('currentCase().diarioNotas'),undefined);
});

test('Ficha evolutiva exige fonte literal, preserva isolamento, sinaliza fonte alterada',()=>{
 const sample={id:"ficha-a",nome:"Carla fictícia",sessoes:[{id:"fa1",data:"2026-11-01",texto:"Registro manual de sono.",mlSegments:[{speaker:"paciente",text:"Tenho medo de pedir ajuda.",startMs:0},{speaker:"terapeuta",text:"Isso é uma pergunta.",startMs:1000}],mlTranscricao:"Tenho medo de pedir ajuda. Isso é uma pergunta.",mlEvidence:[]}]};
 context.fichaSample=sample;
 assert.equal(js('ecoFichaValid(fichaSample,{sessionId:"fa1",origin:"paciente",quote:"Tenho medo de pedir ajuda."})'),true);
 assert.equal(js('ecoFichaValid(fichaSample,{sessionId:"fa1",origin:"paciente",quote:"Isso é uma pergunta."})'),false);
 assert.equal(js('ecoFichaValid(fichaSample,{sessionId:"outro",origin:"manual",quote:"Registro manual"})'),false);
 assert.equal(js('ecoLongRows(fichaSample).length'),1);
 assert.equal(js('ecoLongRows(fichaSample,"2026-11-02").length'),0);
 js('fichaSample.fichaEvolutiva=[{id:"obs",kind:"Sono",sessionId:"fa1",origin:"manual",quote:"Registro manual",text:"Relato de sono"}]');
 assert.match(js('ecoFichaBody(fichaSample)'),/Relato de sono/);
 js('fichaSample.sessoes[0].texto="Fonte modificada"');
 assert.match(js('ecoFichaBody(fichaSample)'),/Fonte alterada ou removida/);
});

test('Grade do paciente visível antes do resumo, com acesso direto à anamnese e ficha',()=>{
 js("openedId=state.cases[0].id;screen='case';tab='sintese'");
 const view=js("caseScreen()");
 assert.ok(view.indexOf("eco-module-grid")<view.indexOf("Resumo do caso"));
 assert.match(view,/button type="button" class="eco-module-card" data-act="caseTab" data-id="anamnese"/);
 assert.match(view,/data-id="fichaEvolutiva"/);
 js("act('caseTab','anamnese')");
 assert.match(js("caseScreen()"),/Anamnese Clínica em 10 Categorias/);
 js("act('caseTab','sintese')");
 assert.match(js("caseScreen()"),/Módulos do paciente/);
});

test('Vosk rápido é selecionável e não troca silenciosamente para Whisper',()=>{
 js("act('ecoOpen',state.cases[0].id);ecoChosenMic='vosk';ecoChosenAsr='vosk';ecoWorkspaceTab='captura'");
 const view=js("sessionScreen()");
 assert.match(view,/value="vosk" selected/);
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const engine=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperFileEngine.kt',import.meta.url),'utf8');
 assert.match(ml,/fileModelName="vosk"\.equals\(engine\)\?"vosk"/);
 assert.match(engine,/if\(voskFast\)/);
 js("ecoChosenMic='tagarela';ecoChosenAsr='tagarela'");
});
