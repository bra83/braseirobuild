package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile

/**
 * Generated only from OSE_CLASSIC_SPELL_LIBRARY_V2_CORRECTED.json.
 * This is content binding, not a generic spell-effect engine.
 * Free-text mechanics remain non-executable until a spell-specific resolver is audited.
 */
enum class BoundSpellDurationKind { INSTANT, FIXED_ROUNDS, FIXED_TURNS, FIXED_HOURS, FIXED_DAYS, PERMANENT, SOURCE_FORMULA }

data class BoundSpellDuration(
    val kind: BoundSpellDurationKind,
    val amount: Int? = null,
    val sourceText: String
)

data class BoundSpellContentRecord(
    val contentId: String,
    val profile: RuleProfile,
    val tradition: String,
    val circle: Int,
    val name: String,
    val printedPage: Int,
    val rangeText: String,
    val duration: BoundSpellDuration,
    val reverseForms: List<String>,
    val diceExpressions: List<String>,
    val savingThrowMentions: List<String>,
    val refereeJudgmentSignal: Boolean,
    val sourceDatasetId: String,
    val sourceSha256: String,
    val executableEffectResolverId: String? = null
)

object ClassicSpellContentRuntimeRegistry {
    const val DATASET_ID = "ose.classic.spell_library.v2.corrected"
    const val SOURCE_SHA256 = "9b23bea5cb6d0b11ff72b7d6111cbf00a88c8054cd381e51d92f7438e586741d"

