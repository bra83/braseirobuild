import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const app=read('App.tsx');
const service=read('services/geminiService.ts');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const checks={
  'version-4.9.205':version.includes("APP_VERSION = '4.9.205'")&&version.includes('APP_VERSION_CODE = 49205')&&pkg.version==='4.9.205'&&gradle.includes('versionCode = 49205')&&gradle.includes('versionName = "4.9.205"'),
  'atlas-does-not-block-opening':!app.includes("Antes de começar, escolha no Atlas o hex onde a Companhia está. A aventura não será iniciada numa posição inventada."),
  'opening-usable-gate':app.includes('function openingNarrationIsUsable')&&app.includes('text.length >= 1200')&&app.includes('words >= 180'),
  'director-cannot-block-opening':app.includes('Diretor Gemini indisponível; plano local seguro aplicado')&&!app.includes('O Gemini não respondeu à abertura. Nenhum texto local curto foi usado no lugar dele.'),
  'structured-to-plain-gemini-fallback':app.includes('Narrador estruturado falhou; tentando Gemini textual simples')&&app.includes('generateOpeningNarrationPlainText'),
  'plain-opening-function':service.includes('export async function generateOpeningNarrationPlainText')&&service.includes('[ABERTURA DE CAMPANHA — PRIMEIRA CENA JOGÁVEL]'),
  'first-valid-scene-wins-quality':app.includes('qualityAttempt < 2 && !(isOpeningScene && openingNarrationIsUsable(response))'),
  'first-valid-scene-wins-rhythm':app.includes('rhythmAttempt < 2 && !(isOpeningScene && openingNarrationIsUsable(response))'),
  'barbara-advisory-on-valid-opening':app.includes('Barbara recomendou rejeição, mas a primeira cena válida foi preservada'),
  'supervisor-advisory-on-valid-opening':app.includes('Supervisor Barbara não bloqueia primeira cena válida'),
  'final-density-does-not-delete-valid-opening':app.includes('finalRhythmAssessment.repair && !localFallbackActive && !(isOpeningScene && openingNarrationIsUsable(response))'),
};

let failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length) throw new Error('OUA_49205_STARTUP_VERIFY_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.205_STARTUP_OK');
