from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16d1-transform.py')}
code=(repo/'tales-1.3.16d1-transform.py').read_text(encoding='utf-8')
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
# Character sheet: preserve approved layout, make state/cards profile-aware.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/KidScreen.kt'; s=read(rel)
s=one(s,'import androidx.compose.material3.Text\n','import androidx.compose.material3.Text\nimport androidx.compose.material3.OutlinedTextField\n', 'kid outlined input')
s=one(s,'import androidx.compose.runtime.Composable\n','import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n','kid runtime')
s=one(s,'import com.braseiro.talesvtt.game.GameState\n','import com.braseiro.talesvtt.game.GameState\nimport com.braseiro.talesvtt.game.RulesProfile\n','kid profile import')
s=one(s,
'''  onImportKid: (Uri) -> Unit,
  onChangePortrait: (Uri) -> Unit,
  modifier: Modifier = Modifier''',
'''  onImportKid: (Uri) -> Unit,
  onChangePortrait: (Uri) -> Unit,
  onRecordFloodScar: (String) -> Unit,
  modifier: Modifier = Modifier''','kid callback')
s=one(s,
'''  val changePortrait = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onChangePortrait) }
  val contextItems = listOf(''',
'''  val changePortrait = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onChangePortrait) }
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  var pendingScarText by remember(gameState.kid.scarPending, gameState.kid.scars.size) { mutableStateOf("") }
  val contextItems = listOf(''','kid locals')
s=s.replace('ContextItemData("kid_luck", "Sorte", "${gameState.kid.luck} / ${gameState.kid.maxLuck}", ContextIconType.MYSTERY, isHighlighted = true)',
'''if (flood) ContextItemData("kid_scars", "Cicatrizes", "${gameState.kid.scars.size} / 6", ContextIconType.MYSTERY, isHighlighted = true)
    else ContextItemData("kid_luck", "Sorte", "${gameState.kid.luck} / ${gameState.kid.maxLuck}", ContextIconType.MYSTERY, isHighlighted = true)''',1)
s=s.replace('chapterLabel = "KID",','chapterLabel = if (flood) "TEEN" else "KID",',1)
s=s.replace('sceneLabel = "Ficha do personagem",','sceneLabel = if (flood) "Ficha do Teen" else "Ficha do Kid",',1)
s=s.replace('SectionHeader(title = "ESTADO DO KID", color = AppTheme.colors.accentCyan)','SectionHeader(title = if(flood) "ESTADO DO TEEN" else "ESTADO DO KID", color = AppTheme.colors.accentCyan)',1)

anchor='''    Spacer(modifier = Modifier.height(Dimens.spacing12))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
    ) {
      StoryCard(
        modifier = Modifier.weight(1f),
        title = "PROBLEMA",'''
insert='''    if (flood) {
      Spacer(modifier = Modifier.height(Dimens.spacing12))
      AppPanel(borderColor = AppTheme.colors.accentPurple.copy(alpha = 0.45f)) {
        Text("THINGS FROM THE FLOOD", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentPurple))
        KidFact("ANIVERSÁRIO", gameState.kid.birthday.ifBlank { "Não informado" })
        KidFact("VERGONHA", if(gameState.kid.shameAvailable) "${gameState.kid.shameText} • disponível" else "${gameState.kid.shameText} • já usada")
        KidFact("FRICTION DO GRUPO", gameState.kid.friction.ifBlank { "Ainda não definida" })
        if(gameState.kid.hooks.isNotEmpty()) KidFact("HOOKS", gameState.kid.hooks.joinToString(" • "))
        Spacer(modifier = Modifier.height(5.dp))
        Text("CICATRIZES", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
        if(gameState.kid.scars.isEmpty()) Text("Nenhuma Cicatriz.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
        gameState.kid.scars.forEachIndexed { index, scar ->
          Text("${index + 1}. ${scar.text}${if(scar.accepted) " • aceita" else ""}${if(scar.usedForSuccess) " • sucesso já usado" else ""}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary))
        }
        if(gameState.kid.scarPending) {
          Spacer(modifier = Modifier.height(8.dp))
          Text("QUEBRADO • descreva a nova Cicatriz antes de continuar", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
          OutlinedTextField(pendingScarText, { pendingScarText = it }, label = { Text("NOVA CICATRIZ") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(6.dp))
          PrimaryButton("REGISTRAR CICATRIZ", onClick = { onRecordFloodScar(pendingScarText); pendingScarText = "" }, enabled = pendingScarText.isNotBlank(), modifier = Modifier.fillMaxWidth())
        }
        if(gameState.kid.removedFromPlay) {
          Spacer(modifier = Modifier.height(7.dp))
          Text("TEEN FORA DE JOGO • crie um novo Teen para continuar.", style = AppTheme.typography.cardTitle.copy(color = AppTheme.colors.accentAmber))
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
    ) {
      StoryCard(
        modifier = Modifier.weight(1f),
        title = "PROBLEMA",'''
