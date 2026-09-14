from pathlib import Path
import re, sys

root = Path(sys.argv[1])
repo = Path(__file__).resolve().parent

# Preserve all proven work through 1.3.13 first.
ns = {"__name__": "__main__", "__file__": str(repo / "tales-1.3.13-run.py")}
code = (repo / "tales-1.3.13-run.py").read_text(encoding="utf-8")
old_argv = sys.argv[:]
sys.argv = [ns["__file__"], str(root)]
try:
    exec(compile(code, ns["__file__"], "exec"), ns)
finally:
    sys.argv = old_argv


def read(rel): return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def rep(text, old, new, label):
    if old not in text: raise SystemExit(f"anchor not found: {label}")
    return text.replace(old,new,1)

# Version.
rel="app/build.gradle.kts"; t=read(rel)
t=re.sub(r'versionCode\s*=\s*\d+','versionCode = 18',t,count=1)
t=re.sub(r'versionName\s*=\s*"[^"]+"','versionName = "1.3.14"',t,count=1)
write(rel,t)

# Core: source-accurate recovery primitives + complete Tales Change fields.
rel="app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt"; t=read(rel)
t=rep(t,
'''data class ChangeState(\n  val birthdayReviewed: Boolean = false,\n  val birthdayPassed: Boolean = false,\n  val birthdayApplied: Boolean = false,\n  val improvedAttribute: String? = null,\n  val newKidRequired: Boolean = false\n)''',
'''data class ChangeState(\n  val birthdayReviewed: Boolean = false,\n  val birthdayPassed: Boolean = false,\n  val birthdayApplied: Boolean = false,\n  val improvedAttribute: String? = null,\n  val newKidRequired: Boolean = false,\n  val characterReviewConfirmed: Boolean = false\n)''',"ChangeState review")

t=rep(t,
'''  data class ApplyBirthdayAttribute(val attribute: String) : GameEvent\n  data object StartNextSession : GameEvent''',
'''  data class ApplyBirthdayAttribute(val attribute: String) : GameEvent\n  data class ApplyBetweenMysteryChanges(\n    val problem: String? = null,\n    val drive: String? = null,\n    val prideText: String? = null,\n    val iconicItemName: String? = null,\n    val relationships: List<KidRelationship>? = null\n  ) : GameEvent\n  data object ConfirmBetweenMysteryReview : GameEvent\n  data object StartNextSession : GameEvent''',"Change events")

# Insert change reducer before StartNextSession.
anchor='''      GameEvent.StartNextSession -> {\n        when {'''
insert='''      is GameEvent.ApplyBetweenMysteryChanges -> {\n        when {\n          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("Mudanças de personagem só podem ser feitas entre Mistérios.")\n          state.change.characterReviewConfirmed -> fail("A revisão de Mudança já foi confirmada.")\n          event.problem != null && event.problem.trim().isBlank() -> fail("Problema não pode ficar vazio.")\n          event.drive != null && event.drive.trim().isBlank() -> fail("Ímpeto não pode ficar vazio.")\n          event.prideText != null && event.prideText.trim().isBlank() -> fail("Orgulho não pode ficar vazio.")\n          event.iconicItemName != null && event.iconicItemName.trim().isBlank() -> fail("Item Icônico não pode ficar vazio.")\n          event.relationships != null && event.relationships.any { it.name.isBlank() || it.description.isBlank() } -> fail("Relacionamento precisa de pessoa e descrição.")\n          else -> ok(state.copy(kid = state.kid.copy(\n            problem = event.problem?.trim()?.take(1000) ?: state.kid.problem,\n            drive = event.drive?.trim()?.take(1000) ?: state.kid.drive,\n            prideText = event.prideText?.trim()?.take(1000) ?: state.kid.prideText,\n            iconicItemName = event.iconicItemName?.trim()?.take(180) ?: state.kid.iconicItemName,\n            relationships = event.relationships?.take(20) ?: state.kid.relationships\n          )))\n        }\n      }\n\n      GameEvent.ConfirmBetweenMysteryReview -> {\n        when {\n          state.endingMode != EndingMode.MYSTERY || !state.sessionXpAwarded -> fail("A revisão de Mudança só é confirmada entre Mistérios.")\n          state.change.characterReviewConfirmed -> ok(state)\n          else -> ok(state.copy(change = state.change.copy(characterReviewConfirmed = true)))\n        }\n      }\n\n      GameEvent.StartNextSession -> {\n        when {'''
t=rep(t,anchor,insert,"change reducer")
t=t.replace('''          state.endingMode == EndingMode.MYSTERY && !state.change.birthdayReviewed -> fail("Confirme se houve aniversário antes do próximo Mistério.")''',
'''          state.endingMode == EndingMode.MYSTERY && !state.change.characterReviewConfirmed -> fail("Confirme a revisão de Mudança antes do próximo Mistério.")\n          state.endingMode == EndingMode.MYSTERY && !state.change.birthdayReviewed -> fail("Confirme se houve aniversário antes do próximo Mistério.")''',1)
write(rel,t)

