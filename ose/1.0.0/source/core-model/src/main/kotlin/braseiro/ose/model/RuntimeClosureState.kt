package braseiro.ose.model

import kotlinx.serialization.Serializable

/**
 * Runtime-only canonical state needed by inventory/combat/magic closure.
 * Dataset adapters must bind every mechanical value from the active OSE profile.
 * The UI never supplies dice, weights, ranges, damage dice or spell mechanics.
 */
@Serializable
enum class InventoryItemKind {
    WEAPON, ARMOR, SHIELD, AMMUNITION, LIGHT_SOURCE, OIL_FLASK,
    COIN, GEM, JEWELRY, SCROLL, POTION, WAND, ROD, STAFF,
    ADVENTURING_GEAR, FOOD, OTHER
}

@Serializable
data class WeaponRuntimeSnapshot(
    val melee: Boolean = false,
    val missile: Boolean = false,
    val slow: Boolean = false,
    val damageDiceCount: Int = 1,
    val damageDieSides: Int = 6,
    val damageBonus: Int = 0,
    val shortRangeMeters: Int? = null,
    val mediumRangeMeters: Int? = null,
    val longRangeMeters: Int? = null
) {
    init {
        require(damageDiceCount >= 0)
        require(damageDieSides >= 1)
        require(shortRangeMeters == null || shortRangeMeters >= 0)
        require(mediumRangeMeters == null || mediumRangeMeters >= 0)
        require(longRangeMeters == null || longRangeMeters >= 0)
        if (missile) {
            require(shortRangeMeters != null && mediumRangeMeters != null && longRangeMeters != null)
            require(shortRangeMeters <= mediumRangeMeters && mediumRangeMeters <= longRangeMeters)
        }
    }
}

@Serializable
data class InventoryItemSnapshot(
    val itemId: String,
    val ownerId: String,
    val catalogId: String,
    val profile: RuleProfile,
    val sourceId: String,
    val kind: InventoryItemKind,
    val quantity: Int = 1,
    val carried: Boolean = true,
    val equipped: Boolean = false,
    /** Coin-weight per unit, bound from the active profile's item dataset. */
    val unitWeightCoins: Int = 0,
    val armorLoadCategory: ArmorLoadCategory? = null,
    val lightSourceKind: LightSourceKind? = null,
    val weapon: WeaponRuntimeSnapshot? = null
) {
    init {
        require(itemId.isNotBlank())
        require(ownerId.isNotBlank())
        require(catalogId.isNotBlank())
        require(sourceId.isNotBlank())
        require(quantity >= 0)
        require(unitWeightCoins >= 0)
        if (kind == InventoryItemKind.WEAPON) require(weapon != null)
    }

    fun canonical() = copy()
}

@Serializable
data class InventorySnapshot(
    val items: Map<String, InventoryItemSnapshot> = emptyMap(),
    /** Referee judgment persisted for BASIC encumbrance; never inferred from weight. */
    val basicSignificantTreasureOwners: Set<String> = emptySet()
) {
    fun canonical() = copy(
        items = items.toSortedMap().mapValues { it.value.canonical() },
        basicSignificantTreasureOwners = basicSignificantTreasureOwners.toSortedSet()
    )
}

@Serializable
data class OilPoolSnapshot(
    val poolId: String,
    val sourceItemId: String,
    val remainingTurns: Int = 1,
    val active: Boolean = true
) {
    init {
        require(poolId.isNotBlank())
        require(sourceItemId.isNotBlank())
        require(remainingTurns >= 0)
    }
}

@Serializable
data class MemorizedSpellSnapshot(
    val memoryId: String,
    val ownerId: String,
    val spellId: String,
    val spellLevel: Int,
    val profile: RuleProfile,
    val sourceId: String,
    val consumed: Boolean = false,
    val interrupted: Boolean = false
) {
    init {
        require(memoryId.isNotBlank())
        require(ownerId.isNotBlank())
        require(spellId.isNotBlank())
        require(spellLevel >= 1)
        require(sourceId.isNotBlank())
    }
}

@Serializable
data class SpellbookSnapshot(
    val ownerId: String,
    val profile: RuleProfile,
    val spellIds: Set<String> = emptySet(),
    val sourceId: String
) {
    init { require(ownerId.isNotBlank()); require(sourceId.isNotBlank()) }
    fun canonical() = copy(spellIds = spellIds.toSortedSet())
}

@Serializable
enum class SpellDurationUnit { ROUND, TURN, HOUR, DAY, PERMANENT, SPECIAL }

