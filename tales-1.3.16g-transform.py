from pathlib import Path
import sys

repo=Path(__file__).resolve().parent
root=Path(sys.argv[1])
ns={'__name__':'__main__','__file__':str(repo/'tales-1.3.16f-transform.py')}
code=(repo/'tales-1.3.16f-transform.py').read_text(encoding='utf-8')
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
# Flood recovery: Friction is a first-class, GM-approved recovery source.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt'; s=read(rel)
s=one(s,'enum class RecoveryKind { ANCHOR, HIDEOUT, COMFORT }','enum class RecoveryKind { ANCHOR, HIDEOUT, COMFORT, FRICTION }','RecoveryKind friction')
old='''            RecoveryKind.COMFORT -> {
              when {
                state.kid.broken -> rejected += "ação[$index] recovery_comfort: conforto por Perícia não cura Kid Quebrado."
                action.condition == null -> rejected += "ação[$index] recovery_comfort: informe exatamente uma Condição leve."
                else -> if (reduce(GameEvent.HealCondition(action.condition), "recovery_comfort")) raiseRoute(MotorRouteHint.SCENE)
              }
            }
            null -> rejected += "ação[$index] recovery: fonte de recuperação ausente; não existe cura genérica."'''
new='''            RecoveryKind.COMFORT -> {
              when {
                state.kid.broken -> rejected += "ação[$index] recovery_comfort: conforto por Perícia não cura personagem Quebrado."
                action.condition == null -> rejected += "ação[$index] recovery_comfort: informe exatamente uma Condição leve."
                else -> if (reduce(GameEvent.HealCondition(action.condition), "recovery_comfort")) raiseRoute(MotorRouteHint.SCENE)
              }
            }
            RecoveryKind.FRICTION -> {
              if (state.rulesProfile != RulesProfile.FLOOD_TEENS) rejected += "ação[$index] recovery_friction: Friction pertence somente a Flood/Teens."
              else if (reduce(GameEvent.ResolveFriction(troubleOccurred=false), "recovery_friction")) raiseRoute(MotorRouteHint.SCENE)
            }
            null -> rejected += "ação[$index] recovery: fonte de recuperação ausente; não existe cura genérica."'''
s=one(s,old,new,'Friction firewall')
write(rel,s)

# Wire parser accepts an explicit friction source. Approval remains authoritative.
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt'; s=read(rel)
s=one(s,
'''        "comfort", "conforto", "skill" -> RecoveryKind.COMFORT
        else -> null''',
'''        "comfort", "conforto", "skill" -> RecoveryKind.COMFORT
        "friction", "friccao", "fricção" -> RecoveryKind.FRICTION
        else -> null''','wire friction')
write(rel,s)