# Snapshot v11 stores Change confirmation; v10 inventory from 1.3.13 remains intact.
rel="app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt"; t=read(rel)
t=t.replace('private const val VERSION = 10','private const val VERSION = 11',1)
t=rep(t,
'''    line("change.newKidRequired", state.change.newKidRequired)\n    line("xp.answers",''',
'''    line("change.newKidRequired", state.change.newKidRequired)\n    line("change.characterReviewConfirmed", state.change.characterReviewConfirmed)\n    line("xp.answers",''',"snapshot change encode")
# ChangeState decode is near returned GameState; direct safe insertion into constructor args.
t=t.replace('''      newKidRequired = bool("change.newKidRequired")\n    )''',
'''      newKidRequired = bool("change.newKidRequired"),\n      characterReviewConfirmed = if (version >= 11) bool("change.characterReviewConfirmed") else false\n    )''',1)
write(rel,t)

# Recovery firewall: no generic heal-all command. Source must be explicit.
rel="app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt"; t=read(rel)
t=rep(t,
'''enum class MotorRouteHint { NONE, SCENE, TROUBLE, CLIMAX, ENDING }''',
'''enum class MotorRouteHint { NONE, SCENE, TROUBLE, CLIMAX, ENDING }\nenum class RecoveryKind { ANCHOR, HIDEOUT, COMFORT }''',"RecoveryKind")
t=rep(t,
'''  data class Recovery(val approved: Boolean) : MotorAction''',
'''  data class Recovery(\n    val approved: Boolean,\n    val kind: RecoveryKind? = null,\n    val condition: Condition? = null\n  ) : MotorAction''',"Recovery action")
old='''        is MotorAction.Recovery -> {\n          if (!action.approved) {\n            accepted++ // explicit denial is a valid no-op decision from the GM\n          } else reduce(GameEvent.HealAllConditions, "recovery")\n        }'''
new='''        is MotorAction.Recovery -> {\n          if (!action.approved) {\n            accepted++ // explicit denial is a valid no-op decision from the GM\n          } else when (action.kind) {\n            RecoveryKind.ANCHOR -> {\n              if (reduce(GameEvent.HealAllConditions, "recovery_anchor")) raiseRoute(MotorRouteHint.SCENE)\n            }\n            RecoveryKind.HIDEOUT -> {\n              if (state.group.members.size < 2) rejected += "ação[$index] recovery_hideout: exige ao menos 2 Kids na cena qualificada."\n              else if (reduce(GameEvent.HealAllConditions, "recovery_hideout")) raiseRoute(MotorRouteHint.SCENE)\n            }\n            RecoveryKind.COMFORT -> {\n              when {\n                state.kid.broken -> rejected += "ação[$index] recovery_comfort: conforto por Perícia não cura Kid Quebrado."\n                action.condition == null -> rejected += "ação[$index] recovery_comfort: informe exatamente uma Condição leve."\n                else -> if (reduce(GameEvent.HealCondition(action.condition), "recovery_comfort")) raiseRoute(MotorRouteHint.SCENE)\n              }\n            }\n            null -> rejected += "ação[$index] recovery: fonte de recuperação ausente; não existe cura genérica."\n          }\n        }'''
t=rep(t,old,new,"Recovery firewall")
write(rel,t)

