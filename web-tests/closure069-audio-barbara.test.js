import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {BARBARA_113_CANONICAL,BARBARA_113_AUDIO_MIX,barbara113AudioScenePlan} from '../app/src/main/assets/www/core/barbara-113-contract.js';
import {deriveAudioContext,presentationAudioEvents,stableNpcVoiceProfile,createAudioSessionCoordinator} from '../app/src/main/assets/www/core/audio-session.js';
import {TtsPresentationService} from '../app/src/main/assets/www/core/tts-presentation.js';

const ROOT=new URL('../',import.meta.url);const read=p=>fs.readFileSync(new URL(p,ROOT),'utf8');

test('contrato Barbara 1.1.3 preserva protocolo v2 e autoridade mecânica no host WWN',()=>{
  assert.equal(BARBARA_113_CANONICAL.version,'1.1.3');assert.equal(BARBARA_113_CANONICAL.protocolVersion,2);assert.equal(BARBARA_113_CANONICAL.systemId,'worlds_without_number');
  for(const d of ['canonical_state','rules','atlas','rag_sources','travel','time','world','inventory','combat'])assert.equal(BARBARA_113_CANONICAL.authority[d],'host');
  assert.equal(BARBARA_113_CANONICAL.authority.audio,'shared');
  assert.equal(BARBARA_113_CANONICAL.androidSha256,'2d886840aaa933b866a6b6c9d3c452aaffff725147fd81ec567e300a7d2367ca');
});

test('diretor de áudio não lê prosa e nunca altera estado canônico',()=>{
  const events=[{type:'dungeon_entered',event_id:'e1',payload:{}},{type:'combat_started',event_id:'e2',payload:{}}];
  const plan=barbara113AudioScenePlan(events,{context:{location_name:'Ruínas da Vigília',tags:['dungeon']}});
  assert.equal(plan.canonical_state_affected,false);assert.equal(plan.asset_owner,'host');assert.equal(plan.selection_owner,'barbara');
  assert.ok(plan.commands.some(c=>c.channel==='ambience'&&c.asset==='dungeon'));assert.ok(plan.commands.some(c=>c.channel==='music'&&c.asset==='combat'));
  assert.doesNotMatch(JSON.stringify(plan),/segredo|narrativeText|narration prose/i);
});

test('ducking de narração segue exatamente 0.42 música e 0.62 ambiência',()=>{
  const plan=barbara113AudioScenePlan([{type:'combat_started'}],{narrationActive:true});
  assert.equal(plan.mix.effective_music_gain,BARBARA_113_AUDIO_MIX.music_gain*.42);assert.equal(plan.mix.effective_ambience_gain,BARBARA_113_AUDIO_MIX.ambience_gain*.62);
});

test('contexto WWN deriva chuva/dungeon de estado, não de texto narrativo',()=>{
  const state={worldState:{position:{name:'Ruínas da Vigília'},livingWorld:{weather:{label:'CHUVA LEVE'},dayPart:'NOITE'}}};
  const ctx=deriveAudioContext({state,scene:{title:'A porta'}});assert.ok(ctx.tags.includes('rain'));assert.ok(ctx.tags.includes('dungeon'));assert.ok(ctx.tags.includes('night'));
  const ev=presentationAudioEvents({state,scene:{title:'A porta'},combatActive:false});assert.ok(ev.some(e=>e.type==='rain_started'));assert.ok(ev.some(e=>e.type==='dungeon_entered'));
});

test('perfil de voz de NPC é determinístico, estável e dentro dos limites do TTS',()=>{
  const a=stableNpcVoiceProfile('npc:iria:ferreira'),b=stableNpcVoiceProfile('npc:iria:ferreira'),c=stableNpcVoiceProfile('npc:iria:guia');assert.deepEqual(a,b);assert.notDeepEqual(a,c);assert.ok(a.pitch>=.7&&a.pitch<=1.35);assert.ok(a.rate>=.55&&a.rate<=1.45);assert.equal(a.role,'NPC');
});

