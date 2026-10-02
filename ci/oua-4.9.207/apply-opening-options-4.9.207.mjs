import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>{const target=p(rel);fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,text,'utf8');};
const must=(ok,label)=>{if(!ok)throw new Error('OUA_49207: '+label);};

const selector = `import React from 'react';
import { CAMPAIGN_START_OPTIONS, STRIDER_PATRONS } from '../lib/adventureModules';
import { STRIDER_MISSIONS } from '../lib/striderPatronMissions';
import type { CampaignStartMode } from '../lib/adventureTypes';

interface Props {
  mode: CampaignStartMode;
  onChange: (mode: CampaignStartMode) => void | Promise<void>;
  canUseStrider: boolean;
  patronId?: string;
  missionIndex?: number;
  onPatronChange: (patronId: string) => void | Promise<void>;
  onMissionChange: (mission: number | 'random') => void | Promise<void>;
  summary?: string;
}

export const CampaignStartSelector: React.FC<Props> = ({
  mode,
  onChange,
  canUseStrider,
  patronId,
  missionIndex,
  onPatronChange,
  onMissionChange,
  summary,
}) => {
  const patrons = Object.values(STRIDER_PATRONS);
  const missions = patronId ? (STRIDER_MISSIONS[patronId] || []) : [];

  return (
    <section className="mt-4 rounded border border-cyan-950/70 bg-black/25 p-3 text-left">
      <div className="mb-2">
        <span className="block text-[10px] font-black uppercase tracking-[0.18em] text-cyan-500">Como esta crônica começa?</span>
        <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
          Aventuras oficiais e Modo Peregrino posicionam a Companhia automaticamente. Sandbox é o único modo em que você escolhe o hex. Taverna sorteia um local válido do Atlas.
        </p>
      </div>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {CAMPAIGN_START_OPTIONS.map(option => {
          const disabled = option.mode === 'strider' && !canUseStrider;
          const selected = option.mode === mode;
          return (
            <button
              key={option.mode}
              type="button"
              disabled={disabled}
              onClick={() => void onChange(option.mode)}
              className={`rounded border p-3 text-left transition ${selected ? 'border-cyan-500 bg-cyan-950/35 text-cyan-100' : 'border-blue-950 bg-zinc-950/60 text-slate-300'} disabled:cursor-not-allowed disabled:opacity-35`}
            >
              <span className="block text-xs font-bold">
                {option.label}{option.recommended ? ' · recomendado' : ''}
              </span>
              <span className="mt-1 block text-[11px] leading-relaxed text-slate-400">{option.description}</span>
              {disabled && <span className="mt-2 block text-[10px] text-amber-300">Disponível quando houver um Herói em Modo Peregrino.</span>}
            </button>
          );
        })}
      </div>

      {mode === 'strider' && canUseStrider && (
        <div className="mt-4 rounded border border-amber-900/45 bg-amber-950/10 p-3">
          <span className="block text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">1. Escolha o Patrono</span>
          <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
            O Patrono define a tabela de seis sementes de missão do Modo Peregrino. O Gemini não escolhe nem substitui esta decisão.
          </p>
          <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
            {patrons.map(patron => (
              <button
                key={patron.id}
                type="button"
                onClick={() => void onPatronChange(patron.id)}
                className={`rounded border px-3 py-2 text-left text-[11px] transition ${patronId === patron.id ? 'border-amber-400 bg-amber-950/35 text-amber-100' : 'border-zinc-800 bg-black/25 text-slate-300'}`}
              >
                {patron.label}
              </button>
            ))}
          </div>

          {patronId && (
            <div className="mt-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">2. Escolha a missão</span>
                <button
                  type="button"
                  onClick={() => void onMissionChange('random')}
                  className="rounded border border-amber-500/70 bg-amber-950/30 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wide text-amber-100"
                >
                  Sortear missão
                </button>
              </div>
              <div className="mt-2 grid grid-cols-1 gap-2">
                {missions.map((mission, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => void onMissionChange(index)}
                    className={`rounded border p-2 text-left text-[11px] leading-relaxed transition ${missionIndex === index ? 'border-amber-400 bg-amber-950/35 text-amber-50' : 'border-zinc-800 bg-black/20 text-slate-300'}`}
                  >
                    <span className="mr-2 font-black text-amber-300">{index + 1}.</span>{mission}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {summary && <div className="mt-3 rounded border border-blue-900/50 bg-blue-950/15 px-3 py-2 text-[11px] leading-relaxed text-blue-200">{summary}</div>}
    </section>
  );
};
`;
write('components/CampaignStartSelector.tsx', selector);

