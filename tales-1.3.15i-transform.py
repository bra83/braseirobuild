from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.15h-transform.py')}
code=(repo/'tales-1.3.15h-transform.py').read_text(encoding='utf-8')
old=sys.argv[:]
sys.argv=[ns['__file__'],sys.argv[1]]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old

root=Path(sys.argv[1])

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def replace_once(text,old,new,label):
    if old not in text: raise SystemExit(f'anchor not found: {label}')
    return text.replace(old,new,1)

core='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'
t=read(core)

t=replace_once(t,
'''  val waivedTroubles: Set<String> = emptySet(),
  val lastError: String? = null
) {
  fun finalPool(): Int {
    val test = activeTest ?: return 0
    if (kid.broken) return 0
    return (test.basePool() + test.approvedItemBonus + test.informationBonus + (if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0) - kid.conditionPenalty).coerceAtLeast(0)
  }
}''',
'''  val waivedTroubles: Set<String> = emptySet(),
  val contactItemBonusBank: Int = 0,
  val lastError: String? = null
) {
  fun finalPool(): Int {
    val test = activeTest ?: return 0
    if (kid.broken) return 0
    val leadership = leadershipAssignments.entries.firstOrNull { it.key.equals(kid.name, ignoreCase = true) }?.value ?: 0
    return (test.basePool() + test.approvedItemBonus + test.informationBonus + leadership + (if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0) - kid.conditionPenalty).coerceAtLeast(0)
  }
}''','GameState contact/leadership pool')

t=replace_once(t,
'''  data object ApplyInformationBonus : GameEvent
  data class PurchaseBonusEffect(val effectId: String, val target: String? = null) : GameEvent
  data class AssignLeadershipDice(val kid: String, val dice: Int = 1) : GameEvent''',
'''  data object ApplyInformationBonus : GameEvent
  data object ApplyContactItemBonus : GameEvent
  data class PurchaseBonusEffect(val effectId: String, val target: String? = null) : GameEvent
  data class ApplyLeadershipComfort(val condition: Condition) : GameEvent
  data class AssignLeadershipDice(val kid: String, val dice: Int = 1) : GameEvent''','new events')

old_req='''          !CanonicalTalesRules.purposeAllowed(skill, event.request.purpose) -> fail("Finalidade incompatível com a Perícia.")
          key in state.waivedTroubles -> ok(state.copy(activeTest=event.request,testResolution=TestResolution(emptyList(),event.request.requiredSuccesses)))
          else -> ok(state.copy(activeTest=event.request,testResolution=null))'''
new_req='''          !CanonicalTalesRules.purposeAllowed(skill, event.request.purpose) -> fail("Finalidade incompatível com a Perícia.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && event.request.targetKid.isNullOrBlank() -> fail("Conforto por Liderança exige um Kid alvo.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.none { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true } -> fail("Kid alvo do conforto não pertence ao grupo.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.any { member -> event.request.targetKid?.let { target -> (member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true)) && member.name.equals(state.kid.name,true) } == true } -> fail("Liderança conforta outro Kid, não o próprio líder.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.firstOrNull { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true }?.broken == true -> fail("Conforto por Liderança não cura Kid Quebrado.")
          event.request.purpose == TestPurpose.LEADERSHIP_COMFORT && state.group.members.firstOrNull { member -> event.request.targetKid?.let { target -> member.name.equals(target,true) || member.name.substringBefore(' ').equals(target,true) } == true }?.conditions?.isEmpty() == true -> fail("Kid alvo não possui Condição para confortar.")
          key in state.waivedTroubles -> ok(state.copy(activeTest=event.request,testResolution=TestResolution(emptyList(),event.request.requiredSuccesses)))
          else -> ok(state.copy(activeTest=event.request,testResolution=null))'''
t=replace_once(t,old_req,new_req,'comfort request validation')

old_item='''      is GameEvent.ApplyApprovedItem -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")
        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }

      GameEvent.ApplyInformationBonus -> {'''
new_item='''      is GameEvent.ApplyApprovedItem -> {
        val test = state.activeTest ?: return fail("Não há teste ativo.")
        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")
        else if (test.approvedItemBonus != 0) fail("Só um Item pode fornecer bônus ao teste.")
        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")
        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))
      }

      GameEvent.ApplyInformationBonus -> {'''