test('coordenador envia plano nativo sem mutar estado mecânico',()=>{
  let got=null;const native={audioStatus:()=>JSON.stringify({status:'OK',ready:true}),audioApplyScenePlan:j=>{got=JSON.parse(j);return JSON.stringify({status:'OK'})}};const audio=createAudioSessionCoordinator({native});const state={revision:9,hp:3,worldState:{position:{name:'Vila de Iria'}}};const before=structuredClone(state);const out=audio.sync({state,combatActive:false});assert.equal(out.receipt.status,'OK');assert.deepEqual(state,before);assert.equal(got.canonical_state_affected,false);assert.ok(got.commands.some(c=>c.asset==='settlement'));
});

test('TTS usa uma sessão sem vozes simultâneas e chunks seguintes entram em fila',async()=>{
  const calls=[];const provider={speak:async x=>(calls.push(x),{status:'QUEUED'}),stop:()=>({status:'OK'})};const svc=new TtsPresentationService({provider});const text='Primeiro parágrafo. '.repeat(65)+'\n\nSegundo bloco.';const r=await svc.speakVisible({text,rate:1,role:'NARRADOR'});assert.equal(r.status,'OK');assert.ok(calls.length>=2);assert.equal(new Set(calls.map(c=>c.sessionId)).size,1);assert.equal(calls[0].append,false);assert.ok(calls.slice(1).every(c=>c.append===true));assert.ok(calls.every(c=>c.language==='pt-BR'));
});

test('fonte Android contém lifecycle, stop real, áudio procedural e bridge em PT-BR',()=>{
  const main=read('app/src/main/java/com/braseiro/wwn/MainActivity.kt'),audio=read('app/src/main/java/com/braseiro/wwn/bridge/NativeAudioRuntime.kt'),tts=read('app/src/main/java/com/braseiro/wwn/bridge/NativeTtsRuntime.kt'),bridge=read('app/src/main/java/com/braseiro/wwn/bridge/BraseiroBridge.kt');
  assert.match(main,/audioRuntime\.pauseForLifecycle\(\)/);assert.match(main,/audioRuntime\.resumeFromLifecycle\(\)/);assert.match(main,/audioRuntime\.shutdown\(\)/);assert.match(audio,/AudioTrack\.Builder/);assert.match(audio,/narrationActive/);assert.match(tts,/QUEUE_FLUSH/);assert.match(tts,/setPitch/);assert.match(bridge,/audioApplyScenePlan/);assert.match(bridge,/audioStopAll/);
});

test('regressão: primeiro launch é silencioso e synth não usa fallback de ruído branco',()=>{
  const audio=read('app/src/main/java/com/braseiro/wwn/bridge/NativeAudioRuntime.kt'),synth=read('app/src/main/java/com/braseiro/wwn/audio/ProceduralAudioSynth.java');
  assert.match(audio,/KEY_SAFE_AUDIO_DEFAULTS_APPLIED/);
  assert.match(audio,/getBoolean\(KEY_AMBIENCE, false\)/);
  assert.match(audio,/getBoolean\(KEY_MUSIC, false\)/);
  assert.match(synth,/smoothNoise/);
  assert.match(synth,/default -> sample = 0\.0/);
  assert.doesNotMatch(synth,/default -> sample = 0\.07 \* noise/);
});

test('Configurações expõe controle completo de áudio em PT-BR',()=>{const h=read('app/src/main/assets/www/configuracoes.html');for(const id of ['audioAmbience','audioMusic','audioSfx','audioNpcVoice','audioMasterVoice','audioVolume','testAmbience','testNpcVoice','stopAudio'])assert.match(h,new RegExp(`id="${id}"`));assert.match(h,/SESSÃO, AMBIÊNCIA E VOZES/);assert.match(h,/PARAR TUDO/);});
