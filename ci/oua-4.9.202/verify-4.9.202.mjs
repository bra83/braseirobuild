import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const exists=rel=>fs.existsSync(path.join(root,rel));
const must=(cond,msg)=>{if(!cond)throw new Error(msg);};

const version=read('lib/version.ts');
must(version.includes("APP_VERSION = '4.9.202'"),'APP_VERSION 4.9.202 missing');
must(version.includes('APP_VERSION_CODE = 49202'),'versionCode 49202 missing');
const pkg=JSON.parse(read('package.json'));
must(pkg.version==='4.9.202','package.json not 4.9.202');
const gradle=read('android/app/build.gradle.kts');
must(/versionCode\s*=\s*49202/.test(gradle),'Android versionCode 49202 missing');
must(/versionName\s*=\s*"4\.9\.201"/.test(gradle),'Android versionName 4.9.202 missing');

const db=read('lib/db.ts');
must(/const DB_NAME\s*=\s*'braseiro-db'/.test(db),'DB_NAME changed');
must(/const DB_VERSION\s*=\s*19/.test(db),'DB_VERSION changed');
must(db.includes("explicitChoicesVersion?: string"),'Character Paths audit metadata missing');

const factory=read('lib/characterFactory.ts');
const pathStart=factory.indexOf('export function createCharacterFromPath');
const pathEnd=factory.indexOf('export const CHARACTER_PATH_CULTURES',pathStart);
must(pathStart>=0&&pathEnd>pathStart,'Character Paths factory section missing');
const pathFactory=factory.slice(pathStart,pathEnd);
must(pathFactory.includes('applyPreviousExperienceSelection'),'manual Previous Experience resolver not wired');
must(!pathFactory.includes('spendPreviousExperience('),'Character Paths still randomizes Previous Experience');
must(!pathFactory.includes('pick(REWARDS)'),'Character Paths still randomizes Initial Reward');
must(pathFactory.includes("options.initialReward"),'explicit Initial Reward missing');
must(pathFactory.includes("options.primaryProficiency"),'explicit primary combat proficiency missing');
must(pathFactory.includes("options.secondaryProficiency"),'explicit secondary combat proficiency missing');
must(pathFactory.includes("previousExperienceSkillRanks"),'manual skill XP targets missing');
must(pathFactory.includes("previousExperienceProficiencyRanks"),'manual combat proficiency XP targets missing');
must(pathFactory.includes("weapon.group===rewardWeaponGroup"),'weapon reward is not bound to selected weapon');
must(pathFactory.includes("creationPreviousExperienceBudget:previousExperienceBudget"),'path XP budget is not persisted');
must(pathFactory.includes("explicitChoicesVersion:'4.9.201'"),'explicit-choice audit version missing');
must(factory.includes("baseSkills:CANONICAL_SKILLS.map"),'Path creator baseline skill data missing');
must(factory.includes("proficiencyChoices:[...culture.prof2]"),'Path creator legal proficiency options missing');

// Random character generator is intentionally still random; only guided creation must be explicit.
must(factory.includes('export function createRandomCharacter'),'random rules-based generator unexpectedly removed');

const creator=read('components/CharacterPathsCreator.tsx');
for(const needle of [
  'Experiência Prévia · {xpSpent}/{xpBudget} pontos',
  'O VTT não distribui mais estes pontos automaticamente.',
  'Recompensa Inicial · escolha obrigatória',
  'Proficiência principal · começa em 2',
  'Outra Proficiência · começa em 1',
  'previousExperienceSkillRanks',
  'previousExperienceProficiencyRanks',
  'initialRewardWeaponGroup',
  'CreationInfoButton',
]) must(creator.includes(needle),'Character Paths UI missing: '+needle);

const help=read('lib/characterCreationHelp.ts');
const info=read('components/CreationInfoButton.tsx');
for(const skill of ['Fascínio','Atletismo','Vigilância','Caçada','Música','Ofício','Indução','Viagem','Discernimento','Cura','Cortesia','Batalha','Persuasão','Furtividade','Busca','Exploração','Enigma','História']){
  must(help.includes("'"+skill+"'"),'help missing skill '+skill);
}
must(help.includes("conhecimento:'História'")||help.includes("name==='Conhecimento'?'História':name"),'Conhecimento/História alias help missing');
must(info.includes('Ajuda de criação · local'),'local info modal missing');
must(info.includes('Não consulta Gemini, PDF ou RAG'),'local-only help disclosure missing');
must(!/geminiService|generateGemini|searchChunks|manual_chunks/.test(help+info),'creation help must not call Gemini/RAG');