t=replace_once(t,old_item,new_item,'single item guard')

info_block='''      GameEvent.ApplyInformationBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("Bônus de informação deve ser usado antes da rolagem.")
          state.informationBonusBank<=0 -> fail("Nenhum bônus de informação disponível.")
          test.informationBonus>=3 -> fail("Máximo de +3 dados de informação.")
          else -> ok(state.copy(informationBonusBank=state.informationBonusBank-1,activeTest=test.copy(informationBonus=test.informationBonus+1)))
        }
      }

      is GameEvent.PurchaseBonusEffect -> {'''
info_new='''      GameEvent.ApplyInformationBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("Bônus de informação deve ser usado antes da rolagem.")
          state.informationBonusBank<=0 -> fail("Nenhum bônus de informação disponível.")
          test.informationBonus>=3 -> fail("Máximo de +3 dados de informação.")
          else -> ok(state.copy(informationBonusBank=state.informationBonusBank-1,activeTest=test.copy(informationBonus=test.informationBonus+1)))
        }
      }

      GameEvent.ApplyContactItemBonus -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        when {
          state.testResolution!=null -> fail("O contato deve ser usado antes da rolagem.")
          state.contactItemBonusBank !in 1..3 -> fail("Nenhum bônus de contato disponível.")
          test.approvedItemBonus!=0 -> fail("Só um Item pode fornecer bônus ao teste.")
          else -> ok(state.copy(contactItemBonusBank=0,activeTest=test.copy(approvedItemBonus=state.contactItemBonusBank)))
        }
      }

      is GameEvent.PurchaseBonusEffect -> {'''
t=replace_once(t,info_block,info_new,'contact item apply event')

heal_self='''              BonusEffectKind.HEAL_SELF_CONDITION -> {
                if(next.kid.broken) return fail("Este efeito não cura Quebrado.")
                val c=event.target?.let{raw->Condition.entries.firstOrNull{it.name.equals(raw,true)||it.label.equals(raw,true)}} ?: return fail("Escolha uma Condição para curar.")
                if(c !in next.kid.conditions) return fail("Condição não está marcada.")
                next=next.copy(kid=next.kid.copy(conditions=next.kid.conditions-c))
              }
              else -> Unit'''
heal_new='''              BonusEffectKind.HEAL_SELF_CONDITION -> {
                if(next.kid.broken) return fail("Este efeito não cura Quebrado.")
                val c=event.target?.let{raw->Condition.entries.firstOrNull{it.name.equals(raw,true)||it.label.equals(raw,true)}} ?: return fail("Escolha uma Condição para curar.")
                if(c !in next.kid.conditions) return fail("Condição não está marcada.")
                next=next.copy(kid=next.kid.copy(conditions=next.kid.conditions-c))
              }
              BonusEffectKind.HEAL_OTHER_CONDITION -> {
                if(test.purpose!=TestPurpose.LEADERSHIP_COMFORT || !result.baseEffectApplied) return fail("Cure primeiro a Condição básica do Kid confortado.")
                val targetName=test.targetKid.orEmpty()
                val index=next.group.members.indexOfFirst{it.name.equals(targetName,true)||it.name.substringBefore(' ').equals(targetName,true)}
                if(index<0) return fail("Kid confortado não pertence ao grupo.")
                val member=next.group.members[index]
                if(member.broken) return fail("Conforto por Liderança não cura Kid Quebrado.")
                val c=event.target?.let{raw->Condition.entries.firstOrNull{it.name.equals(raw,true)||it.label.equals(raw,true)}} ?: return fail("Escolha uma Condição do Kid confortado.")
                if(c !in member.conditions) return fail("Condição não está marcada no Kid confortado.")
                val members=next.group.members.toMutableList(); members[index]=member.copy(conditions=member.conditions-c)
                next=next.copy(group=next.group.copy(members=members))
              }
              BonusEffectKind.CONTACT_ITEM -> {
                if(next.contactItemBonusBank>=3) return fail("O contato já está preparado como Item +3.")
                next=next.copy(contactItemBonusBank=next.contactItemBonusBank+1)
              }
              else -> Unit'''
