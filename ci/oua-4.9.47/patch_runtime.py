from pathlib import Path
import re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_runtime.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="App.tsx"
t=read(rel)

# Interpretar a observação da mesa física pelas mesmas regras canônicas.
t=req(
    t,
    "import { RollResult, applyFeatOneAsEye, rollMagicalSuccess, secureRoll } from './lib/dice';",
    "import { RollResult, applyFeatOneAsEye, rollMagicalSuccess, rollOneRingFromObservation, secureRoll } from './lib/dice';",
    "dice physical observation import",
)

old="""    let roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + effectiveExtra, skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(heroForRoll, skill, effectiveExtra, loneLandsMode, spendHope ? (useDesperateCourage?2:1) : 0,{isNight:Boolean(master.scene.isDark)||isNight||master.combat.environment==='underground',environment:master.combat.environment,inspired:useBirdsTongueInspired,journeyMode});
    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(heroForRoll.sheet));"""
new="""    let roll: RollResult;
    if (source) {
      // Sucesso Mágico não rola dados por definição; continua sendo resolvido pela regra.
      roll = rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + effectiveExtra, skill, source, Boolean(hero.sheet.attributes.weary));
    } else {
      // Prepara o pool/regra sem aplicar o resultado aleatório gerado pelo fallback.
      // No Android, o resultado autoritativo vem exclusivamente dos dados físicos após
      // o jogador tocar em ROLAR e depois em APLICAR RESULTADO.
      const preparedRoll = rollJourneySkill(
        heroForRoll,
        skill,
        effectiveExtra,
        loneLandsMode,
        spendHope ? (useDesperateCourage ? 2 : 1) : 0,
        {isNight:Boolean(master.scene.isDark)||isNight||master.combat.environment==='underground',environment:master.combat.environment,inspired:useBirdsTongueInspired,journeyMode},
      );
      const physicalRoll = window.BraseiroAndroid?.rollOneRingPhysical;
      if (physicalRoll) {
        const observed = await physicalRoll(
          preparedRoll.successResults?.length || 0,
          preparedRoll.rollMode || loneLandsMode,
          hero.id,
          hero.sheet.identity.name || hero.name || '',
        );
        roll = rollOneRingFromObservation(
          observed.feat_results || [],
          observed.success_results || [],
          Number(preparedRoll.modifier) || 0,
          Number(preparedRoll.targetNumber) || targetNumberForSkill(heroForRoll.sheet, skill),
          skill,
          preparedRoll.rollMode || loneLandsMode,
          Boolean(preparedRoll.weary),
          {
            engine: observed.engine || 'braseiro-physical-dice',
            mode: 'physical_3d',
            seedFeat: observed.physics_seed_feat,
            seedSuccess: observed.physics_seed_success,
            stepsFeat: observed.physics_steps_feat,
            stepsSuccess: observed.physics_steps_success,
            forcedSettle: Boolean(observed.forced_settle_feat || observed.forced_settle_success),
          },
          Boolean(preparedRoll.miserable),
        );
      } else {
        // Fora do Android mantém o fallback atual; nenhuma consequência é aplicada
        // antes da chamada explícita do jogador ao botão de rolagem da Jornada.
        roll = preparedRoll;
      }
    }
    roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(heroForRoll.sheet));"""
t=req(t,old,new,"Journey physical roll path")

# Evento de Jornada: no Android, alvo/detalhe e Dado(s) de Façanha também
# passam pela mesa física. O modo da região determina 1 ou 2 d12.
old="""      const hasStriderJourney=master.company.rulesMode==='strider';
      const detailDie=hasStriderJourney?secureRoll(1,6):undefined;
      const eventRegion=memoryChoice==='border-table'?'border':staged.region;
      const specialForodwaith=Boolean(master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable);
      const rawFeatResults=memoryChoice==='two-best-feat-dice'||memoryChoice==='border-table'?Array.from({length:2},()=>secureRoll(1,12)):specialForodwaith?[secureRoll(1,12)]:journeyFeatResults(eventRegion);
      const featResults=rawFeatResults;"""
new="""      const hasStriderJourney=master.company.rulesMode==='strider';
      const eventRegion=memoryChoice==='border-table'?'border':staged.region;
      const specialForodwaith=Boolean(master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable);
      const memoryRollMode: 'favoured'|'normal'|'ill-favoured' =
        memoryChoice==='two-best-feat-dice'||memoryChoice==='border-table'
          ? 'favoured'
          : specialForodwaith
            ? 'normal'
            : eventRegion==='border' ? 'favoured' : eventRegion==='dark' ? 'ill-favoured' : 'normal';
      const memoryPhysical = window.BraseiroAndroid?.rollOneRingPhysical
        ? await window.BraseiroAndroid.rollOneRingPhysical(hasStriderJourney ? 1 : 0, memoryRollMode, selectedTargetCharacterId || '', 'Evento de Jornada')
        : null;
      const detailDie=hasStriderJourney
        ? Number(memoryPhysical?.success_results?.[0] || secureRoll(1,6))
        : undefined;
      const rawFeatResults=memoryPhysical?.feat_results?.length
        ? memoryPhysical.feat_results
        : memoryChoice==='two-best-feat-dice'||memoryChoice==='border-table'
          ? Array.from({length:2},()=>secureRoll(1,12))
          : specialForodwaith?[secureRoll(1,12)]:journeyFeatResults(eventRegion);
      const featResults=rawFeatResults;"""
t=req(t,old,new,"Journey memory event physical dice")

old="""    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    const targetDie = secureRoll(1, 6);
    const detailDie=master.company.rulesMode==='strider'?secureRoll(1,6):undefined;
    const targetRole=targetDie<=2?'scout':targetDie<=4?'lookout':'hunter';"""
new="""    const region = classifyJourneyRegion(currentHex?.region || '', currentHex?.tags || []);
    const specialForodwaith=Boolean(master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable);
    const eventRollMode: 'favoured'|'normal'|'ill-favoured' = specialForodwaith
      ? 'normal'
      : region==='border' ? 'favoured' : region==='dark' ? 'ill-favoured' : 'normal';
    const isStriderEvent=master.company.rulesMode==='strider';
    const eventPhysical = window.BraseiroAndroid?.rollOneRingPhysical
      ? await window.BraseiroAndroid.rollOneRingPhysical(isStriderEvent ? 2 : 1, eventRollMode, '', 'Evento de Jornada')
      : null;
    const targetDie = Number(eventPhysical?.success_results?.[0] || secureRoll(1, 6));
    const detailDie=isStriderEvent?Number(eventPhysical?.success_results?.[1] || secureRoll(1,6)):undefined;
    const targetRole=targetDie<=2?'scout':targetDie<=4?'lookout':'hunter';"""
t=req(t,old,new,"Journey event target physical dice")

old="""    const rawFeatResults = master.journey.loneLandsForodwaith?.active&&master.journey.loneLandsForodwaith.useSpecialTable
      ?[secureRoll(1,12)]
      :journeyFeatResults(region);
    const featResults = rawFeatResults;"""
new="""    const rawFeatResults = eventPhysical?.feat_results?.length
      ? eventPhysical.feat_results
      : specialForodwaith
        ? [secureRoll(1,12)]
        : journeyFeatResults(region);
    const featResults = rawFeatResults;"""
t=req(t,old,new,"Journey event feat physical dice")

write(rel,t)
print("PATCH_OUA_4.9.47_JOURNEY_PHYSICAL_ROLL_OK")
