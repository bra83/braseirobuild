import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'App.tsx')))throw Error('OUA Android Studio project missing');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const save=(p,v)=>{const f=path.join(root,p);fs.mkdirSync(path.dirname(f),{recursive:true});fs.writeFileSync(f,v,'utf8');};
function swap(s,from,to,name){const i=s.indexOf(from);if(i<0)throw Error('NO_ANCHOR_'+name);return s.slice(0,i)+to+s.slice(i+from.length);}
function replaceSection(s,from,to,newText,name){const a=s.indexOf(from),b=s.indexOf(to,a+from.length);if(a<0||b<=a)throw Error('NO_SECTION_'+name);return s.slice(0,a)+newText+s.slice(b);}
const names=['Zephyr','Puck','Charon','Kore','Fenrir','Leda','Orus','Aoede','Callirrhoe','Autonoe','Enceladus','Iapetus','Umbriel','Algieba','Despina','Erinome','Algenib','Rasalgethi','Laomedeia','Achernar','Alnilam','Schedar','Gacrux','Pulcherrima','Achird','Zubenelgenubi','Vindemiatrix','Sadachbia','Sadaltager','Sulafat'];
const config=[
"export const TTS_VOICES = "+JSON.stringify(names)+" as const;",
"export type VoiceChoice = typeof TTS_VOICES[number];",
"export interface BraseiroTtsConfig {voice:VoiceChoice; vocalProfile:string; vocalStyle:string; modulation:number;}",
"export const DEFAULT_TTS_CONFIG: BraseiroTtsConfig={",
" voice:'Charon',vocalProfile:'Voz adulta madura, acolhedora, português brasileiro natural, um só mestre de RPG.',",
" vocalStyle:'Narração imersiva, ritmo conversacional, emoção natural, sem locução publicitária.',modulation:25};",
"const KEY='braseiro.tts.mestreUnico.v2';",
"export function getBraseiroTtsConfig():BraseiroTtsConfig {",
" if(typeof window==='undefined')return {...DEFAULT_TTS_CONFIG};",
" try {const v=JSON.parse(window.localStorage.getItem(KEY)||'{}');",
" const voice:TtsVoiceChoice=TTS_VOICES.includes(v.voice)?v.voice:'Charon';",
" return {voice,vocalProfile:String(v.vocalProfile||DEFAULT_TTS_CONFIG.vocalProfile).slice(0,750),",
" vocalStyle:String(v.vocalStyle||DEFAULT_TTS_CONFIG.vocalStyle).slice(0,750),",
" modulation:Math.max(0,Math.min(50,Number.isFinite(+v.modulation)?+v.modulation:25))};",
" } catch {return {...DEFAULT_TTS_CONFIG};} }",
"export type TtsVoiceChoice=VoiceChoice;",
"export function saveBraseiroTtsConfig(v:BraseiroTtsConfig) {window.localStorage.setItem(KEY,JSON.stringify(v));}",
"export function voiceDirection(c:BraseiroTtsConfig,character:string,emotion:string,narrator:boolean):string {",
" const types=['timbre ligeiramente mais grave e firme','timbre ligeiramente mais suave e leve','cadência um pouco mais lenta e áspera','cadência um pouco mais viva'];",
" const index=Array.from(character||'personagem').reduce((h,ch)=>(h*31+ch.charCodeAt(0))>>>0,7)%types.length;",
" const change=narrator?'narrador principal':c.modulation<5?'voz original sem diferença':types[index];",
" const strength=c.modulation+' por cento, sempre discreto';",
" return [c.vocalProfile,c.vocalStyle,'mesma identidade vocal do Mestre durante toda a cena',change,'intensidade da modulação: '+strength,'emoção: '+(emotion||'natural')].join('; ');",
"}"
].join('\n')+'\n';
save('lib/braseiroTtsConfig.ts',config);
const panel=[
"import React,{useState} from 'react';",
"import {GEMINI_TTS_MODEL_OPTIONS,getGeminiTtsModel,saveGeminiTtsModel} from '../lib/geminiModels';",
"import {TTS_VOICES,getBraseiroTtsConfig,saveBraseiroTtsConfig,type BraseiroTtsConfig} from '../lib/braseiroTtsConfig';",
"export function BraseiroTtsPanel(){",
" const [model,setModel]=useState(()=>getGeminiTtsModel());",
" const [config,setConfig]=useState<BraseiroTtsConfig>(()=>getBraseiroTtsConfig());",
" const [saved,setSaved]=useState(false);",
" function update(next:BraseiroTtsConfig){saveBraseiroTtsConfig(next);setConfig(next);setSaved(true);}",
" return <section aria-label='Configurações Braseiro TTS' className='mt-6 mb-24 rounded-xl border border-amber-700/60 bg-slate-950/90 p-4 text-slate-100 shadow-lg space-y-4'>",
"  <h2 className='text-xl font-semibold text-amber-200'>Braseiro TTS · Mestre Único</h2>",
"  <p className='text-sm text-slate-300'>Uma voz principal com modulação discreta para NPCs. As alterações são aplicadas à próxima narração.</p>",
"  <label className='block text-sm font-semibold'>Modelo Gemini TTS",
"   <select aria-label='Modelo Gemini TTS' className='mt-2 w-full rounded bg-slate-900 border border-slate-600 p-3 text-slate-100' value={model} onChange={e=>{saveGeminiTtsModel(e.target.value);setModel(e.target.value);setSaved(true);}}>",
"    {GEMINI_TTS_MODEL_OPTIONS.map(m=><option key={m.id} value={m.id}>{m.label}</option>)}",
"   </select>",
"  </label>",
"  <label className='block text-sm font-semibold'>Voz do Mestre — catálogo de vozes Gemini",
"   <select aria-label='Voz do Mestre' className='mt-2 w-full rounded bg-slate-900 border border-slate-600 p-3 text-slate-100' value={config.voice} onChange={e=>update({...config,voice:e.target.value as BraseiroTtsConfig['voice']})}>",
"     {TTS_VOICES.map(v=><option key={v} value={v}>{v}</option>)}",
"   </select>",
"  </label>",
"  <p className='text-xs text-slate-400'>30 vozes predefinidas. A disponibilidade depende do modelo selecionado; este catálogo é integrado ao aplicativo, não sincronizado em tempo real.</p>",
"  <label className='block text-sm font-semibold'>Perfil vocal",
"   <textarea aria-label='Perfil vocal' rows={3} className='mt-2 w-full rounded bg-slate-900 border border-slate-600 p-3 text-slate-100' value={config.vocalProfile} onChange={e=>update({...config,vocalProfile:e.target.value})}/>",
"  </label>",
"  <label className='block text-sm font-semibold'>Estilo vocal",
"   <textarea aria-label='Estilo vocal' rows={3} className='mt-2 w-full rounded bg-slate-900 border border-slate-600 p-3 text-slate-100' value={config.vocalStyle} onChange={e=>update({...config,vocalStyle:e.target.value})}/>",
"  </label>",
"  <label className='block text-sm font-semibold'>Modulação dos NPCs: {config.modulation}%",
"   <input aria-label='Modulação dos NPCs' className='mt-3 w-full' type='range' min='0' max='50' step='5' value={config.modulation} onChange={e=>update({...config,modulation:Number(e.target.value)})}/>",
"  </label>",
"  {saved&&<p role='status' className='text-emerald-300 text-sm'>Configuração salva neste dispositivo.</p>}",
"  <p className='text-xs text-slate-400'>Kokoro e Android TTS permanecem separados. Nenhuma chave paga será ativada automaticamente.</p>",
" </section>;",
"}"
].join('\n')+'\n';
save('components/BraseiroTtsPanel.tsx',panel);
{
 let s=read('types.ts');
 const old="export type TtsVoiceName = 'Charon' | 'Kore' | 'Puck' | 'Fenrir' | 'Aoede' | 'Gacrux' | 'Algenib' | 'Sulafat';";
 s=swap(s,old,"export type TtsVoiceName = "+names.map(x=>"'"+x+"'").join(' | ')+";",'voice-type');
 save('types.ts',s);
}
{
 let s=read('services/geminiService.ts');
 s=swap(s,"import { GeminiStoryResponse, StoryChoice, TtsVoiceName } from '../types';","import { GeminiStoryResponse, StoryChoice, TtsVoiceName } from '../types';\nimport { TTS_VOICES } from '../lib/braseiroTtsConfig';",'service-import');
 s=swap(s,"const ALLOWED_TTS_VOICES: TtsVoiceName[] = ['Charon', 'Kore', 'Puck', 'Fenrir', 'Aoede', 'Gacrux', 'Algenib', 'Sulafat'];","const ALLOWED_TTS_VOICES: TtsVoiceName[] = [...TTS_VOICES];",'service-voice-list');
 save('services/geminiService.ts',s);
}
{
 let s=read('App.tsx');
 s=swap(s,"import { SettingsView } from './components/SettingsView';","import { SettingsView } from './components/SettingsView';\nimport { BraseiroTtsPanel } from './components/BraseiroTtsPanel';\nimport { getBraseiroTtsConfig,voiceDirection } from './lib/braseiroTtsConfig';",'app-import');
 const block=[
"  const voiceForSegment = (segment: VoiceSegment): { voice: TtsVoiceName; style: string } => {",
"    const c=getBraseiroTtsConfig();",
"    const narrator=segment.speakerType==='narrator';",
"    const npc=narrator?undefined:(segment.speakerId?allNpcs.find(n=>n.id===segment.speakerId):allNpcs.find(n=>normalizeText(n.name)===normalizeText(segment.speakerName)));",
"    const voice=c.voice;",
"    const style=voiceDirection(c,segment.speakerId||segment.speakerName||'mestre',segment.emotion||npc?.emotionalState||'natural',narrator);",
"    return { voice, style:narrator?style+'; '+CHARON_NARRATOR_STYLE:style };",
"  };",
""].join('\n')+'\n';
 s=replaceSection(s,"  const voiceForSegment = (segment: VoiceSegment): { voice: TtsVoiceName; style: string } => {","  const playCharonNarration = async (node: SceneNode) => {",block,'voice-routing');
 const from="      case 'configuracoes': return <SettingsView ";
 const i=s.indexOf(from);
 if(i<0)throw Error('CONFIG_TAB_MISSING');
 const end=s.indexOf('\n      case ',i+from.length);
 if(end<0)throw Error('CONFIG_TAB_END_MISSING');
 const line=s.slice(i,end).trimEnd();
 if(!line.endsWith('/>;'))throw Error('CONFIG_TAB_UNEXPECTED');
 const changed=line.replace('return <SettingsView ','return <><SettingsView ').replace(/ \/>;$/,' /><BraseiroTtsPanel /></>;');
 s=s.slice(0,i)+changed+s.slice(end);
 save('App.tsx',s);
}
for(const [p,needle] of [['App.tsx','<BraseiroTtsPanel />'],['components/BraseiroTtsPanel.tsx','Perfil vocal'],['components/BraseiroTtsPanel.tsx','Estilo vocal'],['components/BraseiroTtsPanel.tsx','Voz do Mestre'],['components/BraseiroTtsPanel.tsx','Modulação dos NPCs']]){
 if(!read(p).includes(needle))throw Error('UI_INTEGRATION_FAILED_'+p+'_'+needle);
}
console.log('BRASEIRO_TTS_VISIBLE_SETTINGS_PASS model=1 voices=30 profile=1 style=1 modulation=1');