t=replace_once(t,heal_self,heal_new,'heal other/contact purchase')

assign_anchor='''      is GameEvent.AssignLeadershipDice -> {'''
comfort_case='''      is GameEvent.ApplyLeadershipComfort -> {
        val test=state.activeTest ?: return fail("Não há teste ativo.")
        val result=state.testResolution ?: return fail("Role Liderança antes de aplicar conforto.")
        val targetName=test.targetKid.orEmpty()
        val index=state.group.members.indexOfFirst{it.name.equals(targetName,true)||it.name.substringBefore(' ').equals(targetName,true)}
        when {
          test.purpose!=TestPurpose.LEADERSHIP_COMFORT -> fail("O teste ativo não é de conforto por Liderança.")
          result.successes<test.requiredSuccesses -> fail("Conforto exige sucesso no teste de Liderança.")
          result.baseEffectApplied -> fail("A Condição básica do conforto já foi curada.")
          index<0 -> fail("Kid confortado não pertence ao grupo.")
          state.group.members[index].name.equals(state.kid.name,true) -> fail("O líder não conforta a si mesmo com este efeito.")
          state.group.members[index].broken -> fail("Conforto por Liderança não cura Kid Quebrado.")
          event.condition !in state.group.members[index].conditions -> fail("Condição não está marcada no Kid confortado.")
          else -> {
            val members=state.group.members.toMutableList()
            val member=members[index]
            members[index]=member.copy(conditions=member.conditions-event.condition)
            ok(state.copy(group=state.group.copy(members=members),testResolution=result.copy(baseEffectApplied=true)))
          }
        }
      }

      is GameEvent.AssignLeadershipDice -> {'''
t=replace_once(t,assign_anchor,comfort_case,'leadership comfort reducer')

roll_old='''            val successes=event.dice.count{it==6}
            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) CanonicalTalesRules.leadershipPool(successes) else state.leadershipPoolDice
            ok(state.copy(leadershipPoolDice=pool,leadershipAssignments=if(pool!=state.leadershipPoolDice) emptyMap() else state.leadershipAssignments,testResolution=TestResolution(event.dice,successes)))'''
roll_new='''            val successes=event.dice.count{it==6}
            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) CanonicalTalesRules.leadershipPool(successes) else state.leadershipPoolDice
            val assignedKey=state.leadershipAssignments.keys.firstOrNull{it.equals(state.kid.name,true)}
            val remainingAssignments=if(assignedKey!=null) state.leadershipAssignments-assignedKey else state.leadershipAssignments
            ok(state.copy(
              leadershipPoolDice=pool,
              leadershipAssignments=if(pool!=state.leadershipPoolDice) emptyMap() else remainingAssignments,
              testResolution=TestResolution(event.dice,successes)
            ))'''
t=replace_once(t,roll_old,roll_new,'consume leadership assignment')

t=replace_once(t,
'''            grantedSuccesses = emptyMap(),
            waivedTroubles = emptySet()
          ))''',
'''            grantedSuccesses = emptyMap(),
            waivedTroubles = emptySet(),
            contactItemBonusBank = 0
          ))''','reset contact bank')
write(core,t)

# Canonical local contract documents the now-executable special cases.
rel='app/src/main/java/com/braseiro/talesvtt/game/CanonicalTalesRules.kt'
t=read(rel)
t=replace_once(t,
'''Information effects bank +1 die each, max +3. Leadership preparation gives pool 2/4/6 for 1/2/3+ successes, never for the leader.
Conditions: Chateado, Assustado, Exausto, Machucado. Broken auto-fails until valid recovery. NPCs never roll.''',
'''Information effects bank +1 die each, max +3. Leadership preparation gives pool 2/4/6 for 1/2/3+ successes, never for the leader; assigned dice are consumed by that Kid's next roll.
Leadership comfort: on success heal exactly one chosen light Condition from the other target Kid; each lead_more extra success heals one additional Condition from that same Kid; lead_self heals one light Condition from the leader. None heals Broken.
Contact Item effect banks +1 per purchased extra success up to +3; the resulting contact bonus is used once as the single Item bonus of a later test and is then consumed.
Conditions: Chateado, Assustado, Exausto, Machucado. Broken auto-fails until valid recovery. NPCs never roll.''','canonical leadership/contact contract')
write(rel,t)

