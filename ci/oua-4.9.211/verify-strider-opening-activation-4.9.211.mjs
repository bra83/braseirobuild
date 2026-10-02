import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const app=read('App.tsx');
const selector=read('components/CampaignStartSelector.tsx');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const handlerStart=app.indexOf('const handleCampaignStartModeChange');
const patronStart=app.indexOf('const handleCampaignStartPatronChange');
const missionStart=app.indexOf('const handleCampaignStartMissionChange');
const modeBlock=app.slice(handlerStart,patronStart);
const patronBlock=app.slice(patronStart,missionStart);

const checks={
  'version-4.9.211':version.includes("APP_VERSION = '4.9.211'")&&version.includes('APP_VERSION_CODE = 49211')&&pkg.version==='4.9.211'&&gradle.includes('versionCode = 49211')&&gradle.includes('versionName = "4.9.211"'),
  'strider-selection-activates-rules':modeBlock.includes("type: 'set_rules_mode'")&&modeBlock.includes("requestedRulesMode = mode === 'strider' ? 'strider' : 'standard'"),
  'switch-away-restores-standard':modeBlock.includes("Modo padrão restaurado ao trocar a abertura"),
  'rules-persist-characters':modeBlock.includes('saveMasterTransaction')&&modeBlock.includes('setCharacters(mechanical.characters)'),
  'active-sheet-synced':modeBlock.includes('normalizeCharacterSheet(activeCharacter.sheet)'),
  'patron-imported':app.includes("import { STRIDER_PATRONS } from './lib/adventureModules';"),
  'patron-selection-is-mechanical':patronBlock.includes("type: 'set_company_patron'")&&patronBlock.includes('patron: patron.label'),
  'patron-persists-master-state':patronBlock.includes('saveMasterTransaction')&&patronBlock.includes("campaignStart: { mode: 'strider', patronId }"),
  'ui-explains-auto-activation':selector.includes('ativa automaticamente as regras do Modo Peregrino'),
};
const failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.211_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.211_OK');