# -----------------------------------------------------------------------------
# Motor prompt: remove the old Tales-only system-level contradiction. The exact
# local mechanical contract delivered in each request now owns profile mechanics.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt'; s=read(rel)
s=s.replace('''Você é o Motor Barbara, Mestre autônomo do VTT BRASEIRO para Tales from the Loop no perfil Tales/Kids.''','''Você é o Motor Barbara, Mestre autônomo do VTT BRASEIRO para Tales from the Loop / Things from the Flood. Cada pedido informa active_profile e mechanical_contract. Você deve mestrar somente o perfil ativo.''',1)
s=s.replace('''- Recuperação nunca é genérica: Anchor/Âncora qualificada cura todas as Condições; Hideout/Esconderijo qualificado com 2+ Kids cura todas; comfort/conforto por Perícia cura exatamente uma Condição leve e nunca cura Quebrado. Se a cena não satisfizer os requisitos ficcionais, recovery deve vir approved=false.''','''- Recuperação nunca é genérica e sempre depende do perfil ativo: Anchor/Âncora qualificada pode curar conforme mechanical_contract; Hideout/Esconderijo existe somente quando o contrato Tales permitir; Friction existe somente quando o contrato Flood permitir; comfort/conforto por Perícia cura conforme o contrato e nunca ignora Quebrado. Se a cena não satisfizer os requisitos ficcionais, recovery deve vir approved=false.''',1)
s=s.replace('''- Sorte, Forçar e Orgulho são resolvidos pelo engine. Ajuda e Item precisam primeiro de sua validação ficcional: help_approve aplica exatamente +1 dado; iconic_item_approve aplica exatamente +2 dados ao Item Icônico; mystery_item_approve só aceita o bônus +1..+3 já armazenado no Item real. Objeto comum tem bônus 0. Você nunca inventa outros bônus nem gasta recursos do jogador.''','''- Recursos de personagem específicos do perfil (por exemplo Sorte/Orgulho ou Vergonha/Cicatrizes) são resolvidos pelo engine conforme mechanical_contract. Ajuda e Item precisam primeiro de validação ficcional: help_approve aplica exatamente +1 dado; iconic_item_approve aplica exatamente +2 dados ao Item Icônico; mystery_item_approve só aceita o bônus +1..+3 já armazenado no Item real. Objeto comum tem bônus 0. Você nunca inventa bônus nem gasta recursos do jogador.''',1)
s=s.replace('''- Problema Estendido Tales: Normal=2x Kids, Difícil=3x Kids, Quase impossível=4x Kids; uma cena por Kid.
- Não use Shame, Scars, Friction ou regras de Things from the Flood.''','''- Problema Estendido usa exclusivamente os multiplicadores, condições e consequências declarados em mechanical_contract do perfil ativo. Nunca importe os alvos do outro perfil.
- Nunca misture perfis. Se active_profile=tales_kid, não use Shame/Scars/Friction nem regras Flood. Se active_profile=flood_teen, não use Luck/Pride nem regras de idade/alvos Tales.''',1)
s=s.replace('''- recovery {approved:true|false,source: anchor|hideout|comfort,condition?}''','''- recovery {approved:true|false,source: anchor|hideout|comfort|friction,condition?}''',1)
# Make task prose profile-neutral where it materially addresses the player character.
s=s.replace('''nenhuma pressão cotidiana pode contradizer o fato de o Kid já estar na cena.''','''nenhuma pressão cotidiana pode contradizer o fato de o personagem já estar na cena.''',1)
s=s.replace('''Descreva o que o Kid consegue ver, ouvir, cheirar, sentir no corpo e alcançar fisicamente''','''Descreva o que o personagem consegue ver, ouvir, cheirar, sentir no corpo e alcançar fisicamente''',1)
s=s.replace('''Nunca altere recursos do Kid diretamente''','''Nunca altere recursos do personagem diretamente''',1)
s=s.replace('''Se o Kid já está fora de casa''','''Se o personagem já está fora de casa''',1)
write(rel,s)

# -----------------------------------------------------------------------------
# Group screen preserves the shell but exposes Flood/Teens + Friction instead
# of telling Flood players to recover via the Tales Hideout rule.
# -----------------------------------------------------------------------------
rel='app/src/main/java/com/braseiro/talesvtt/screens/GroupScreen.kt'; s=read(rel)
if 'import com.braseiro.talesvtt.components.PrimaryButton\n' not in s:
    s=s.replace('import com.braseiro.talesvtt.components.PortraitSlot\n','import com.braseiro.talesvtt.components.PortraitSlot\nimport com.braseiro.talesvtt.components.PrimaryButton\n',1)
if 'import com.braseiro.talesvtt.game.RulesProfile\n' not in s:
    s=s.replace('import com.braseiro.talesvtt.game.GameState\n','import com.braseiro.talesvtt.game.GameState\nimport com.braseiro.talesvtt.game.RulesProfile\n',1)
