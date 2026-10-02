import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const p = rel => path.join(root, rel);
const read = rel => fs.readFileSync(p(rel), 'utf8').replace(/\r\n/g, '\n');
const write = (rel, text) => fs.writeFileSync(p(rel), text, 'utf8');
const must = (ok, label) => { if (!ok) throw new Error('OUA_49210: ' + label); };

let resolver = read('lib/adventureStartResolver.ts');

if (!resolver.includes('function randomIndex(size:number)')) {
  const anchor = "function stableIndex(seed:string,size:number){if(size<=1)return 0;let hash=2166136261;for(const ch of seed){hash^=ch.charCodeAt(0);hash=Math.imul(hash,16777619);}return Math.abs(hash>>>0)%size;}\n";
  must(resolver.includes(anchor), 'stableIndex anchor');
  resolver = resolver.replace(anchor, anchor + `
function randomIndex(size:number){
  if(size<=1)return 0;
  const cryptoApi=globalThis.crypto;
  if(cryptoApi?.getRandomValues){
    const value=new Uint32Array(1);
    cryptoApi.getRandomValues(value);
    return value[0]%size;
  }
  return Math.floor(Math.random()*size);
}
`);
}

const oldCandidates = `function tavernCandidates(locations:TerraMediaLocation[]){
  const named=locations.filter(location=>firstHex(location)&&/taverna|taberna|estalagem|pousada|inn|tavern|prancing pony|ponei saltitante/.test(locationText(location)));
  return named.length?named:locations.filter(location=>firstHex(location)&&/assentamento|cidade|vila|aldeia|town|village|settlement|bri|bree|hobbiton|mithlond|rivendell|valfenda/.test(locationText(location)));
}`;
const newCandidates = `function tavernCandidates(locations:TerraMediaLocation[]){
  const named=locations.filter(location=>firstHex(location)&&/taverna|taberna|estalagem|pousada|inn|tavern|prancing pony|ponei saltitante/.test(locationText(location)));
  const settlements=locations.filter(location=>firstHex(location)&&/assentamento|cidade|vila|aldeia|town|village|settlement|bri|bree|hobbiton|mithlond|rivendell|valfenda|porto|port|refugio|refúgio/.test(locationText(location)));
  const unique=new Map<string,TerraMediaLocation>();
  for(const location of [...named,...settlements]){
    const key=String(location.id||'')+'|'+String(firstHex(location)||'')+'|'+norm(location.name);
    if(!unique.has(key))unique.set(key,location);
  }
  return [...unique.values()];
}`;
must(resolver.includes(oldCandidates), 'tavernCandidates block');
resolver = resolver.replace(oldCandidates, newCandidates);

const oldPick = "const chosen=candidates[stableIndex(campaign.id+':tavern-start',candidates.length)]";
must(resolver.includes(oldPick), 'deterministic tavern pick');
resolver = resolver.replace(oldPick, "const chosen=candidates[randomIndex(candidates.length)]");

const oldFacts = "openingFacts:['Modo Taverna: o host escolheu este local e fixou a Companhia no Atlas.','A primeira cena acontece nesta taverna; o Gemini não escolhe outra cidade nem move o grupo.']";
if (resolver.includes(oldFacts)) {
  resolver = resolver.replace(oldFacts, "openingFacts:['Modo Taverna: o host sorteou este local aleatoriamente e fixou a Companhia no Atlas.','A primeira cena acontece nesta taverna; o Gemini não escolhe outra cidade nem move o grupo.']");
}
write('lib/adventureStartResolver.ts', resolver);

let selector = read('components/CampaignStartSelector.tsx');
selector = selector.replace(
  "          const disabled = option.mode === 'strider' && !canUseStrider;\n          const selected = option.mode === mode;",
  "          const selected = option.mode === mode;"
);
selector = selector.replace(/\n\s*disabled=\{disabled\}/g, '');
selector = selector.replace(" disabled:cursor-not-allowed disabled:opacity-35", "");
selector = selector.replace(/\n\s*\{disabled && <span className="mt-2 block text-\[10px\] text-amber-300">Disponível quando houver um Herói em Modo Peregrino\.<\/span>\}/g, '');
selector = selector.replace("{mode === 'strider' && canUseStrider && (", "{mode === 'strider' && (");
const striderIntro = `          <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
            O Patrono define a tabela de seis sementes de missão do Modo Peregrino. O Gemini não escolhe nem substitui esta decisão.
          </p>`;
if (selector.includes(striderIntro) && !selector.includes('A abertura Peregrino pode ser escolhida diretamente aqui.')) {
  selector = selector.replace(striderIntro, striderIntro + `
          {!canUseStrider && <p className="mt-2 text-[10px] leading-relaxed text-amber-200">
            A abertura Peregrino pode ser escolhida diretamente aqui. Escolha o Patrono e a missão abaixo.
          </p>}`);
}
write('components/CampaignStartSelector.tsx', selector);

let modules = read('lib/adventureModules.ts');
modules = modules.replace(
  "description:'Marco do livro básico. O Atlas posiciona a Companhia automaticamente na região do marco.'",
  "description:'A Estrela da Névoa, local de interesse do Livro Básico. O Atlas posiciona a Companhia automaticamente na região correta.'"
);
write('lib/adventureModules.ts', modules);

let version = read('lib/version.ts')
  .replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.210'")
  .replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49210');
write('lib/version.ts', version);

const pkgPath = p('package.json');
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
pkg.version = '4.9.210';
fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');

let gradle = read('android/app/build.gradle.kts')
  .replace(/versionCode\s*=\s*\d+/, 'versionCode = 49210')
  .replace(/versionName\s*=\s*"[^"]+"/, 'versionName = "4.9.210"');
write('android/app/build.gradle.kts', gradle);

console.log('OUA 4.9.210 tavern randomness + selectable Strider opening applied');
