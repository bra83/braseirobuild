import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];if(!root)throw Error('Missing project');
const rd=f=>fs.readFileSync(path.join(root,f),'utf8');
const base=JSON.parse(rd('public/tor2e-ui/party-token-manifest.json'));
const extra=JSON.parse(rd('public/tor2e-ui/hero-token-pack-352.json'));
const normalize=s=>String(s||'').replace(/^\.\/+/,'').replace(/^\/+|\/+$/g,'');
function check(pass,msg){console.log((pass?'PASS ':'FAIL ')+msg);if(!pass)process.exitCode=1;}
check(base.basePath==='./tor2e-ui/assets','baseline old path reproduced');
check(extra.basePath==='tor2e-ui/assets','new path reproduced');
check(normalize(base.basePath)===normalize(extra.basePath),'new matcher merges old and new roots');
check(base.tokens.length===120 && extra.tokens.length===352,'120 originals and 352 embedded originals');
const merged=new Map(base.tokens.map(t=>[t.id,t]));
for(const t of extra.tokens)if(!merged.has(t.id) && t.race!=='enemy')merged.set(t.id,t);
check(merged.size===472,'combined catalog has 472 unique IDs');
check([...merged.values()].filter(t=>t.race!=='enemy').length===460,'hero selection has 460 characters');
const lib=rd('lib/partyTokenLibrary.ts');
check(lib.includes('const baseKey=clean(base.basePath.replace(')&&lib.includes('const extraKey=clean(extra.basePath.replace('),'real runtime source normalizes manifest basePaths');
check(!lib.includes('if(!extra||base.basePath!==extra.basePath)return base'),'old bug removed from runtime');
check(lib.includes("cache:'no-store'"),'no stale manifest cache');
const picker=rd('components/TokenPicker.tsx');
check(picker.includes('raceFilter')&&picker.includes('importedTokens.map'),'hero picker supports categorized imported characters');
check(picker.includes('Pacote adicional de 352 tokens indisponível'),'visible error if catalog fails to load');
for(const t of extra.tokens){if(!fs.existsSync(path.join(root,'public/tor2e-ui/assets',t.asset))){check(false,'missing embedded '+t.asset);break;}}
check(extra.tokens.every(t=>fs.existsSync(path.join(root,'public/tor2e-ui/assets',t.asset))),'all 352 actual PNG files embedded');
check(rd('lib/version.ts').includes("'4.9.215'")&&rd('android/app/build.gradle.kts').includes('versionCode = 49215'),'version updated');
if(process.exitCode)process.exit(process.exitCode);
console.log('OUA_49215_HERO_CATALOG_460_SELECTABLE_PASS');
