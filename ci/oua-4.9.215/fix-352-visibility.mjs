import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root)throw new Error('Missing OUA root directory');
const p=f=>path.join(root,f);
const rd=f=>fs.readFileSync(p(f),'utf8');
const wr=(f,s)=>fs.writeFileSync(p(f),s,'utf8');
const swap=(s,a,b,label)=>{const n=s.split(a).length-1;if(n!==1)throw Error(label+' expected once, saw '+n);return s.replace(a,b);};
let lib=rd('lib/partyTokenLibrary.ts');
lib=swap(lib,
  "if(!extra||base.basePath!==extra.basePath)return base;",
  ["if(!extra)return base;",
   "    const baseKey=clean(base.basePath.replace(/^\\.\\//, ''));",
   "    const extraKey=clean(extra.basePath.replace(/^\\.\\//, ''));",
   "    if(baseKey!==extraKey){",
   "      console.error('[OUA_TOKEN_CATALOG] Asset roots do not match:',baseKey,extraKey);",
   "      return base;",
   "    }"].join('\n'),
  'original basePath mismatch');
lib=swap(lib,"cache:'force-cache'","cache:'no-store'",'catalog fetch cache');
wr('lib/partyTokenLibrary.ts',lib);
let ui=rd('components/TokenPicker.tsx');
ui=swap(ui,
  '{partyManifest && <h3',
  '{partyManifest?.tokens.length === 120 && <p role="alert" className="my-2 text-sm text-red-300">Pacote adicional de 352 tokens indisponível. A biblioteca deve conter 472 tokens. Verifique a instalação do aplicativo.</p>}\n        {partyManifest && <h3',
  'catalog missing warning');
wr('components/TokenPicker.tsx',ui);
wr('lib/version.ts',rd('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.215'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49215'));
const pkg=JSON.parse(rd('package.json'));pkg.version='4.9.215';wr('package.json',JSON.stringify(pkg,null,2)+'\n');
wr('android/app/build.gradle.kts',rd('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/, 'versionCode = 49215').replace(/versionName\s*=\s*"[^"]+"/, 'versionName = "4.9.215"'));
console.log('OUA_49215_352_TOKEN_CATALOG_BASEPATH_FIXED');