let resolver=read('lib/adventureStartResolver.ts');
resolver=resolver.replace(
  "export function campaignStartCanBegin(campaign:CampaignState){const selection=normalizeCampaignStart(campaign.campaignStart);return selection.mode!=='sandbox'||Boolean(normalizeMasterState(campaign).position.currentHexId);}",
  `export function campaignStartCanBegin(campaign:CampaignState){
  const selection=normalizeCampaignStart(campaign.campaignStart);
  if(selection.mode==='sandbox')return Boolean(normalizeMasterState(campaign).position.currentHexId);
  if(selection.mode==='strider'){
    const missionIndex=Number(selection.missionIndex);
    return Boolean(selection.patronId&&STRIDER_PATRONS[selection.patronId]&&Number.isInteger(missionIndex)&&missionIndex>=0&&missionIndex<6);
  }
  return true;
}`
);
const labelAnchor="export function campaignStartLabel(input?:CampaignState['campaignStart']|null){const selection=normalizeCampaignStart(input);return CAMPAIGN_START_OPTIONS.find(option=>option.mode===selection.mode)?.label||selection.mode;}\n";
must(resolver.includes(labelAnchor),'campaignStartLabel anchor');
resolver=resolver.replace(labelAnchor,labelAnchor+`
export function drawStriderMissionIndex(patronId:string):number{
  const missions=STRIDER_MISSIONS[patronId]||[];
  if(!missions.length)return 0;
  const cryptoApi=globalThis.crypto;
  if(cryptoApi?.getRandomValues){
    const value=new Uint32Array(1);
    cryptoApi.getRandomValues(value);
    return value[0]%missions.length;
  }
  return Math.floor(Math.random()*missions.length);
}
`);
resolver=resolver.replace('export async function resolveCampaignStart(campaign:CampaignState,characters:Character[]):Promise<ResolvedCampaignStart>{','export async function resolveCampaignStart(campaign:CampaignState,_characters:Character[]):Promise<ResolvedCampaignStart>{');
const striderStart=resolver.indexOf("  if(selection.mode==='strider'){");
const striderEnd=resolver.indexOf("  const module=selection.mode==='lone-lands'?",striderStart);
must(striderStart>=0&&striderEnd>striderStart,'Strider resolver block');
const striderBlock=`  if(selection.mode==='strider'){
    const id=selection.patronId||'';
    if(!id||!STRIDER_PATRONS[id]){
      return{ok:false,requiresManualAtlas:false,mode:selection.mode,selection,module:null,hexId:null,locationName:campaign.currentLocation||'Eriador',openingFacts:[],resolutionSource:'strider-patron-required',error:'Escolha o Patrono antes de começar o Modo Peregrino.'};
    }
    const patron=STRIDER_PATRONS[id],missions=STRIDER_MISSIONS[patron.id]||[];
    const missionIndex=Number(selection.missionIndex);
    if(!Number.isInteger(missionIndex)||missionIndex<0||missionIndex>=missions.length){
      return{ok:false,requiresManualAtlas:false,mode:selection.mode,selection,module:null,hexId:null,locationName:campaign.currentLocation||'Eriador',openingFacts:[],resolutionSource:'strider-mission-required',error:'Escolha uma missão do Patrono ou use Sortear missão.'};
    }
    const missionSeed=missions[missionIndex],resolved=resolveDefinition(patron.start,locations,hexes);
    return{ok:Boolean(resolved.hexId),requiresManualAtlas:false,mode:selection.mode,selection:{...selection,patronId:patron.id,missionIndex,resolvedHexId:resolved.hexId||undefined,resolvedLocationName:resolved.locationName,venueName:resolved.venueName,resolutionSource:resolved.source},module:null,hexId:resolved.hexId,locationName:resolved.locationName,venueName:resolved.venueName,missionSeed,openingFacts:['Modo Peregrino: missão inicial derivada das tabelas de Patrono das páginas 5–7.','Patrono: '+patron.label+'.','Semente: '+missionSeed,'Questões em aberto permanecem desconhecidas até serem descobertas.'],resolutionSource:resolved.source,error:resolved.hexId?undefined:'Não foi possível resolver no Atlas um ponto canônico para o Patrono.'};
  }
`;
resolver=resolver.slice(0,striderStart)+striderBlock+resolver.slice(striderEnd);
write('lib/adventureStartResolver.ts',resolver);

