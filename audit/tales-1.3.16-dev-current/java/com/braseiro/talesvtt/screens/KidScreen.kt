package com.braseiro.talesvtt.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.IconicItemVisual
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.PortraitSlot
import com.braseiro.talesvtt.components.PrimaryButton
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.Condition
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.RulesProfile
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun KidScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onExportKid: (Uri) -> Unit,
  onImportKid: (Uri) -> Unit,
  onChangePortrait: (Uri) -> Unit,
  onRecordFloodScar: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val exportKid = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(onExportKid) }
  val importKid = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onImportKid) }
  val changePortrait = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onChangePortrait) }
  val flood = gameState.rulesProfile == RulesProfile.FLOOD_TEENS
  var pendingScarText by remember(gameState.kid.scarPending, gameState.kid.scars.size) { mutableStateOf("") }
  val contextItems = listOf(
    ContextItemData("kid_loc", "Local Atual", gameState.scene.location, ContextIconType.LOCATION),
    ContextItemData("kid_age", "Idade", "${gameState.kid.age} anos", ContextIconType.CHARACTERS),
    if (flood) ContextItemData("kid_scars", "Cicatrizes", "${gameState.kid.scars.size} / 6", ContextIconType.MYSTERY, isHighlighted = true)
    else ContextItemData("kid_luck", "Sorte", "${gameState.kid.luck} / ${gameState.kid.maxLuck}", ContextIconType.MYSTERY, isHighlighted = true)
  )

  AppShell(
    currentDestination = AppDestination.KID,
    screenTitle = gameState.kid.name,
    chapterLabel = if (flood) "TEEN" else "KID",
    sceneLabel = if (flood) "Ficha do Teen" else "Ficha do Kid",
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SectionHeader(title = "VISÃO GERAL", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)
    ) {
      PortraitSlot(
        name = gameState.kid.name.substringBefore(' '),
        archetype = gameState.kid.archetype,
        imageRes = portraitDrawable(gameState.kid.portraitAssetKey),
        imageBase64 = gameState.kid.portraitImageBase64,
        size = 124.dp,
        borderColor = AppTheme.colors.accentCyan
      )
      AppPanel(modifier = Modifier.weight(1f), contentPadding = 10.dp) {
        KidFact("NOME", gameState.kid.name)
        KidFact("ARQUÉTIPO", gameState.kid.archetype)
        KidFact("IDADE", "${gameState.kid.age} anos")
        KidFact("DESCRIÇÃO", gameState.kid.description)
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))
    SectionHeader(title = "FICHA • ARQUIVO", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing6)
    ) {
      SecondaryButton(
        "TROCAR FOTO",
        onClick = { changePortrait.launch(arrayOf("image/*")) },
        modifier = Modifier.weight(1f)
      )
      SecondaryButton(
        "IMPORTAR JSON",
        onClick = { importKid.launch(arrayOf("application/json", "text/json", "text/plain")) },
        modifier = Modifier.weight(1f)
      )
      PrimaryButton(
        "EXPORTAR JSON",
        onClick = { exportKid.launch("Ficha_${gameState.kid.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.json") },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))
    SectionHeader(title = "ATRIBUTOS & PERÍCIAS", color = AppTheme.colors.accentAmber)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
    ) {
      AttributeCard(
        modifier = Modifier.weight(1f),
        title = "CORPO", value = gameState.kid.attributes["CORPO"] ?: 0, iconRes = R.drawable.attr_body,
        accent = AppTheme.colors.accentOlive,
        skills = listOf("Furtividade", "Força", "Movimentação").map { it to (gameState.kid.skills[it] ?: 0) }
      )
      AttributeCard(
        modifier = Modifier.weight(1f),
        title = "TECNOLOGIA", value = gameState.kid.attributes["TECNOLOGIA"] ?: 0, iconRes = R.drawable.attr_tech,
        accent = AppTheme.colors.accentAmber,
        skills = listOf("Mecânica", "Programação", "Cálculo").map { it to (gameState.kid.skills[it] ?: 0) }
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
    ) {
      AttributeCard(
        modifier = Modifier.weight(1f),
        title = "CORAÇÃO", value = gameState.kid.attributes["CORAÇÃO"] ?: 0, iconRes = R.drawable.attr_heart,
        accent = AppTheme.colors.accentPurple,
        skills = listOf("Contatos", "Charme", "Liderança").map { it to (gameState.kid.skills[it] ?: 0) }
      )
      AttributeCard(
        modifier = Modifier.weight(1f),
        title = "MENTE", value = gameState.kid.attributes["MENTE"] ?: 0, iconRes = R.drawable.attr_mind,
        accent = AppTheme.colors.accentCyan,
        skills = listOf("Investigação", "Conhecimento", "Empatia").map { it to (gameState.kid.skills[it] ?: 0) }
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))
    SectionHeader(title = if(flood) "ESTADO DO TEEN" else "ESTADO DO KID", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing8))

    AppPanel {
      Text(
        text = "CONDIÇÕES",
        style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary)
      )
      Spacer(modifier = Modifier.height(6.dp))
      ConditionRow("Chateado", R.drawable.tales_condition_upset, Condition.UPSET in gameState.kid.conditions)
      ConditionRow("Assustado", R.drawable.tales_condition_scared, Condition.SCARED in gameState.kid.conditions)
      ConditionRow("Exausto", R.drawable.tales_condition_exhausted, Condition.EXHAUSTED in gameState.kid.conditions)
      ConditionRow("Machucado", R.drawable.tales_condition_injured, Condition.INJURED in gameState.kid.conditions)
      ConditionRow("Quebrado", R.drawable.tales_condition_broken, gameState.kid.broken)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (gameState.kid.broken) "Quebrado: falha automática em rolagens até ser curado." else "${gameState.kid.conditions.size} Condição(ões) ativa(s): −${gameState.kid.conditionPenalty} dado(s) em todas as rolagens.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }

    if (flood) {
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
        title = "PROBLEMA",
        text = gameState.kid.problem,
        iconRes = R.drawable.char_problem,
        accent = AppTheme.colors.accentAmber
      )
      StoryCard(
        modifier = Modifier.weight(1f),
        title = "ÍMPETO",
        text = gameState.kid.drive,
        iconRes = R.drawable.char_drive,
        accent = AppTheme.colors.accentCyan
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)
    ) {
      StoryCard(
        modifier = Modifier.weight(1f),
        title = if(flood) "VERGONHA" else "ORGULHO",
        text = if(flood) {
          if(gameState.kid.shameAvailable) "${gameState.kid.shameText} Disponível." else "${gameState.kid.shameText} Vergonha já utilizada."
        } else if (gameState.kid.prideAvailable) "${gameState.kid.prideText} Disponível neste Mistério." else "${gameState.kid.prideText} Orgulho já utilizado.",
        iconRes = R.drawable.char_pride,
        accent = AppTheme.colors.accentPurple
      )
      StoryCard(
        modifier = Modifier.weight(1f),
        title = "ÂNCORA",
        text = gameState.kid.anchorName,
        iconRes = R.drawable.char_anchor,
        accent = AppTheme.colors.accentOlive
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))

    AppPanel(borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.45f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(IconicItemVisual.drawable(gameState.kid.iconicItemName)),
          contentDescription = gameState.kid.iconicItemName,
          modifier = Modifier.size(70.dp),
          contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "ITEM ICÔNICO",
            style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber)
          )
          Text(
            text = gameState.kid.iconicItemName,
            style = AppTheme.typography.cardTitle
          )
          Text(
            text = "+2 dados quando claramente útil e aceito pelo Mestre.",
            style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))

    AppPanel {
      Text(
        text = "RELACIONAMENTOS",
        style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan)
      )
      Spacer(modifier = Modifier.height(6.dp))
      gameState.kid.relationships.forEach { relationship ->
        RelationshipRow(relationship.name, relationship.description)
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing12))

    AppPanel {
      Text(
        text = "EXPERIÊNCIA",
        style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan)
      )
      Text(text = "${gameState.kid.xp} XP", style = AppTheme.typography.cardTitle)
      Text(
        text = "5 XP podem aumentar uma Perícia em +1 antes ou depois da sessão. Máximo 5.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

@Composable
private fun KidFact(label: String, value: String) {
  Text(text = label, style = AppTheme.typography.caption.copy(color = AppTheme.colors.accentAmber))
  Text(text = value, style = AppTheme.typography.body.copy(fontSize = 13.sp))
  Spacer(modifier = Modifier.height(5.dp))
}

@Composable
private fun AttributeCard(
  title: String,
  value: Int,
  iconRes: Int,
  accent: Color,
  skills: List<Pair<String, Int>>,
  modifier: Modifier = Modifier
) {
  AppPanel(modifier = modifier, borderColor = accent.copy(alpha = 0.55f), contentPadding = 9.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(iconRes),
        contentDescription = title,
        modifier = Modifier.size(32.dp),
        contentScale = ContentScale.Fit
      )
      Spacer(modifier = Modifier.width(7.dp))
      Column {
        Text(text = title, style = AppTheme.typography.label.copy(color = accent))
        Text(
          text = value.toString(),
          style = AppTheme.typography.screenTitle.copy(fontSize = 24.sp, color = AppTheme.colors.textPrimary)
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    skills.forEach { (name, score) ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = name, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary))
        Text(text = score.toString(), style = AppTheme.typography.bodySmall.copy(color = accent, fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
private fun ConditionRow(name: String, iconRes: Int, active: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Image(
      painter = painterResource(iconRes),
      contentDescription = name,
      modifier = Modifier.size(28.dp),
      contentScale = ContentScale.Fit
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = name, style = AppTheme.typography.body, modifier = Modifier.weight(1f))
    Box(
      modifier = Modifier
        .size(13.dp)
        .background(
          if (active) AppTheme.colors.accentCyan else Color.Transparent,
          RoundedCornerShape(99.dp)
        )
        .border(
          1.dp,
          if (active) AppTheme.colors.accentCyan else AppTheme.colors.borderMedium,
          RoundedCornerShape(99.dp)
        )
    )
  }
}

@Composable
private fun StoryCard(
  title: String,
  text: String,
  iconRes: Int,
  accent: Color,
  modifier: Modifier = Modifier
) {
  AppPanel(modifier = modifier, borderColor = accent.copy(alpha = 0.35f), contentPadding = 9.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(iconRes),
        contentDescription = title,
        modifier = Modifier.size(30.dp),
        contentScale = ContentScale.Fit
      )
      Spacer(modifier = Modifier.width(7.dp))
      Text(text = title, style = AppTheme.typography.label.copy(color = accent))
    }
    Spacer(modifier = Modifier.height(5.dp))
    Text(text = text, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary))
  }
}

@Composable
private fun RelationshipRow(name: String, description: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    Text(text = name, style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold))
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = description, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
  }
}


private fun portraitDrawable(assetKey: String): Int = when (assetKey.removeSuffix(".png")) {
  "kid_portrait_01", "tales_type_computer_geek" -> R.drawable.kid_portrait_01
  "kid_portrait_02", "tales_type_popular" -> R.drawable.kid_portrait_02
  "kid_portrait_03", "tales_type_jock" -> R.drawable.kid_portrait_03
  "kid_portrait_04", "tales_type_bookworm" -> R.drawable.kid_portrait_04
  else -> R.drawable.kid_portrait_01
}
