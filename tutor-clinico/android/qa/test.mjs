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
 assert.ok(kt.includes('transcribeChunk(false)'));
 assert.match(kt,/WhisperConfig\(language = "pt"/);
 assert.match(kt,/File.createTempFile/);
 assert.match(kt,/finally/);
 assert.match(kt,/it.delete\(\)/);
 assert.match(app,/whisper-android:1.0.0/);
 assert.match(wf,/ggml-base.bin/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/Whisper offline/);
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


test('ECO G3 Whisper microphone uses bounded queue and audio-only ephemeral chunks',()=>{
 const kt=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/WhisperLiveEngine.kt',import.meta.url),'utf8');
 const ml=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MlSessionEngine.java',import.meta.url),'utf8');
 const main=fs.readFileSync(new URL('../app/src/main/java/br/com/tutorclinico/MainActivity.java',import.meta.url),'utf8');
 assert.match(kt,/ArrayBlockingQueue<Chunk>\(2\)/);
 assert.match(kt,/AudioRecord.Builder/);
 assert.match(kt,/queue\.offer\(Chunk/);
 assert.match(kt,/File\.createTempFile\(CACHE_PREFIX, ".wav"/);
 assert.match(kt,/if \(!file\.delete\(\)\)/);
 assert.match(kt,/Whisper\.transcribe/);
 assert.match(kt,/listener\.segment/);
 assert.match(kt,/processamento mais lento que a gravação/);
 assert.match(ml,/beginWhisperMicrophone/);
 assert.match(ml,/live\.finishRecording/);
 assert.match(main,/startMlCaptureWithEngine/);
 assert.match(main,/WhisperLiveEngine\.clearStaleFiles/);
 js("act('ecoOpen',state.cases[0].id);act('ecoWorkspaceTab','captura')");
 assert.match(js("sessionScreen()"),/Motor para o microfone/);
 assert.match(js("sessionScreen()"),/Whisper Base offline/);
 assert.match(js("sessionScreen()"),/Vosk · rápido/);
});
test('ECO G3 live mic engine selection passes to Android bridge and blocks parallel imports',()=>{
 const cid=js('state.cases[0].id');
 js('act("ecoOpen",cid);ecoConsent=true;ecoChosenMic="whisper-base"');
 context.window.NativeVault.saveDraft=()=> 'OK';
 context.window.NativeVault.startMlCaptureWithEngine=(consent,engine)=>{context.liveChoice={consent,engine};};
 js('act("ecoStart")');
 assert.deepEqual(context.liveChoice,{consent:true,engine:'whisper-base'});
 assert.equal(js("ecoIsCapturing()"),true);
 js('window.nativeMlState("FINALIZANDO MICROFONE · TRANSCRIÇÃO DE TRECHOS PENDENTES")');
 assert.equal(js("ecoIsCapturing()"),true);
 js('window.nativeMlState("CONCLUÍDO · MICROFONE WHISPER BASE · REVISE O TEXTO")');
 assert.equal(js("ecoIsCapturing()"),false);
});

console.log('Teste de comportamento executado sem Android SDK; requer ainda compilação/teste no aparelho.');