s=one(s,
'''  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(''',
'''  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onResolveFrictionIntent: () -> Unit,
  modifier: Modifier = Modifier
) {
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  val contextItems = listOf(''','Group signature')
s=s.replace('''    ContextItemData("grp_members", "Kids Presentes", "${gameState.group.members.size} Kids", ContextIconType.CHARACTERS, isHighlighted = true),''','''    ContextItemData("grp_members", if(flood) "Teens Presentes" else "Kids Presentes", "${gameState.group.members.size} ${if(flood) "Teens" else "Kids"}", ContextIconType.CHARACTERS, isHighlighted = true),''',1)
s=s.replace('''    ContextItemData("grp_hideout", "Esconderijo", gameState.group.hideoutName, ContextIconType.MYSTERY)''','''    if(flood) ContextItemData("grp_friction", "Friction", gameState.kid.friction.ifBlank { "Não definida" }, ContextIconType.MYSTERY)
    else ContextItemData("grp_hideout", "Esconderijo", gameState.group.hideoutName, ContextIconType.MYSTERY)''',1)
s=s.replace('''    chapterLabel = "KIDS & ESCONDERIJO",''','''    chapterLabel = if(flood) "TEENS & FRICTION" else "KIDS & ESCONDERIJO",''',1)
s=s.replace('''    SectionHeader(title = "NOSSOS KIDS", color = AppTheme.colors.accentCyan)''','''    SectionHeader(title = if(flood) "NOSSOS TEENS" else "NOSSOS KIDS", color = AppTheme.colors.accentCyan)''',1)
old='''    SectionHeader(title = "ESCONDERIJO", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.35f)) {
      Image(
        painter = painterResource(R.drawable.scene_hideout),
        contentDescription = gameState.group.hideoutName,
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = gameState.group.hideoutName, style = AppTheme.typography.cardTitle)
      Text(
        text = gameState.group.hideoutDescription,
        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Quando 2 ou mais Kids estão presentes, próximos e a cena permanece segura, a recuperação pode ocorrer conforme as regras do Esconderijo.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }'''
new='''    SectionHeader(title = if(flood) "FRICTION" else "ESCONDERIJO", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel(borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.35f)) {
      if(flood) {
        Text(text = gameState.kid.friction.ifBlank { "Friction ainda não definida." }, style = AppTheme.typography.cardTitle)
        Text(
          text = "Para resolver Friction, os Teens precisam de uma cena segura e próxima entre si. O Motor valida a cena; se surgir Trouble na própria cena, ninguém cura Condições.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
          text = "PEDIR CENA PARA RESOLVER FRICTION",
          onClick = onResolveFrictionIntent,
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Image(
          painter = painterResource(R.drawable.scene_hideout),
          contentDescription = gameState.group.hideoutName,
          modifier = Modifier.fillMaxWidth().height(190.dp),
          contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = gameState.group.hideoutName, style = AppTheme.typography.cardTitle)
        Text(text = gameState.group.hideoutDescription, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Quando 2 ou mais Kids estão presentes, próximos e a cena permanece segura, a recuperação pode ocorrer conforme as regras do Esconderijo.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
        )
      }
    }'''
s=one(s,old,new,'Group Friction panel')
write(rel,s)

# Navigation turns the button into a narrated request. It does NOT heal directly.
rel='app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt'; s=read(rel)
s=one(s,
'''      GroupScreen(onNavigate = onNavigate, gameState = gameState)''',
'''      GroupScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onResolveFrictionIntent = { gameViewModel.submitPlayerIntent("Quero reunir os Teens em um lugar seguro e próximo para tentar resolver a Friction do grupo e recuperar Condições, se a cena cumprir os requisitos.") }
      )''','Nav Group friction')
write(rel,s)