@Serializable
data class ActiveSpellEffectSnapshot(
    val effectId: String,
    val spellId: String,
    val sourceOwnerId: String,
    val targetIds: List<String> = emptyList(),
    val durationUnit: SpellDurationUnit,
    val remaining: Int? = null,
    val sourceId: String
) {
    init {
        require(effectId.isNotBlank())
        require(spellId.isNotBlank())
        require(sourceOwnerId.isNotBlank())
        require(sourceId.isNotBlank())
        require(remaining == null || remaining >= 0)
        if (durationUnit == SpellDurationUnit.PERMANENT) require(remaining == null)
    }
    fun canonical() = copy(targetIds = targetIds.sorted())
}

@Serializable
data class MagicRuntimeSnapshot(
    val memorized: Map<String, MemorizedSpellSnapshot> = emptyMap(),
    val spellbooks: Map<String, SpellbookSnapshot> = emptyMap(),
    /** Populated only by a concrete spell resolver with bound spell evidence. */
    val activeEffects: Map<String, ActiveSpellEffectSnapshot> = emptyMap()
) {
    fun canonical() = copy(
        memorized = memorized.toSortedMap(),
        spellbooks = spellbooks.toSortedMap().mapValues { it.value.canonical() },
        activeEffects = activeEffects.toSortedMap().mapValues { it.value.canonical() }
    )
}

@Serializable
data class CombatantSnapshot(
    val actorId: String,
    val side: EncounterSide,
    val characterId: String? = null,
    val hpCurrentNumerator: Int,
    val hpDenominator: Int = 1,
    val armorClassDescending: Int,
    val armorClassAscending: Int,
    val thac0: Int,
    val attackBonusAscending: Int,
    val meleeAttackModifier: Int = 0,
    val missileAttackModifier: Int = 0,
    val meleeDamageModifier: Int = 0,
    val encounterMoveMeters: Int = 12,
    val savingThrowTargets: Map<String, Int> = emptyMap(),
    /** Canonical status input set by rules/world adapters; null means casting is not known blocked. */
    val castingBlockedReason: String? = null,
    val alive: Boolean = true
) {
    init {
        require(actorId.isNotBlank())
        require(hpDenominator >= 1)
        require(encounterMoveMeters >= 0)
        require(savingThrowTargets.values.all { it >= 0 })
    }
}

@Serializable
data class PendingCombatEffectSnapshot(
    val effectId: String,
    val sourceActorId: String,
    val targetActorId: String,
    val sourceSide: EncounterSide,
    val phase: CombatActionPhase,
    /** Negative values are damage; positive values may be used by a concrete proven effect adapter. */
    val hpDeltaNumerator: Int,
    val causeId: String
) {
    init {
        require(effectId.isNotBlank())
        require(sourceActorId.isNotBlank())
        require(targetActorId.isNotBlank())
        require(causeId.isNotBlank())
    }
}

@Serializable
enum class TreasureLifecycle { FOUND, RECOVERED }

@Serializable
data class TreasureXpSnapshot(
    val treasureId: String,
    val profile: RuleProfile,
    val sourceId: String,
    val gpValue: Long,
    val magical: Boolean,
    val lifecycle: TreasureLifecycle = TreasureLifecycle.FOUND,
    val xpAwarded: Boolean = false
) {
    init {
        require(treasureId.isNotBlank())
        require(sourceId.isNotBlank())
        require(gpValue >= 0L)
    }
}

@Serializable
enum class MonsterDefeatOutcome { KILLED, TRICKED, CAPTURED, DRIVEN_OFF, OTHER_PROVEN }

@Serializable
data class DefeatedMonsterXpSnapshot(
    val eventId: String,
    val monsterContentId: String,
    val profile: RuleProfile,
    val sourceId: String,
    /** XP already bound from the active profile's monster/content record. Never supplied by UI. */
    val boundXp: Long,
    val outcome: MonsterDefeatOutcome,
    val xpAwarded: Boolean = false
) {
    init {
        require(eventId.isNotBlank())
        require(monsterContentId.isNotBlank())
        require(sourceId.isNotBlank())
        require(boundXp >= 0L)
    }
}

@Serializable
data class AppliedClassProgressionSnapshot(
    val classId: String,
    val level: Int,
    val xpThreshold: Long,
    val hitDiceExpression: String,
    val thac0: Int,
    val attackBonusAscending: Int,
    val savingThrows: Map<String, Int>,
    val spellSlots: List<Int> = emptyList(),
    val sourceId: String
) {
    init { require(classId.isNotBlank()); require(level >= 1); require(xpThreshold >= 0L); require(sourceId.isNotBlank()) }
    fun canonical() = copy(savingThrows = savingThrows.toSortedMap())
}

