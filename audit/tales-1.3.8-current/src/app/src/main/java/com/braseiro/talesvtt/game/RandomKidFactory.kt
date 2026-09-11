package com.braseiro.talesvtt.game

import kotlin.random.Random

/**
 * Creates a Tales/Kids character that obeys the creation limits encoded in the canonical ledger:
 * age 10..15; four attributes 1..5 summing to age; 10 skill points; three Type key skills up to 3,
 * every other skill up to 1; Luck max = 15 - age.
 */
object RandomKidFactory {
  private data class TypeSpec(
    val label: String,
    val portrait: String,
    val keySkills: Set<String>
  )

  private val types = listOf(
    TypeSpec("Caxias", "tales_type_bookworm", setOf("Cálculo", "Investigação", "Conhecimento")),
    TypeSpec("Informático", "tales_type_computer_geek", setOf("Cálculo", "Programação", "Conhecimento")),
    TypeSpec("Caipira", "tales_type_hick", setOf("Força", "Movimentação", "Mecânica")),
    TypeSpec("Esportista", "tales_type_jock", setOf("Força", "Movimentação", "Contatos")),
    TypeSpec("Popular", "tales_type_popular", setOf("Contatos", "Charme", "Liderança")),
    TypeSpec("Rocker", "tales_type_rocker", setOf("Movimentação", "Charme", "Empatia")),
    TypeSpec("Encrenqueiro", "tales_type_troublemaker", setOf("Força", "Furtividade", "Liderança")),
    TypeSpec("Estranho", "tales_type_weirdo", setOf("Furtividade", "Investigação", "Empatia"))
  )


  private val portraitPool = listOf(
    "kid_portrait_01",
    "kid_portrait_02",
    "kid_portrait_03",
    "kid_portrait_04"
  )

  private val firstNames = listOf("Lasse", "Anita", "Jonas", "Malin", "Erik", "Sara", "Mikael", "Elin", "Oskar", "Karin", "Nils", "Sofia")
  private val lastNames = listOf("Svensson", "Holmberg", "Berg", "Ek", "Lind", "Nilsson", "Andersson", "Lund", "Karlsson", "Nyström")
  private val anchors = listOf("Astrid — avó", "Gunnar — professor de ciências", "Ingrid — vizinha", "Bo — treinador", "Eva — tia", "Lars — bibliotecário")
  private val problems = listOf(
    "Meus pais não confiam em mim.",
    "Tenho medo de decepcionar meus amigos.",
    "Minha família está escondendo alguma coisa.",
    "Estou sempre metido em problemas na escola.",
    "Ninguém leva minhas ideias a sério.",
    "Quero provar que consigo fazer tudo sozinho."
  )
  private val drives = listOf(
    "Descobrir a verdade.", "Proteger meus amigos.", "Entender como as máquinas funcionam.",
    "Mostrar que sou capaz.", "Encontrar uma explicação para o impossível.", "Não deixar ninguém para trás."
  )
  private val prides = listOf(
    "Eu nunca desisto.", "Eu entendo máquinas como ninguém.", "Eu sei quando alguém está mentindo.",
    "Eu consigo entrar onde ninguém consegue.", "Eu mantenho a cabeça fria.", "Eu sempre encontro uma saída."
  )
  private val iconicItems = listOf(
    "Walkie-talkie velho", "Câmera instantânea", "Binóculos do avô", "Chave inglesa antiga",
    "Bússola de bolso", "Gravador cassete", "Canivete", "Jaqueta jeans cheia de patches"
  )

  fun create(random: Random = Random.Default): KidState {
    val type = types.random(random)
    val age = random.nextInt(10, 16)

    val attrNames = listOf("CORPO", "TECNOLOGIA", "CORAÇÃO", "MENTE")
    val attrs = linkedMapOf<String, Int>().apply { attrNames.forEach { put(it, 1) } }
    var remainingAttr = age - 4
    while (remainingAttr > 0) {
      val candidates = attrNames.filter { (attrs[it] ?: 1) < 5 }
      val chosen = candidates.random(random)
      attrs[chosen] = (attrs[chosen] ?: 1) + 1
      remainingAttr--
    }

    val skills = linkedMapOf<String, Int>().apply { TalesSkillCatalog.skillToAttribute.keys.forEach { put(it, 0) } }
    var remainingSkills = 10
    while (remainingSkills > 0) {
      val candidates = skills.keys.filter { skill ->
        val max = if (skill in type.keySkills) 3 else 1
        (skills[skill] ?: 0) < max
      }
      require(candidates.isNotEmpty()) { "Não há capacidade de perícias suficiente para 10 pontos." }
      val chosen = candidates.random(random)
      skills[chosen] = (skills[chosen] ?: 0) + 1
      remainingSkills--
    }

    val name = "${firstNames.random(random)} ${lastNames.random(random)}"
    return KidState(
      name = name,
      archetype = type.label,
      portraitAssetKey = portraitPool.random(random),
      portraitImageBase64 = null,
      description = "Um Kid de $age anos, ${type.label.lowercase()}, criado aleatoriamente para uma nova aventura.",
      problem = problems.random(random),
      drive = drives.random(random),
      prideText = prides.random(random),
      anchorName = anchors.random(random),
      iconicItemName = iconicItems.random(random),
      relationships = listOf(
        KidRelationship("Outro Kid do grupo", "amizade que será definida em jogo"),
        KidRelationship("NPC da escola", "relação importante da Vida Cotidiana"),
        KidRelationship("NPC da família", "adulto importante fora do grupo")
      ),
      age = age,
      luck = 15 - age,
      conditions = emptySet(),
      broken = false,
      prideAvailable = true,
      xp = 0,
      attributes = attrs,
      skills = skills
    )
  }


  /**
   * Validates an already-created Kid loaded from persistence. Unlike validateCreation,
   * Skills may have advanced up to 5, but age/attributes/Luck and the canonical 12-skill
   * schema must still be coherent.
   */
  fun validatePersistentState(kid: KidState): Boolean {
    if (kid.name.isBlank()) return false
    if (kid.age !in 10..15) return false
    if (kid.attributes.keys != setOf("CORPO", "TECNOLOGIA", "CORAÇÃO", "MENTE")) return false
    if (kid.attributes.values.any { it !in 1..5 } || kid.attributes.values.sum() != kid.age) return false
    if (kid.skills.keys != TalesSkillCatalog.skillToAttribute.keys) return false
    if (kid.skills.values.any { it !in 0..5 }) return false
    if (kid.luck !in 0..(15 - kid.age)) return false
    if (kid.xp !in 0..10) return false
    return true
  }

  fun creationTypeLabels(): Set<String> = types.map { it.label }.toSet()

  fun creationKeySkills(typeLabel: String): Set<String>? =
    types.firstOrNull { it.label.equals(typeLabel, ignoreCase = true) }?.keySkills

  fun validateCreation(kid: KidState): Boolean {
    val type = types.firstOrNull { it.label.equals(kid.archetype, true) } ?: return false
    if (kid.age !in 10..15) return false
    if (kid.attributes.keys != setOf("CORPO", "TECNOLOGIA", "CORAÇÃO", "MENTE")) return false
    if (kid.attributes.values.any { it !in 1..5 } || kid.attributes.values.sum() != kid.age) return false
    if (kid.skills.keys != TalesSkillCatalog.skillToAttribute.keys) return false
    if (kid.skills.values.sum() != 10) return false
    if (kid.skills.any { (skill, value) -> value !in 0..(if (skill in type.keySkills) 3 else 1) }) return false
    if (kid.luck != 15 - kid.age) return false
    return true
  }
}
