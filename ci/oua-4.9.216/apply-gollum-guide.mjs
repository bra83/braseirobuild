import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=process.argv[2];
if(!root)throw Error('OUA root argument required');
const abs=p=>path.join(root,p);
const read=p=>fs.readFileSync(abs(p),'utf8');
const put=(p,s)=>{fs.mkdirSync(path.dirname(abs(p)),{recursive:true});fs.writeFileSync(abs(p),s,'utf8');};
const patch=(text,from,to,label)=>{
  const count=text.split(from).length-1;
  if(count!==1)throw Error('OUA_49216_ANCHOR '+label+' count='+count);
  return text.replace(from,to);
};
const here=path.dirname(fileURLToPath(import.meta.url));
const src=p=>fs.readFileSync(path.join(here,p),'utf8');
put('lib/gollumGuidance.ts',src('gollumGuidance.ts'));
put('components/GollumGuide.tsx',src('GollumGuide.tsx'));
let app=read('App.tsx');
app=patch(app,"import { DiceView } from './components/DiceView';","import { DiceView } from './components/DiceView';\nimport { GollumGuide } from './components/GollumGuide';",'import');
const hook="  const handleStopNarration = () => {";
const speak=[
"  // Guide is read-only; TTS is the existing narrator channel, not a new engine.",
"  const speakGollumGuide = async (message: string): Promise<void> => {",
"    if (!message.trim() || isNarrationLoading) return;",
"    cancelNarrationPlayback();",
"    const sessionId = AudioEngine.beginNarrationSession('gollum-guide', 'charon');",
"    let status: 'finished' | 'failed' = 'finished';",
"    setIsNarrationLoading(true); setNarrationError(null);",
"    try {",
"      if (localStorage.getItem('braseiro.narrationVoiceBackend') === 'kokoro') {",
"        await speakWithAndroid(message);",
"      } else {",
"        const voice = await generateNarration(message, 'Charon',",
"          'A small elderly creature, gravelly, breathy, hushed and whispery; curious, cautious and quietly sardonic; speak Brazilian Portuguese clearly; no long pauses; do not read style instructions.');",
"        if (!voice) throw new Error('A voz do guia nao foi gerada.');",
"        await AudioEngine.ready();",
"        await AudioEngine.playVoice(voice);",
"      }",
"    } catch (error) {",
"      status = 'failed';",
"      console.warn('[GOLLUM_GUIDE_TTS]', error);",
"      setNarrationError(error instanceof Error ? error.message : 'Voz do guia indisponivel.');",
"    } finally {",
"      AudioEngine.finishNarrationSession(status, sessionId);",
"      setIsNarrationLoading(false);",
"    }",
"  };",
hook
].join('\n');
app=patch(app,hook,speak,'tts hook');
const panel="      <QuickAudioPanel open={quickAudioOpen} onClose={() => setQuickAudioOpen(false)} onStopAll={handleStopAll} />";
const ui=[
"      <GollumGuide",
"        campaignId={campaign.id}",
"        tab={activeTab}",
"        hasHero={characters.length > 0}",
"        busy={isStoryLoading}",
"        pendingTest={campaign.pendingTest}",
"        pendingRoll={campaign.pendingDiceRoll}",
"        journeyPending={Boolean(normalizeMasterState(campaign).journey?.pendingRoll)}",
"        onNavigate={tab => setActiveTab(tab)}",
"        onSpeak={message => { void speakGollumGuide(message); }}",
"      />",
panel
].join('\n');
app=patch(app,panel,ui,'guide UI');
put('App.tsx',app);
let v=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.216'").replace(/APP_VERSION_CODE\s*=\s*\d+/,'APP_VERSION_CODE = 49216');
if(!v.includes("'4.9.216'"))throw Error('version failed');
put('lib/version.ts',v);
const pkg=JSON.parse(read('package.json'));pkg.version='4.9.216';put('package.json',JSON.stringify(pkg,null,2)+'\n');
let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49216').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.216"');
put('android/app/build.gradle.kts',gradle);
console.log('OUA_49216_GOLLUM_READONLY_GUIDE_APPLIED');