# Wire schema for explicit Recovery.
rel="app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt"; t=read(rel)
old='''    "recovery" -> MotorAction.Recovery(a.optBoolean("approved", false))'''
new='''    "recovery" -> {\n      val kind = when (a.optString("source", a.optString("kind")).lowercase()) {\n        "anchor", "ancora", "âncora" -> RecoveryKind.ANCHOR\n        "hideout", "esconderijo" -> RecoveryKind.HIDEOUT\n        "comfort", "conforto", "skill" -> RecoveryKind.COMFORT\n        else -> null\n      }\n      val condition = when (a.optString("condition", a.optString("key")).lowercase()) {\n        "upset", "chateado" -> Condition.UPSET\n        "scared", "assustado" -> Condition.SCARED\n        "exhausted", "exausto" -> Condition.EXHAUSTED\n        "injured", "machucado", "ferido" -> Condition.INJURED\n        else -> null\n      }\n      MotorAction.Recovery(a.optBoolean("approved", false), kind, condition)\n    }'''
t=rep(t,old,new,"Recovery parser")
write(rel,t)

# GM instructions make recovery explicit and independent from reference/RAG context.
rel="app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"; t=read(rel)
t=t.replace('- recovery {approved}\n','- recovery {approved,source: anchor|hideout|comfort,condition?}\n',1)
t=t.replace('REGRAS INEGOCIÁVEIS:\n','REGRAS INEGOCIÁVEIS:\n- Recuperação nunca é genérica: Anchor/Âncora qualificada cura todas as Condições; Hideout/Esconderijo qualificado com 2+ Kids cura todas; comfort/conforto por Perícia cura exatamente uma Condição leve e nunca cura Quebrado. Se a cena não satisfizer os requisitos ficcionais, recovery deve vir approved=false.\n',1)
write(rel,t)

# ViewModel exposes deterministic Change edits and explicit confirmation.
rel="app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt"; t=read(rel)
anchor='''  fun applyBirthdayAttribute(attribute: String) = dispatch(GameEvent.ApplyBirthdayAttribute(attribute))\n  fun startNextSession() = dispatch(GameEvent.StartNextSession)'''
new='''  fun applyBirthdayAttribute(attribute: String) = dispatch(GameEvent.ApplyBirthdayAttribute(attribute))\n\n  fun saveBetweenMysteryChanges(problem: String, drive: String, pride: String, iconic: String, relationshipsText: String) {\n    val relationships = relationshipsText.lineSequence()\n      .map { it.trim() }\n      .filter { it.isNotBlank() }\n      .mapNotNull { line ->\n        val separator = when {\n          " — " in line -> " — "\n          " - " in line -> " - "\n          else -> null\n        }\n        if (separator == null) null else {\n          val name = line.substringBefore(separator).trim()\n          val description = line.substringAfter(separator).trim()\n          if (name.isBlank() || description.isBlank()) null else KidRelationship(name, description)\n        }\n      }.toList()\n    if (relationships.isEmpty()) {\n      _dataStatus.value = "Relacionamentos: use uma linha por pessoa no formato Nome — descrição."\n      return\n    }\n    dispatch(GameEvent.ApplyBetweenMysteryChanges(problem, drive, pride, iconic, relationships))\n  }\n\n  fun confirmBetweenMysteryReview() = dispatch(GameEvent.ConfirmBetweenMysteryReview)\n  fun startNextSession() = dispatch(GameEvent.StartNextSession)'''
t=rep(t,anchor,new,"ViewModel Change")
write(rel,t)

# Navigation callbacks.
rel="app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt"; t=read(rel)
t=rep(t,
'''        onApplyBirthdayAttribute = gameViewModel::applyBirthdayAttribute,\n        onStartNextSession = {''',
'''        onApplyBirthdayAttribute = gameViewModel::applyBirthdayAttribute,\n        onSaveCharacterChanges = gameViewModel::saveBetweenMysteryChanges,\n        onConfirmCharacterChanges = gameViewModel::confirmBetweenMysteryReview,\n        onStartNextSession = {''',"Ending callbacks")
write(rel,t)

