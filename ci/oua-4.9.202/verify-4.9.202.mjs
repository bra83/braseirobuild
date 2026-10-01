import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const must=(ok,msg)=>{if(!ok)throw new Error(msg);};

const version=read('lib/version.ts');
must(version.includes("APP_VERSION = '4.9.202'"),'APP_VERSION 4.9.202 missing');
must(version.includes('APP_VERSION_CODE = 49202'),'versionCode 49202 missing');
const pkg=JSON.parse(read('package.json')); must(pkg.version==='4.9.202','package version mismatch');
const gradle=read('android/app/build.gradle.kts');
must(/versionCode\s*=\s*49202/.test(gradle),'Android versionCode mismatch');
must(/versionName\s*=\s*"4\.9\.202"/.test(gradle),'Android versionName mismatch');

const db=read('lib/db.ts');
must(/const DB_NAME\s*=\s*'braseiro-db'/.test(db),'DB_NAME changed');
must(/const DB_VERSION\s*=\s*19/.test(db),'DB_VERSION changed');

const profile=read('lib/humanMasterProfile.ts');
must(profile.length>35000,'canonical human GM profile is truncated');
for(const marker of ['PERFIL TÉCNICO CANÔNICO','MESTRE HUMANO IMERSIVO','ficção','agência','NPC']) must(profile.includes(marker),'profile marker missing: '+marker);
must(profile.includes("1.0-canonical-full-4.9.202"),'canonical profile version marker missing');

const gemini=read('services/geminiService.ts');
must(gemini.includes('HUMAN_MASTER_PROFILE'),'Gemini no longer embeds full profile');
must(gemini.includes('${HUMAN_MASTER_PROFILE}'),'full profile interpolation missing from system instruction');

const app=read('App.tsx');
must(app.includes('const isOpeningScene = isHostOpening ||'),'opening no longer recognizes empty campaign');
must(app.includes('REESCRITA OBRIGATÓRIA PELO PERFIL CANÔNICO'),'hard rewrite loop missing');
must(app.includes('Gate narrativo 4.9.202 recusou texto curto'),'final narrative minimum gate missing');
must(app.includes('densityAttempt < 4'),'multi-pass density enforcement missing');
must(!app.includes('Se o provedor remoto ainda devolver texto curto após os reparos, mantenha o texto remoto.'),'old short-text escape still present');

const sanitizer=read('lib/narrativeSanitizer.ts');
must(sanitizer.includes("join('\\n\\n').replace(/[ \\t]{2,}/g"),'paragraph-preserving sanitizer missing');
must(!sanitizer.includes("join('\\n\\n').replace(/\\s{2,}/g"),'sanitizer still collapses paragraph breaks');

const rhythm=read('lib/narrativeRhythm.ts');
must(/cadence: 'opening'[\s\S]{0,650}targetCharMin: 6500, targetCharMax: 9000/.test(rhythm),'opening density regressed');
must(/cadence: 'arrival'[\s\S]{0,650}targetCharMin: 5200, targetCharMax: 7500/.test(rhythm),'arrival density regressed');
must(/cadence: 'exploration'[\s\S]{0,650}targetCharMin: 5200, targetCharMax: 7500/.test(rhythm),'exploration density regressed');

const party=read('lib/partyTokenLibrary.ts');
must(party.includes('existing.sheetId === manifest?.packId'),'token pack cache revision refresh missing');
const manifestPath=path.join(root,'public/tor2e-ui/party-token-manifest.json');
const manifest=JSON.parse(fs.readFileSync(manifestPath,'utf8'));
must(manifest.packId==='tor2e-party-characters-v2-clean-crops','clean token pack id missing');
must(manifest.cropRevision==='4.9.202-clean-components','token crop revision missing');
must(manifest.tokenCount===manifest.tokens.length,'tokenCount mismatch');
must(manifest.tokenCount>=40,'too many party tokens were discarded by crop repair');
const base=path.join(root,'public/tor2e-ui/assets');
for(const token of manifest.tokens){must(fs.existsSync(path.join(base,token.asset)),'missing cleaned party token '+token.id);}

const charFactory=read('lib/characterFactory.ts');
must(charFactory.includes("explicitChoicesVersion:'4.9.201'"),'4.9.201 creation fixes regressed');

const main=read('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
must(main.includes('private val neuralTargetChars = 450'),'Kokoro 4.9.200 flow regressed');
must(main.includes('private val neuralMinChars = 250'),'Kokoro min chunk regressed');
must(main.includes('private val neuralMaxChars = 650'),'Kokoro max chunk regressed');

console.log('PASS OUA 4.9.202: full canonical GM profile, hard narrative minimum, paragraphs preserved, party tokens cleaned, previous fixes preserved');