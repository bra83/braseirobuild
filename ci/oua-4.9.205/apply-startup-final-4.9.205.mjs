import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || process.cwd());
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8').replace(/\r\n/g, '\n');
const write = (rel, text) => fs.writeFileSync(path.join(root, rel), text, 'utf8');
const must = (ok, label) => { if (!ok) throw new Error('OUA_STARTUP_49205: ' + label); };
const replaceOnce = (text, oldText, newText, label) => {
  must(text.includes(oldText), label);
  return text.replace(oldText, newText);
};

let app = read('App.tsx');
let service = read('services/geminiService.ts');

if (!app.includes('function openingNarrationIsUsable')) {
  const anchor = 'function openingNarrationNeedsRepair(response: { atmospheric_narration?: string }, profileId: AdventureProfileId): boolean {';
  must(app.includes(anchor), 'openingNarrationNeedsRepair helper missing');
  app = app.replace(anchor, `function openingNarrationIsUsable(response: { atmospheric_narration?: string }): boolean {
  const text = String(response.atmospheric_narration || '').trim();
  const words = text.split(/\\s+/).filter(Boolean).length;
  return text.length >= 1200 && words >= 180;
}

${anchor}`);
}

// The hidden Director is helpful, but it must never prevent the very first scene.
// If it fails, use the deterministic local director and still call the Gemini narrator.
const oldDirectorCatch = `      } catch (error) {
        if (!isRecoverableGeminiFailure(error)) throw error;
        if (isOpeningScene) {
          throw new Error('O Gemini não respondeu à abertura. Nenhum texto local curto foi usado no lugar dele. Confira a chave e o modelo de texto em Configurações. Detalhe: ' + (error instanceof Error ? error.message : String(error)));
        }
        localFallbackActive = true;
        plan = createLocalDirectorPlan(base, userAction, selectedManuals as any);
        console.warn('[BRASEIRO_LOCAL_FALLBACK] Diretor local ativado:', error);
      }`;
const newDirectorCatch = `      } catch (error) {
        if (!isRecoverableGeminiFailure(error)) throw error;
        plan = createLocalDirectorPlan(base, userAction, selectedManuals as any);
        if (!isOpeningScene) localFallbackActive = true;
        console.warn('[BRASEIRO_STARTUP] Diretor Gemini indisponível; plano local seguro aplicado e Narrador ainda será tentado:', error);
      }`;
if (app.includes(oldDirectorCatch)) app = app.replace(oldDirectorCatch, newDirectorCatch);
must(app.includes('Diretor Gemini indisponível; plano local seguro aplicado'), 'director startup fallback not installed');

// Add a plain-text Gemini fallback for opening. This deliberately avoids the large
// structured narrator schema if a model/API combination rejects or truncates it.
if (!service.includes('generateOpeningNarrationPlainText')) {
  const marker = 'export async function answerRulesQuestion';
  must(service.includes(marker), 'service insertion marker missing');
  const fn = `
export async function generateOpeningNarrationPlainText(
  publicContext: string,
  userAction: string,
  plan: DirectorPlan,
): Promise<GeminiStoryResponse> {
  const prompt = \`[ABERTURA DE CAMPANHA — PRIMEIRA CENA JOGÁVEL]
\${publicContext}

AÇÃO/SEMENTE: \${userAction}

PLANO VISÍVEL: \${JSON.stringify({
    intent: plan.intent,
    visibleFacts: plan.visibleFacts,
    revelationsAllowed: plan.revelationsAllowed,
    narrationBrief: plan.narrationBrief,
  })}

Escreva SOMENTE a narração da primeira cena, em português brasileiro natural, imersiva e jogável.
Alvo: 5.200–7.500 caracteres em 7–12 parágrafos.
Preserve agência: não decida ações futuras dos jogadores, não invente regra, não revele segredo.
Comece pelo espaço e situação presentes e termine quando houver uma decisão real.\`;

  const response = await generateGemini(
    getGeminiTextModel(),
    [{ parts: [{ text: prompt }] }],
    { temperature: 0.78, maxOutputTokens: 9000 },
    SYSTEM_INSTRUCTION,
  );
  const narration = responseText(response as any).trim();
  if (narration.length < 800) throw new Error('Fallback textual do Gemini respondeu curto demais para formar uma cena.');

  return {
    atmospheric_narration: narration,
    action_result: '',
    visualPrompt: narration.slice(0, 1200),
    sceneVisualPrompt: narration.slice(0, 700),
    location: '',
    choices: [],
    requiredTest: null,
    audioPlan: { environment: [], ambience: [], ambienceDetails: [], musicMood: [], sfx: [], stopPreviousAmbience: false },
    events: [],
    imageRecommended: true,
    usedSourceNumbers: [],
    sourceUsageSummary: '',
    voiceScript: [],
  };
}

`;
  service = service.replace(marker, fn + marker);
}