let state=read('lib/adventureState.ts');
state=state.replace("import { ADVENTURE_MODULES } from './adventureModules';","import { ADVENTURE_MODULES, STRIDER_PATRONS } from './adventureModules';\nimport { STRIDER_MISSIONS } from './striderPatronMissions';");
const summaryOld="  if(start?.mode==='strider')return start.patronId?'Patrono: '+start.patronId+(Number.isInteger(start.missionIndex)?' · missão '+(Number(start.missionIndex)+1):''):'O Patrono e a missão serão resolvidos ao começar.';";
must(state.includes(summaryOld),'Strider startSummary anchor');
state=state.replace(summaryOld,`  if(start?.mode==='strider'){
    if(!start.patronId)return'Escolha o Patrono do Modo Peregrino.';
    const patron=STRIDER_PATRONS[start.patronId];
    if(!patron)return'Escolha um Patrono válido.';
    if(!Number.isInteger(start.missionIndex))return'Patrono: '+patron.label+' · escolha uma missão ou use Sortear missão.';
    const index=Number(start.missionIndex);
    const mission=(STRIDER_MISSIONS[start.patronId]||[])[index];
    return'Patrono: '+patron.label+' · missão '+(index+1)+(mission?' · '+mission:'');
  }`);
write('lib/adventureState.ts',state);

let app=read('App.tsx');
const resolverImport="import { campaignStartCanBegin, resolveCampaignStart } from './lib/adventureStartResolver';";
must(app.includes(resolverImport),'App resolver import');
app=app.replace(resolverImport,"import { campaignStartCanBegin, drawStriderMissionIndex, resolveCampaignStart } from './lib/adventureStartResolver';");
const handlerStart=app.indexOf('  const handleCampaignStartModeChange = async (mode: CampaignStartMode) => {');
const handlerEnd=app.indexOf('\n\n  const handleImportNpcPortrait',handlerStart);
must(handlerStart>=0&&handlerEnd>handlerStart,'campaign start handlers');
const handlers=`  const handleCampaignStartModeChange = async (mode: CampaignStartMode) => {
    const base = campaignRef.current;
    if (!base || base.history.length) return;
    const adventureId = mode === 'star-of-mist' ? 'core-star-of-the-mist' : mode === 'lone-lands' ? 'lone-lands-troll-hole' : undefined;
    const updated: CampaignState = {
      ...base,
      campaignStart: { mode, adventureId },
      adventureRuntime: undefined,
      updatedAt: Date.now(),
    };
    await persistCampaign(updated, 'seleção de início da campanha');
  };

  const handleCampaignStartPatronChange = async (patronId: string) => {
    const base = campaignRef.current;
    if (!base || base.history.length) return;
    const updated: CampaignState = {
      ...base,
      campaignStart: { mode: 'strider', patronId },
      adventureRuntime: undefined,
      updatedAt: Date.now(),
    };
    await persistCampaign(updated, 'Patrono do Modo Peregrino');
  };

  const handleCampaignStartMissionChange = async (mission: number | 'random') => {
    const base = campaignRef.current;
    if (!base || base.history.length) return;
    const current = base.campaignStart;
    const patronId = current?.mode === 'strider' ? current.patronId : undefined;
    if (!patronId) return;
    const missionIndex = mission === 'random' ? drawStriderMissionIndex(patronId) : mission;
    const updated: CampaignState = {
      ...base,
      campaignStart: { ...current, mode: 'strider', patronId, missionIndex },
      adventureRuntime: undefined,
      updatedAt: Date.now(),
    };
    await persistCampaign(updated, mission === 'random' ? 'missão do Patrono sorteada' : 'missão do Patrono escolhida');
  };`;
