from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16e-transform.py')}
code=(repo/'tales-1.3.16e-transform.py').read_text(encoding='utf-8')
old_argv=sys.argv[:]
sys.argv=[ns['__file__'],str(root)]
try:
    exec(compile(code,ns['__file__'],'exec'),ns)
finally:
    sys.argv=old_argv

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
def one(text,old,new,label):
    if old not in text: raise SystemExit('missing anchor: '+label)
    return text.replace(old,new,1)

# -----------------------------------------------------------------------------
# Flood gets its own source-derived Bonus Effect catalogue. IDs are namespaced
# so a Tales effect can never be spent under Flood (or vice versa).
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/CanonicalFloodRules.kt'; s=read(rel)
anchor='''  val types=listOf(
'''
insert='''  val bonusEffects=listOf(
    BonusEffectRule("flood_sneak_give","Furtividade","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_sneak_find","Furtividade","Encontrar algo inesperado ou mais do que procurava",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_give","Força","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_force_impress","Força","Impressionar, assustar ou humilhar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_pin","Força","Prender o oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_take","Força","Tomar algo do oponente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_unconscious","Força","Deixar o oponente inconsciente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_force_repeat","Força","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_move_give","Movimentação","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_move_impress","Movimentação","Impressionar alguém",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_move_hidden","Movimentação","Permanecer sem ser detectado",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_upgrade","Mecânica","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("flood_tinker_give","Mecânica","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_tinker_repeat","Mecânica","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_tinker_fast","Mecânica","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_quiet","Mecânica","Fazer silenciosamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_tinker_show","Mecânica","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_upgrade","Programação","Aumentar em +1 o Item criado, até +3",BonusEffectKind.ITEM_BONUS_UPGRADE),
    BonusEffectRule("flood_program_give","Programação","Dar 1 sucesso a outro Teen",BonusEffectKind.GIVE_SUCCESS),
    BonusEffectRule("flood_program_repeat","Programação","Não rolar novamente para exatamente o mesmo Trouble",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_program_fast","Programação","Fazer rapidamente",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_info","Programação","Obter informação nova ou inesperada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_program_show","Programação","Exibir habilidade",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_calculate_info","Cálculo","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_contact_tools","Contatos","O contato tem as ferramentas certas",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_heal","Contatos","O contato cura uma Condição",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("flood_contact_people","Contatos","O contato traz mais pessoas para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_repeat","Contatos","Não rolar para encontrar este contato de novo neste Mistério",BonusEffectKind.SAME_TROUBLE_WAIVER,false),
    BonusEffectRule("flood_contact_knows","Contatos","O contato sabe algo importante",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_contact_item","Contatos","Usar o contato uma vez como Item +1, até +3",BonusEffectKind.CONTACT_ITEM),
    BonusEffectRule("flood_charm_believe","Charme","A pessoa continua acreditando",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_relation","Charme","Criar uma relação duradoura",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_risk","Charme","A pessoa corre riscos para ajudar",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_convince","Charme","A pessoa tenta convencer outros",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_impress","Charme","A pessoa fica assustada, confusa ou impressionada",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_charm_love","Charme","A pessoa se apaixona",BonusEffectKind.NARRATIVE),
    BonusEffectRule("flood_lead_more","Liderança","Curar mais uma Condição do Teen confortado",BonusEffectKind.HEAL_OTHER_CONDITION),
    BonusEffectRule("flood_lead_self","Liderança","Curar uma Condição própria",BonusEffectKind.HEAL_SELF_CONDITION),
    BonusEffectRule("flood_investigate_info","Investigação","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_comprehend_info","Conhecimento","Informação extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE),
    BonusEffectRule("flood_empathize_info","Empatia","Pergunta extra e +1 dado ao usar a informação",BonusEffectKind.INFORMATION_DIE)
  )

  val types=listOf(
'''
s=one(s,anchor,insert,'Flood effect catalog')
anchor='''  fun type(label:String)=types.firstOrNull{it.label.equals(label.trim(),true)}
'''
insert='''  fun effectsForSkill(skill:String)=bonusEffects.filter{it.skill.equals(skill.trim(),true)}
  fun effect(id:String)=bonusEffects.firstOrNull{it.id==id}
  fun troubleKey(reason:String)=reason.trim().lowercase().replace(Regex("\\s+")," ")
  fun leadershipPool(successes:Int)=successes.coerceIn(0,3)*2
  fun purposeAllowed(skill:String,purpose:TestPurpose)=when(purpose){
    TestPurpose.STANDARD->true
    TestPurpose.BUILD_ITEM->skill.equals("Mecânica",true)
    TestPurpose.PROGRAM_ITEM->skill.equals("Programação",true)
    TestPurpose.LEADERSHIP_PREPARE,TestPurpose.LEADERSHIP_COMFORT->skill.equals("Liderança",true)
    TestPurpose.QUESTIONING->skill in setOf("Cálculo","Investigação","Conhecimento","Empatia")
  }
  fun type(label:String)=types.firstOrNull{it.label.equals(label.trim(),true)}
'''
s=one(s,anchor,insert,'Flood effect helpers')
s=s.replace('''Iconic Item +2. Other real Items +1..+3 when applicable; ordinary props +0. Non-Iconic Items are normally lost between Mysteries unless one becomes the new Iconic Item.
Conditions:''',
'''Iconic Item +2. Other real Items +1..+3 when applicable; ordinary props +0. Non-Iconic Items are lost between Mysteries unless one becomes the new Iconic Item; replacement Iconic Item is always +2 mechanically.
Each success above the required successes may buy one effect from the Flood Skill effect catalog; the same source-listed effect may be bought repeatedly unless its fiction is inherently one-shot. No Tales-only effect ID is valid in Flood.
Information effects bank +1 die each, max +3. Lead preparation converts 1/2/3+ successes to a 2/4/6 die pool for other Teens; Lead comfort heals one Condition on success and its source-listed bonus effects may heal an additional target Condition or one of the leader's own.
Conditions:''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# One profile router is now the only place shared code asks for rules.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/RulesProfile.kt'; s=read(rel)
anchor='''  fun mechanicalContract(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.localMechanicalContract()
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.localMechanicalContract()
  }
'''
insert='''  fun mechanicalContract(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.localMechanicalContract()
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.localMechanicalContract()
  }
  fun effectsForSkill(profile:RulesProfile,skill:String):List<BonusEffectRule> = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.effectsForSkill(skill)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.effectsForSkill(skill)
  }
  fun effect(profile:RulesProfile,id:String):BonusEffectRule? = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.effect(id)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.effect(id)
  }
  fun purposeAllowed(profile:RulesProfile,skill:String,purpose:TestPurpose):Boolean = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.purposeAllowed(skill,purpose)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.purposeAllowed(skill,purpose)
  }
  fun validItem(profile:RulesProfile,item:KidItemState):Boolean = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.validItem(item)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.validItem(item)
  }
  fun troubleKey(profile:RulesProfile,reason:String):String = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.troubleKey(reason)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.troubleKey(reason)
  }
  fun leadershipPool(profile:RulesProfile,successes:Int):Int = when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.leadershipPool(successes)
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.leadershipPool(successes)
  }
