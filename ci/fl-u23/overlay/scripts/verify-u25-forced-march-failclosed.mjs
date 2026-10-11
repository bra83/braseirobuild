/** U25: execute production forcedMarchPlan with a bounded mock rules engine.
 * Prevents NaN/Infinity/overlong input from freezing the trip-planning UI.
 * This is not an IndexedDB/browser/Android proof.
 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import ts from './load-typescript.cjs';
const source=readFileSync(new URL('../src/services/TravelCommitService.ts',import.meta.url),'utf8');
const tree=ts.createSourceFile('TravelCommitService.ts',source,ts.ScriptTarget.Latest,true,ts.ScriptKind.TS);
assert.equal(tree.parseDiagnostics.length,0,'TravelCommitService TypeScript parses');
const decl=tree.statements.filter(node=>ts.isVariableStatement(node))
  .flatMap(node=>[...node.declarationList.declarations])
  .find(node=>node.name.getText(tree)==='forcedMarchPlan');
assert.ok(decl,'canonical preflight function exists');
const text='const '+decl.getText(tree)+';return forcedMarchPlan;';
const code=ts.transpileModule(text,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText;
const deps={JourneyRulesEngine:{forcedMarch:()=>({allowed:true,penalty:0})},
 CampaignService:{advanceState:(c,q)=>({...c,currentDay:c.currentDay+Math.floor(q/4)})},
 quarterKey:(c)=>'day:'+c.currentDay+':'+c.currentPeriod};
const plan=new Function(...Object.keys(deps),code)(...Object.values(deps));
const campaign={id:'A',currentDay:1,currentPeriod:'Morning'};
const world={journey:{walkingDay:1,walkingQuartersToday:0,forcedMarchFailuresToday:0,quarterTravelProgress:0}};
let checks=0;
for(const value of [NaN,Infinity,-Infinity,-1,4.00001,1e200]){
 assert.throws(()=>plan(campaign,world,value,false),/Custo de viagem inválido/,String(value));checks++;
}
assert.deepEqual(plan(campaign,world,0,false),[],'zero-cost preflight remains no-op');checks++;
assert.ok(Array.isArray(plan(campaign,world,1,false)),'one quarter is supported');checks++;
assert.ok(Array.isArray(plan(campaign,world,4,false)),'four quarters are bounded');checks++;
const cls=tree.statements.find(node=>ts.isClassDeclaration(node)&&node.name?.text==='TravelCommitService');
assert.ok(cls,'travel service exists');checks++;
const prep=cls.members.find(node=>ts.isMethodDeclaration(node)&&node.name?.getText(tree)==='prepareForcedMarchChecks');
assert.ok(prep,'prepare check exists');checks++;
const prepText=prep.getText(tree);
assert.ok(prepText.includes('Number.isFinite(requestedPeriods)')&&prepText.includes('requestedPeriods < 0'),
 'prep refuses malformed requests before constructing a plan');checks++;
const commit=cls.members.find(node=>ts.isMethodDeclaration(node)&&node.name?.getText(tree)==='commit');
assert.ok(commit,'travel commit exists');checks++;
const commitText=commit.getText(tree);
assert.ok(commitText.includes('Number.isFinite(requestedPeriods)')&&commitText.includes('Number.isFinite(requestedDistanceKm)'),
 'commit guards both cost dimensions');checks++;
assert.ok(commitText.includes('activeMeta?.value &&') &&
  commitText.includes('activeMeta.value !== snapshot.id'),
  'commit checks active campaign identity inside transaction');checks++;
assert.ok(commitText.includes('requestedPeriods <= 0'),'movement has positive quarter cost');checks++;
console.log('U25_FORCED_MARCH_FAIL_CLOSED PASS '+checks+'/'+checks+' (production function + source contract)');