    private val records = listOf(
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Curar Ferimentos Leves",
            printedPage=76, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf("Infligir Ferimentos Leves"), diceExpressions=listOf("1d6+1"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Detectar Magia",
            printedPage=76, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Detectar o Mal",
            printedPage=76, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Luz",
            printedPage=76, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf("Escuridão"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Proteção Contra o Mal",
            printedPage=77, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção", "jogadas de proteção contra ataques ou habi-"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Purificar Comida e Água",
            printedPage=77, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Remover Medo",
            printedPage=77, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf("Causar Medo"), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C1_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=1, name="Resistir ao Frio",
            printedPage=77, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção contra ataques"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Benção",
            printedPage=78, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf("Mau Agouro"), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Descobrir Alinhamento",
            printedPage=78, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_ROUNDS, 1, "1 rodada"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Encantar Serpentes",
            printedPage=78, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "1d4+1 rodadas ou turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d4+1"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Encontrar Armadilhas",
            printedPage=79, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Falar com Animais",
            printedPage=79, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Imobilizar Pessoa",
            printedPage=79, rangeText="55 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 9, "9 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d4"),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Resistir ao Fogo",
            printedPage=79, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C2_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=2, name="Silêncio, 4,5 m de Raio",
            printedPage=79, rangeText="55 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Aumentar Animal",
            printedPage=80, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Curar Doença",
            printedPage=80, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf("Causar Doença"), diceExpressions=listOf("2d12"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Encantar Arma",
            printedPage=80, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 1, "1 turno"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Localizar Objeto",
            printedPage=80, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Luz Contínua",
            printedPage=81, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf("Escuridão Contínua"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C3_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=3, name="Remover Maldição",
            printedPage=81, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "instantânea / permanente (amaldiçoar)"),
            reverseForms=listOf("Amaldiçoar"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Bastões em Serpentes",
            printedPage=82, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d4", "2d8"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Criar Água",
            printedPage=82, rangeText="toque",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Curar Ferimentos Graves",
            printedPage=82, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf("Infligir Ferimentos Graves"), diceExpressions=listOf("2d6", "2d6+2"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Falar com Plantas",
            printedPage=82, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 3, "3 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Neutralizar Veneno",
            printedPage=83, rangeText="o conjurador ou uma criatura ou",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C4_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=4, name="Proteção Contra o Mal, 3 m de Raio",
            printedPage=83, rangeText="3 m ao redor do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Comungar",
            printedPage=84, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 3, "3 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Criar Comida",
            printedPage=84, rangeText="surge na presença do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Dissipar o Mal",
            printedPage=84, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração (até 1 turno) ou"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Missão",
            printedPage=84, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "até que a missão seja cumprida /"),
            reverseForms=listOf("Anular Missão"), diceExpressions=listOf(),
            savingThrowMentions=listOf("Jogada de proteção", "jogada de proteção contra feitiços"), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Praga de Insetos",
            printedPage=85, rangeText="146 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração (até 1 dia)"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_CLERIC_C5_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="CLERIC", circle=5, name="Reviver os Mortos",
            printedPage=85, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf("Dedo da Morte"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Detectar Magia",
            printedPage=88, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Disco Flutuante",
            printedPage=88, rangeText="1,8 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Encantar Pessoa",
            printedPage=88, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "um ou mais dias (veja abaixo)"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção", "jogadas de proteção contra feitiços com"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Escudo",
            printedPage=88, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Ler Idiomas",
            printedPage=88, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Ler Magia",
            printedPage=89, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 1, "1 turno"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Luz",
            printedPage=89, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "6 turnos +1 por nível"),
            reverseForms=listOf("Escuridão"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Mísseis Mágicos",
            printedPage=89, rangeText="45 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 1, "1 turno"),
            reverseForms=listOf(), diceExpressions=listOf("1d6+1"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Obstruir Porta",
            printedPage=89, rangeText="3m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "2d6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("2d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Proteção Contra o Mal",
            printedPage=90, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção", "jogadas de proteção contra ataques ou habi-"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Sono",
            printedPage=90, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "4d4 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("2d8", "4d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C1_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=1, name="Ventriloquismo",
            printedPage=90, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Arrombar",
            printedPage=91, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_ROUNDS, 1, "1 rodada"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Detectar Invisibilidade",
            printedPage=91, rangeText="3 m por nível",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Detectar o Mal",
            printedPage=91, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Fechadura Arcana",
            printedPage=91, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Força Fantasmagórica",
            printedPage=91, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração"),
            reverseForms=listOf(), diceExpressions=listOf("1d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Imagem Espelhada",
            printedPage=92, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Invisibilidade",
            printedPage=92, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "permanente (mas pode ser quebra-"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Levitar",
            printedPage=92, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "6 turnos +1 por nível"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Localizar Objeto",
            printedPage=92, rangeText="18 m +3 m por nível",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 2, "2 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Luz Contínua",
            printedPage=93, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf("Escuridão Contínua"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="PES - Percepção Extrassensorial",
            printedPage=93, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C2_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=2, name="Teia",
            printedPage=93, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 48, "48 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d6", "2d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Bola de Fogo",
            printedPage=94, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Celeridade",
            printedPage=94, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 3, "3 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Clarividência",
            printedPage=94, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Dissipar Magia",
            printedPage=94, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Imobilizar Pessoa",
            printedPage=94, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "1 turno por nível"),
            reverseForms=listOf(), diceExpressions=listOf("1d4"),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Infravisão",
            printedPage=94, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_DAYS, 1, "1 dia"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Invisibilidade, 3 m de Raio",
            printedPage=95, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "permanente (mas pode ser quebra-"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Proteção Contra o Mal, 3 m de Raio",
            printedPage=95, rangeText="3 m ao redor do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Proteção Contra Projéteis Mundanos",
            printedPage=95, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Relâmpago",
            printedPage=95, rangeText="55 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf("jogada de proteção contra feitiços"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Respirar Debaixo D’Água",
            printedPage=95, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_DAYS, 1, "1 dia"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C3_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=3, name="Voo",
            printedPage=95, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "1d6 turnos +1 por nível"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Aumentar Plantas",
            printedPage=98, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Confusão",
            printedPage=98, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_ROUNDS, 12, "12 rodadas"),
            reverseForms=listOf(), diceExpressions=listOf("2d6", "3d6"),
            savingThrowMentions=listOf("jogada de proteção contra feitiços", "jogadas de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Encantar Monstro",
            printedPage=98, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "um ou mais dias (veja abaixo)"),
            reverseForms=listOf(), diceExpressions=listOf("3d6"),
            savingThrowMentions=listOf("jogada de proteção", "jogadas de proteção contra feitiços com"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Metamorfose em Massa",
            printedPage=99, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Metamorfosear-se",
            printedPage=99, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "6 turnos +1 por nível"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogadas de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Metamorfosear Outros",
            printedPage=99, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços para anular"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Muralha de Fogo",
            printedPage=99, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Muralha de Gelo",
            printedPage=100, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Olho Arcano",
            printedPage=100, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Porta Dimensional",
            printedPage=100, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_ROUNDS, 1, "1 rodada"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços para resistir"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Remover Maldição",
            printedPage=100, rangeText="o conjurador ou uma criatura tocada",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "instantânea / permanente (amaldiçoar)"),
            reverseForms=listOf("Amaldiçoar"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C4_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=4, name="Terreno Ilusório",
            printedPage=101, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "até ser tocado"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Animar os Mortos",
            printedPage=101, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Conjurar Elemental",
            printedPage=102, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "permanente (até que seja banido ou"),
            reverseForms=listOf(), diceExpressions=listOf("1d8", "3d8"),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Contato Extraplanar",
            printedPage=103, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "uma conversa"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Criar Passagem",
            printedPage=103, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 3, "3 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Enfraquecer Intelecto",
            printedPage=104, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços (com uma"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Imobilizar Monstro",
            printedPage=104, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "6 turnos +1 por nível"),
            reverseForms=listOf(), diceExpressions=listOf("1d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Muralha de Pedra",
            printedPage=104, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Névoa Mortal",
            printedPage=104, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Recipiente Arcano",
            printedPage=104, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "especial"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra feitiços para"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Telecinesia",
            printedPage=105, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração (até 6 rodadas)"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Teleporte",
            printedPage=105, rangeText="3 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf("1d10"),
            savingThrowMentions=listOf("jogada de proteção contra feitiços para"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C5_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=5, name="Transmutar Pedra em Lama",
            printedPage=105, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "3d6 dias / permanente (transmutar"),
            reverseForms=listOf("Transmutar Lama em Pedra"), diceExpressions=listOf("3d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_01", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Caçador Invisível",
            printedPage=106, rangeText="invocado na presença do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "uma missão"),
            reverseForms=listOf(), diceExpressions=listOf("4d4"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_02", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Concha Anti-Magia",
            printedPage=106, rangeText="o conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 12, "12 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_03", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Controlar o Clima",
            printedPage=106, rangeText="220 m em volta do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "concentração"),
            reverseForms=listOf(), diceExpressions=listOf("1d8", "2d8"),
            savingThrowMentions=listOf("jogada de proteção"), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_04", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Desintegrar",
            printedPage=107, rangeText="18 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra morte para"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_05", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Dividir Águas",
            printedPage=107, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_06", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Feitiço da Morte",
            printedPage=107, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.INSTANT, null, "instantânea"),
            reverseForms=listOf(), diceExpressions=listOf("4d8"),
            savingThrowMentions=listOf("jogada de proteção contra morte ou mor-"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_07", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Mover Terra",
            printedPage=107, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_08", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Pedra em Carne",
            printedPage=107, rangeText="36 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf("Carne em Pedra"), diceExpressions=listOf(),
            savingThrowMentions=listOf("jogada de proteção contra paralisia"), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_09", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Projetar Imagem",
            printedPage=107, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 6, "6 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_10", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Reduzir Água",
            printedPage=108, rangeText="72 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.FIXED_TURNS, 10, "10 turnos"),
            reverseForms=listOf(), diceExpressions=listOf(),
            savingThrowMentions=listOf(), refereeJudgmentSignal=false,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_11", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Reencarnação",
            printedPage=108, rangeText="surge na presença do conjurador",
            duration=BoundSpellDuration(BoundSpellDurationKind.PERMANENT, null, "permanente"),
            reverseForms=listOf(), diceExpressions=listOf("1d6"),
            savingThrowMentions=listOf(), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        ),
        BoundSpellContentRecord(
            contentId="OSE_CLASSIC_SPELL_MAGIC_USER_C6_12", profile=RuleProfile.OSE_CLASSIC_FANTASY,
            tradition="MAGIC_USER", circle=6, name="Tarefa",
            printedPage=109, rangeText="9 m",
            duration=BoundSpellDuration(BoundSpellDurationKind.SOURCE_FORMULA, null, "permanente /"),
            reverseForms=listOf("Anular Tarefa"), diceExpressions=listOf(),
            savingThrowMentions=listOf("Jogada de proteção", "jogada de proteção", "jogada de proteção contra feitiços para evitar"), refereeJudgmentSignal=true,
            sourceDatasetId=DATASET_ID, sourceSha256=SOURCE_SHA256
        )
    ).associateBy { it.contentId }

    init {
        require(records.size == 106)
        require(records.keys.size == 106)
        require(records.values.count { it.tradition == "CLERIC" } == 34)
        require(records.values.count { it.tradition == "MAGIC_USER" } == 72)
        require(records.values.all { it.rangeText.isNotBlank() && it.duration.sourceText.isNotBlank() })
        require(records.values.all { it.executableEffectResolverId == null })
    }

    fun lookup(profile: RuleProfile, contentId: String, datasetId: String = DATASET_ID): BoundSpellContentRecord? {
        if (profile != RuleProfile.OSE_CLASSIC_FANTASY) return null
        if (datasetId != DATASET_ID) return null
        return records[contentId]
    }

    fun all(profile: RuleProfile, datasetId: String = DATASET_ID): List<BoundSpellContentRecord> =
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY && datasetId == DATASET_ID) records.values.sortedBy { it.contentId } else emptyList()

    fun canExecuteEffect(profile: RuleProfile, contentId: String, datasetId: String = DATASET_ID): Boolean =
        lookup(profile, contentId, datasetId)?.executableEffectResolverId != null
}
