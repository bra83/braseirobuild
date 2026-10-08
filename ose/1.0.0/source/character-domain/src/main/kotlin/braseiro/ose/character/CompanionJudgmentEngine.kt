package braseiro.ose.character

import braseiro.ose.model.*

/**
 * An explicitly bounded read-only companion reasoning model.
 *
 * Never reads undiscovered hexes, secret dungeon nodes, NPC private plans or
 * GM-only adventure text. Its hypotheses are labelled uncertain. It does not
 * roll dice, change turns, resolve mechanics or mutate campaign state.
 */
data class CompanionJudgment(
    val characterId: String,
    val name: String,
    val perception: String,
    val hypothesis: String,
    val motivation: String,
    val reason: String,
    val risk: String,
    val stance: String,
    val suggestedAction: String,
    val mechanicalIntent: String
)

object CompanionJudgmentEngine {
    fun opinions(envelope: CampaignEnvelope): List<CompanionJudgment> =
        envelope.campaignState.party.characterIds
            .filter { CharacterControlCoordinator.mode(envelope, it) != CharacterControlMode.PLAYER }
            .map { assess(envelope, it) }

    fun assess(envelope: CampaignEnvelope, characterId: String): CompanionJudgment {
        CharacterControlCoordinator.mode(envelope, characterId)
        val state = envelope.campaignState
        val game = state.game
        val c = game.characters[characterId] ?: error("UNKNOWN_CHARACTER_ID")
        val location = state.position.primary
        val weather = game.livingWorld.weather.kind
        val injury = c.hitPoints.maxNumerator > 0 &&
            c.hitPoints.currentNumerator * 2 <= c.hitPoints.maxNumerator
        val lowFood = game.inventory.items.values
            .filter { it.kind == InventoryItemKind.FOOD &&
                it.ownerId in state.party.characterIds }
            .sumOf { it.quantity } < state.party.characterIds.size
        val cautious = c.attributes.wis >= 13 || injury
        val analytical = c.attributes.int >= 13 || c.classIds.any {
            it in setOf("MAGIC_USER", "ILLUSIONIST", "ELF")
        }
        val protector = c.classIds.any {
            it in setOf("CLERIC", "FIGHTER", "PALADIN", "KNIGHT")
        }
        val scout = c.classIds.any {
            it in setOf("THIEF", "RANGER", "ASSASSIN", "ACROBAT")
        }
        val exposure = weather in setOf(WeatherKind.STORM, WeatherKind.COLD, WeatherKind.RAIN)
        val perception = when (location) {
            is SpatialRef.Dungeon ->
                "Estamos em uma área de masmorra já alcançada; não conheço o que há além do que foi revelado."
            is SpatialRef.Hex -> {
                val here = game.hexWorld?.cells?.firstOrNull {
                    it.q == location.q && it.r == location.r && (it.known || it.visible)
                }
                if (here == null) "Estamos nos ermos; não tenho informações confirmadas do terreno adiante."
                else "Estamos nos ermos, em terreno ${here.terrain.name.lowercase().replace('_', ' ')}; o caminho adiante não está confirmado."
            }
            is SpatialRef.Settlement ->
                "Estamos em um assentamento; só posso avaliar o que já está publicamente visível."
            is SpatialRef.Scene ->
                "A cena está em andamento; só disponho do que já foi mostrado à companhia."
        }
        val hypothesis = when {
            injury -> "Talvez estejamos subestimando quanto esforço ainda conseguimos suportar."
            lowFood -> "Talvez precisemos assegurar provisões antes de assumir mais riscos."
            exposure -> "O tempo pode dificultar uma decisão apressada."
            scout -> "Podemos descobrir mais antes de comprometer toda a companhia."
            analytical -> "Ainda pode faltar uma peça para compreender a situação."
            else -> "Uma escolha menos precipitada pode revelar uma alternativa."
        }
        val motivation = when {
            injury -> "Quero sobreviver e continuar útil ao grupo."
            protector -> "Quero evitar que outro companheiro sofra um dano evitável."
            scout -> "Prefiro obter informação antes de expor os outros."
            analytical -> "Gosto de compreender o problema antes de arriscar recursos."
            c.attributes.cha >= 13 -> "Prefiro manter o grupo unido e ouvir as divergências."
            else -> "Quero ajudar sem assumir riscos desnecessários pelos outros."
        }
        val stance = when {
            injury && cautious -> "OBJECT"
            lowFood && cautious -> "CAUTION"
            exposure && cautious -> "CAUTION"
            analytical && !scout -> "UNCERTAIN"
            else -> "SUPPORT"
        }
        val risk = when {
            injury -> "Um confronto ou deslocamento agora pode agravar meu ferimento."
            lowFood -> "As provisões podem não sustentar uma exploração prolongada."
            exposure -> "Exposição e deslocamento sob este clima podem ser perigosos."
            location is SpatialRef.Dungeon -> "Um caminho ou ameaça não revelados podem surpreender o grupo."
            else -> "A situação pode conter perigos ainda não percebidos."
        }
        val suggestedAction = when {
            injury -> "Sugiro examinar os ferimentos e discutir uma pausa segura; não obrigo ninguém a parar."
            lowFood -> "Sugiro conferir nossas provisões antes de escolher uma rota."
            scout -> "Posso observar sinais e acessos conhecidos sem levar o grupo comigo."
            analytical -> "Proponho comparar o que já vimos antes de decidir."
            protector -> "Proponho organizar uma posição defensiva sem avançar."
            else -> "Eu ouviria os outros antes de assumir uma direção."
        }
        val reason = when (stance) {
            "OBJECT" -> "Eu me oporia a uma ordem precipitada enquanto estivermos fragilizados."
            "CAUTION" -> "Apoio o objetivo, mas defenderia uma preparação antes de executá-lo."
            "UNCERTAIN" -> "Minha interpretação é apenas uma hipótese; posso estar enganado."
            else -> "Acredito que esta pequena iniciativa pode ajudar sem dominar a decisão."
        }
        return CompanionJudgment(c.characterId,c.name,perception,hypothesis,motivation,
            reason,risk,stance,suggestedAction,suggestedAction)
    }
}