// Import the simple fallback.
if (!app.includes('generateOpeningNarrationPlainText')) {
  must(app.includes("from './services/geminiService';"), 'gemini service import missing');
  app = app.replace(
    /import \{([^}]+)\} from '\.\/services\/geminiService';/,
    (full, names) => {
      const list = names.split(',').map(v => v.trim()).filter(Boolean);
      if (!list.includes('generateOpeningNarrationPlainText')) list.push('generateOpeningNarrationPlainText');
      return "import { " + list.join(', ') + " } from './services/geminiService';";
    },
  );
}

// First narrator call: structured -> simple plain-text Gemini fallback.
const narratorCall = '        response = await generateNarrationFromPlan(validatedPublicContext, userAction, plan);';
if (app.includes(narratorCall)) {
  app = app.replace(narratorCall, `        try {
          response = await generateNarrationFromPlan(validatedPublicContext, userAction, plan);
        } catch (narratorError) {
          if (!isOpeningScene) throw narratorError;
          console.warn('[BRASEIRO_STARTUP] Narrador estruturado falhou; tentando Gemini textual simples:', narratorError);
          response = await generateOpeningNarrationPlainText(validatedPublicContext, userAction, plan);
        }`);
}
must(app.includes('Narrador estruturado falhou; tentando Gemini textual simples'), 'structured narrator fallback not installed');

// FIRST VALID SCENE WINS: after a usable opening exists, quality/style repairs
// are advisory and cannot destroy the scene.
app = app.replace(
  'if (isOpeningScene && openingNarrationNeedsRepair(response, adventureProfile)) {',
  'if (isOpeningScene && openingNarrationNeedsRepair(response, adventureProfile) && !openingNarrationIsUsable(response)) {',
);
app = app.replace(
  'for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2; qualityAttempt += 1) {',
  'for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2 && !(isOpeningScene && openingNarrationIsUsable(response)); qualityAttempt += 1) {',
);
app = app.replace(
  'for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2; rhythmAttempt += 1) {',
  'for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2 && !(isOpeningScene && openingNarrationIsUsable(response)); rhythmAttempt += 1) {',
);

const barbaraReject = `        if (barbaraNarrativeValidation.valid === false) {
          throw new Error(\`Barbara rejeitou a proposta narrativa sem alterar a campanha: \${barbaraNarrativeValidation.code || barbaraNarrativeValidation.reason || 'violação de agência/cobertura'}\`);
        }`;
if (app.includes(barbaraReject)) {
  app = app.replace(barbaraReject, `        if (barbaraNarrativeValidation.valid === false) {
          if (isOpeningScene && openingNarrationIsUsable(response)) {
            console.warn('[BRASEIRO_STARTUP] Barbara recomendou rejeição, mas a primeira cena válida foi preservada:', barbaraNarrativeValidation);
          } else {
            throw new Error(\`Barbara rejeitou a proposta narrativa sem alterar a campanha: \${barbaraNarrativeValidation.code || barbaraNarrativeValidation.reason || 'violação de agência/cobertura'}\`);
          }
        }`);
}

const supervisorReject = `        if (supervisorFinal.valid === false) {
          throw new Error(\`Motor Barbara 1.1.3 Supervisor rejeitou a narrativa: \${supervisorFinal.reason || 'falha estrutural do supervisor'}\`);
        }`;
if (app.includes(supervisorReject)) {
  app = app.replace(supervisorReject, `        if (supervisorFinal.valid === false) {
          if (isOpeningScene && openingNarrationIsUsable(response)) {
            console.warn('[BRASEIRO_STARTUP] Supervisor Barbara não bloqueia primeira cena válida:', supervisorFinal.reason || 'falha estrutural');
          } else {
            throw new Error(\`Motor Barbara 1.1.3 Supervisor rejeitou a narrativa: \${supervisorFinal.reason || 'falha estrutural do supervisor'}\`);
          }
        }`);
}

app = app.replace(
  'if (isOpeningScene && !localFallbackActive && openingNarrationNeedsRepair(response, adventureProfile)) {',
  'if (isOpeningScene && !localFallbackActive && openingNarrationNeedsRepair(response, adventureProfile) && !openingNarrationIsUsable(response)) {',
);
app = app.replace(
  'if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive) {',
  'if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive && !(isOpeningScene && openingNarrationIsUsable(response))) {',
);

// Atlas may define the opening automatically later, but lack of hex cannot block scene creation.
const hardAtlasError = "setStoryError('Antes de começar, escolha no Atlas o hex onde a Companhia está. A aventura não será iniciada numa posição inventada.');";
must(!app.includes(hardAtlasError), 'hard Atlas gate is still present');

// Version identity.
let version = read('lib/version.ts');
version = version.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.205'");
version = version.replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49205');
write('lib/version.ts', version);

const pkgPath = path.join(root, 'package.json');
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
pkg.version = '4.9.205';
fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');

let gradle = read('android/app/build.gradle.kts');
gradle = gradle.replace(/versionCode\s*=\s*\d+/, 'versionCode = 49205');
gradle = gradle.replace(/versionName\s*=\s*"[^"]+"/, 'versionName = "4.9.205"');
write('android/app/build.gradle.kts', gradle);

write('App.tsx', app);
write('services/geminiService.ts', service);

console.log('OUA 4.9.205 FIRST_VALID_SCENE_WINS patch applied');
