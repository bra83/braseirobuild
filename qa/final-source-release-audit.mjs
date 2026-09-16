import fs from 'node:fs';
import path from 'node:path';

const ROOT=process.cwd();
const WWW=path.join(ROOT,'app/src/main/assets/www');
const failures=[];
const forbiddenDirNames=new Set(['build','.gradle','.idea','.kotlin']);
const forbiddenFileNames=new Set(['local.properties']);
const forbiddenExts=new Set(['.apk','.aab','.jks','.keystore']);

function walk(dir){
  const out=[];
  for(const ent of fs.readdirSync(dir,{withFileTypes:true})){
    const p=path.join(dir,ent.name);
    if(ent.isDirectory()) out.push(...walk(p)); else out.push(p);
  }
  return out;
}
function walkDirs(dir){
  const out=[];
  for(const ent of fs.readdirSync(dir,{withFileTypes:true})){
    if(!ent.isDirectory()) continue;
    const p=path.join(dir,ent.name); out.push(p,...walkDirs(p));
  }
  return out;
}
const files=walk(ROOT);
for(const d of walkDirs(ROOT)) if(forbiddenDirNames.has(path.basename(d))) failures.push(`forbidden directory: ${path.relative(ROOT,d)}`);
for(const f of files){
  const name=path.basename(f), lower=name.toLowerCase();
  if(forbiddenFileNames.has(name)) failures.push(`forbidden file: ${path.relative(ROOT,f)}`);
  if(forbiddenExts.has(path.extname(lower))) failures.push(`forbidden binary/key: ${path.relative(ROOT,f)}`);
  if(lower==='.env'||lower.startsWith('.env.')) failures.push(`forbidden env: ${path.relative(ROOT,f)}`);
}

// Every production JSON asset must parse.
for(const f of files.filter(f=>f.startsWith(WWW+path.sep)&&f.endsWith('.json'))){
  try{JSON.parse(fs.readFileSync(f,'utf8'));}catch(e){failures.push(`invalid json ${path.relative(ROOT,f)}: ${e.message}`)}
}

// All literal local HTML href/src targets must exist.
for(const f of files.filter(f=>f.startsWith(WWW+path.sep)&&f.endsWith('.html'))){
  const text=fs.readFileSync(f,'utf8');
  for(const m of text.matchAll(/\b(?:href|src)=["']([^"']+)["']/g)){
    const raw=m[1];
    if(/^(?:https?:|data:|mailto:|tel:|javascript:|#|android:)/.test(raw)) continue;
    const clean=raw.split('#')[0].split('?')[0];
    if(!clean) continue;
    if(!fs.existsSync(path.resolve(path.dirname(f),clean))) failures.push(`missing html target ${path.relative(ROOT,f)} -> ${raw}`);
  }
}

// Production entry screens must not own Wave fixture data. Legacy SCR-015/SCR-016 are explicitly isolated.
const productionJs=['app.js','jornada.js','ficha.js','mochila.js','diario.js','regras.js','configuracoes.js','criacao.js','gestao.js','operacoes.js','scr014.js','scr022.js'];
for(const name of productionJs){
  const f=path.join(WWW,name), text=fs.readFileSync(f,'utf8');
  if(/wave[0-9].*fixture|\.fixture\.json|scr014-browser-fixture/i.test(text)) failures.push(`production fixture authority: ${name}`);
}

// Starter seed must use the current production class dialect.
const starter=JSON.parse(fs.readFileSync(path.join(WWW,'data/wwn.starter-character.v1.json'),'utf8'));
if(starter.classId!=='EXPERT'||starter.class!=='EXPERT') failures.push('starter class dialect is not EXPERT');

if(failures.length){
  console.error(JSON.stringify({status:'FAIL',failures},null,2));
  process.exit(1);
}
console.log(JSON.stringify({status:'PASS',sourceOnly:true,jsonAssets:files.filter(f=>f.startsWith(WWW+path.sep)&&f.endsWith('.json')).length,productionScreens:productionJs.length}));