const sheet=read('components/CharacterSheetView.tsx');
must(sheet.includes('★ Favorecida'),'Favoured skill inline marker missing');
must(sheet.includes('<b>Perícias Favorecidas:</b>'),'Favoured skills summary missing');
must(sheet.includes('CreationInfoButton entry={skillHelp(skill.name)}'),'skill info button missing from normal/manual sheet');
must(sheet.includes('Ficha criada por uma versão anterior das Trilhas.'),'legacy Path warning missing');

// Preserve 4.9.200 human-GM opening.
const rhythm=read('lib/narrativeRhythm.ts');
must(/cadence: 'opening'[\s\S]{0,600}targetParagraphMin: 9, targetParagraphMax: 13/.test(rhythm),'4.9.200 opening paragraph target regressed');
must(/cadence: 'opening'[\s\S]{0,800}targetWordMin: 1000, targetWordMax: 1450/.test(rhythm),'4.9.200 opening word target regressed');
must(/cadence: 'opening'[\s\S]{0,900}targetCharMin: 6500, targetCharMax: 9000/.test(rhythm),'4.9.200 opening char target regressed');
const opening=read('lib/openingPlanner.ts');
for(const needle of ['NÍVEL 4 CINEMATOGRÁFICO, MAS INTERATIVO','NOME CANÔNICO DA REGIÃO/LOCAL','mundo já em movimento','9–13 parágrafos substanciais','6.500–9.000 caracteres','PRIMEIRA DECISÃO REAL']){
  must(opening.includes(needle),'4.9.200 opening contract regressed: '+needle);
}