# Regression: no system-level Tales lock may override the selected profile.
test=r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FloodMotorAndFrictionV1316Test {
  private fun src(rel:String):String {
    val a=File("src/main/java/com/braseiro/talesvtt/$rel"); val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
    return listOf(a,b).first{it.isFile}.readText()
  }

  @Test fun wireRecognizesFrictionRecovery(){
    val parsed=MotorBarbaraWireAdapter.parse("""{"profile":"flood_teen","actions":[{"type":"recovery","approved":true,"source":"friction"}]}""")
    val r=parsed.packet.actions.single() as MotorAction.Recovery
    assertEquals(RecoveryKind.FRICTION,r.kind)
  }

  @Test fun floodFrictionRecoveryHealsOnlyThroughFloodFirewall(){
    val teen=RandomTeenFactory.create(kotlin.random.Random(3)).copy(conditions=setOf(Condition.UPSET,Condition.SCARED))
    val group=GroupState(members=listOf(GroupMemberState(teen.name,teen.archetype,teen.portraitAssetKey,teen.conditions,false)))
    val flood=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,group=group)
    val packet=MotorPacket(MotorProfile.FLOOD_TEENS,listOf(MotorAction.Recovery(true,RecoveryKind.FRICTION,null)))
    val out=MotorBarbaraFirewall.apply(flood,GmPrivateState(),packet)
    assertTrue(out.rejected.isEmpty()); assertTrue(out.gameState.kid.conditions.isEmpty())

    val tales=GameState()
    val talesPacket=MotorPacket(MotorProfile.TALES_KIDS,listOf(MotorAction.Recovery(true,RecoveryKind.FRICTION,null)))
    val blocked=MotorBarbaraFirewall.apply(tales,GmPrivateState(),talesPacket)
    assertTrue(blocked.rejected.isNotEmpty())
  }

  @Test fun systemPromptIsProfileNeutralAndMechanicalContractOwnsRules(){
    val s=src("game/MotorBarbaraTransport.kt")
    assertTrue(s.contains("active_profile e mechanical_contract"))
    assertTrue(s.contains("source: anchor|hideout|comfort|friction"))
    assertTrue(s.contains("Nunca misture perfis"))
    assertFalse(s.contains("Mestre autônomo do VTT BRASEIRO para Tales from the Loop no perfil Tales/Kids"))
    assertFalse(s.contains("Problema Estendido Tales: Normal=2x Kids"))
  }

  @Test fun groupScreenNeverOffersTalesHideoutRecoveryToFlood(){
    val s=src("screens/GroupScreen.kt")
    assertTrue(s.contains("TEENS & FRICTION"))
    assertTrue(s.contains("PEDIR CENA PARA RESOLVER FRICTION"))
    assertTrue(s.contains("if(flood)"))
  }
}
'''
write('app/src/test/java/com/braseiro/talesvtt/game/FloodMotorAndFrictionV1316Test.kt',test)

resume=root/'TALES_RETOMADA_CURRENT.md'
text=resume.read_text(encoding='utf-8') if resume.exists() else '# TALES FROM THE LOOP — RETOMADA ATUAL\n'
text+='''\n## Bloco 1.3.16g-dev — Motor neutro por perfil + Friction jogável\n- SYSTEM_PROMPT deixou de impor Tales/Kids sobre campanhas Flood; active_profile + mechanical_contract passam a decidir a mecânica.\n- Recovery ganhou fonte Friction, aceita somente em Flood/Teens e sempre após aprovação ficcional do Motor.\n- Grupo Flood mostra Teens + Friction, não a recuperação por Esconderijo de Tales.\n- Botão de Friction pede uma cena ao Mestre; nunca cura diretamente pela UI.\n- Nenhum APK publicado.\n'''
resume.write_text(text,encoding='utf-8')

assert 'RecoveryKind { ANCHOR, HIDEOUT, COMFORT, FRICTION }' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt')
assert 'active_profile e mechanical_contract' in read('app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt')
assert 'TEENS & FRICTION' in read('app/src/main/java/com/braseiro/talesvtt/screens/GroupScreen.kt')
print('TALES/FLOOD 1.3.16g motor profile neutrality + Friction recovery: OK')