# Ending screen: small functional Change editor, no redesign.
rel="app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt"; t=read(rel)
t=t.replace('import androidx.compose.material3.Text\n','import androidx.compose.material3.Text\nimport androidx.compose.material3.OutlinedTextField\n',1)
t=t.replace('import androidx.compose.runtime.Composable\n','import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.setValue\nimport androidx.compose.runtime.saveable.rememberSaveable\n',1)
t=rep(t,
'''  onApplyBirthdayAttribute: (String) -> Unit,\n  onStartNextSession: () -> Unit,''',
'''  onApplyBirthdayAttribute: (String) -> Unit,\n  onSaveCharacterChanges: (String, String, String, String, String) -> Unit,\n  onConfirmCharacterChanges: () -> Unit,\n  onStartNextSession: () -> Unit,''',"Ending signature")
# Insert local editable state after contextItems.
anchor='''  val contextItems = listOf(\n    ContextItemData("end_myst", if (gameState.endingMode == EndingMode.SESSION) "Sessão" else "Mistério", "Concluído", ContextIconType.MYSTERY, isHighlighted = true),\n    ContextItemData("end_kid", "Kid", gameState.kid.name.substringBefore(' '), ContextIconType.CHARACTERS),\n    ContextItemData("end_xp", "XP Atual", gameState.kid.xp.toString(), ContextIconType.MYSTERY)\n  )'''
new=anchor+'''\n  var changeProblem by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.problem) }\n  var changeDrive by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.drive) }\n  var changePride by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.prideText) }\n  var changeIconic by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.iconicItemName) }\n  var changeRelationships by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.relationships.joinToString("\\n") { "${it.name} — ${it.description}" }) }'''
t=rep(t,anchor,new,"Ending local state")
# Replace simple Change paragraph panel with functional editor.
old='''      AppPanel {\n        Text(\n          text = "Entre Mistérios, todas as Condições são curadas. Problema, Orgulho, Item Icônico e Relacionamentos podem ser revisados quando a ficção justificar.",\n          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)\n        )\n      }'''
new='''      AppPanel {\n        Text(\n          text = "Entre Mistérios, todas as Condições são curadas. Problema, Ímpeto, Orgulho, Item Icônico e Relacionamentos podem mudar quando fizer sentido na história.",\n          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)\n        )\n        if (gameState.sessionXpAwarded && !gameState.change.characterReviewConfirmed) {\n          Spacer(modifier = Modifier.height(8.dp))\n          OutlinedTextField(changeProblem, { changeProblem = it }, label = { Text("PROBLEMA") }, modifier = Modifier.fillMaxWidth())\n          OutlinedTextField(changeDrive, { changeDrive = it }, label = { Text("ÍMPETO") }, modifier = Modifier.fillMaxWidth())\n          OutlinedTextField(changePride, { changePride = it }, label = { Text("ORGULHO") }, modifier = Modifier.fillMaxWidth())\n          OutlinedTextField(changeIconic, { changeIconic = it }, label = { Text("ITEM ICÔNICO") }, modifier = Modifier.fillMaxWidth())\n          OutlinedTextField(changeRelationships, { changeRelationships = it }, label = { Text("RELACIONAMENTOS • Nome — descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2)\n          Spacer(modifier = Modifier.height(8.dp))\n          SecondaryButton(\n            text = "SALVAR REVISÃO",\n            onClick = { onSaveCharacterChanges(changeProblem, changeDrive, changePride, changeIconic, changeRelationships) },\n            modifier = Modifier.fillMaxWidth()\n          )\n          Spacer(modifier = Modifier.height(6.dp))\n          PrimaryButton(\n            text = "CONFIRMAR MUDANÇA",\n            onClick = onConfirmCharacterChanges,\n            modifier = Modifier.fillMaxWidth()\n          )\n        } else if (gameState.change.characterReviewConfirmed) {\n          Text("Revisão de Mudança confirmada.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))\n        }\n      }'''
t=rep(t,old,new,"Ending Change editor")
# Require both review and birthday before next Mystery.
t=t.replace('''      val changeReady = gameState.endingMode != EndingMode.MYSTERY ||\n        (gameState.change.birthdayReviewed && (!gameState.change.birthdayPassed || gameState.change.birthdayApplied))''',
'''      val changeReady = gameState.endingMode != EndingMode.MYSTERY ||\n        (gameState.change.characterReviewConfirmed && gameState.change.birthdayReviewed && (!gameState.change.birthdayPassed || gameState.change.birthdayApplied))''',1)
write(rel,t)

