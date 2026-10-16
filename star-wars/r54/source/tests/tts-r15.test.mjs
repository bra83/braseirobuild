import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { speakNarration, stopNarration, normalizeTtsRate } from '../app/src/main/assets/vtt/js/ui/tts.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

class FakeUtterance { constructor(text){this.text=text;this.lang='';this.rate=1;} }
class FakeSpeech { constructor(){this.spoken=[];this.cancelled=0;} cancel(){this.cancelled++} speak(u){this.spoken.push(u)} }
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('TTS lê exatamente a narração recebida, em PT-BR, e cancela fala anterior',()=>{
  const speech=new FakeSpeech();
  const r=speakNarration('A porta do hangar se abre.',{speech,Utterance:FakeUtterance,rate:1.2});
  assert.equal(r.text,'A porta do hangar se abre.'); assert.equal(r.lang,'pt-BR'); assert.equal(r.rate,1.2);
  assert.equal(speech.cancelled,1); assert.equal(speech.spoken.length,1); assert.equal(speech.spoken[0].text,r.text);
  assert.equal(stopNarration({speech}),true); assert.equal(speech.cancelled,2);
});

test('TTS falha claramente quando a API de voz está indisponível e normaliza velocidade',()=>{
  assert.throws(()=>speakNarration('Texto',{speech:null,Utterance:null}),/não está disponível/i);
  assert.equal(normalizeTtsRate(99),1.4); assert.equal(normalizeTtsRate(0.1),0.6);
});

test('ajustes de TTS persistem no estado sem afetar configuração do Mestre',()=>{
  const storage=new MemoryStorage(); const key='r15-tts';
  const game=new GameController(new LocalStorageRepository(storage,key));
  game.configureTts({enabled:true,rate:1.2});
  const reopened=new GameController(new LocalStorageRepository(storage,key));
  assert.equal(reopened.state.settings.ttsEnabled,true); assert.equal(reopened.state.settings.ttsRate,1.2); assert.equal(reopened.state.settings.masterProvider,'none');
});

test('interface oferece voz somente para a narração visível da Cena',()=>{
  const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
  const js=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
  assert.match(html,/id="ttsPlayNarration"[^>]*>Ouvir narração/);
  assert.match(html,/TTS lê somente a narração visível da cena/);
  assert.match(js,/speakNarration\(game\.state\.scene\?\.narration/);
  assert.doesNotMatch(js,/speakNarration\([^\n]*(character|rules|world)/i);
});