'''
s=one(s,anchor,insert,'RulesProfiles routing')
write(rel,s)

# -----------------------------------------------------------------------------
# Shared reducer delegates rule questions to the selected profile.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt'; s=read(rel)
s=s.replace('''        val key = CanonicalTalesRules.troubleKey(event.request.reason)''','''        val key = RulesProfiles.troubleKey(state.rulesProfile,event.request.reason)''',1)
s=s.replace('''          !CanonicalTalesRules.purposeAllowed(skill, event.request.purpose) -> fail("Finalidade incompatível com a Perícia.")''','''          !RulesProfiles.purposeAllowed(state.rulesProfile,skill,event.request.purpose) -> fail("Finalidade incompatível com a Perícia no perfil ativo.")''',1)
s=s.replace('''        val rule=CanonicalTalesRules.effect(event.effectId) ?: return fail("Efeito bônus não canônico.")''','''        val rule=RulesProfiles.effect(state.rulesProfile,event.effectId) ?: return fail("Efeito bônus não canônico para o perfil ativo.")''',1)
s=s.replace('''              BonusEffectKind.SAME_TROUBLE_WAIVER -> next=next.copy(waivedTroubles=next.waivedTroubles+CanonicalTalesRules.troubleKey(test.reason))''','''              BonusEffectKind.SAME_TROUBLE_WAIVER -> next=next.copy(waivedTroubles=next.waivedTroubles+RulesProfiles.troubleKey(state.rulesProfile,test.reason))''',1)
s=s.replace('''          !CanonicalTalesRules.validItem(item) -> fail("Item viola o contrato Tales/Kids.")''','''          !RulesProfiles.validItem(state.rulesProfile,item) -> fail("Item viola o contrato do perfil ativo.")''',1)
s=s.replace('''            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) CanonicalTalesRules.leadershipPool(successes) else state.leadershipPoolDice''','''            val pool=if(state.activeTest?.purpose==TestPurpose.LEADERSHIP_PREPARE && successes >= (state.activeTest?.requiredSuccesses ?: 1)) RulesProfiles.leadershipPool(state.rulesProfile,successes) else state.leadershipPoolDice''',1)
# Keep profile firewall strict for bonus-effect target language and skill spend.
s=s.replace('''                if(member.name.equals(state.kid.name,true)) return fail("O Kid não pode dar sucesso a si mesmo.")''','''                if(member.name.equals(state.kid.name,true)) return fail(if(state.rulesProfile==RulesProfile.FLOOD_TEENS) "O Teen não pode dar sucesso a si mesmo." else "O Kid não pode dar sucesso a si mesmo.")''',1)
s=s.replace('''          canonical == null -> fail("Perícia inválida para Tales/Kids.")''','''          canonical == null -> fail("Perícia inválida para o perfil ativo.")''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Motor firewall also delegates item/effect validity to the active profile.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'; s=read(rel)
s=s.replace('''          else if (request == null) rejected += "ação[$index] test_request: Perícia não canônica para Tales/Kids."''','''          else if (request == null) rejected += "ação[$index] test_request: Perícia não canônica para o perfil ativo."''',1)
s=s.replace('''            item.bonus != action.bonus || !CanonicalTalesRules.validMysteryItemBonus(action.bonus) -> rejected += "ação[$index] mystery_item_approve: bônus não corresponde ao Item real."''','''            item.bonus != action.bonus || !RulesProfiles.validItem(state.rulesProfile,item) -> rejected += "ação[$index] mystery_item_approve: bônus não corresponde ao Item real do perfil ativo."''',1)
s=s.replace('''          val upgrades=result?.bonusEffects?.count{CanonicalTalesRules.effect(it.effectId)?.kind==BonusEffectKind.ITEM_BONUS_UPGRADE} ?: 0''','''          val upgrades=result?.bonusEffects?.count{RulesProfiles.effect(state.rulesProfile,it.effectId)?.kind==BonusEffectKind.ITEM_BONUS_UPGRADE} ?: 0''',1)
s=s.replace('''            !CanonicalTalesRules.validItem(action.item) -> rejected += "ação[$index] item_add: Item viola bônus/tipo canônico."''','''            !RulesProfiles.validItem(state.rulesProfile,action.item) -> rejected += "ação[$index] item_add: Item viola bônus/tipo canônico do perfil ativo."''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# ViewModel proposal validates the real owned Item through the profile router.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt'; s=read(rel)
s=s.replace('''    if (item.kind != KidItemKind.MYSTERY || !CanonicalTalesRules.validMysteryItemBonus(item.bonus)) {
      _dataStatus.value = "Este objeto não possui bônus mecânico de Item do Mistério."
      return
    }''','''    if (item.kind != KidItemKind.MYSTERY || !RulesProfiles.validItem(_state.value.rulesProfile,item)) {
      _dataStatus.value = "Este objeto não possui bônus mecânico de Item no perfil ativo."
      return
    }''',1)
s=s.replace('''    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar o Item do Mistério '${item.name}', cujo bônus registrado no estado é +${item.bonus}."))''','''    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar o Item '${item.name}', cujo bônus registrado no estado é +${item.bonus}."))''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# UI now exposes the active profile's local Bonus Effects instead of hiding Flood.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt'; s=read(rel)
if 'import com.braseiro.talesvtt.game.RulesProfiles\n' not in s:
    s=s.replace('import com.braseiro.talesvtt.game.RulesProfile\n','import com.braseiro.talesvtt.game.RulesProfile\nimport com.braseiro.talesvtt.game.RulesProfiles\n',1)
