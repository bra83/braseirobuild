import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>{const out=p(rel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text,'utf8');};
const must=(ok,label)=>{if(!ok)throw new Error('OUA_49209: '+label);};

let immersive=read('components/immersive/ImmersiveJourneyView.tsx');

if(!immersive.includes("import { CampaignStartSelector } from '../CampaignStartSelector';")){
  const importAnchor="import { EriadorMap } from '../EriadorMap';\n";
  must(immersive.includes(importAnchor),'immersive import anchor');
  immersive=immersive.replace(importAnchor,importAnchor+"import { CampaignStartSelector } from '../CampaignStartSelector';\nimport type { CampaignStartMode } from '../../lib/adventureTypes';\n");
}

const propsAnchor="  onStartAdventure: () => void | Promise<void>;\n  canStartAdventure: boolean;\n";
must(immersive.includes(propsAnchor),'immersive props anchor');
if(!immersive.includes('campaignStartMode: CampaignStartMode;')){
  immersive=immersive.replace(propsAnchor,propsAnchor+
`  campaignStartMode: CampaignStartMode;
  onCampaignStartModeChange: (mode: CampaignStartMode) => void | Promise<void>;
  campaignStartPatronId?: string;
  campaignStartMissionIndex?: number;
  onCampaignStartPatronChange: (patronId: string) => void | Promise<void>;
  onCampaignStartMissionChange: (mission: number | 'random') => void | Promise<void>;
  campaignStartSummary?: string;
  canUseStrider: boolean;
`);
}

const loadingAnchor="    {(startRequested || (props.isStoryLoading && !node)) && <div aria-live=\"polite\"";
must(immersive.includes(loadingAnchor),'immersive loading anchor');

if(!immersive.includes('data-braseiro-opening-selector="true"')){
  const selectorBlock=`    {!node && !chronicleOpen && !startRequested && !props.isStoryLoading && <div data-braseiro-opening-selector="true" style={{
      position: 'absolute',
      left: 10,
      right: 10,
      top: 'calc(max(8px, env(safe-area-inset-top, 0px)) + 150px)',
      bottom: 'calc(' + (navHeight + 84) + 'px + env(safe-area-inset-bottom, 0px))',
      zIndex: 86,
      overflowY: 'auto',
      overflowX: 'hidden',
      WebkitOverflowScrolling: 'touch',
      pointerEvents: 'auto',
      touchAction: 'pan-y',
      borderRadius: 14,
      border: '1px solid rgba(183,145,91,.66)',
      background: 'linear-gradient(180deg, rgba(24,18,12,.98), rgba(10,8,6,.98))',
      boxShadow: '0 16px 34px rgba(0,0,0,.56)',
      padding: '12px 12px 14px',
    }}>
      <div style={{ marginBottom: 8, textAlign: 'center' }}>
        <div style={{ fontSize: 11, fontWeight: 900, letterSpacing: '.12em', textTransform: 'uppercase', color: '#d9b87a' }}>Tipo de abertura</div>
        <div style={{ marginTop: 4, fontSize: 12, lineHeight: 1.35, color: '#c9b997' }}>Escolha como esta campanha começa antes de tocar em Começar aventura.</div>
      </div>
      <CampaignStartSelector
        mode={props.campaignStartMode}
        onChange={props.onCampaignStartModeChange}
        canUseStrider={props.canUseStrider}
        patronId={props.campaignStartPatronId}
        missionIndex={props.campaignStartMissionIndex}
        onPatronChange={props.onCampaignStartPatronChange}
        onMissionChange={props.onCampaignStartMissionChange}
        summary={props.campaignStartSummary}
      />
    </div>}

`;
  immersive=immersive.replace(loadingAnchor,selectorBlock+loadingAnchor);
}

const oldHint=`      {!node && !props.canStartAdventure && <div style={{ ...glassPanel, width: '100%', boxSizing: 'border-box', borderRadius: 10, padding: '8px 12px', textAlign: 'center', fontSize: 12, color: '#ead9b7' }}>Selecione um hex no mapa e toque <strong>Posicionar</strong> para definir onde a Companhia começa.</div>}`;
if(immersive.includes(oldHint)){
  immersive=immersive.replace(oldHint,`      {!node && props.campaignStartMode === 'sandbox' && !props.canStartAdventure && <div style={{ ...glassPanel, width: '100%', boxSizing: 'border-box', borderRadius: 10, padding: '8px 12px', textAlign: 'center', fontSize: 12, color: '#ead9b7' }}>Sandbox: selecione um hex no mapa e toque <strong>Posicionar</strong> para definir onde a Companhia começa.</div>}`);
}

write('components/immersive/ImmersiveJourneyView.tsx',immersive);

let view=read('components/GameView.tsx');
const immersiveCallAnchor="      onStartAdventure={onStartAdventure}\n      canStartAdventure={canStartAdventure}\n";
must(view.includes(immersiveCallAnchor),'GameView immersive call anchor');
if(!view.includes('campaignStartMode={campaignStartMode}')){
  view=view.replace(immersiveCallAnchor,immersiveCallAnchor+
`      campaignStartMode={campaignStartMode}
      onCampaignStartModeChange={onCampaignStartModeChange}
      campaignStartPatronId={campaignStartPatronId}
      campaignStartMissionIndex={campaignStartMissionIndex}
      onCampaignStartPatronChange={onCampaignStartPatronChange}
      onCampaignStartMissionChange={onCampaignStartMissionChange}
      campaignStartSummary={campaignStartSummary}
      canUseStrider={canUseStrider}
`);
}
write('components/GameView.tsx',view);

let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.209'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49209');
write('lib/version.ts',version);
const pkgPath=p('package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));pkg.version='4.9.209';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');
let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49209').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.209"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.209 immersive opening selector applied');
