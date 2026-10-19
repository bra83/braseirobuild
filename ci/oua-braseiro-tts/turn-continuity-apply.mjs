import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(process.argv[2]||process.cwd());
const read=f=>fs.readFileSync(path.join(root,f),'utf8');
const write=(f,s)=>fs.writeFileSync(path.join(root,f),s,'utf8');
function change(s,oldValue,newValue,label){
  const i=s.indexOf(oldValue);
  if(i<0||s.indexOf(oldValue,i+oldValue.length)>=0)throw Error('OUA_49213 missing/duplicate '+label);
  return s.slice(0,i)+newValue+s.slice(i+oldValue.length);
}
let app=read('App.tsx');
const pre=[
'    base = await reconcileCampaignLocationWithAtlas(base);',
'    if (campaignRef.current?.id === base.id && campaignRef.current !== base) {',
'      campaignRef.current = base;',
'      setCampaign(base);',
'    }',
'    // O espelho é gravado antes de qualquer chamada externa. Toda preparação da',
'    // próxima cena ocorre em memória e só substitui esta revisão após o commit.',
'    saveEmergencyCampaignMirror(base);'
].join('\n');
const safe=[
'    try {',
'      base = await reconcileCampaignLocationWithAtlas(base);',
'      if (campaignRef.current?.id === base.id && campaignRef.current !== base) {',
'        campaignRef.current = base;',
'        setCampaign(base);',
'      }',
'      saveEmergencyCampaignMirror(base);',
'    } catch (preflightError) {',
"      console.error('[BRASEIRO_TURN_PREFLIGHT]', preflightError);",
"      setStoryError('Falha na preparação do próximo turno antes do Gemini. A campanha foi preservada. ' + friendlyTurnError(preflightError));",
'      setIsStoryLoading(false);',
'      return;',
'    }'
].join('\n');
app=change(app,pre,safe,'preflight');
app=change(app,
"    if (active.pendingTest) {\n      setActiveTab('jogo');",
"    if (active.pendingTest) {\n      setStoryError('Há um teste pendente. Resolva a rolagem indicada antes de avançar a história.');\n      setActiveTab('jogo');",'choice pending dice');
app=change(app,
"    if (active.pendingTest && !isDmMode) {\n      setActiveTab('jogo');",
"    if (active.pendingTest && !isDmMode) {\n      setStoryError('Há um teste pendente. Resolva a rolagem indicada antes de avançar a história.');\n      setActiveTab('jogo');",'typed pending dice');
app=change(app,
"        if (begun.duplicateCommitted) {\n          setIsStoryLoading(false);",
"        if (begun.duplicateCommitted) {\n          setStoryError('Esta ação já foi processada. A cena anterior foi preservada para impedir consequências duplicadas.');\n          setIsStoryLoading(false);",'duplicate feedback');
const density='if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0) {';
if(app.split(density).length!==3)throw Error('OUA_49213 expected exactly two strict density checks');
app=app.replaceAll(density,'if (narrativeIntent.advancesFiction && isOpeningScene && turnRhythm.targetCharMin > 0) {');
app=change(app,
'if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive) {',
'if (narrativeIntent.advancesFiction && isOpeningScene && turnRhythm.targetCharMin > 0 && finalRhythmAssessment.repair && !localFallbackActive) {','extra density repair');
app=change(app,
'for (let qualityAttempt = 0; quality.repair && qualityAttempt < 2; qualityAttempt += 1) {',
'for (let qualityAttempt = 0; quality.repair && qualityAttempt < (isOpeningScene ? 2 : 0); qualityAttempt += 1) {','quality repair cost');
app=change(app,
'for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < 2; rhythmAttempt += 1) {',
'for (let rhythmAttempt = 0; rhythm.repair && rhythmAttempt < (isOpeningScene ? 2 : 0); rhythmAttempt += 1) {','rhythm repair cost');
const locationAnchor='      const canonicalLocation = mechanicallyUpdated.masterState?.position.locationName || mechanicallyUpdated.currentLocation;';
app=change(app,locationAnchor,[
"      if (narrativeIntent.advancesFiction && !String(response.atmospheric_narration || '').trim()) {",
"        throw new Error('O Gemini não forneceu uma nova cena. A ação não foi gravada; tente novamente ou selecione outro modelo de texto.');",
'      }',
"      if (narrativeIntent.advancesFiction && !isOpeningScene && turnRhythm.targetCharMin > 0 && String(response.atmospheric_narration || '').length < turnRhythm.targetCharMin) {",
"        console.warn('[BRASEIRO_TURN_DENSITY]', 'Continuação aproveitada sem descartar a cena gerada.');",
'      }',locationAnchor].join('\n'),'valid continuation guard');
app=change(app,
'      let response: GeminiStoryResponse;\n      if (localFallbackActive) {',
"      let response: GeminiStoryResponse;\n      console.info('[BRASEIRO_TURN_STAGE] narration-request');\n      if (localFallbackActive) {",'narration trace');
app=change(app,
'      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);',
"      console.info('[BRASEIRO_TURN_STAGE] narration-ready');\n      const finalQuality = assessNarrativeQuality(response, base, narrativeIntent, isOpeningScene);",'narration complete trace');
app=change(app,
'      try {\n        plan = await generateDirectorPlan(directorContext, actionForDirector);',
"      try {\n        console.info('[BRASEIRO_TURN_STAGE] director-request');\n        plan = await generateDirectorPlan(directorContext, actionForDirector);\n        console.info('[BRASEIRO_TURN_STAGE] director-ready');",'director trace');
app=change(app,
'      const committed = activeTurnId && activeTurnKey',
"      console.info('[BRASEIRO_TURN_STAGE] commit-request');\n      const committed = activeTurnId && activeTurnKey",'commit trace');
const commitIndex=app.indexOf("      console.info('[BRASEIRO_TURN_STAGE] commit-request');");
const committedIndex=app.indexOf('      saveEmergencyCampaignMirror(committed);',commitIndex);
if(commitIndex<0||committedIndex<0)throw Error('OUA_49213 commit-ready trace');
app=app.slice(0,committedIndex)+"      console.info('[BRASEIRO_TURN_STAGE] commit-ready');\n"+app.slice(committedIndex);
write('App.tsx',app);
let svc=read('services/geminiService.ts');
svc=change(svc,
"return {cadence,minChars:cadence==='opening'?Math.max(6500,minChars):minChars,minParagraphs};",
"return {cadence,minChars:cadence==='opening'?Math.max(6500,minChars):0,minParagraphs:cadence==='opening'?minParagraphs:0};",
'remote density only opening');
write('services/geminiService.ts',svc);
let version=read('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.213'").replace(/APP_VERSION_CODE\s*=\s*\d+/,'APP_VERSION_CODE = 49213');
write('lib/version.ts',version);
let pkg=JSON.parse(read('package.json'));pkg.version='4.9.213';write('package.json',JSON.stringify(pkg,null,2)+'\n');
let gradle=read('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49213').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.213"');
write('android/app/build.gradle.kts',gradle);
console.log('OUA_49213_TURN_CONTINUATION_PATCH_PASS');