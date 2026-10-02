import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const repo=path.resolve(process.env.GITHUB_WORKSPACE||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>{const out=path.join(root,rel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text,'utf8');};
const must=(ok,label)=>{if(!ok)throw new Error('OUA_49206: '+label);};
const copyTemplate=(src,dst)=>write(dst,fs.readFileSync(path.join(repo,'ci/oua-4.9.206/templates',src),'utf8'));

copyTemplate('lib/adventureTypes.ts','lib/adventureTypes.ts');
copyTemplate('lib/adventureModules.ts','lib/adventureModules.ts');
copyTemplate('lib/striderPatronMissions.ts','lib/striderPatronMissions.ts');
copyTemplate('lib/adventureStartResolver.ts','lib/adventureStartResolver.ts');
copyTemplate('lib/adventureState.ts','lib/adventureState.ts');
copyTemplate('components/CampaignStartSelector.tsx','components/CampaignStartSelector.tsx');

let db=read('lib/db.ts');
const dbAnchor='  recentChoiceFamilies?: string[];\n}';
must(db.includes(dbAnchor),'CampaignState tail');
db=db.replace(dbAnchor,`  recentChoiceFamilies?: string[];
  /** Escolha e resolução persistente do início da campanha. */
  campaignStart?: import('./adventureTypes').CampaignStartSelection;
  /** Estado persistente do Adventure Engine; nunca reconstruído pela IA. */
  adventureRuntime?: import('./adventureTypes').AdventureRuntimeState;
}`);
write('lib/db.ts',db);

let app=read('App.tsx');
const mapImport="import type { MapDiscovery } from './lib/map';";
must(app.includes(mapImport),'App map type import');
app=app.replace(mapImport,mapImport+`
import { campaignStartCanBegin, resolveCampaignStart } from './lib/adventureStartResolver';
import { activateAdventureAfterOpening, applyResolvedCampaignStart, buildAdventureOpeningContext, buildAdventureRuntimeContext, recordAdventureScene, startSummary } from './lib/adventureState';
import type { CampaignStartMode } from './lib/adventureTypes';`);

const newCampaignAnchor="    history: [], pendingDiceRoll: null, pendingTest: null, events: [], firstImageGenerated: false,";
must(app.includes(newCampaignAnchor),'newCampaign defaults');
app=app.replace(newCampaignAnchor,"    history: [], pendingDiceRoll: null, pendingTest: null, events: [], firstImageGenerated: false,\n    campaignStart: { mode: 'star-of-mist', adventureId: 'core-star-of-the-mist' },");

const normalizedAnchor="    events: Array.isArray(input.events) ? input.events : [],\n    schemaVersion:";
must(app.includes(normalizedAnchor),'normalize campaign event anchor');
app=app.replace(normalizedAnchor,"    events: Array.isArray(input.events) ? input.events : [],\n    campaignStart: input.campaignStart || fallback.campaignStart,\n    adventureRuntime: input.adventureRuntime,\n    schemaVersion:");

const processContextAnchor="      let publicContext = \`\${orchestrator.publicContext}\${continuityContext}\${narrativeContext}\`;";
must(app.includes(processContextAnchor),'processTurn public context');
app=app.replace(processContextAnchor,processContextAnchor+`
      const adventureRuntimeContext = buildAdventureRuntimeContext(base);
      directorContext += adventureRuntimeContext;
      publicContext += adventureRuntimeContext;`);

const updatedAnchor="      if (updated.masterState && narrativeIntent.advancesFiction) {";
must(app.includes(updatedAnchor),'processTurn updated anchor');
app=app.replace(updatedAnchor,`      if (narrativeIntent.advancesFiction) {
        updated = recordAdventureScene(updated, newNode.id, canonicalLocation);
      }
${updatedAnchor}`);

const start=app.indexOf('const handleStartAdventure = async () => {');
const end=app.indexOf('\n\n  const handleImportNpcPortrait',start);
must(start>=0&&end>start,'handleStartAdventure block');
const replacement=`const handleStartAdventure = async () => {
    if (isStoryLoading) return;
    setActiveTab('jogo');
    const activeCampaign = campaignRef.current;
    if (!activeCampaign) return;
    let recoveryCampaign = activeCampaign;

    setIsStoryLoading(true);
    setStoryError(null);
    setNarrationError(null);

    try {
      const resolvedStart = await resolveCampaignStart(activeCampaign, characters);
      if (!resolvedStart.ok) {
        if (resolvedStart.requiresManualAtlas) {
          window.setTimeout(() => document.getElementById('atlas-terra-media')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);
        }
        throw new Error(resolvedStart.error || 'O ponto inicial da campanha não pôde ser resolvido.');
      }

      const positioned = await applyResolvedCampaignStart(activeCampaign, resolvedStart);
      const positionedCommitted = await persistCampaign(positioned, 'Adventure Engine: posição inicial');
      recoveryCampaign = positionedCommitted;
      const discovery = await loadMapDiscovery(positionedCommitted.id).catch(() => null);

      const partySet = new Set(positionedCommitted.partyCharacterIds || []);
      const party = characters.filter(character => partySet.has(character.id));
      const companyNames = party.map(character => character.sheet.identity.name || character.name).filter(Boolean);
      const companyText = companyNames.length
        ? \`Companhia: \${companyNames.join(', ')}.\`
        : 'Companhia: personagens já persistidos na campanha.';

      const profile = getAdventureProfile(adventureProfile);
      const architecture = chooseOpeningArchitecture(positionedCommitted, party, allNpcs, adventureProfile);
      const openingClock = chooseOpeningClock(positionedCommitted, architecture.id);
      const remembered = rememberOpening(positionedCommitted, architecture.id);
      const rememberedMaster = normalizeMasterState(remembered);
      const sourceCampaign: CampaignState = {
        ...remembered,
        masterState: {
          ...rememberedMaster,
          worldClock: { ...rememberedMaster.worldClock, hour: openingClock.hour, minute: openingClock.minute },
          updatedAt: Date.now(),
        },
        updatedAt: Date.now(),
      };

      const canonicalLocation = sourceCampaign.masterState?.position.locationName || sourceCampaign.currentLocation || 'Eriador';
      const currentHexId = discovery?.currentHexId || sourceCampaign.masterState?.position.currentHexId || '';
      const locationInstruction = \`Posição cartográfica confirmada pelo host: hex \${currentHexId}, local/região \${canonicalLocation}. Não escreva o código do hex na prosa.\`;
      const adventureContext = buildAdventureOpeningContext(sourceCampaign, resolvedStart);
      const openingContext = [
        adventureContext,
        locationInstruction,
        companyText,
        \`Perfil de aventura: \${profile.label}. \${profile.prompt}\`,
        openingInstruction(architecture),
        \`Horário canônico: \${String(openingClock.hour).padStart(2, '0')}:\${String(openingClock.minute).padStart(2, '0')} (\${timePeriodFromHour(openingClock.hour)}).\`,
        \`Local canônico que deve aparecer naturalmente na abertura: \${canonicalLocation}.\`,
        sourceCampaign.campaignSummary ? \`Resumo persistido da campanha: \${sourceCampaign.campaignSummary}\` : '',
      ].filter(Boolean).join('\\n');

      const direct = await generateOpeningNarrationDirect(
        openingContext,
        '[INICIAR AVENTURA GUIADA] Crie a primeira cena agora e devolva agência ao jogador.',
      );

      const sceneId = \`scene_opening_\${Date.now()}\`;
      const event = makeEvent('scene_started', sceneId);
      const narrationWords = direct.text.split(/\\s+/).filter(Boolean).length;
      const node = {
        id: sceneId,
        timestamp: Date.now(),
        playerAction: '[INICIAR AVENTURA GUIADA]',
        atmospheric_narration: direct.text,
        action_result: '',
        visualPrompt: direct.text.slice(0, 900),
        sceneVisualPrompt: direct.text.slice(0, 900),
        location: canonicalLocation,
        timeOfDay: timePeriodFromHour(openingClock.hour),
        worldHour: openingClock.hour,
        worldMinute: openingClock.minute,
        choices: [],
        voiceScript: [],
        requiredTest: null,
        statUpdates: {},
        presentNpcIds: sourceCampaign.presentNpcIds || [],
        events: [event],
        sources: [],
        narrativeMeta: {
          intentClass: 'action-diegetic',
          advancesFiction: true,
          generationSource: 'gemini-direct-opening-adventure-engine',
          generationModel: direct.model,
          narrationWordCount: narrationWords,
          qualityFlags: [],
          rhythmFlags: [],
        },
      } as SceneNode;

      let updated: CampaignState = {
        ...sourceCampaign,
        currentLocation: canonicalLocation,
        history: [...(sourceCampaign.history || []), node],
        events: [...(sourceCampaign.events || []), event],
        pendingDiceRoll: null,
        pendingTest: null,
        updatedAt: Date.now(),
      };
      updated = activateAdventureAfterOpening(updated, sceneId, canonicalLocation);

      const committed = await persistCampaign(updated, 'abertura direta Gemini validada');
      saveEmergencyCampaignMirror(committed);
      campaignRef.current = committed;
      setCampaign(committed);
      setPresentNpcs(allNpcs.filter(npc => committed.presentNpcIds.includes(npc.id)));
      setCurrentImage(null);
      cancelNarrationPlayback();
      setIsStoryLoading(false);
    } catch (error) {
      console.error('[BRASEIRO_ADVENTURE_START]', error);
      saveEmergencyCampaignMirror(recoveryCampaign);
      campaignRef.current = recoveryCampaign;
      setCampaign(recoveryCampaign);
      setStoryError(error instanceof Error ? error.message : String(error));
      setIsStoryLoading(false);
    }
  };`;
app=app.slice(0,start)+replacement+app.slice(end);

const importNpcMarker='  const handleImportNpcPortrait';
const insertAt=app.indexOf(importNpcMarker);
must(insertAt>0,'start selection handler insertion');
const selectorHandler=`
  const handleCampaignStartModeChange = async (mode: CampaignStartMode) => {
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

`;
app=app.slice(0,insertAt)+selectorHandler+app.slice(insertAt);

const oldGameProps="onStartAdventure={handleStartAdventure} canStartAdventure={Boolean(normalizeMasterState(campaign).position.currentHexId)} adventureProfile={adventureProfile}";
must(app.includes(oldGameProps),'GameView start props');
app=app.replace(oldGameProps,"onStartAdventure={handleStartAdventure} canStartAdventure={campaignStartCanBegin(campaign)} campaignStartMode={(campaign.campaignStart?.mode || 'star-of-mist') as CampaignStartMode} onCampaignStartModeChange={handleCampaignStartModeChange} campaignStartSummary={startSummary(campaign)} canUseStrider={partyCharacters.some(character => character.sheet.other?.rulesMode === 'strider')} adventureProfile={adventureProfile}");
write('App.tsx',app);

let view=read('components/GameView.tsx');
const castImport="import { AdventureCastAndHexes } from './AdventureCastAndHexes';";
if(view.includes(castImport))view=view.replace(castImport,castImport+"\nimport { CampaignStartSelector } from './CampaignStartSelector';\nimport type { CampaignStartMode } from '../lib/adventureTypes';");
else{
  const firstImportEnd=view.indexOf('\n');
  view=view.slice(0,firstImportEnd+1)+"import { CampaignStartSelector } from './CampaignStartSelector';\nimport type { CampaignStartMode } from '../lib/adventureTypes';\n"+view.slice(firstImportEnd+1);
}
const propAnchor='  canStartAdventure: boolean;\n';
must(view.includes(propAnchor),'GameView canStartAdventure prop');
view=view.replace(propAnchor,propAnchor+"  campaignStartMode: CampaignStartMode;\n  onCampaignStartModeChange: (mode: CampaignStartMode) => void | Promise<void>;\n  campaignStartSummary?: string;\n  canUseStrider: boolean;\n");
const destructureAnchor='  canStartAdventure,\n  adventureProfile,';
must(view.includes(destructureAnchor),'GameView destructure');
view=view.replace(destructureAnchor,"  canStartAdventure,\n  campaignStartMode,\n  onCampaignStartModeChange,\n  campaignStartSummary,\n  canUseStrider,\n  adventureProfile,");

view=view.replace('Escolha o estilo da crônica. A aventura só começa depois que a Companhia tiver um hex definido no Atlas.','Escolha como a crônica começa e depois o estilo de narração. Aventuras oficiais, Modo Peregrino e Taverna posicionam a Companhia automaticamente no Atlas.');
const profileGrid='<div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">\n                    {ADVENTURE_PROFILES.map(profile => (';
must(view.includes(profileGrid),'GameView profile grid');
view=view.replace(profileGrid,`<CampaignStartSelector mode={campaignStartMode} onChange={onCampaignStartModeChange} canUseStrider={canUseStrider} summary={campaignStartSummary} />
                  <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {ADVENTURE_PROFILES.map(profile => (`);
view=view.replace('Defina primeiro a posição da Companhia no Atlas. O Mestre não inventará um hex inicial.','No modo Sandbox, escolha primeiro a posição da Companhia no Atlas. Nos demais modos o Adventure Engine posiciona o grupo automaticamente.');
write('components/GameView.tsx',view);

let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.206'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49206');
write('lib/version.ts',version);
const pkgPath=path.join(root,'package.json');const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));pkg.version='4.9.206';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');
let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49206').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.206"');write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.206 Adventure Engine + campaign start modes applied');
