import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root=path.resolve(process.argv[2]||process.cwd());
const scriptDir=path.dirname(fileURLToPath(import.meta.url));
const templates=path.join(scriptDir,'templates');

const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const write=(rel,text)=>{const out=path.join(root,rel);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text,'utf8');};
const copy=(srcRel,dstRel)=>write(dstRel,fs.readFileSync(path.join(templates,srcRel),'utf8'));
const req=(text,old,replacement,label)=>{
  if(!text.includes(old))throw new Error('OUA 4.9.201 missing anchor: '+label);
  return text.replace(old,replacement);
};

copy('lib/characterCreationHelp.ts','lib/characterCreationHelp.ts');
copy('components/CreationInfoButton.tsx','components/CreationInfoButton.tsx');
copy('components/CharacterPathsCreator.tsx','components/CharacterPathsCreator.tsx');

// -----------------------------------------------------------------------------
// Character Paths mechanics: all player choices are explicit and deterministic.
// -----------------------------------------------------------------------------
let factory=read('lib/characterFactory.ts');

factory=req(factory,
`function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>, budget=10):string[]{
  const trace:string[]=[]; const seen=new Set<string>();`,
`function spendPreviousExperience(ranks:Record<string,number>, prof:Record<string,number>, budget=10):string[]{
  const trace:string[]=[]; const seen=new Set<string>();`,
'previous experience function');

const peAnchor=`export function validateRuleLegalCharacter(character:Character):string[]{`;
const peHelper=`
function applyPreviousExperienceSelection(
  ranks:Record<string,number>,
  prof:Record<string,number>,
  budget:number,
  skillTargets:Partial<Record<string,number>>|undefined,
  proficiencyTargets:Partial<Record<string,number>>|undefined,
):string[]{
  if(!skillTargets||!proficiencyTargets) throw new Error('Experiência Prévia exige distribuição manual completa.');
  const skillCost=[0,1,2,3,5];
  const proficiencyCost=[0,2,4,6];
  let spent=0;
  const trace:string[]=[];
  for(const name of CANONICAL_SKILLS){
    const from=Number(ranks[name]||0);
    const to=Number(skillTargets[name]);
    if(!Number.isInteger(to)||to<from||to>4) throw new Error('Graduação inválida de Experiência Prévia para '+name+'.');
    let cost=0;
    for(let rank=from+1;rank<=to;rank++)cost+=skillCost[rank]||999;
    if(cost){trace.push('Perícia '+name+' → '+to+' ('+cost+' PE)');spent+=cost;}
    ranks[name]=to;
  }
  for(const name of ['Machados','Arcos','Lanças','Espadas'] as Proficiency[]){
    const from=Number(prof[name]||0);
    const to=Number(proficiencyTargets[name]);
    if(!Number.isInteger(to)||to<from||to>3) throw new Error('Graduação inválida de Experiência Prévia para '+name+'.');
    let cost=0;
    for(let rank=from+1;rank<=to;rank++)cost+=proficiencyCost[rank]||999;
    if(cost){trace.push('Proficiência '+name+' → '+to+' ('+cost+' PE)');spent+=cost;}
    prof[name]=to;
  }
  if(spent!==budget) throw new Error('Experiência Prévia deve gastar exatamente '+budget+' pontos; distribuição enviada gastou '+spent+'.');
  return trace;
}

`;
factory=req(factory,peAnchor,peHelper+peAnchor,'manual previous experience helper');

factory=req(factory,
`  coreVirtueMasterySkills?: string[];
}`,
`  coreVirtueMasterySkills?: string[];
  primaryProficiency?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
  secondaryProficiency?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
  previousExperienceSkillRanks?: Partial<Record<string,number>>;
  previousExperienceProficiencyRanks?: Partial<Record<string,number>>;
  initialReward?: (typeof REWARDS)[number];
  initialRewardWeaponGroup?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
}`,
'CharacterPathCreationOptions explicit choices');