@Serializable
data class LevelUpRecordSnapshot(
    val levelUpId: String,
    val recipientId: String,
    val classId: String,
    val fromLevel: Int,
    val toLevel: Int,
    val hpGainNumerator: Int,
    val hpGainDenominator: Int = 1,
    val dieRolled: Int? = null,
    val sourceId: String,
    val sessionId: String
) {
    init {
        require(levelUpId.isNotBlank()); require(recipientId.isNotBlank()); require(classId.isNotBlank())
        require(fromLevel >= 1 && toLevel == fromLevel + 1); require(hpGainNumerator >= 0); require(hpGainDenominator > 0)
        require(sourceId.isNotBlank()); require(sessionId.isNotBlank())
    }
}

@Serializable
data class XpRecipientSnapshot(
    val recipientId: String,
    val profile: RuleProfile,
    val surviving: Boolean = true,
    val retainer: Boolean = false,
    val classIds: List<String>,
    /** Final class-specific prime-requisite percentage derived from canonical character state. */
    val primeModifierPercentByClass: Map<String, Int> = emptyMap(),
    val xpByClass: Map<String, Long> = emptyMap(),
    /** Canonical class levels; one entry per owned class. */
    val levelByClass: Map<String, Int> = classIds.associateWith { 1 },
    /** Final CON modifier from canonical character state; required only when a new HD is rolled. */
    val constitutionModifier: Int? = null,
    /** Exact row last applied from the profile-bound progression table. */
    val progressionByClass: Map<String, AppliedClassProgressionSnapshot> = emptyMap(),
    /** Fractional HP gains are retained here until the owning character-state adapter commits them. */
    val pendingHpGainNumerator: Int = 0,
    val pendingHpGainDenominator: Int = 1
) {
    init {
        require(recipientId.isNotBlank())
        require(classIds.isNotEmpty())
        require(classIds.all { it.isNotBlank() })
        require(xpByClass.values.all { it >= 0L })
        require(levelByClass.values.all { it >= 1 })
        require(pendingHpGainNumerator >= 0 && pendingHpGainDenominator > 0)
    }
    fun canonical() = copy(
        classIds = classIds.distinct().sorted(),
        primeModifierPercentByClass = primeModifierPercentByClass.toSortedMap(),
        xpByClass = xpByClass.toSortedMap(),
        levelByClass = levelByClass.toSortedMap(),
        progressionByClass = progressionByClass.toSortedMap().mapValues { it.value.canonical() }
    )
}

@Serializable
data class XpAwardRecordSnapshot(
    val awardId: String,
    val sourceEventIds: List<String>,
    val totalXpBeforeIndividualModifiers: Long,
    val deltasByRecipientClass: Map<String, Long>
) {
    init {
        require(awardId.isNotBlank())
        require(totalXpBeforeIndividualModifiers >= 0L)
        require(deltasByRecipientClass.values.all { it >= 0L })
    }
    fun canonical() = copy(
        sourceEventIds = sourceEventIds.distinct().sorted(),
        deltasByRecipientClass = deltasByRecipientClass.toSortedMap()
    )
}

@Serializable
data class ExperienceRuntimeSnapshot(
    val treasures: Map<String, TreasureXpSnapshot> = emptyMap(),
    val defeatedMonsters: Map<String, DefeatedMonsterXpSnapshot> = emptyMap(),
    val recipients: Map<String, XpRecipientSnapshot> = emptyMap(),
    val awards: List<XpAwardRecordSnapshot> = emptyList(),
    /** Stable campaign-session identity supplied by persistence/session lifecycle, never by player input. */
    val activeSessionId: String? = null,
    /** recipientId:classId keys that already advanced this session. */
    val leveledThisSession: Set<String> = emptySet(),
    val levelUps: List<LevelUpRecordSnapshot> = emptyList()
) {
    fun canonical() = copy(
        treasures = treasures.toSortedMap(),
        defeatedMonsters = defeatedMonsters.toSortedMap(),
        recipients = recipients.toSortedMap().mapValues { it.value.canonical() },
        awards = awards.map { it.canonical() }.sortedBy { it.awardId },
        leveledThisSession = leveledThisSession.toSortedSet(),
        levelUps = levelUps.sortedBy { it.levelUpId }
    )
}

