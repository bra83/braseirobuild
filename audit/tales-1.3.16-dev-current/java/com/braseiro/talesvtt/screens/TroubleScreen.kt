package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppIcons
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.FreeActionPrompt
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.game.CanonicalTalesRules
import com.braseiro.talesvtt.game.BonusEffectKind
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.TestPurpose
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun TroubleScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onRoll: () -> Unit,
  onHelp: () -> Unit,
  onLuck: () -> Unit,
  onPush: (Condition?) -> Unit,
  onPride: () -> Unit,
  onShame: () -> Unit,
  onScar: (Int) -> Unit,
  onInformationBonus: () -> Unit,
  onContactItemBonus: () -> Unit,
  onLeadershipComfort: (Condition) -> Unit,
  onPurchaseBonusEffect: (String,String?) -> Unit,
  onSubmitResult: () -> Unit,
  onDeclareAction: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val test = gameState.activeTest
  val result = gameState.testResolution
  val activeCondition = gameState.kid.conditions.firstOrNull()
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS

  val contextItems = listOf(
    ContextItemData("tr_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION),
    ContextItemData("tr_kid", if(flood) "Teen" else "Kid", gameState.kid.name.substringBefore(' '), ContextIconType.CHARACTERS, isHighlighted = true),
    ContextItemData(
      "tr_cond",
      "Condições",
      if (gameState.kid.broken) "Quebrado" else "${gameState.kid.conditions.size} ativa(s)",
      ContextIconType.MYSTERY
    )
  )

  AppShell(
    currentDestination = AppDestination.SCENE,
    screenTitle = gameState.scene.location,
    chapterLabel = "CENA",
    sceneLabel = "Teste pedido pelo Mestre",
    worldStatus = WorldStatusData(gameState.scene.seasonYear, gameState.scene.weather, gameState.scene.time),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = R.drawable.scene_loop_gate,
      locationName = "LOOP INDUSTRIES",
      sceneTone = "Suspense",
      showTts = false,
      onCharactersClick = { onNavigate(AppDestination.GROUP) },
      onLocationClick = { onNavigate(AppDestination.MAP) }
    )

    Spacer(modifier = Modifier.height(Dimens.spacing10))

    gameState.lastPlayerAction?.let { action ->
      AppPanel {
        Text(text = "SUA AÇÃO", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
        Text(text = "“$action”", style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary))
      }
      Spacer(modifier = Modifier.height(Dimens.spacing10))
    }

    gameState.lastError?.takeIf { it.isNotBlank() }?.let { error ->
      AppPanel(borderColor = AppTheme.colors.accentAmber) {
        Text(
          text = "ATENÇÃO",
          style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = error,
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary)
        )
      }
      Spacer(modifier = Modifier.height(Dimens.spacing10))
    }

    if (test == null) {
      AppPanel(borderColor = AppTheme.colors.accentCyan) {
        Text(text = "NENHUM TESTE ATIVO", style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.accentCyan))
        Text(
          text = "O Mestre ainda não pediu uma rolagem para esta ação.",
          style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary)
        )
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryButton(text = "VOLTAR À CENA", onClick = { onNavigate(AppDestination.SCENE) }, modifier = Modifier.fillMaxWidth())
      }
      return@AppShell
    }

    AppPanel(borderColor = AppTheme.colors.accentCyan, borderWidth = 1.5.dp) {
      Text(text = "TESTE PEDIDO PELO MESTRE", style = AppTheme.typography.sectionTitle.copy(color = AppTheme.colors.accentCyan))
      Text(text = test.reason, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        RollTerm(attributeIcon(test.attributeName), test.attributeName, test.attributeValue.toString())
        if (test.skillName != null) {
          Text("+", style = AppTheme.typography.screenTitle.copy(color = AppTheme.colors.accentCyan))
          RollTerm(skillIcon(test.skillName), test.skillName, test.skillValue.toString())
        }
        if (gameState.kid.conditionPenalty > 0) {
          Text("−", style = AppTheme.typography.screenTitle.copy(color = AppTheme.colors.accentCyan))
          RollTerm(
            R.drawable.tales_condition_scared,
            activeCondition?.label?.uppercase() ?: "CONDIÇÃO",
            gameState.kid.conditionPenalty.toString()
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = if (gameState.kid.broken) "QUEBRADO • FALHA AUTOMÁTICA" else "POOL FINAL  •  ${gameState.finalPool()} DADOS",
        style = AppTheme.typography.screenTitle.copy(color = AppTheme.colors.textPrimary, fontSize = 19.sp)
      )
      Text(
        text = "Cada 6 = 1 sucesso. Alvo: ${test.requiredSuccesses} sucesso${if (test.requiredSuccesses == 1) "" else "s"}.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )

      if (test.helperKid != null) {
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = "Ajuda: ${test.helperKid} • +1 dado • compartilha a consequência em caso de falha.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentOlive)
        )
      }
      if (test.approvedItemBonus > 0) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Item aprovado pelo Mestre: +${test.approvedItemBonus} dados.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentPurple)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (result == null) {
        if (gameState.informationBonusBank > 0 && test.informationBonus < 3) {
          SecondaryButton(text="USAR +1 DADO DE INFORMAÇÃO • ${gameState.informationBonusBank} guardado(s)",onClick=onInformationBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        if (gameState.contactItemBonusBank > 0 && test.approvedItemBonus == 0) {
          SecondaryButton(text="USAR CONTATO COMO ITEM • +${gameState.contactItemBonusBank} DADO(S)",onClick=onContactItemBonus,modifier=Modifier.fillMaxWidth())
          Spacer(modifier=Modifier.height(7.dp))
        }
        PrimaryButton(
          text = if (gameState.kid.broken) "REGISTRAR FALHA" else "ROLAR ${gameState.finalPool()} DADOS",
          onClick = onRoll,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(7.dp))
        if (test.helperKid == null) {
          SecondaryButton(
            text = "PEDIR AJUDA  •  +1 dado de ${if(flood) "um Teen" else "um Kid"}",
            icon = AppIcons.Group,
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth()
          )
        }
      } else {
        RollResultPanel(gameState, onLuck, onPush, onPride, onShame, onScar)
        LeadershipComfortEffect(gameState,onLeadershipComfort)
        if(!flood) CanonicalBonusEffects(gameState,onPurchaseBonusEffect)
        Spacer(modifier = Modifier.height(10.dp))
        val pendingComfort = test.purpose == TestPurpose.LEADERSHIP_COMFORT && result.successes >= test.requiredSuccesses && !result.baseEffectApplied
        PrimaryButton(
          text = when { result.reportedToGm -> "RESULTADO ENVIADO AO MESTRE"; pendingComfort -> "ESCOLHA A CONDIÇÃO CURADA"; else -> "ENVIAR RESULTADO AO MESTRE" },
          onClick = onSubmitResult,
          enabled = !result.reportedToGm && !pendingComfort,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    if (result == null) {
      Spacer(modifier = Modifier.height(Dimens.spacing12))
      SectionHeader(title = "ANTES DE ROLAR", color = AppTheme.colors.accentAmber)
      Spacer(modifier = Modifier.height(Dimens.spacing6))
      SecondaryButton(
        text = "PROPOR ITEM POSSUÍDO",
        icon = AppIcons.Backpack,
        onClick = { onNavigate(AppDestination.BACKPACK) },
        modifier = Modifier.fillMaxWidth()
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    FreeActionPrompt(
      onSubmit = {
        onDeclareAction(it)
        onNavigate(AppDestination.SCENE)
      }
    )
    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun RollResultPanel(
  gameState: GameState,
  onLuck: () -> Unit,
  onPush: (Condition?) -> Unit,
  onPride: () -> Unit,
  onShame: () -> Unit,
  onScar: (Int) -> Unit
) {
  val test = gameState.activeTest ?: return
  val result = gameState.testResolution ?: return
  val success = result.successes >= test.requiredSuccesses
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS

  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    Text(
      text = "DADOS  •  ${if (result.dice.isEmpty()) "—" else result.dice.joinToString("  ")}",
      style = AppTheme.typography.cardTitle.copy(color = AppTheme.colors.textPrimary)
    )
    Text(
      text = "${result.successes} sucesso${if (result.successes == 1) "" else "s"}  •  ${if (success) "SUCESSO" else "FALHA"}",
      style = AppTheme.typography.sectionTitle.copy(color = if (success) AppTheme.colors.accentOlive else AppTheme.colors.accentAmber)
    )

    if (!flood && !success && !result.luckUsed && gameState.kid.luck > 0) {
      SecondaryButton(
        text = "USAR SORTE  •  rerrolar dados sem 6",
        onClick = onLuck,
        modifier = Modifier.fillMaxWidth()
      )
    }
    if (!result.pushed && !gameState.kid.broken) {
      var choosingPushCondition by remember(result) { mutableStateOf(false) }
      val availableConditions = Condition.entries.filterNot { it in gameState.kid.conditions }
      if (!choosingPushCondition) {
        SecondaryButton(
          text = if (availableConditions.isEmpty()) {
            "FORÇAR  •  ficar Quebrado e rerrolar não-6"
          } else {
            "FORÇAR  •  escolher 1 Condição e rerrolar não-6"
          },
          onClick = {
            if (availableConditions.isEmpty()) onPush(null) else choosingPushCondition = true
          },
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Text(
          text = "ESCOLHA A CONDIÇÃO QUE SERÁ MARCADA",
          style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber)
        )
        availableConditions.forEach { condition ->
          SecondaryButton(
            text = "MARCAR ${condition.label.uppercase()} E FORÇAR",
            onClick = { onPush(condition) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
    if (!flood && gameState.kid.prideAvailable && !result.prideUsed) {
      SecondaryButton(
        text = "USAR ORGULHO  •  +1 sucesso automático",
        onClick = onPride,
        modifier = Modifier.fillMaxWidth()
      )
    }
    if (flood && gameState.kid.shameAvailable) {
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
  }
}

@Composable
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

@Composable
private fun RollTerm(iconRes: Int, label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Image(
      painter = painterResource(iconRes),
      contentDescription = label,
      modifier = Modifier.size(38.dp),
      contentScale = ContentScale.Fit
    )
    Text(text = label, style = AppTheme.typography.caption.copy(color = AppTheme.colors.textSecondary))
    Text(text = value, style = AppTheme.typography.screenTitle.copy(color = AppTheme.colors.textPrimary))
  }
}


private fun attributeIcon(name: String): Int = when (name.uppercase()) {
  "CORPO" -> R.drawable.attr_body
  "TECNOLOGIA" -> R.drawable.attr_tech
  "CORAÇÃO" -> R.drawable.attr_heart
  else -> R.drawable.attr_mind
}

private fun skillIcon(name: String): Int = when (name.uppercase()) {
  "FURTIVIDADE" -> R.drawable.skill_sneak
  "FORÇA" -> R.drawable.skill_force
  "MOVIMENTAÇÃO" -> R.drawable.skill_move
  "MECÂNICA" -> R.drawable.skill_tinker
  "PROGRAMAÇÃO" -> R.drawable.skill_program
  "CÁLCULO" -> R.drawable.skill_calculate
  "CONTATOS" -> R.drawable.skill_contact
  "CHARME" -> R.drawable.skill_charm
  "LIDERANÇA" -> R.drawable.skill_lead
  "INVESTIGAÇÃO" -> R.drawable.skill_investigate
  "CONHECIMENTO" -> R.drawable.skill_comprehend
  "EMPATIA" -> R.drawable.skill_empathize
  else -> R.drawable.skill_investigate
}
