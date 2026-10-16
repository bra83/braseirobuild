import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
let ts;
try {
  ts = require('typescript');
} catch {
  ts = require('/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript/lib/typescript.js');
}
import { fileURLToPath } from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const parent=path.resolve(here,'..');
const root=fs.existsSync(path.join(here,'app/src/main/assets/vtt')) ? here : parent;
const vtt=path.join(root,'app/src/main/assets/vtt');
const entry='js/core/uiBridge.mjs';
const output=path.join(vtt,'js/core/uiBridge.bundle.js');

function norm(p){return p.split('/').reduce((a,x)=>{if(!x||x==='.')return a;if(x==='..')a.pop();else a.push(x);return a;},[]).join('/');}
function resolve(from,spec){if(!spec.startsWith('.')) return null; const b=from.split('/');b.pop(); let r=norm(`${b.join('/')}/${spec}`); if(!path.extname(r)) r+='.mjs'; return r;}
function deps(id,source){
  const info=ts.preProcessFile(source,true,true);
  return info.importedFiles.map(x=>resolve(id,x.fileName)).filter(Boolean);
}
const seen=new Set(), order=[];
function visit(id){
  if(seen.has(id)) return; seen.add(id);
  const abs=path.join(vtt,id); if(!fs.existsSync(abs)) throw new Error(`Missing local module ${id}`);
  const src=fs.readFileSync(abs,'utf8');
  for(const d of deps(id,src)) visit(d);
  order.push({id,src});
}
visit(entry);
// Read version from build.gradle.kts dynamically
const gradleFile = path.join(root, 'app/build.gradle.kts');
let appVersion = '1.0.131';
if (fs.existsSync(gradleFile)) {
  const gText = fs.readFileSync(gradleFile, 'utf8');
  const m = gText.match(/versionName\s*=\s*"([^"]+)"/);
  if (m) appVersion = m[1];
}

order.sort((a,b)=>a.id===entry?-1:b.id===entry?1:a.id.localeCompare(b.id));
const chunks=[];
chunks.push(`/* Braseiro FINAL ${appVersion} offline engine bundle — generated from local canonical .mjs sources. */`);
chunks.push('(function(g){');
chunks.push("const M=Object.create(null),C=Object.create(null);");
chunks.push("function norm(p){const a=[];for(const x of p.split('/')){if(!x||x==='.')continue;if(x==='..')a.pop();else a.push(x)}return a.join('/')}");
chunks.push("function resolve(from,spec){if(!spec.startsWith('.'))return spec;const b=from.split('/');b.pop();return norm(b.join('/')+'/'+spec)}");
chunks.push('function define(id,f){M[id]=f}');
chunks.push("function req(id,from=''){id=from?resolve(from,id):norm(id);if(C[id])return C[id].exports;const fn=M[id];if(!fn)throw new Error('Module not found: '+id+' from '+from);const module={exports:{}};C[id]=module;fn(module,module.exports,(x)=>req(x,id));return module.exports}");
for(const {id,src} of order){
  const compiled=ts.transpileModule(src,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,allowJs:true,esModuleInterop:false,sourceMap:false,removeComments:false},fileName:id.replace(/\.mjs$/i,'.js')}).outputText.trimEnd();
  chunks.push(`define(${JSON.stringify(id)},function(module,exports,require){\n${compiled}\n});`);
}
chunks.push(`try{const m=req(${JSON.stringify(entry)});m.installBraseiroUiBridge(g);g.__BRASEIRO_ENGINE_BUNDLE__={ok:true,version:m.UI_BRIDGE_VERSION||null,modules:${order.length},final:'${appVersion}-r54-p${appVersion.split('.').at(-1)}'};}catch(e){console.error(e);g.__BRASEIRO_ENGINE_BUNDLE__={ok:false,error:String(e&&e.stack||e)}};`);
chunks.push('})(window);');
fs.writeFileSync(output,chunks.join('\n')+'\n');
console.log(JSON.stringify({ok:true,entry,output:path.relative(root,output),modules:order.length},null,2));