app=app.slice(0,handlerStart)+handlers+app.slice(handlerEnd);

const oldProps="campaignStartMode={(campaign.campaignStart?.mode || 'star-of-mist') as CampaignStartMode} onCampaignStartModeChange={handleCampaignStartModeChange} campaignStartSummary={startSummary(campaign)} canUseStrider={partyCharacters.some(character => character.sheet.other?.rulesMode === 'strider')} adventureProfile={adventureProfile}";
must(app.includes(oldProps),'GameView campaign start props');
const newProps="campaignStartMode={(campaign.campaignStart?.mode || 'star-of-mist') as CampaignStartMode} onCampaignStartModeChange={handleCampaignStartModeChange} campaignStartPatronId={campaign.campaignStart?.patronId} campaignStartMissionIndex={campaign.campaignStart?.missionIndex} onCampaignStartPatronChange={handleCampaignStartPatronChange} onCampaignStartMissionChange={handleCampaignStartMissionChange} campaignStartSummary={startSummary(campaign)} canUseStrider={partyCharacters.some(character => character.sheet.other?.rulesMode === 'strider')} adventureProfile={adventureProfile}";
app=app.replaceAll(oldProps,newProps);
write('App.tsx',app);

let view=read('components/GameView.tsx');
const propAnchor="  campaignStartMode: CampaignStartMode;\n  onCampaignStartModeChange: (mode: CampaignStartMode) => void | Promise<void>;\n";
must(view.includes(propAnchor),'GameView campaign start prop anchor');
view=view.replace(propAnchor,propAnchor+"  campaignStartPatronId?: string;\n  campaignStartMissionIndex?: number;\n  onCampaignStartPatronChange: (patronId: string) => void | Promise<void>;\n  onCampaignStartMissionChange: (mission: number | 'random') => void | Promise<void>;\n");
const destructureAnchor="  campaignStartMode,\n  onCampaignStartModeChange,\n";
must(view.includes(destructureAnchor),'GameView campaign start destructure');
view=view.replace(destructureAnchor,destructureAnchor+"  campaignStartPatronId,\n  campaignStartMissionIndex,\n  onCampaignStartPatronChange,\n  onCampaignStartMissionChange,\n");
const selectorOld='<CampaignStartSelector mode={campaignStartMode} onChange={onCampaignStartModeChange} canUseStrider={canUseStrider} summary={campaignStartSummary} />';
must(view.includes(selectorOld),'CampaignStartSelector invocation');
view=view.replace(selectorOld,`<CampaignStartSelector
                    mode={campaignStartMode}
                    onChange={onCampaignStartModeChange}
                    canUseStrider={canUseStrider}
                    patronId={campaignStartPatronId}
                    missionIndex={campaignStartMissionIndex}
                    onPatronChange={onCampaignStartPatronChange}
                    onMissionChange={onCampaignStartMissionChange}
                    summary={campaignStartSummary}
                  />`);
write('components/GameView.tsx',view);

let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.207'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49207');
write('lib/version.ts',version);
const pkgPath=p('package.json');const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));pkg.version='4.9.207';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');
let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49207').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.207"');write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.207 explicit opening options applied');
