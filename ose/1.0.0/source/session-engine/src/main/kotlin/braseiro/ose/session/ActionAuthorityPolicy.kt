package braseiro.ose.session

/** A caller-supplied employer ID is not a delegation token. Validate before RNG or receipts. */
object ActionAuthorityPolicy {
    private val employerCommands = setOf(
        "RETAINER_RECRUIT", "RETAINER_WAGE_BASELINE", "RETAINER_PAY_DAILY"
    )
    private val administrativeCommands = setOf(
        "RETAINER_WAGE_BASELINE", "RETAINER_PAY_DAILY"
    )
    fun requireBoundActor(mechanicalIntent: String, actorCharacterId: String?) {
        if (mechanicalIntent.substringBefore(':') !in employerCommands) return
        require(!actorCharacterId.isNullOrBlank()) { "EMPLOYER_COMMAND_REQUIRES_CHARACTER_ACTION" }
        val employer = mechanicalIntent.substringAfter(':', "").substringBefore(':').trim()
        require(employer.isNotBlank() && employer == actorCharacterId) { "EMPLOYER_ACTOR_MISMATCH" }
    }
    fun triggersCompanionAutonomy(mechanicalIntent: String): Boolean =
        mechanicalIntent.substringBefore(':') !in administrativeCommands
}
