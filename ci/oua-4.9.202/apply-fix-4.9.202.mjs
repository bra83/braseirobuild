import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root=path.resolve(process.argv[2]||process.cwd());
const scriptDir=path.dirname(fileURLToPath(import.meta.url));
const templates=path.join(scriptDir,'templates');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8').replace(/\r\n/g,'\n');
const write=(rel,text)=>{const out=path.join(root,rel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text,'utf8');};
const req=(text,old,replacement,label)=>{if(!text.includes(old))throw new Error('OUA 4.9.202 missing anchor: '+label);return text.replace(old,replacement);};
const copy=(srcRel,dstRel)=>write(dstRel,fs.readFileSync(path.join(templates,srcRel),'utf8'));

// Canonical profile: exact full user-authored file embedded at runtime.
copy('lib/humanMasterProfile.ts','lib/humanMasterProfile.ts');

// Preserve paragraph structure. The old sanitizer used \s{2,}, which collapsed \n\n.
let sanitizer=read('lib/narrativeSanitizer.ts');
sanitizer=sanitizer.replace(
  "return paragraphs.join('\\n\\n').replace(/\\s{2,}/g, ' ').replace(/\\s+([,.;:!?])/g, '$1').trim();",
  "return paragraphs.join('\\n\\n').replace(/[ \\t]{2,}/g, ' ').replace(/[ \\t]+([,.;:!?])/g, '$1').trim();"
);
sanitizer=sanitizer.replace(
  "return text.replace(/\\s{2,}/g, ' ').replace(/\\s+([,.;:!?])/g, '$1');",
  "return text.replace(/[ \\t]{2,}/g, ' ').replace(/[ \\t]+([,.;:!?])/g, '$1');"
);
write('lib/narrativeSanitizer.ts',sanitizer);

// Built-in party token cache must refresh when the cleaned pack revision changes.
let party=read('lib/partyTokenLibrary.ts');
party=req(party,
`export async function installPartyTokenById(tokenId: string): Promise<IndividualToken | null> {
  const existing = (await getStoreData<IndividualToken>('individual_tokens').catch(() => [])).find(item => item.id === tokenId);
  if (existing?.blob) return existing;
  const manifest = await loadPartyTokenManifest();
  const entry = manifest?.tokens.find(item => item.id === tokenId);`,
`export async function installPartyTokenById(tokenId: string): Promise<IndividualToken | null> {
  const manifest = await loadPartyTokenManifest();
  const existing = (await getStoreData<IndividualToken>('individual_tokens').catch(() => [])).find(item => item.id === tokenId);
  if (existing?.blob && existing.sheetId === manifest?.packId) return existing;
  const entry = manifest?.tokens.find(item => item.id === tokenId);`,
'party token pack refresh');
write('lib/partyTokenLibrary.ts',party);

// Opening detection + hard narrative density gate after sanitization.
let app=read('App.tsx');
app=req(app,
`      const isOpeningScene = /^\\[INICIAR AVENTURA GUIADA\\]/i.test(userAction);`,
`      const isOpeningScene = isHostOpening || /^\\[INICIAR AVENTURA GUIADA\\]/i.test(userAction);`,
'opening detection uses empty campaign too');

const oldBlock=`      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);

      let finalRhythmAssessment = assessNarrativeRhythm(response, turnRhythm);
      if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive) {
        response = await generateNarrationFromPlan(validatedPublicContext, \`${userAction}
[REPARO FINAL OBRIGATÓRIO DE DENSIDADE — NÃO MUDE O ESTADO NEM AS DECISÕES DO JOGADOR]\`, {
          ...plan,
          narrationBrief: \`${plan.narrationBrief || ''}
${rhythmRepairBrief(turnRhythm, finalRhythmAssessment.flags)}
O perfil técnico do Mestre é obrigatório. atmospheric_narration deve ultrapassar ${turnRhythm.targetCharMin} caracteres sem inventar acontecimentos futuros; aprofunde a MESMA batida por espaço, percepção sensorial, movimento, cultura observável, comportamento de NPCs e consequências já validadas.\`,
        });
        finalRhythmAssessment = assessNarrativeRhythm(response, turnRhythm);
      }
      // Se o provedor remoto ainda devolver texto curto após os reparos, mantenha o texto remoto.
      // Nunca substitua silenciosamente a cena por uma narração offline.
      const canonicalLocation = mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation;
      response = sanitizeNarrativeResponse(response, sceneEnvironment, canonicalLocation);
      if (narrativeIntent.advancesFiction) {
        response = ensureEnvironmentAnchoredResponse(response, environmentBinding);
        response.audioPlan = enrichAudioPlanWithEnvironment(response.audioPlan, sceneEnvironment);
      }`;

