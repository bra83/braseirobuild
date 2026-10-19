import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'App.tsx')))throw Error('OUA source missing');
const get=p=>fs.readFileSync(path.join(root,p),'utf8');
const put=(p,s)=>fs.writeFileSync(path.join(root,p),s,'utf8');
function replace(s,a,b,name){const i=s.indexOf(a);if(i<0||s.indexOf(a,i+1)>=0)throw Error('ANCHOR:'+name);return s.slice(0,i)+b+s.slice(i+a.length);}
function slice(s,a,b,v,name){const i=s.indexOf(a),j=s.indexOf(b,i+a.length);if(i<0||j<=i)throw Error('SECTION:'+name);return s.slice(0,i)+v+s.slice(j);}
const nl='\n';
{
let s=get('lib/geminiModels.ts');
s=replace(s,"  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — recomendado' },",[
"  { id: 'gemini-3.8-flash-tts', label: 'Gemini 3.8 Flash TTS — recomendado' },",
"  { id: 'gemini-3.8-flash-lite-tts', label: 'Gemini 3.8 Flash-Lite TTS — menor latência' },",
"  { id: 'gemini-3.1-flash-tts-preview', label: 'Gemini 3.1 Flash TTS Preview — compatibilidade' },"].join(nl),'models');
s=replace(s,"const TTS_DEFAULT = 'gemini-3.1-flash-tts-preview';","const TTS_DEFAULT = 'gemini-3.8-flash-tts';",'default');
put('lib/geminiModels.ts',s);
}
{
let s=get('services/geminiService.ts');
s=replace(s,"const DEFAULT_TTS_MODELS = ['gemini-3.1-flash-tts-preview', 'gemini-2.5-flash-preview-tts'];","const DEFAULT_TTS_MODELS = ['gemini-3.8-flash-lite-tts', 'gemini-3.1-flash-tts-preview'];",'fallback');
s=replace(s,'tts-v9-\${safeVoice}-pt-BR-1.0','tts-v10-\${safeVoice}-pt-BR-1.0','cache');
const v=[
"          const modern = model.startsWith('gemini-3.8-');",
"          const transcript = normalizedText.replace(/\\[(?:tom|emoção|estilo|voz):[^\\]]+\\]/gi,'').trim();",
"          const part = modern ? { text: transcript, speech_metadata: { style: stableStyle } }",
"            : { text: 'Leia apenas a narração em português brasileiro: ' + transcript };",
"          const response = await generateGemini(model,",
"            [{ role:'user', parts:[part] }],",
"            {responseModalities:['AUDIO'],speechConfig:{voiceConfig:modern",
"              ? {voice:safeVoice} : {prebuiltVoiceConfig:{voiceName:safeVoice}}}},",
"          );",
""].join(nl);
s=slice(s,'          const response = await generateGemini('+nl,"          const audio = response.candidates?.[0]?.content?.parts?.find(part => part.inlineData)?.inlineData?.data;",v,'browser synthesis');
put('services/geminiService.ts',s);
}
{
let s=get('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
s=replace(s,'tts-v9|$model|$safeVoice|pt-BR|$stableStyle|$text','tts-v10|$model|$safeVoice|pt-BR|$stableStyle|$text','cache-native');
const v=[
'        val modern = model.startsWith("gemini-3.8-")',
'        val transcript = text.replace(Regex("\\\\[(tom|emoção|estilo|voz):[^\\\\]]+\\\\]", RegexOption.IGNORE_CASE), "").trim()',
'        val part = JSONObject().put("text", if (modern) transcript else "Leia apenas a narração em português brasileiro: $transcript")',
'        if (modern) part.put("speech_metadata", JSONObject().put("style", stableStyle))',
'        val voiceConfig = if (modern) JSONObject().put("voice", safeVoice)',
'            else JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", safeVoice))',
'        val body = JSONObject().apply {',
'            put("contents", JSONArray().put(JSONObject().put("role","user").put("parts", JSONArray().put(part))))',
'            put("generationConfig", JSONObject().apply {',
'                put("responseModalities", JSONArray().put("AUDIO"))',
'                put("speechConfig", JSONObject().put("voiceConfig", voiceConfig))',
'            })',
'        }.toString()',
''].join(nl);
s=slice(s,'        val prompt = """Perfil de áudio fixo: $stableStyle','        val envelope = performGeminiRequest(model, body, paidOnly)',v,'native synthesis');
put('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt',s);
}
{
let s=get('App.tsx');
const v=[
"  const voiceForSegment = (segment: VoiceSegment): { voice: TtsVoiceName; style: string } => {",
"    if(segment.speakerType==='narrator') return {voice:'Charon',style:CHARON_NARRATOR_STYLE};",
"    const npc=segment.speakerId?allNpcs.find(item=>item.id===segment.speakerId):allNpcs.find(item=>normalizeText(item.name)===normalizeText(segment.speakerName));",
"    const key=segment.speakerId||segment.speakerName||'npc';",
"    const variants=['ligeiramente mais firme e grave','ligeiramente mais suave e leve','ritmo ligeiramente mais lento','cadência um pouco mais viva'];",
"    const index=Array.from(key).reduce((a,c)=>(a*31+c.charCodeAt(0))>>>0,7)%variants.length;",
"    return {voice:'Charon',style:CHARON_NARRATOR_STYLE+'; mesmo narrador; interpretação discreta: '+variants[index]+'; emoção: '+(segment.emotion||npc?.emotionalState||'natural')};",
"  };",
""].join(nl)+nl;
s=slice(s,'  const voiceForSegment = (segment: VoiceSegment): { voice: TtsVoiceName; style: string } => {','  const playCharonNarration = async (node: SceneNode) => {',v,'single voice');
put('App.tsx',s);
}
if(!get('services/geminiService.ts').includes('speech_metadata')||!get('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt').includes('speech_metadata'))throw Error('metadata not integrated');
console.log('BRASEIRO_TTS_PATCH_PASS: native + browser 3.8, voice consistent, model fallback');