# Snapshot v13 persists contact item bonus.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt'
t=read(rel)
t=t.replace('private const val VERSION = 12','private const val VERSION = 13',1)
t=replace_once(t,
'''    line("bonus.waivedTroubles", state.waivedTroubles.joinToString("|") { enc(it) })

    line("reveal.clues"''',
'''    line("bonus.waivedTroubles", state.waivedTroubles.joinToString("|") { enc(it) })
    line("bonus.contactItem", state.contactItemBonusBank)

    line("reveal.clues"''','snapshot encode contact')
t=replace_once(t,
'''      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec).toSet() else emptySet(),
      lastError = null''',
'''      waivedTroubles = if (version >= 12) map["bonus.waivedTroubles"].orEmpty().split('|').filter{it.isNotBlank()}.map(::dec).toSet() else emptySet(),
      contactItemBonusBank = if (version >= 13) int("bonus.contactItem").coerceIn(0,3) else 0,
      lastError = null''','snapshot decode contact')
write(rel,t)

# ViewModel exposes deterministic actions to the existing UI.
rel='app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt'
t=read(rel)
t=replace_once(t,
'''  fun usePride() = dispatch(GameEvent.UsePride)
  fun applyInformationBonus() = dispatch(GameEvent.ApplyInformationBonus)
  fun purchaseBonusEffect(effectId:String,target:String?) = dispatch(GameEvent.PurchaseBonusEffect(effectId,target))''',
'''  fun usePride() = dispatch(GameEvent.UsePride)
  fun applyInformationBonus() = dispatch(GameEvent.ApplyInformationBonus)
  fun applyContactItemBonus() = dispatch(GameEvent.ApplyContactItemBonus)
  fun applyLeadershipComfort(condition:Condition) = dispatch(GameEvent.ApplyLeadershipComfort(condition))
  fun purchaseBonusEffect(effectId:String,target:String?) = dispatch(GameEvent.PurchaseBonusEffect(effectId,target))''','viewmodel methods')
write(rel,t)

# Trouble UI: use banked contact bonus, apply comfort base effect, and choose real bonus targets.
rel='app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt'
t=read(rel)
t=replace_once(t,
'''import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination''',
'''import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.TestPurpose
import com.braseiro.talesvtt.model.AppDestination''','Trouble TestPurpose import')
t=replace_once(t,
'''  onPride: () -> Unit,
  onInformationBonus: () -> Unit,
  onPurchaseBonusEffect: (String,String?) -> Unit,''',
'''  onPride: () -> Unit,
  onInformationBonus: () -> Unit,
  onContactItemBonus: () -> Unit,
  onLeadershipComfort: (Condition) -> Unit,
  onPurchaseBonusEffect: (String,String?) -> Unit,''','Trouble callbacks')
t=replace_once(t,
'''        if (gameState.informationBonusBank > 0 && test.informationBonus < 3) {
          SecondaryButton(text="USAR +1 DADO DE INFORMAÇÃO • ${gameState.informationBonusBank} guardado(s)",onClick=onInformationBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        PrimaryButton(''',
'''        if (gameState.informationBonusBank > 0 && test.informationBonus < 3) {
          SecondaryButton(text="USAR +1 DADO DE INFORMAÇÃO • ${gameState.informationBonusBank} guardado(s)",onClick=onInformationBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        if (gameState.contactItemBonusBank > 0 && test.approvedItemBonus == 0) {
          SecondaryButton(text="USAR CONTATO COMO ITEM • +${gameState.contactItemBonusBank} DADO(S)",onClick=onContactItemBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        PrimaryButton(''','contact UI before roll')