// Preserve 4.9.200 neural Android/Kokoro flow.
const main=read('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
must(main.includes('private val neuralMinChars = 250'),'Kokoro min chunk regressed');
must(main.includes('private val neuralTargetChars = 450'),'Kokoro target chunk regressed');
must(main.includes('private val neuralMaxChars = 650'),'Kokoro max chunk regressed');
must(main.includes('if(isNeuralTtsProfile()) return buildNeuralSpeechSegments(text,cues,engineMax)'),'Kokoro large-chunk path regressed');
must(/override fun onDone\(id:String\)[\s\S]{0,300}advanceSpeechQueue\(p.rootId\)/.test(main),'Kokoro onDone chain regressed');
must(/val mode=if\(p.index>0\)TextToSpeech\.QUEUE_ADD else TextToSpeech\.QUEUE_FLUSH/.test(main),'Kokoro queue contract regressed');
must(/private fun scheduleSpeechWatchdog\(part:SpeechPart\)\{if\(isNeuralTtsProfile\(\)\)return/.test(main),'Kokoro neural watchdog regressed');

// Preserve free-key/Gemini and no-RAG-hard-gate repairs from 4.9.199.
const geminiTransport=read('lib/geminiTransport.ts');
must(geminiTransport.includes('x-goog-api-key'),'Gemini free-key header regressed');
const app=read('App.tsx');
must(!app.includes('Nenhum manual de O Um Anel 2e está pronto para uma resolução mecânica'),'RAG manual hard gate regressed');

// 4.9.202 — canonical profile is an executable output contract.
const geminiService=read('services/geminiService.ts');
must(geminiService.includes('type NarrativeDensityContract'),'narrative density contract missing');
must(geminiService.includes('BRASEIRO_PROFILE_HARD_GATE'),'remote profile hard gate missing');
must(geminiService.includes('maxOutputTokens:16000'),'density repair output budget missing');
must(geminiService.includes('HUMAN_MASTER_PROFILE'),'canonical human master profile not embedded in narrator repair');
must(geminiService.includes('_braseiroProfileEnforced'),'profile enforcement marker missing');
must(app.includes('Perfil narrativo obrigatório não cumprido pelo provedor'),'host final density rejection missing');
must(!app.includes('mantenha o texto remoto'),'silent acceptance of short remote narration still present');

// 4.9.202 — selected party tokens are rebuilt and picker cannot show spill outside cards.
const picker=read('components/TokenPicker.tsx');
const partyTokens=read('lib/partyTokenLibrary.ts');
must(picker.includes('overflow-hidden'),'party picker still allows neighboring figures to spill outside cards');
must(picker.includes('Personagem recortado'),'picker does not prefer corrected selected-token blob');
must(picker.includes("scale-[1.18]"),'picker isolation preview zoom missing');
must(partyTokens.includes("PARTY_TOKEN_CROP_VERSION = 'autocrop-v2'"),'party crop version missing');
must(partyTokens.includes('autoCropPartyTokenBlob'),'automatic token cropper missing');
must(partyTokens.includes("sheetId === expectedSheetId"),'legacy bad token crops are not invalidated');
// Full canonical profile and sanitizer paragraph preservation.
const canonicalProfile=read('lib/humanMasterProfile.ts');
must(canonicalProfile.includes("1.0-canonical-full-4.9.202"),'canonical full profile revision missing');
must(canonicalProfile.length>35000,'canonical human master profile appears truncated');
const sanitizer=read('lib/narrativeSanitizer.ts');
must(sanitizer.includes("join('\\n\\n').replace(/[ \\t]{2,}/g"),'sanitizer does not preserve paragraph separators');
must(!sanitizer.includes("join('\\n\\n').replace(/\\s{2,}/g"),'sanitizer still collapses paragraphs');

// Build-time crop pass must replace the bundled pack itself, not only CSS/runtime display.
const manifestPath=path.join(root,'public/tor2e-ui/party-token-manifest.json');
if(fs.existsSync(manifestPath)){
  const manifest=JSON.parse(fs.readFileSync(manifestPath,'utf8'));
  must(manifest.packId==='tor2e-party-characters-v2-clean-crops','clean party token pack was not generated');
  must(manifest.cropRevision==='4.9.202-clean-components','party token crop revision missing');
  must(manifest.tokenCount===manifest.tokens.length,'party token manifest count mismatch');
  must(manifest.tokenCount>=40,'crop repair discarded too many tokens');
}
// UTF-8 gate on changed and critical files.
for(const rel of [
  'components/CharacterPathsCreator.tsx','components/CharacterSheetView.tsx','components/CreationInfoButton.tsx',
  'lib/characterFactory.ts','lib/characterCreationHelp.ts','lib/db.ts','services/geminiService.ts',
  'android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt'
]){
  const text=read(rel);
  must(!/(?:Ã[¡-¿]|Â[¡-¿·]|â€”|â€œ|â€|resoluÃ|conexÃ|estÃ¡|estÃ£|nÃ£o|vocÃª|narraÃ§)/.test(text),'possible mojibake in '+rel);
}

const assetsDir=path.join(root,'android/app/src/main/assets');
if(exists('android/app/src/main/assets/index.html')){
  const jsFiles=[];
  const walk=dir=>{for(const entry of fs.readdirSync(dir,{withFileTypes:true})){const abs=path.join(dir,entry.name);if(entry.isDirectory())walk(abs);else if(entry.isFile()&&entry.name.endsWith('.js'))jsFiles.push(abs);}};
  walk(assetsDir);
  must(jsFiles.length>0,'no bundled Android JS found');
  const bundled=jsFiles.map(file=>fs.readFileSync(file,'utf8')).join('\n');
  for(const needle of ['Recompensa Inicial · escolha obrigatória','O VTT não distribui mais estes pontos automaticamente.','Ajuda de criação · local','★ Favorecida']){
    must(bundled.includes(needle),'Android bundle missing 4.9.202 UI marker: '+needle);
  }
  must(bundled.includes('6.500–9.000')||bundled.includes('6.500\u20139.000'),'Android bundle lost 4.9.200 opening density');
  must(!/(?:resoluÃ|conexÃ|estÃ¡|nÃ£o|vocÃª|narraÃ§|â€”|â€œ|â€)/.test(bundled),'mojibake in Android bundle');
}

console.log('PASS OUA 4.9.202: hard narrative profile gate + automatic party token recrop + all previous regressions');
