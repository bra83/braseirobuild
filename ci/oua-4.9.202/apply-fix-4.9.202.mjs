import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root=path.resolve(process.argv[2]||process.cwd());
const scriptDir=path.dirname(fileURLToPath(import.meta.url));
const templates=path.join(scriptDir,'templates');
const copy=(srcRel,dstRel)=>{const out=path.join(root,dstRel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,fs.readFileSync(path.join(templates,srcRel),'utf8'),'utf8');};
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>fs.writeFileSync(path.join(root,rel),text,'utf8');

copy('App.tsx','App.tsx');
copy('services/geminiService.ts','services/geminiService.ts');
copy('components/TokenPicker.tsx','components/TokenPicker.tsx');
copy('lib/partyTokenLibrary.ts','lib/partyTokenLibrary.ts');

let version=read('lib/version.ts');
version=version.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.202'");
version=version.replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49202');
write('lib/version.ts',version);

const pkgPath=path.join(root,'package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));
pkg.version='4.9.202';
fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts');
gradle=gradle.replace(/versionCode\s*=\s*\d+/,'versionCode = 49202');
gradle=gradle.replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.202"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.202 profile hard gate + token recrop applied');
