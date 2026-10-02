import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>{const out=p(rel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text,'utf8');};
const must=(ok,label)=>{if(!ok)throw new Error('OUA_49208: '+label);};

let view=read('components/GameView.tsx');

const selectorNeedle='<CampaignStartSelector\n                    mode={campaignStartMode}';
const selectorStart=view.indexOf(selectorNeedle);
must(selectorStart>=0,'embedded campaign start selector');
const selectorEnd=view.indexOf('/>',selectorStart);
must(selectorEnd>selectorStart,'embedded selector end');
view=view.slice(0,selectorStart)+view.slice(selectorEnd+2);

const narrationAnchor='              {visibleNarration || visibleActionResult ? (';
must(view.includes(narrationAnchor),'narration conditional anchor');

const standalone=`              {history.length === 0 && !isViewingHistory && (
                <section id="campaign-opening-type" className="mb-5 rounded-md border-2 border-cyan-700/70 bg-cyan-950/10 p-4 shadow-[0_0_24px_rgba(8,145,178,0.08)]">
                  <span className="block text-[10px] font-black uppercase tracking-[0.2em] text-cyan-400">Tipo de abertura / início da campanha</span>
                  <p className="mt-1 text-xs leading-relaxed text-slate-400">
                    Escolha primeiro como esta campanha começa. Esta escolha define aventura, Patrono ou política de localização antes do Gemini criar a primeira cena.
                  </p>
                  <CampaignStartSelector
                    mode={campaignStartMode}
                    onChange={onCampaignStartModeChange}
                    canUseStrider={canUseStrider}
                    patronId={campaignStartPatronId}
                    missionIndex={campaignStartMissionIndex}
                    onPatronChange={onCampaignStartPatronChange}
                    onMissionChange={onCampaignStartMissionChange}
                    summary={campaignStartSummary}
                  />
                </section>
              )}

`;
view=view.replace(narrationAnchor,standalone+narrationAnchor);
view=view.replace(
  'Escolha como a crônica começa e depois o estilo de narração. Aventuras oficiais, Modo Peregrino e Taverna posicionam a Companhia automaticamente no Atlas.',
  'Depois de definir o tipo de abertura acima, escolha o estilo de narração. Aventuras oficiais, Modo Peregrino e Taverna posicionam a Companhia automaticamente no Atlas.'
);
write('components/GameView.tsx',view);

let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.208'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49208');
write('lib/version.ts',version);

const pkgPath=p('package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));
pkg.version='4.9.208';
fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49208').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.208"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.208 opening type selector visibility fix applied');