factory=req(factory,
`  const ranks:Record<string,number>={};
  CANONICAL_SKILLS.forEach((name,i)=>ranks[name]=culture.skills[i]);
  const primary=pick(culture.prof2);
  const groups=(['Machados','Arcos','Lanças','Espadas'] as Proficiency[]);
  const other=pick(groups.filter(group=>group!==primary));
  const prof:Record<string,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0,[primary]:2,[other]:1};

  const baseBudget=rulesMode==='strider'?15:10;
  const previousExperienceBudget=Math.max(0,baseBudget+Number(event.previousExperienceDelta||0));
  trace.push(...spendPreviousExperience(ranks,prof,previousExperienceBudget));
  trace.push('Experiência Prévia: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.');`,
`  const ranks:Record<string,number>={};
  CANONICAL_SKILLS.forEach((name,i)=>ranks[name]=culture.skills[i]);
  const groups=(['Machados','Arcos','Lanças','Espadas'] as Proficiency[]);
  const primary=options.primaryProficiency as Proficiency|undefined;
  const other=options.secondaryProficiency as Proficiency|undefined;
  if(!primary||!culture.prof2.includes(primary)) throw new Error('Escolha explicitamente uma Proficiência principal permitida pela Cultura.');
  if(!other||!groups.includes(other)||other===primary) throw new Error('Escolha explicitamente uma segunda Proficiência diferente da principal.');
  const prof:Record<string,number>={Machados:0,Arcos:0,Lanças:0,Espadas:0,[primary]:2,[other]:1};
  trace.push('Proficiência principal escolhida: '+primary+' 2.');
  trace.push('Segunda Proficiência escolhida: '+other+' 1.');

  const baseBudget=rulesMode==='strider'?15:10;
  const previousExperienceBudget=Math.max(0,baseBudget+Number(event.previousExperienceDelta||0));
  trace.push(...applyPreviousExperienceSelection(ranks,prof,previousExperienceBudget,options.previousExperienceSkillRanks,options.previousExperienceProficiencyRanks));
  trace.push('Experiência Prévia escolhida pelo jogador: '+previousExperienceBudget+'/'+previousExperienceBudget+' pontos.');`,
'explicit path proficiencies and XP');

factory=req(factory,
`  const reward=pick(REWARDS);
  const features=[...background.distinctiveFeatures];`,
`  const reward=String(options.initialReward||'') as (typeof REWARDS)[number];
  if(!REWARDS.includes(reward)) throw new Error('Escolha uma Recompensa Inicial válida.');
  const rewardNeedsWeapon=['Afiada','Cruel','Atroz'].includes(reward);
  const rewardWeaponGroup=options.initialRewardWeaponGroup as Proficiency|undefined;
  if(rewardNeedsWeapon&&(!rewardWeaponGroup||Number(prof[rewardWeaponGroup]||0)<=0)) throw new Error('Escolha qual arma inicial recebe a Recompensa '+reward+'.');
  if(!rewardNeedsWeapon&&rewardWeaponGroup) throw new Error('Esta Recompensa não é aplicada a uma arma.');
  trace.push('Recompensa Inicial escolhida pelo jogador: '+reward+(rewardWeaponGroup?' · '+rewardWeaponGroup:'')+'.');
  const features=[...background.distinctiveFeatures];`,
'explicit initial reward');

factory=req(factory,
`  const profEntries=(Object.entries(prof) as Array<[Proficiency,number]>).filter(([,value])=>value>0);
  const rewardNeedsWeapon=['Afiada','Cruel','Atroz'].includes(reward);

  const weapons:CharacterSheet['combat']['weapons']=profEntries.map(([group],index)=>{`,
`  const profEntries=(Object.entries(prof) as Array<[Proficiency,number]>).filter(([,value])=>value>0);

  const weapons:CharacterSheet['combat']['weapons']=profEntries.map(([group],index)=>{`,
'remove duplicate rewardNeedsWeapon');

factory=req(factory,
`    if(index===0&&reward==='Atroz') weapon.damage+=1;
    if(index===0&&reward==='Cruel') weapon.injury+=2;`,
`    if(group===rewardWeaponGroup&&reward==='Atroz') weapon.damage+=1;
    if(group===rewardWeaponGroup&&reward==='Cruel') weapon.injury+=2;`,
'weapon reward target mechanics');

