import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');
const exists = rel => fs.existsSync(path.join(root, rel));
const must = (cond, msg) => { if (!cond) throw new Error(msg); };

const version = read('lib/version.ts');
must(version.includes("APP_VERSION = '4.9.200'"), 'APP_VERSION 4.9.200 missing');
must(version.includes('APP_VERSION_CODE = 49200'), 'versionCode 49200 missing');
const pkg = JSON.parse(read('package.json'));
must(pkg.version === '4.9.200', `package.json expected 4.9.200, got ${pkg.version}`);
const gradle = read('android/app/build.gradle.kts');
must(/versionCode\s*=\s*49200/.test(gradle), 'Android versionCode 49200 missing');
must(/versionName\s*=\s*"4\.9\.200"/.test(gradle), 'Android versionName 4.9.200 missing');

const rhythm = read('lib/narrativeRhythm.ts');
must(/cadence: 'opening'[\s\S]{0,600}targetParagraphMin: 9, targetParagraphMax: 13/.test(rhythm), 'opening paragraph target not strengthened');
must(/cadence: 'opening'[\s\S]{0,800}targetWordMin: 1000, targetWordMax: 1450/.test(rhythm), 'opening word target not strengthened');
must(/cadence: 'opening'[\s\S]{0,900}targetCharMin: 6500, targetCharMax: 9000/.test(rhythm), 'opening char target not strengthened');
must(rhythm.includes('não comece automaticamente por Você/Vocês'), 'opening camera anti-formula missing');
must(rhythm.includes('nome canônico da região/local'), 'canonical location requirement missing');

const opening = read('lib/openingPlanner.ts');
for (const needle of [
  'NÍVEL 4 CINEMATOGRÁFICO, MAS INTERATIVO',
  'não comece automaticamente por “Você” ou “Vocês”',
  'NOME CANÔNICO DA REGIÃO/LOCAL',
  'mundo já em movimento',
  '9–13 parágrafos substanciais',
  '6.500–9.000 caracteres',
  'PRIMEIRA DECISÃO REAL',
]) must(opening.includes(needle), `opening contract missing: ${needle}`);

const app = read('App.tsx');
must(app.includes('balanced: { words: 1000, paragraphs: 9, chars: 6500 }'), 'balanced opening repair gate missing');
must(app.includes('cinematic: { words: 1100, paragraphs: 10, chars: 7200 }'), 'cinematic opening repair gate missing');
must(app.includes('const canonicalOpeningLocation ='), 'canonical opening location binding missing');
must(app.includes('NÃO o substitua por um tipo genérico de terreno'), 'generic terrain guard missing');

const gemini = read('services/geminiService.ts');
must(gemini.includes('ABERTURA DE AVENTURA é Nível 4 do perfil'), 'system opening profile missing');
must(gemini.includes('NÃO comece toda cena por “Você/Vocês”'), 'system anti-formula rule missing');
must(gemini.includes('Se a CADÊNCIA for opening'), 'dynamic opening cadence rule missing');
must(gemini.includes('6.500–9.000 caracteres, 9–13 parágrafos'), 'opening density prompt missing');
must(!gemini.includes('dirija-se aos jogadores/à Companhia em segunda pessoa'), 'old rigid second-person instruction still present');

const main = read('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
must(main.includes('private val neuralMinChars = 250'), 'neural min 250 missing');
must(main.includes('private val neuralTargetChars = 450'), 'neural target 450 missing');
must(main.includes('private val neuralMaxChars = 650'), 'neural max 650 missing');
must(main.includes('if(isNeuralTtsProfile()) return buildNeuralSpeechSegments(text,cues,engineMax)'), 'neural large-chunk path missing');
must(main.includes("closest(setOf('.','!','?'),true)?.let{return it};closest(setOf(';',','),false)?.let{return it}"), 'natural punctuation priority missing');
must(/override fun onDone\(id:String\)[\s\S]{0,300}advanceSpeechQueue\(p.rootId\)/.test(main), 'next-block onDone sequencing missing');
must(/val mode=if\(p.index>0\)TextToSpeech\.QUEUE_ADD else TextToSpeech\.QUEUE_FLUSH/.test(main), 'QUEUE_FLUSH/QUEUE_ADD contract missing');
must(/private fun scheduleSpeechWatchdog\(part:SpeechPart\)\{if\(isNeuralTtsProfile\(\)\)return/.test(main), 'neural watchdog must remain disabled');

const settings = read('components/SettingsView.tsx');
must(settings.includes('Blocos naturais ~450 caracteres (250–650), com várias frases por bloco'), 'settings description out of sync');

const sourceFiles = [
  'App.tsx','services/geminiService.ts','lib/openingPlanner.ts','lib/narrativeRhythm.ts',
  'components/SettingsView.tsx','android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt'
];
for (const rel of sourceFiles) {
  const t = read(rel);
  must(!/(?:Ã[¡-¿]|Â[¡-¿·]|â€”|â€œ|â€|resoluÃ|conexÃ|estÃ¡|estÃ£|nÃ£o|vocÃª|narraÃ§)/.test(t), `possible mojibake in ${rel}`);
}

const assetsDir = path.join(root, 'android/app/src/main/assets');
if (exists('android/app/src/main/assets/index.html')) {
  const jsFiles = [];
  const walkJs = dir => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const abs = path.join(dir, entry.name);
      if (entry.isDirectory()) walkJs(abs);
      else if (entry.isFile() && entry.name.endsWith('.js')) jsFiles.push(abs);
    }
  };
  walkJs(assetsDir);
  must(jsFiles.length > 0, 'no bundled Android JS found');
  const bundled = jsFiles.map(name => fs.readFileSync(name, 'utf8')).join('\n');
  must(bundled.includes('NÍVEL 4 CINEMATOGRÁFICO') || bundled.includes('N\u00cdVEL 4 CINEMATOGR\u00c1FICO'), 'bundled runtime missing opening profile');
  must(bundled.includes('6.500–9.000') || bundled.includes('6.500\u20139.000'), 'bundled runtime missing opening density');
  must(bundled.includes('Blocos naturais ~450 caracteres'), 'bundled runtime missing TTS settings contract');
  must(!/(?:resoluÃ|conexÃ|estÃ¡|nÃ£o|vocÃª|narraÃ§|â€”|â€œ|â€)/.test(bundled), 'mojibake found in bundled WebView JS');
}

console.log('PASS OUA 4.9.200 verification: narrative profile, opening density, UTF-8 and neural TTS flow');