s=one(s,anchor,insert,'kid Flood panel')
s=s.replace('''        title = "ORGULHO",
        text = if (gameState.kid.prideAvailable) "${gameState.kid.prideText} Disponível neste Mistério." else "${gameState.kid.prideText} Orgulho já utilizado.",''',
'''        title = if(flood) "VERGONHA" else "ORGULHO",
        text = if(flood) {
          if(gameState.kid.shameAvailable) "${gameState.kid.shameText} Disponível." else "${gameState.kid.shameText} Vergonha já utilizada."
        } else if (gameState.kid.prideAvailable) "${gameState.kid.prideText} Disponível neste Mistério." else "${gameState.kid.prideText} Orgulho já utilizado.",''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Trouble: Flood never exposes Tales Luck/Pride. Shame and unused Scars are real.
# Tales-only bonus-effect UI is hidden until Flood has its own local effect table.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt'; s=read(rel)
s=one(s,'import com.braseiro.talesvtt.game.TestPurpose\n','import com.braseiro.talesvtt.game.TestPurpose\nimport com.braseiro.talesvtt.game.RulesProfile\n','trouble profile import')
s=one(s,
'''  onPush: (Condition?) -> Unit,
  onPride: () -> Unit,
  onInformationBonus: () -> Unit,''',
'''  onPush: (Condition?) -> Unit,
  onPride: () -> Unit,
  onShame: () -> Unit,
  onScar: (Int) -> Unit,
  onInformationBonus: () -> Unit,''','trouble callbacks')
s=one(s,
'''  val result = gameState.testResolution
  val activeCondition = gameState.kid.conditions.firstOrNull()
''',
'''  val result = gameState.testResolution
  val activeCondition = gameState.kid.conditions.firstOrNull()
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
''','trouble flood local')
s=s.replace('ContextItemData("tr_kid", "Kid", gameState.kid.name.substringBefore(\' \'), ContextIconType.CHARACTERS, isHighlighted = true)',
'''ContextItemData("tr_kid", if(flood) "Teen" else "Kid", gameState.kid.name.substringBefore(' '), ContextIconType.CHARACTERS, isHighlighted = true)''',1)
s=s.replace('text = "Ajuda: ${test.helperKid} • +1 dado • compartilha a consequência em caso de falha.",','text = "Ajuda: ${test.helperKid} • +1 dado • compartilha a consequência em caso de falha.",',1)
s=s.replace('text = "PEDIR AJUDA  •  +1 dado de um Kid",','text = "PEDIR AJUDA  •  +1 dado de ${if(flood) "um Teen" else "um Kid"}",',1)
s=s.replace('''        RollResultPanel(gameState, onLuck, onPush, onPride)
        LeadershipComfortEffect(gameState,onLeadershipComfort)
        CanonicalBonusEffects(gameState,onPurchaseBonusEffect)''',
'''        RollResultPanel(gameState, onLuck, onPush, onPride, onShame, onScar)
        LeadershipComfortEffect(gameState,onLeadershipComfort)
        if(!flood) CanonicalBonusEffects(gameState,onPurchaseBonusEffect)''',1)

s=one(s,
'''  onLuck: () -> Unit,
  onPush: (Condition?) -> Unit,
  onPride: () -> Unit
) {
  val test = gameState.activeTest ?: return
  val result = gameState.testResolution ?: return
  val success = result.successes >= test.requiredSuccesses
''',
'''  onLuck: () -> Unit,
  onPush: (Condition?) -> Unit,
  onPride: () -> Unit,
  onShame: () -> Unit,
  onScar: (Int) -> Unit
) {
  val test = gameState.activeTest ?: return
  val result = gameState.testResolution ?: return
  val success = result.successes >= test.requiredSuccesses
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
''','result profile signature')
s=s.replace('if (!success && !result.luckUsed && gameState.kid.luck > 0) {','if (!flood && !success && !result.luckUsed && gameState.kid.luck > 0) {',1)
s=s.replace('if (gameState.kid.prideAvailable && !result.prideUsed) {','if (!flood && gameState.kid.prideAvailable && !result.prideUsed) {',1)
anchor='''    if (!flood && gameState.kid.prideAvailable && !result.prideUsed) {
      SecondaryButton(
        text = "USAR ORGULHO  •  +1 sucesso automático",
        onClick = onPride,
        modifier = Modifier.fillMaxWidth()
      )
    }
'''
insert=anchor+'''    if (flood && gameState.kid.shameAvailable) {
      SecondaryButton(
        text = "USAR VERGONHA  •  +1 sucesso automático",
        onClick = onShame,
        modifier = Modifier.fillMaxWidth()
      )
    }
    if (flood) {
      gameState.kid.scars.forEachIndexed { index, scar ->
        if (!scar.usedForSuccess) {
          SecondaryButton(
            text = "USAR CICATRIZ ${index + 1}  •  +1 sucesso automático",
            onClick = { onScar(index) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
'''
s=one(s,anchor,insert,'Flood shame/scar buttons')
write(rel,s)

# -----------------------------------------------------------------------------
# Ending: exact Flood XP questions, Change fields, scars, Friction and age flow.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt'; s=read(rel)
s=one(s,'import com.braseiro.talesvtt.game.KidItemKind\n','import com.braseiro.talesvtt.game.KidItemKind\nimport com.braseiro.talesvtt.game.RulesProfile\n','ending profile import')
s=one(s,
'''  onPromoteMysteryItem: (String) -> Unit,
  onStartNextSession: () -> Unit,''',
'''  onPromoteMysteryItem: (String) -> Unit,
  onHealFloodScar: (Int) -> Unit,
  onSetFloodFriction: (String) -> Unit,
  onStartNextSession: () -> Unit,''','ending callbacks')
s=one(s,
'''  val contextItems = listOf(''',
'''  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  val contextItems = listOf(''','ending flood local')
s=s.replace('ContextItemData("end_kid", "Kid", gameState.kid.name.substringBefore(\' \'), ContextIconType.CHARACTERS)',
'''ContextItemData("end_kid", if(flood) "Teen" else "Kid", gameState.kid.name.substringBefore(' '), ContextIconType.CHARACTERS)''',1)
s=s.replace('''  var changePride by rememberSaveable(gameState.endingMode) { mutableStateOf(gameState.kid.prideText) }''',
'''  var changePride by rememberSaveable(gameState.endingMode, flood) { mutableStateOf(if(flood) gameState.kid.shameText else gameState.kid.prideText) }
  var changeFriction by rememberSaveable(gameState.endingMode, flood) { mutableStateOf(gameState.kid.friction) }''',1)
s=s.replace('''text = if (gameState.endingMode == EndingMode.SESSION) "Calcule o XP desta sessão. Condições permanecem até uma recuperação válida ou o fim do Mistério." else "Os Kids voltam para casa. Agora é hora de responder XP e revisar Mudanças.",''',
'''text = if (gameState.endingMode == EndingMode.SESSION) "Calcule o XP desta sessão. Condições permanecem até uma recuperação válida ou o fim do Mistério." else if(flood) "Os Teens entram na fase de Mudança: XP, Cicatrizes, Friction e revisão do personagem." else "Os Kids voltam para casa. Agora é hora de responder XP e revisar Mudanças.",''',1)
old='''      val questions = listOf(
        "Você participou da sessão?",
        "Você esteve em apuros por causa do seu Problema ou Relacionamentos?",
        "Você usou ou lutou com seu Orgulho?",
        "Você se colocou em risco pelos outros Kids?",
        "Você aprendeu algo novo?"
      )'''
new='''      val questions = if(flood) listOf(
        "Você participou da sessão?",
        "Seu Problema ou Relacionamentos colocaram você em Trouble?",
        "Você ficou Quebrado e precisou receber uma Cicatriz?",
        "Você se colocou em perigo para resolver o Mistério?",
        "Você aprendeu algo novo?"
      ) else listOf(
        "Você participou da sessão?",
        "Você esteve em apuros por causa do seu Problema ou Relacionamentos?",
        "Você usou ou lutou com seu Orgulho?",
        "Você se colocou em risco pelos outros Kids?",
        "Você aprendeu algo novo?"
      )'''
s=one(s,old,new,'ending XP questions')
s=s.replace('''text = "Entre Mistérios, todas as Condições são curadas. Problema, Ímpeto, Orgulho, Item Icônico e Relacionamentos podem mudar quando fizer sentido na história.",''',
'''text = if(flood) "Entre Mistérios, todas as Condições são curadas. Problema, Vergonha, Item Icônico e Relacionamentos podem mudar; Friction pode ser redefinida entre sessões." else "Entre Mistérios, todas as Condições são curadas. Problema, Ímpeto, Orgulho, Item Icônico e Relacionamentos podem mudar quando fizer sentido na história.",''',1)
s=s.replace('''          OutlinedTextField(changeDrive, { changeDrive = it }, label = { Text("ÍMPETO") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(changePride, { changePride = it }, label = { Text("ORGULHO") }, modifier = Modifier.fillMaxWidth())''',
'''          if(!flood) OutlinedTextField(changeDrive, { changeDrive = it }, label = { Text("ÍMPETO") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(changePride, { changePride = it }, label = { Text(if(flood) "VERGONHA" else "ORGULHO") }, modifier = Modifier.fillMaxWidth())''',1)
# Insert Flood-specific Friction + accepted-scar controls before save review.
anchor='''          Spacer(modifier = Modifier.height(8.dp))
          SecondaryButton(
            text = "SALVAR REVISÃO",'''
insert='''          if(flood) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(changeFriction, { changeFriction = it }, label = { Text("FRICTION DO GRUPO") }, modifier = Modifier.fillMaxWidth())
            SecondaryButton(text="SALVAR FRICTION",onClick={onSetFloodFriction(changeFriction)},enabled=changeFriction.isNotBlank(),modifier=Modifier.fillMaxWidth())
            val healable=gameState.kid.scars.withIndex().filter{it.value.accepted}
            if(healable.isNotEmpty()) {
              Spacer(modifier=Modifier.height(8.dp))
              Text("CICATRIZES ACEITAS • cure apenas quando o grupo concordar que a ação/cena justificou",style=AppTheme.typography.label.copy(color=AppTheme.colors.accentAmber))
              healable.forEach { indexed ->
                SecondaryButton(text="GRUPO APROVOU • CURAR CICATRIZ ${indexed.index + 1}",onClick={onHealFloodScar(indexed.index)},modifier=Modifier.fillMaxWidth())
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          SecondaryButton(
            text = "SALVAR REVISÃO",'''
s=one(s,anchor,insert,'ending friction scars')
# Replace Birthday branch text/controls only inside passed-not-applied block.
old='''            gameState.change.birthdayPassed && !gameState.change.birthdayApplied -> {
              Text(
                text = "Escolha 1 Atributo para receber +1. Nenhum Atributo pode ultrapassar 5.",
                style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
              )
              gameState.kid.attributes.forEach { (attribute, current) ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = attribute, style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary), modifier = Modifier.weight(1f))
                  Text(text = if (current >= 5) "$current • MÁXIMO" else "$current → ${current + 1}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))
                  Spacer(modifier = Modifier.width(8.dp))
                  SecondaryButton(
                    text = if (current >= 5) "MÁXIMO" else "+1",
                    onClick = { onApplyBirthdayAttribute(attribute) },
                    enabled = current < 5,
                    modifier = Modifier.width(92.dp)
                  )
                }
              }
            }'''
new='''            gameState.change.birthdayPassed && !gameState.change.birthdayApplied -> {
              if(flood) {
                Text("Se tempo suficiente passou, o Teen fica um ano mais velho. Atributos não aumentam por aniversário em Flood.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
                Spacer(modifier=Modifier.height(8.dp))
                PrimaryButton(text="APLICAR +1 ANO • ${gameState.kid.age} → ${gameState.kid.age + 1}",onClick={onApplyBirthdayAttribute("")},modifier=Modifier.fillMaxWidth())
              } else {
                Text(
                  text = "Escolha 1 Atributo para receber +1. Nenhum Atributo pode ultrapassar 5.",
                  style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
                )
                gameState.kid.attributes.forEach { (attribute, current) ->
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = attribute, style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary), modifier = Modifier.weight(1f))
                    Text(text = if (current >= 5) "$current • MÁXIMO" else "$current → ${current + 1}", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))
                    Spacer(modifier = Modifier.width(8.dp))
                    SecondaryButton(text = if (current >= 5) "MÁXIMO" else "+1",onClick = { onApplyBirthdayAttribute(attribute) },enabled = current < 5,modifier = Modifier.width(92.dp))
                  }
                }
              }
            }'''
s=one(s,old,new,'Flood age UI')
old='''            gameState.change.birthdayPassed && gameState.change.birthdayApplied -> {
              Text(
                text = "Aniversário aplicado • ${gameState.kid.age} anos • ${gameState.change.improvedAttribute ?: "Atributo"} +1 • Sorte máxima ${gameState.kid.maxLuck}.",
                style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan)
              )
              if (gameState.change.newKidRequired) {
                Text(
                  text = "Aos 16 anos este Kid segue em frente. É necessário criar um novo Kid antes de continuar.",
                  style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber)
                )
              }
            }'''
new='''            gameState.change.birthdayPassed && gameState.change.birthdayApplied -> {
              Text(
                text = if(flood) "Mudança de idade aplicada • ${gameState.kid.age} anos." else "Aniversário aplicado • ${gameState.kid.age} anos • ${gameState.change.improvedAttribute ?: "Atributo"} +1 • Sorte máxima ${gameState.kid.maxLuck}.",
                style = AppTheme.typography.body.copy(color = AppTheme.colors.accentCyan)
              )
              if (gameState.change.newKidRequired) {
                Text(
                  text = if(flood) "Aos 20 anos o Teen deixa a adolescência. É necessário criar um novo Teen." else "Aos 16 anos este Kid segue em frente. É necessário criar um novo Kid antes de continuar.",
                  style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentAmber)
                )
              }
            }'''
s=one(s,old,new,'Flood age complete')
write(rel,s)

# -----------------------------------------------------------------------------
# Navigation wires Flood-specific controls into the existing screens.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt'; s=read(rel)
s=one(s,
'''        onImportKid = gameViewModel::importKid,
        onChangePortrait = gameViewModel::setKidPortrait
      )''',
'''        onImportKid = gameViewModel::importKid,
        onChangePortrait = gameViewModel::setKidPortrait,
        onRecordFloodScar = gameViewModel::recordFloodScar
      )''','nav kid scar')
s=one(s,
'''        onPush = gameViewModel::push,
        onPride = gameViewModel::usePride,
        onInformationBonus = gameViewModel::applyInformationBonus,''',
'''        onPush = gameViewModel::push,
        onPride = gameViewModel::usePride,
        onShame = gameViewModel::useShame,
        onScar = gameViewModel::useScar,
        onInformationBonus = gameViewModel::applyInformationBonus,''','nav trouble Flood')
s=one(s,
'''        onPromoteMysteryItem = gameViewModel::promoteMysteryItemToIconic,
        onStartNextSession = {''',
'''        onPromoteMysteryItem = gameViewModel::promoteMysteryItemToIconic,
        onHealFloodScar = gameViewModel::healFloodScar,
        onSetFloodFriction = gameViewModel::setFloodFriction,
        onStartNextSession = {''','nav ending Flood')
write(rel,s)

# Small profile-aware Backpack labels; mechanics remain validated by Motor/reducer.
rel='app/src/main/java/com/braseiro/talesvtt/screens/BackpackScreen.kt'; s=read(rel)
if 'RulesProfile' not in s:
    s=s.replace('import com.braseiro.talesvtt.game.GameState\n','import com.braseiro.talesvtt.game.GameState\nimport com.braseiro.talesvtt.game.RulesProfile\n',1)
    s=s.replace('''  val contextItems = listOf(''','''  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  val contextItems = listOf(''',1)
s=s.replace('ContextItemData("bp_kid", "Kid", gameState.kid.name, ContextIconType.CHARACTERS)','ContextItemData("bp_kid", if(flood) "Teen" else "Kid", gameState.kid.name, ContextIconType.CHARACTERS)',1)
s=s.replace('chapterLabel = "ITENS DO KID",','chapterLabel = if(flood) "ITENS DO TEEN" else "ITENS DO KID",',1)
write(rel,s)

# Regression: Flood surfaces must never expose Tales Luck/Pride as actions.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FloodProfileUiV1316Test {
  private fun source(rel:String):String {
    val a=File("src/main/java/com/braseiro/talesvtt/$rel")
    val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
    return listOf(a,b).first{it.isFile}.readText()
  }

  @Test fun troubleBranchesShameScarsAwayFromLuckPride(){
    val s=source("screens/TroubleScreen.kt")
    assertTrue(s.contains("RulesProfile.FLOOD_TEENS"))
    assertTrue(s.contains("USAR VERGONHA"))
    assertTrue(s.contains("USAR CICATRIZ"))
    assertTrue(s.contains("!flood && !success && !result.luckUsed"))
    assertTrue(s.contains("!flood && gameState.kid.prideAvailable"))
    assertTrue(s.contains("if(!flood) CanonicalBonusEffects"))
  }

  @Test fun characterSheetExposesFloodStateAndPendingScar(){
    val s=source("screens/KidScreen.kt")
    assertTrue(s.contains("FRICTION DO GRUPO"))
    assertTrue(s.contains("REGISTRAR CICATRIZ"))
    assertTrue(s.contains("gameState.kid.shameText"))
    assertTrue(s.contains("gameState.kid.hooks"))
  }

  @Test fun endingHasFloodXpChangeAndAgeBranches(){
    val s=source("screens/EndingScreen.kt")
    assertTrue(s.contains("Você ficou Quebrado e precisou receber uma Cicatriz?"))
    assertTrue(s.contains("FRICTION DO GRUPO"))
    assertTrue(s.contains("GRUPO APROVOU • CURAR CICATRIZ"))
    assertTrue(s.contains("APLICAR +1 ANO"))
    assertTrue(s.contains("Atributos não aumentam por aniversário em Flood"))
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodProfileUiV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16e-dev — UI Flood ligada ao motor\n- Ficha preserva layout aprovado e exibe Teen, aniversário, Shame, Hooks, Friction, Scars e registro de Scar pendente.\n- Trouble esconde Sorte/Orgulho no Flood e expõe Shame + Scars; efeitos Tales ficam bloqueados na UI Flood até catálogo Flood local.\n- Encerramento usa as cinco perguntas Flood, Change Flood, cura de Scars aceitas, Friction e idade até saída aos 20.\n- Mochila troca Kid/Teen sem alterar mecânicas.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'USAR VERGONHA' in read('app/src/main/java/com/braseiro/talesvtt/screens/TroubleScreen.kt')
assert 'REGISTRAR CICATRIZ' in read('app/src/main/java/com/braseiro/talesvtt/screens/KidScreen.kt')
assert 'APLICAR +1 ANO' in read('app/src/main/java/com/braseiro/talesvtt/screens/EndingScreen.kt')
print('TALES/FLOOD 1.3.16e profile-aware UI: OK')