factory=req(factory,
`    description:index===0&&rewardNeedsWeapon?'Arma com Recompensa inicial: '+reward+'.':'Arma inicial correspondente à Proficiência de Combate.',
    equipped:index===0,damage:weapon.damage,injury:weapon.injury,
    tags:[weapon.group,...(index===0&&rewardNeedsWeapon?['Recompensa:'+reward]:[])],`,
`    description:weapon.group===rewardWeaponGroup&&rewardNeedsWeapon?'Arma com Recompensa inicial: '+reward+'.':'Arma inicial correspondente à Proficiência de Combate.',
    equipped:index===0,damage:weapon.damage,injury:weapon.injury,
    tags:[weapon.group,...(weapon.group===rewardWeaponGroup&&rewardNeedsWeapon?['Recompensa:'+reward]:[])],`,
'weapon reward inventory target');

factory=req(factory,
`      virtues:virtue,
      rewards:reward+' ('+(reward==='Reforçado'?'Broquel':reward==='Ajustado'||reward==='Fabricação Habilidosa'?'Camisa de Couro':weapons[0]?.name||'arma inicial')+')',
      flaws,`,
`      virtues:virtue,
      rewards:reward+' ('+(reward==='Reforçado'?'Broquel':reward==='Ajustado'||reward==='Fabricação Habilidosa'?'Camisa de Couro':weapons.find(weapon=>weapon.group===rewardWeaponGroup)?.name||'arma inicial')+')',
      flaws,`,
'reward recorded target');

factory=req(factory,
`      distinctiveFeatures:features,creationTrace:trace,rulesMode,
      characterPath:{
        backgroundId:background.id,importantEventId:event.id,importantEventFeat:event.feat,
        tnImprovementAttribute:options.tnImprovementAttribute,
        addedFavouredSkills:requestedAdds,removedFavouredSkills:requestedRemovals,
      },`,
`      distinctiveFeatures:features,creationTrace:trace,rulesMode,creationPreviousExperienceBudget:previousExperienceBudget,
      characterPath:{
        backgroundId:background.id,importantEventId:event.id,importantEventFeat:event.feat,
        tnImprovementAttribute:options.tnImprovementAttribute,
        addedFavouredSkills:requestedAdds,removedFavouredSkills:requestedRemovals,
        primaryProficiency:primary,secondaryProficiency:other,
        initialReward:reward,initialRewardWeaponGroup:rewardWeaponGroup,
        previousExperienceSpent:previousExperienceBudget,
        explicitChoicesVersion:'4.9.201',
      },`,
'persist Character Paths explicit choices');

factory=req(factory,
`export const CHARACTER_PATH_CULTURES = CULTURES
  .filter(culture=>['bardings','dwarves','elves','hobbits','bree','rangers'].includes(culture.id))
  .map(culture=>({id:culture.id as CharacterPathCultureId,name:culture.culture}));`,
`export const CHARACTER_PATH_CULTURES = CULTURES
  .filter(culture=>['bardings','dwarves','elves','hobbits','bree','rangers'].includes(culture.id))
  .map(culture=>({
    id:culture.id as CharacterPathCultureId,
    name:culture.culture,
    baseSkills:CANONICAL_SKILLS.map((name,index)=>({name,rank:culture.skills[index]})),
    proficiencyChoices:[...culture.prof2],
  }));`,
'Character Paths culture creation data');

write('lib/characterFactory.ts',factory);

// Persist optional audit metadata without changing the IndexedDB schema/version.
let db=read('lib/db.ts');
db=req(db,
`      addedFavouredSkills?: string[];
      removedFavouredSkills?: string[];
    };`,
`      addedFavouredSkills?: string[];
      removedFavouredSkills?: string[];
      primaryProficiency?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
      secondaryProficiency?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
      initialReward?: string;
      initialRewardWeaponGroup?: 'Machados'|'Arcos'|'Lanças'|'Espadas';
      previousExperienceSpent?: number;
      explicitChoicesVersion?: string;
    };`,
'characterPath persisted metadata');
write('lib/db.ts',db);

// -----------------------------------------------------------------------------
// Character sheet: make Favoured skills obvious on narrow screens + local help.
// This is also the manual/blank-sheet creation surface.
// -----------------------------------------------------------------------------
let sheet=read('components/CharacterSheetView.tsx');
sheet=req(sheet,
`import { virtuesForCulture } from '../lib/supplementRules';`,
`import { virtuesForCulture } from '../lib/supplementRules';
import { CreationInfoButton } from './CreationInfoButton';
import { REWARD_HELP, VIRTUE_HELP, skillHelp } from '../lib/characterCreationHelp';`,
'sheet help imports');

