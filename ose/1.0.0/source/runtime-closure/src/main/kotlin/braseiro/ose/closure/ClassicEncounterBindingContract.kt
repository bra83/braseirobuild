package braseiro.ose.closure

/**
 * Repairs the sole Classic encounter identity left as MISSING_BINDING.
 * The wilderness table entry "Salteador" is not resolved by fuzzy monster-name matching:
 * Classic Rules Tome titles identify Salteador as the level-2 title of the Ladrão/Thief class.
 * Therefore the encounter binds to a classed NPC identity, not Bandido, Bandoleiro or Mosca Salteadora.
 */
data class ClassedNpcEncounterBinding(
    val encounterLabel: String,
    val classKey: String,
    val classRuleId: String,
    val level: Int,
    val encounterSourceLocation: String,
    val identitySourceLocation: String
)

object ClassicEncounterBindingContract {
    val salteador = ClassedNpcEncounterBinding(
        encounterLabel = "Salteador",
        classKey = "THIEF",
        classRuleId = "CLASSIC.CLASS.THIEF",
        level = 2,
        encounterSourceLocation = "Classic Rules Tome, Wilderness Secondary M, human column, rolls 10-11 (dataset source_pdf_page=222)",
        identitySourceLocation = "Classic Rules Tome p.36-37, level titles: Ladrão — Aprendiz, Salteador, ..."
    )

    fun exact(label: String, profile: OseProfile): ClassedNpcEncounterBinding? =
        if (profile == OseProfile.CLASSIC && label == salteador.encounterLabel) salteador else null

    /** Explicitly forbidden: no name similarity to monster records. */
    fun fuzzyMonsterFallback(label: String): Nothing? = null
}
