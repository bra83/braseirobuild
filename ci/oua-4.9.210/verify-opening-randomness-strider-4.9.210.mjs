import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');

const resolver=read('lib/adventureStartResolver.ts');
const selector=read('components/CampaignStartSelector.tsx');
const modules=read('lib/adventureModules.ts');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const checks={
  'version-4.9.210':version.includes("APP_VERSION = '4.9.210'")&&version.includes('APP_VERSION_CODE = 49210')&&pkg.version==='4.9.210'&&gradle.includes('versionCode = 49210')&&gradle.includes('versionName = "4.9.210"'),
  'true-random-tavern-index':resolver.includes('function randomIndex(size:number)')&&resolver.includes('candidates[randomIndex(candidates.length)]'),
  'no-campaign-id-tavern-hash':!resolver.includes("stableIndex(campaign.id+':tavern-start'"),
  'named-plus-settlements':resolver.includes('const settlements=locations.filter')&&resolver.includes('for(const location of [...named,...settlements])'),
  'candidate-dedup':resolver.includes('const unique=new Map<string,TerraMediaLocation>()'),
  'tavern-random-fact':resolver.includes('o host sorteou este local aleatoriamente'),
  'strider-not-disabled':!selector.includes("const disabled = option.mode === 'strider'")&&!selector.includes('disabled={disabled}'),
  'strider-panel-always-opens':selector.includes("{mode === 'strider' && ("),
  'strider-explicit-help':selector.includes('A abertura Peregrino pode ser escolhida diretamente aqui.'),
  'star-of-mist-description-clear':modules.includes('A Estrela da Névoa, local de interesse do Livro Básico.'),
};
const failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.210_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.210_OK');
