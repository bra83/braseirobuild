import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const p=rel=>path.join(root,rel);
const read=rel=>fs.readFileSync(p(rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(p(rel),text,'utf8');
const must=(ok,label)=>{if(!ok)throw new Error('OUA_49211: '+label);};

let app=read('App.tsx');

const typeImport="import type { CampaignStartMode } from './lib/adventureTypes';";
must(app.includes(typeImport),'CampaignStartMode import');
if(!app.includes("import { STRIDER_PATRONS } from './lib/adventureModules';")){
  app=app.replace(typeImport,typeImport+"\nimport { STRIDER_PATRONS } from './lib/adventureModules';");
}

const start=app.indexOf('  const handleCampaignStartModeChange = async (mode: CampaignStartMode) => {');
const end=app.indexOf('\n\n  const handleImportNpcPortrait',start);
must(start>=0&&end>start,'campaign start handler block');

const replacement=`  const handleCampaignStartModeChange = async (mode: CampaignStartMode) => {
    const base = campaignRef.current;
    if (!base || base.history.length) return;

    const adventureId = mode === 'star-of-mist'
      ? 'core-star-of-the-mist'
      : mode === 'lone-lands'
        ? 'lone-lands-troll-hole'
        : undefined;

    const requestedRulesMode = mode === 'strider' ? 'strider' : 'standard';
    const currentRulesMode = normalizeMasterState(base).company.rulesMode === 'strider' ? 'strider' : 'standard';

    if (mode === 'strider' || currentRulesMode !== requestedRulesMode) {
      const mechanical = applyMasterCommands(base, characters, allNpcs, [{
        type: 'set_rules_mode',
        mode: requestedRulesMode,
        reason: mode === 'strider'
          ? 'Modo Peregrino ativado pela escolha de abertura da campanha.'
          : 'Modo padrão restaurado ao trocar a abertura para uma opção não Peregrino.',
      } as MasterCommand]);

      if (mechanical.rejected.length) {
        setStoryError(mechanical.rejected.map(item => item.reason).join(' | '));
        return;
      }

      const activeId = base.activeCharacterId;
      const activeCharacter = mechanical.characters.find(character => character.id === activeId);
      const updated: CampaignState = {
        ...mechanical.campaign,
        campaignStart: { mode, adventureId },
        adventureRuntime: undefined,
        ...(activeCharacter ? { characterSheet: normalizeCharacterSheet(activeCharacter.sheet) } : {}),
        updatedAt: Date.now(),
      };

      const committed = await saveMasterTransaction(
        updated,
        mechanical.characters,
        mechanical.audits,
        mode === 'strider' ? 'ativar Modo Peregrino pela abertura' : 'seleção de início da campanha',
        mechanical.npcs,
      );
      saveEmergencyCampaignMirror(committed);
      campaignRef.current = committed;
      setCampaign(committed);
      setCharacters(mechanical.characters);
      setAllNpcs(mechanical.npcs);
      return;
    }

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
    const patron = STRIDER_PATRONS[patronId];
    if (!patron) {
      setStoryError('Patrono inválido para o Modo Peregrino.');
      return;
    }

    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'set_company_patron',
      patron: patron.label,
      reason: 'Patrono escolhido na abertura do Modo Peregrino.',
    } as MasterCommand]);

    if (mechanical.rejected.length) {
      setStoryError(mechanical.rejected.map(item => item.reason).join(' | '));
      return;
    }

    const activeId = base.activeCharacterId;
    const activeCharacter = mechanical.characters.find(character => character.id === activeId);
    const updated: CampaignState = {
      ...mechanical.campaign,
      campaignStart: { mode: 'strider', patronId },
      adventureRuntime: undefined,
      ...(activeCharacter ? { characterSheet: normalizeCharacterSheet(activeCharacter.sheet) } : {}),
      updatedAt: Date.now(),
    };

    const committed = await saveMasterTransaction(
      updated,
      mechanical.characters,
      mechanical.audits,
      'Patrono do Modo Peregrino',
      mechanical.npcs,
    );
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
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

app=app.slice(0,start)+replacement+app.slice(end);
write('App.tsx',app);

let selector=read('components/CampaignStartSelector.tsx');
const helpOld='A abertura Peregrino pode ser escolhida diretamente aqui. Escolha o Patrono e a missão abaixo.';
if(selector.includes(helpOld)){
  selector=selector.replace(helpOld,'Ao escolher esta abertura, o VTT ativa automaticamente as regras do Modo Peregrino para a Companhia e o herói. Depois escolha o Patrono e a missão.');
}
write('components/CampaignStartSelector.tsx',selector);

let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.211'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49211');
write('lib/version.ts',version);

const pkgPath=p('package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));
pkg.version='4.9.211';
fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49211').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.211"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.211 opening selection now activates actual Strider rules');