s=s.replace('''        if(!flood) CanonicalBonusEffects(gameState,onPurchaseBonusEffect)''','''        CanonicalBonusEffects(gameState,onPurchaseBonusEffect)''',1)
s=s.replace('''  val rules=CanonicalTalesRules.effectsForSkill(skill)''','''  val rules=RulesProfiles.effectsForSkill(gameState.rulesProfile,skill)''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Regression exercises authority separation, item lifecycle and Flood effects.
# -----------------------------------------------------------------------------
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class FloodEffectsAndItemsV1316Test {
  private fun teenState():GameState = GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=RandomTeenFactory.create(Random(88)))
  private fun request(state:GameState,skill:String,purpose:TestPurpose=TestPurpose.STANDARD):TestRequest =
    state.kid.testForSkill("mesmo trouble",skill,1)!!.copy(purpose=purpose)

  @Test fun floodAndTalesEffectIdsAreMutuallyExclusive(){
    assertNull(RulesProfiles.effect(RulesProfile.FLOOD_TEENS,"force_pin"))
    assertNotNull(RulesProfiles.effect(RulesProfile.FLOOD_TEENS,"flood_force_pin"))
    assertNull(RulesProfiles.effect(RulesProfile.TALES_KIDS,"flood_force_pin"))
  }

  @Test fun floodCalculateExtraSuccessBanksInformationDie(){
    var s=teenState()
    s=s.copy(activeTest=request(s,"Cálculo"),testResolution=TestResolution(listOf(6,6),2))
    s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("flood_calculate_info"))
    assertNull(s.lastError); assertEquals(1,s.informationBonusBank)
  }

  @Test fun floodForceWaiverAutoResolvesExactSameTrouble(){
    var s=teenState()
    val r=request(s,"Força")
    s=s.copy(activeTest=r,testResolution=TestResolution(listOf(6,6),2))
    s=TalesReducer.reduce(s,GameEvent.PurchaseBonusEffect("flood_force_repeat")); assertNull(s.lastError)
    s=TalesReducer.reduce(s,GameEvent.ClearTest)
    s=TalesReducer.reduce(s,GameEvent.GmRequestTest(r))
    assertNull(s.lastError); assertNotNull(s.testResolution); assertEquals(1,s.testResolution!!.successes)
  }

  @Test fun floodRealItemsRequirePlusOneToPlusThree(){
    var s=teenState()
    s=TalesReducer.reduce(s,GameEvent.AddItem(KidItemState("scanner","",1,KidItemKind.MYSTERY)))
    assertNull(s.lastError); assertEquals(1,s.kid.items.size)
    val bad=TalesReducer.reduce(s,GameEvent.AddItem(KidItemState("zero","",0,KidItemKind.MYSTERY)))
    assertNotNull(bad.lastError)
  }

  @Test fun replacingIconicThenConfirmingChangeLosesOtherFloodItems(){
    val teen=RandomTeenFactory.create(Random(89)).copy(items=listOf(
      KidItemState("scanner","",1,KidItemKind.MYSTERY),KidItemState("drone","",3,KidItemKind.MYSTERY)))
    var s=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,endingMode=EndingMode.MYSTERY,sessionXpAwarded=true)
    s=TalesReducer.reduce(s,GameEvent.PromoteMysteryItemToIconic("scanner")); assertNull(s.lastError)
    assertEquals("scanner",s.kid.iconicItemName); assertEquals(1,s.kid.items.size)
    s=TalesReducer.reduce(s,GameEvent.ConfirmBetweenMysteryReview); assertNull(s.lastError)
    assertTrue(s.kid.items.isEmpty())
    assertEquals(CanonicalFloodRules.ICONIC_ITEM_BONUS,2)
  }

  @Test fun floodLeadershipPrepareUsesTwoFourSixPool(){
    var s=teenState(); s=s.copy(activeTest=request(s,"Liderança",TestPurpose.LEADERSHIP_PREPARE))
    val dice=List(s.finalPool()){if(it<2)6 else 2}
    s=TalesReducer.reduce(s,GameEvent.RollResult(dice))
    assertNull(s.lastError); assertEquals(4,s.leadershipPoolDice)
  }

  @Test fun sourceFilesRouteEffectsAndItemsThroughRulesProfiles(){
    fun src(rel:String):String {
      val a=File("src/main/java/com/braseiro/talesvtt/$rel"); val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
      return listOf(a,b).first{it.isFile}.readText()
    }
    val core=src("game/TalesCore.kt"); val motor=src("game/MotorBarbaraContract.kt"); val ui=src("screens/TroubleScreen.kt")
    assertTrue(core.contains("RulesProfiles.effect(state.rulesProfile"))
    assertTrue(core.contains("RulesProfiles.validItem(state.rulesProfile"))
    assertTrue(motor.contains("RulesProfiles.validItem(state.rulesProfile"))
    assertTrue(motor.contains("RulesProfiles.effect(state.rulesProfile"))
    assertTrue(ui.contains("RulesProfiles.effectsForSkill(gameState.rulesProfile"))
    assertFalse(ui.contains("if(!flood) CanonicalBonusEffects"))
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodEffectsAndItemsV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16f-dev — efeitos e Itens Flood\n- Flood ganhou catálogo local próprio de Bonus Effects, derivado das 12 Perícias do manual e com IDs separados de Tales.\n- Reducer, Motor Barbara, UI de Trouble e validação de Itens consultam o perfil ativo via RulesProfiles.\n- Flood aceita Itens mecânicos +1..+3, Item Icônico +2, criação com upgrades até +3 e perda de Itens não icônicos na Mudança.\n- Efeitos de informação, waiver, Lead e cura usam o catálogo Flood; nenhum ID Tales é aceito no perfil Flood.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'flood_force_pin' in read('app/src/main/java/com/braseiro/talesvtt/game/CanonicalFloodRules.kt')
assert 'RulesProfiles.effect(state.rulesProfile' in read('app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt')
assert 'RulesProfiles.validItem(state.rulesProfile' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt')
print('TALES/FLOOD 1.3.16f effects + items authority: OK')