t=replace_once(t,
'''        RollResultPanel(gameState, onLuck, onPush, onPride)
        CanonicalBonusEffects(gameState,onPurchaseBonusEffect)
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryButton(
          text = if (result.reportedToGm) "RESULTADO ENVIADO AO MESTRE" else "ENVIAR RESULTADO AO MESTRE",
          onClick = onSubmitResult,
          enabled = !result.reportedToGm,''',
'''        RollResultPanel(gameState, onLuck, onPush, onPride)
        LeadershipComfortEffect(gameState,onLeadershipComfort)
        CanonicalBonusEffects(gameState,onPurchaseBonusEffect)
        Spacer(modifier = Modifier.height(10.dp))
        val pendingComfort = test.purpose == TestPurpose.LEADERSHIP_COMFORT && result.successes >= test.requiredSuccesses && !result.baseEffectApplied
        PrimaryButton(
          text = when { result.reportedToGm -> "RESULTADO ENVIADO AO MESTRE"; pendingComfort -> "ESCOLHA A CONDIÇÃO CURADA"; else -> "ENVIAR RESULTADO AO MESTRE" },
          onClick = onSubmitResult,
          enabled = !result.reportedToGm && !pendingComfort,''','comfort UI/result gate')

old_bonus='''@Composable
private fun CanonicalBonusEffects(gameState:GameState,onPurchase:(String,String?)->Unit) {
  val test=gameState.activeTest ?: return
  val result=gameState.testResolution ?: return
  val skill=test.skillName?.let{com.braseiro.talesvtt.game.TalesSkillCatalog.canonicalSkill(it)} ?: return
  val credits=(result.successes-test.requiredSuccesses-result.bonusEffects.size).coerceAtLeast(0)
  if(credits<=0) return
  val rules=CanonicalTalesRules.effectsForSkill(skill)
  if(rules.isEmpty()) return
  Spacer(modifier=Modifier.height(10.dp))
  Text("SUCESSOS EXTRAS • $credits PARA GASTAR",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentPurple))
  rules.forEach { rule ->
    val target=when(rule.kind) {
      BonusEffectKind.GIVE_SUCCESS -> gameState.group.members.firstOrNull{!it.name.equals(gameState.kid.name,true)}?.name
      BonusEffectKind.HEAL_SELF_CONDITION -> gameState.kid.conditions.firstOrNull()?.name
      else -> null
    }
    val needsTarget=rule.kind in setOf(BonusEffectKind.GIVE_SUCCESS,BonusEffectKind.HEAL_SELF_CONDITION)
    SecondaryButton(text=rule.label.uppercase(),onClick={onPurchase(rule.id,target)},enabled=!needsTarget||target!=null,modifier=Modifier.fillMaxWidth())
  }
}
'''
new_bonus='''@Composable
private fun LeadershipComfortEffect(gameState:GameState,onApply:(Condition)->Unit) {
  val test=gameState.activeTest ?: return
  val result=gameState.testResolution ?: return
  if(test.purpose!=TestPurpose.LEADERSHIP_COMFORT || result.successes<test.requiredSuccesses || result.baseEffectApplied) return
  val target=test.targetKid.orEmpty()
  val member=gameState.group.members.firstOrNull{it.name.equals(target,true)||it.name.substringBefore(' ').equals(target,true)} ?: return
  Spacer(modifier=Modifier.height(10.dp))
  Text("CONFORTO • ESCOLHA 1 CONDIÇÃO DE ${member.name.substringBefore(' ').uppercase()}",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentOlive))
  member.conditions.forEach { condition ->
    SecondaryButton(text="CURAR ${condition.label.uppercase()}",onClick={onApply(condition)},modifier=Modifier.fillMaxWidth())
  }
}

@Composable
private fun CanonicalBonusEffects(gameState:GameState,onPurchase:(String,String?)->Unit) {
  val test=gameState.activeTest ?: return
  val result=gameState.testResolution ?: return
  val skill=test.skillName?.let{com.braseiro.talesvtt.game.TalesSkillCatalog.canonicalSkill(it)} ?: return
  val credits=(result.successes-test.requiredSuccesses-result.bonusEffects.size).coerceAtLeast(0)
  if(credits<=0) return
  if(test.purpose==TestPurpose.LEADERSHIP_COMFORT && result.successes>=test.requiredSuccesses && !result.baseEffectApplied) return
  val rules=CanonicalTalesRules.effectsForSkill(skill)
  if(rules.isEmpty()) return
  Spacer(modifier=Modifier.height(10.dp))
  Text("SUCESSOS EXTRAS • $credits PARA GASTAR",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentPurple))
  rules.forEach { rule ->
    when(rule.kind) {
      BonusEffectKind.GIVE_SUCCESS -> gameState.group.members.filterNot{it.name.equals(gameState.kid.name,true)}.forEach { member ->
        SecondaryButton(text="${rule.label.uppercase()} • ${member.name.substringBefore(' ').uppercase()}",onClick={onPurchase(rule.id,member.name)},modifier=Modifier.fillMaxWidth())
      }
      BonusEffectKind.HEAL_SELF_CONDITION -> gameState.kid.conditions.forEach { condition ->
        SecondaryButton(text="${rule.label.uppercase()} • ${condition.label.uppercase()}",onClick={onPurchase(rule.id,condition.name)},enabled=!gameState.kid.broken,modifier=Modifier.fillMaxWidth())
      }
      BonusEffectKind.HEAL_OTHER_CONDITION -> {
        val target=test.targetKid.orEmpty()
        val member=gameState.group.members.firstOrNull{it.name.equals(target,true)||it.name.substringBefore(' ').equals(target,true)}
        member?.conditions?.forEach { condition ->
          SecondaryButton(text="${rule.label.uppercase()} • ${condition.label.uppercase()}",onClick={onPurchase(rule.id,condition.name)},enabled=!member.broken,modifier=Modifier.fillMaxWidth())
        }
      }
      BonusEffectKind.CONTACT_ITEM -> SecondaryButton(text="${rule.label.uppercase()} • ATUAL +${gameState.contactItemBonusBank}",onClick={onPurchase(rule.id,null)},enabled=gameState.contactItemBonusBank<3,modifier=Modifier.fillMaxWidth())
      else -> SecondaryButton(text=rule.label.uppercase(),onClick={onPurchase(rule.id,null)},modifier=Modifier.fillMaxWidth())
    }
  }
}
'''
t=replace_once(t,old_bonus,new_bonus,'bonus effects chooser')
write(rel,t)