sheet=req(sheet,
`  const currentVirtueNames = useMemo(() => sheet.other.virtues.split(/[;\n,]+/).map(value => value.trim()).filter(Boolean), [sheet.other.virtues]);
  const wisdom = Number(sheet.attributes.wisdom || 1);`,
`  const currentVirtueNames = useMemo(() => sheet.other.virtues.split(/[;\n,]+/).map(value => value.trim()).filter(Boolean), [sheet.other.virtues]);
  const initialRewardName = useMemo(() => Object.keys(REWARD_HELP).find(name => sheet.other.rewards.includes(name)) || '', [sheet.other.rewards]);
  const favouredSkillNames = useMemo(() => sheet.skills.filter(skill => skill.favorite).map(skill => skill.name), [sheet.skills]);
  const legacyCharacterPath = Boolean(sheet.other.characterPath && sheet.other.characterPath.explicitChoicesVersion !== '4.9.201');
  const wisdom = Number(sheet.attributes.wisdom || 1);`,
'sheet derived creation help');

sheet=req(sheet,
`          {sheet.other.culturalBlessing && <p><b>Bênção cultural:</b> {sheet.other.culturalBlessing}</p>}
          <div className="mt-2 grid gap-2 sm:grid-cols-2">`,
`          {sheet.other.culturalBlessing && <p><b>Bênção cultural:</b> {sheet.other.culturalBlessing}</p>}
          <p className="mt-1"><b>Perícias Favorecidas:</b> {favouredSkillNames.join(' · ') || '—'}</p>
          {legacyCharacterPath&&<div className="mt-2 rounded border border-amber-800/70 bg-amber-950/25 p-2 text-amber-100"><b>Ficha criada por uma versão anterior das Trilhas.</b> A versão 4.9.201 não reescreve silenciosamente sua ficha salva: a Recompensa e a Experiência Prévia existentes permanecem ativas, mas novas criações passam a exigir essas escolhas do jogador.</div>}
          <div className="mt-2 grid gap-2 sm:grid-cols-2">`,
'sheet favoured summary and legacy safety');

sheet=req(sheet,
`                    <th className="py-1">Especialização</th>
                    <th className="py-1 text-center">Favorito</th>`,
`                    <th className="py-1">Especialização</th>
                    <th className="py-1 text-center">Favorecida</th>`,
'favoured table label');

sheet=req(sheet,
`                      <td className="py-2 font-bold text-blue-200">{skill.name}</td>`,
`                      <td className="py-2 font-bold text-blue-200">
                        <div className="flex items-center gap-1.5"><span>{skill.name}</span><CreationInfoButton entry={skillHelp(skill.name)}/></div>
                        {skill.favorite&&<span className="mt-1 inline-block rounded border border-amber-800/60 bg-amber-950/30 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wide text-amber-300">★ Favorecida</span>}
                      </td>`,
'inline favoured skill marker/help');

sheet=req(sheet,
`                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Virtudes</label>`,
`                <label className="mb-1 flex items-center gap-1 text-[11px] font-semibold uppercase text-blue-600 font-serif">Virtudes {currentVirtueNames[0]&&<CreationInfoButton entry={VIRTUE_HELP[currentVirtueNames[0]]}/>}</label>`,
'virtue info button');

sheet=req(sheet,
`                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Recompensas & Armas Nomeadas</label>`,
`                <label className="mb-1 flex items-center gap-1 text-[11px] font-semibold uppercase text-blue-600 font-serif">Recompensas & Armas Nomeadas {initialRewardName&&<CreationInfoButton entry={REWARD_HELP[initialRewardName]}/>}</label>`,
'reward info button');

write('components/CharacterSheetView.tsx',sheet);

// Version bump only. No DB name/version mutation.
let version=read('lib/version.ts');
version=version.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.201'");
version=version.replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49201');
write('lib/version.ts',version);

const pkgPath=path.join(root,'package.json');
const pkg=JSON.parse(fs.readFileSync(pkgPath,'utf8'));pkg.version='4.9.201';fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

let gradle=read('android/app/build.gradle.kts');
gradle=gradle.replace(/versionCode\s*=\s*\d+/,'versionCode = 49201');
gradle=gradle.replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.201"');
write('android/app/build.gradle.kts',gradle);

console.log('OUA 4.9.201 character creation repair applied');