const newBlock=`      const canonicalLocation = mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation;

      // 4.9.202: cenas imersivas não podem mais escapar curtas depois dos reparos.
      // O gate roda DEPOIS do sanitizer, porque o sanitizer pode remover parágrafos operacionais.
      if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && !localFallbackActive) {
        for (let densityAttempt = 0; densityAttempt < 4; densityAttempt += 1) {
          const cleanedCandidate = sanitizeNarrativeResponse(response, sceneEnvironment, canonicalLocation);
          const density = assessNarrativeRhythm(cleanedCandidate, turnRhythm);
          const chars = String(cleanedCandidate.atmospheric_narration || '').trim().length;
          const paragraphCount = String(cleanedCandidate.atmospheric_narration || '').trim()
            ? String(cleanedCandidate.atmospheric_narration || '').trim().split(/\\n\\s*\\n/).filter(Boolean).length
            : 0;
          const minimumParagraphs = Math.max(1, turnRhythm.targetParagraphMin - 1);
          if (chars >= turnRhythm.targetCharMin && paragraphCount >= minimumParagraphs) {
            response = cleanedCandidate;
            break;
          }
          if (densityAttempt === 3) {
            throw new Error(
              \`O Narrador devolveu uma cena curta demais para o perfil canônico (\${chars}/\${turnRhythm.targetCharMin} caracteres; \${paragraphCount}/\${minimumParagraphs} parágrafos). A cena não foi salva. Tente novamente.\`,
            );
          }
          const previousNarration = String(cleanedCandidate.atmospheric_narration || '').trim();
          response = await generateNarrationFromPlan(
            validatedPublicContext,
            \`\${userAction}
[REESCRITA OBRIGATÓRIA PELO PERFIL CANÔNICO — NÃO AVANCE O ESTADO]
A proposta anterior teve \${chars} caracteres e \${paragraphCount} parágrafos. Reescreva INTEGRALMENTE a MESMA batida, preservando exatamente fatos, posição, tempo, NPCs, consequência mecânica e ponto de decisão. Não resuma.
PROPOSTA ANTERIOR:
\${previousNarration}\`,
            {
              ...plan,
              requiredTest: isOpeningScene ? null : plan.requiredTest,
              narrationBrief: \`\${plan.narrationBrief || ''}
\${rhythmRepairBrief(turnRhythm, density.flags)}
O PERFIL TÉCNICO CANÔNICO COMPLETO anexado à system instruction é obrigatório, não uma referência opcional. A nova atmospheric_narration deve alcançar PELO MENOS \${turnRhythm.targetCharMin} caracteres após sanitização e manter parágrafos reais separados por linhas em branco. Aprofunde a mesma situação por enquadramento, espaço, movimento, sensorialidade pertinente, comportamento humano, informação em camadas e reação do mundo. Não crie uma segunda cena, não resolva a escolha do jogador e não termine automaticamente com uma pergunta.\`,
            },
          );
        }
      }

      response = sanitizeNarrativeResponse(response, sceneEnvironment, canonicalLocation);
      if (narrativeIntent.advancesFiction) {
        response = ensureEnvironmentAnchoredResponse(response, environmentBinding);
        response.audioPlan = enrichAudioPlanWithEnvironment(response.audioPlan, sceneEnvironment);
      }

      // Barreiras finais: nunca persista uma cena imersiva abaixo do mínimo,
      // nem depois de uma transformação de ambiente/sanitização.
      if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0) {
        const finalChars = String(response.atmospheric_narration || '').trim().length;
        const finalParagraphs = String(response.atmospheric_narration || '').trim()
          ? String(response.atmospheric_narration || '').trim().split(/\\n\\s*\\n/).filter(Boolean).length
          : 0;
        if (finalChars < turnRhythm.targetCharMin || finalParagraphs < Math.max(1, turnRhytm.targetParagraphMin - 1)) {
          throw new Error(
            \`Gate narrativo 4.9.202 recusou texto curto (\${finalChars}/\${turnRhythm.targetCharMin} caracteres; \${finalParagraphs} parágrafos). Nada foi salvo.\`,
          );
        }
      }
      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);
      const finalRhythmAssessment = assessNarrativeRhythm(response, turnRhythm);`;

app=req(app,oldBlock,newBlock,'hard post-sanitize narrative gate');
write('App.tsx',app);

// Version bump only; DB/package identity stays unchanged.
let version=read('lib/version.ts');
version=version.replace(/APP_VERSION\\s*=\\s*'a[^']+'/, "APP_VERSION = '4.9.202'");
version=version.replace(/APP_VERSION_CODE\\s*=\\s*\\d+/, 'APP_VERSION_CODE = 49202');
write('lib/version.ts',version);

const pkgPath=path.join(root,'package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));pkg.version='4.9.202';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts');
gradle=gradle.replace(/versionCode\\s*=\\s*\\d+/,'versionCode = 49202');
gradle=gradle.replace(/versionName\\s*=\\s*"[^"]+"/,'versionName = "4.9.202"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.202 narrative/profile/token-cache patch applied');