# Wire callbacks through navigation.
rel='app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt'
t=read(rel)
t=replace_once(t,
'''        onPride = gameViewModel::usePride,
        onInformationBonus = gameViewModel::applyInformationBonus,
        onPurchaseBonusEffect = gameViewModel::purchaseBonusEffect,''',
'''        onPride = gameViewModel::usePride,
        onInformationBonus = gameViewModel::applyInformationBonus,
        onContactItemBonus = gameViewModel::applyContactItemBonus,
        onLeadershipComfort = gameViewModel::applyLeadershipComfort,
        onPurchaseBonusEffect = gameViewModel::purchaseBonusEffect,''','navigation callbacks')
write(rel,t)

# Extend 1.3.15 tests with deterministic Leadership/Contacts coverage.
test='''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class LeadershipContactsV1315Test {
  private fun groupWithTarget(vararg targetConditions:Condition, broken:Boolean=false):GroupState = GroupState(members=listOf(
    GroupMemberState("Lasse Svensson","Informático","kid_portrait_01"),
    GroupMemberState("Anita Holmberg","Criança Popular","kid_portrait_02",targetConditions.toSet(),broken)
  ))

  private fun comfortState(successes:Int=3, targetConditions:Set<Condition> = setOf(Condition.SCARED,Condition.UPSET)):GameState {
    val kid=KidState(name="Lasse Svensson",conditions=setOf(Condition.EXHAUSTED))
    val test=kid.testForSkill("Confortar Anita","Liderança",1)!!.copy(purpose=TestPurpose.LEADERSHIP_COMFORT,targetKid="Anita Holmberg")
    return GameState(kid=kid,group=groupWithTarget(*targetConditions.toTypedArray()),activeTest=test,testResolution=TestResolution(List(successes){6},successes))
  }

  @Test fun leadershipComfortHealsChosenTargetCondition() {
    val state=comfortState()
    val next=TalesReducer.reduce(state,GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    assertNull(next.lastError)
    assertTrue(next.testResolution!!.baseEffectApplied)
    val anita=next.group.members.first{it.name.startsWith("Anita")}
    assertFalse(Condition.SCARED in anita.conditions)
    assertTrue(Condition.UPSET in anita.conditions)
  }

  @Test fun leadershipExtraCanHealAnotherTargetCondition() {
    val base=TalesReducer.reduce(comfortState(),GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    val next=TalesReducer.reduce(base,GameEvent.PurchaseBonusEffect("lead_more",Condition.UPSET.name))
    assertNull(next.lastError)
    val anita=next.group.members.first{it.name.startsWith("Anita")}
    assertTrue(anita.conditions.isEmpty())
  }

  @Test fun leadershipExtraCanHealLeader() {
    val base=TalesReducer.reduce(comfortState(),GameEvent.ApplyLeadershipComfort(Condition.SCARED))
    val next=TalesReducer.reduce(base,GameEvent.PurchaseBonusEffect("lead_self",Condition.EXHAUSTED.name))
    assertNull(next.lastError)
    assertFalse(Condition.EXHAUSTED in next.kid.conditions)
  }

  @Test fun comfortNeverHealsBrokenTarget() {
    val kid=KidState(name="Lasse Svensson")
    val request=kid.testForSkill("Confortar Anita","Liderança",1)!!.copy(purpose=TestPurpose.LEADERSHIP_COMFORT,targetKid="Anita Holmberg")
    val state=GameState(kid=kid,group=groupWithTarget(Condition.SCARED,broken=true))
    val next=TalesReducer.reduce(state,GameEvent.GmRequestTest(request))
    assertNotNull(next.lastError)
  }

  @Test fun contactBuildsOneUseItemUpToThree() {
    val kid=KidState(conditions=emptySet())
    val contactTest=kid.testForSkill("Encontrar técnico","Contatos",1)!!
    var state=GameState(kid=kid,activeTest=contactTest,testResolution=TestResolution(listOf(6,6,6,6),4))
    repeat(3) { state=TalesReducer.reduce(state,GameEvent.PurchaseBonusEffect("contact_item",null)); assertNull(state.lastError) }
    assertEquals(3,state.contactItemBonusBank)
    state=TalesReducer.reduce(state,GameEvent.ClearTest)
    val nextTest=kid.testForSkill("Abrir painel","Mecânica",1)!!
    state=TalesReducer.reduce(state,GameEvent.GmRequestTest(nextTest))
    state=TalesReducer.reduce(state,GameEvent.ApplyContactItemBonus)
    assertNull(state.lastError)
    assertEquals(3,state.activeTest!!.approvedItemBonus)
    assertEquals(0,state.contactItemBonusBank)
  }

  @Test fun contactCannotStackWithAnotherItem() {
    val kid=KidState(conditions=emptySet())
    val test=kid.testForSkill("Abrir painel","Mecânica",1)!!.copy(approvedItemBonus=2)
    val state=GameState(kid=kid,activeTest=test,contactItemBonusBank=2)
    val next=TalesReducer.reduce(state,GameEvent.ApplyContactItemBonus)
    assertNotNull(next.lastError)
    assertEquals(2,next.contactItemBonusBank)
  }

  @Test fun leadershipAssignmentAddsDiceThenIsConsumed() {
    val kid=KidState(name="Anita Holmberg",conditions=emptySet())
    val test=kid.testForSkill("Convencer adulto","Charme",1)!!
    var state=GameState(kid=kid,activeTest=test,leadershipAssignments=mapOf("Anita Holmberg" to 2))
    val expected=test.basePool()+2
    assertEquals(expected,state.finalPool())
    state=TalesReducer.reduce(state,GameEvent.RollResult(List(expected){1}))
    assertNull(state.lastError)
    assertFalse(state.leadershipAssignments.keys.any{it.equals("Anita Holmberg",true)})
  }

  @Test fun snapshotPersistsContactAndLeadershipResources() {
    val state=GameState(contactItemBonusBank=2,leadershipPoolDice=4,leadershipAssignments=mapOf("Anita Holmberg" to 2))
    val decoded=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals(2,decoded.contactItemBonusBank)
    assertEquals(4,decoded.leadershipPoolDice)
    assertEquals(2,decoded.leadershipAssignments["Anita Holmberg"])
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/LeadershipContactsV1315Test.kt',test)

print('TALES 1.3.15i Leadership/Contacts closure: OK')