# Tests for the new source-grounded contracts.
test_dir=root/"app/src/test/java/com/braseiro/talesvtt/game"; test_dir.mkdir(parents=True,exist_ok=True)
(test_dir/"RecoveryAndChangeV1314Test.kt").write_text(r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class RecoveryAndChangeV1314Test {
  @Test fun comfortHealsExactlyOneMildConditionAndNeverBroken() {
    val base=GameState(kid=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED)))
    val healed=MotorBarbaraFirewall.apply(base,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.COMFORT,Condition.UPSET))))
    assertEquals(setOf(Condition.SCARED),healed.gameState.kid.conditions)
    val broken=base.copy(kid=base.kid.copy(broken=true))
    val rejected=MotorBarbaraFirewall.apply(broken,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.COMFORT,Condition.UPSET))))
    assertTrue(rejected.rejected.isNotEmpty())
  }

  @Test fun anchorAndQualifiedHideoutHealAll() {
    val sick=KidState(conditions=setOf(Condition.UPSET,Condition.INJURED),broken=true)
    val anchor=MotorBarbaraFirewall.apply(GameState(kid=sick),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.ANCHOR))))
    assertTrue(anchor.gameState.kid.conditions.isEmpty()); assertFalse(anchor.gameState.kid.broken)
    val hideout=MotorBarbaraFirewall.apply(GameState(kid=sick),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,RecoveryKind.HIDEOUT))))
    assertTrue(hideout.gameState.kid.conditions.isEmpty()); assertFalse(hideout.gameState.kid.broken)
  }

  @Test fun genericRecoveryIsRejected() {
    val state=GameState(kid=KidState(conditions=setOf(Condition.UPSET)))
    val result=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.Recovery(true,null,null))))
    assertTrue(result.rejected.isNotEmpty()); assertEquals(setOf(Condition.UPSET),result.gameState.kid.conditions)
  }

  @Test fun talesChangeAllowsOnlyBetweenMysteriesAndRequiresConfirmation() {
    val relationship=KidRelationship("Sara","minha amiga mais próxima")
    var state=GameState(endingMode=EndingMode.MYSTERY,sessionXpAwarded=true)
    state=TalesReducer.reduce(state,GameEvent.ApplyBetweenMysteryChanges("Novo problema","Novo ímpeto","Novo orgulho","Novo item",listOf(relationship)))
    assertNull(state.lastError); assertEquals("Novo problema",state.kid.problem); assertEquals("Novo ímpeto",state.kid.drive)
    state=TalesReducer.reduce(state,GameEvent.ConfirmBetweenMysteryReview)
    assertTrue(state.change.characterReviewConfirmed)
  }

  @Test fun activePlayCannotEditChangeFields() {
    val state=TalesReducer.reduce(GameState(),GameEvent.ApplyBetweenMysteryChanges(problem="hack"))
    assertNotNull(state.lastError)
  }

  @Test fun nextMysteryRequiresChangeReview() {
    var state=GameState(endingMode=EndingMode.MYSTERY,sessionXpAwarded=true,change=ChangeState(birthdayReviewed=true,birthdayApplied=true))
    val blocked=TalesReducer.reduce(state,GameEvent.StartNextSession)
    assertNotNull(blocked.lastError)
    state=TalesReducer.reduce(state,GameEvent.ConfirmBetweenMysteryReview)
    val allowed=TalesReducer.reduce(state,GameEvent.StartNextSession)
    assertNull(allowed.lastError); assertEquals(EndingMode.NONE,allowed.endingMode)
  }
}
''',encoding="utf-8")

resume=root/"TALES_RETOMADA_CURRENT.md"; r=resume.read_text(encoding="utf-8")
r+='''\n## Bloco 1.3.14 — recuperação e Mudança canônicas\n- Recuperação genérica removida: Âncora e Esconderijo qualificados curam todas as Condições; conforto por Perícia cura exatamente uma Condição leve e nunca cura Quebrado.\n- Motor precisa declarar a fonte da recuperação; sem fonte, a cura é rejeitada em vez de inventada.\n- Entre Mistérios, Problema, Ímpeto, Orgulho, Item Icônico e Relacionamentos podem ser revisados em estado próprio de Mudança.\n- Próximo Mistério só inicia depois da revisão de Mudança e da revisão de aniversário.\n- Confirmação de Mudança persiste no save; edição continua proibida durante jogo ativo.\n'''
resume.write_text(r,encoding="utf-8")

# Hard checks.
assert 'versionName = "1.3.14"' in read("app/build.gradle.kts")
assert 'enum class RecoveryKind' in read("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt")
assert 'recovery: fonte de recuperação ausente' in read("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt")
assert 'characterReviewConfirmed' in read("app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt")
assert 'CONFIRMAR MUDANÇA' in read("app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt")
print("TALES 1.3.14 transform: OK")
