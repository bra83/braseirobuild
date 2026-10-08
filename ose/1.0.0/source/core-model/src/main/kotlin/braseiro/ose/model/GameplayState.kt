package braseiro.ose.model

import kotlinx.serialization.Serializable

@Serializable enum class AttributeKey { STR, INT, WIS, DEX, CON, CHA }
@Serializable data class Attributes(
    val str: Int, val int: Int, val wis: Int, val dex: Int, val con: Int, val cha: Int
) {
    init { values().forEach { require(it in 3..18) } }
    fun get(key: AttributeKey): Int = when (key) {
        AttributeKey.STR -> str; AttributeKey.INT -> int; AttributeKey.WIS -> wis
        AttributeKey.DEX -> dex; AttributeKey.CON -> con; AttributeKey.CHA -> cha
    }
    fun with(key: AttributeKey, value: Int): Attributes = when (key) {
        AttributeKey.STR -> copy(str=value); AttributeKey.INT -> copy(int=value); AttributeKey.WIS -> copy(wis=value)
        AttributeKey.DEX -> copy(dex=value); AttributeKey.CON -> copy(con=value); AttributeKey.CHA -> copy(cha=value)
    }
    fun values() = listOf(str,int,wis,dex,con,cha)
}

@Serializable data class SavingThrows(
    val deathPoison: Int,
    val wands: Int,
    val paralysisPetrification: Int,
    val breath: Int,
    val spellsRodsStaves: Int
)

@Serializable data class HitPoints(
    val currentNumerator: Int,
    val maxNumerator: Int,
    val denominator: Int = 1
) {
    init { require(denominator > 0 && maxNumerator >= 1 && currentNumerator <= maxNumerator) }
    fun damage(points: Int): HitPoints = copy(currentNumerator = currentNumerator - points * denominator)
    fun heal(points: Int): HitPoints = copy(currentNumerator = minOf(maxNumerator, currentNumerator + points * denominator))
    val dead: Boolean get() = currentNumerator <= 0
    fun display(): String = if (denominator == 1) "$currentNumerator" else "$currentNumerator/$denominator"
}

@Serializable data class CharacterSnapshot(
    val characterId: String,
    val name: String,
    val creationMethod: String,
    val raceId: String,
    val classIds: List<String>,
    val levelByClass: Map<String, Int>,
    val xpByClass: Map<String, Long>,
    val attributes: Attributes,
    val hitPoints: HitPoints,
    val armorClassDescending: Int,
    val armorClassAscending: Int,
    val thac0: Int,
    val attackBonusAscending: Int,
    val savingThrows: SavingThrows,
    val languages: List<String> = emptyList(),
    val flags: Set<String> = emptySet(),
    val evidenceRefs: List<String> = emptyList(),
    /** Frozen class-specific XP modifiers derived from canonical creation state. Missing entry = MISSING_EVIDENCE, never implicit 0. */
    val primeModifierPercentByClass: Map<String, Int> = emptyMap(),
    val weaponProficiencies: Set<String> = emptySet(),
    val weaponSpecialization: String? = null,
    val weaponProficiencySlotsTotal: Int = 0,
    val weaponProficiencySlotsSpent: Int = 0,
    val secondarySkills: List<String> = emptyList()
) {
    init {
        require(characterId.isNotBlank()); require(name.isNotBlank()); require(classIds.isNotEmpty())
        require(weaponProficiencySlotsTotal >= 0 && weaponProficiencySlotsSpent >= 0)
        require(weaponProficiencySlotsSpent <= weaponProficiencySlotsTotal)
    }
    fun canonical() = copy(
        classIds = classIds.distinct().sorted(),
        levelByClass = levelByClass.toSortedMap(),
        xpByClass = xpByClass.toSortedMap(),
        languages = languages.distinct().sorted(),
        flags = flags.toSortedSet(),
        evidenceRefs = evidenceRefs.distinct().sorted(),
        primeModifierPercentByClass = primeModifierPercentByClass.toSortedMap(),
        weaponProficiencies = weaponProficiencies.toSortedSet(),
        secondarySkills = secondarySkills.distinct().sorted()
    )
}

@Serializable data class CharacterCreationDraftSnapshot(
    val draftId: String,
    val profile: RuleProfile,
    val rolledAttributes: Attributes,
    val createdAtRevision: Long
) {
    init { require(draftId.isNotBlank()); require(createdAtRevision >= 0) }
}


@Serializable data class CharacterLoadoutSnapshot(
    val characterId: String,
    val startingWealthGp: Int,
    val remainingWealthGp: Int,
    val purchases: Map<String, Int> = emptyMap(),
    val completed: Boolean = false,
    val sourceRuleId: String
) {
    init {
        require(characterId.isNotBlank())
        require(startingWealthGp >= 0)
        require(remainingWealthGp in 0..startingWealthGp)
        require(purchases.values.all { it >= 0 })
        require(sourceRuleId.isNotBlank())
    }
    fun canonical() = copy(purchases = purchases.toSortedMap())
}

@Serializable data class ReactionReceiptSnapshot(
    val reactionId: String,
    val reactionText: String,
    val mechanicalFeedback: String,
    val mechanicalMutation: Boolean,
    val traceStatus: String,
    val traceNote: String = "",
    val traceEvidenceRefs: List<String> = emptyList(),
    val narration: String? = null
) {
    init { require(reactionId.isNotBlank()); require(reactionText.isNotBlank()) }
    fun canonical() = copy(traceEvidenceRefs = traceEvidenceRefs.distinct().sorted())
}

@Serializable data class SessionSnapshot(
    val phase: String = "PRESTART",
    val visibleNarration: String = "",
    val visibleMechanicalFeedback: String = "",
    val lastPlayerReaction: String = "",
    val journal: List<String> = emptyList(),
    val reactionReceipts: Map<String, ReactionReceiptSnapshot> = emptyMap(),
    val pendingNarrationReactionId: String? = null
) {
    fun canonical() = copy(
        reactionReceipts = reactionReceipts.toSortedMap().mapValues { it.value.canonical() }
    )
}

@Serializable data class DungeonNodeSnapshot(
    val nodeId: String,
    val kind: String,
    val secret: Boolean = false
)
@Serializable data class DungeonEdgeSnapshot(val from: String, val to: String, val kind: String = "PASSAGE", val secret: Boolean = false)
@Serializable data class DungeonSnapshot(
    val dungeonId: String,
    val generatorVersion: Int,
    val seed: ULong,
    val entranceNodeId: String,
    val nodes: List<DungeonNodeSnapshot>,
    val edges: List<DungeonEdgeSnapshot>,
    val knownNodeIds: Set<String> = setOf(),
    val visibleNodeIds: Set<String> = setOf()
) {
    fun canonical() = copy(
        nodes = nodes.sortedBy { it.nodeId },
        edges = edges.sortedWith(compareBy<DungeonEdgeSnapshot>({it.from},{it.to},{it.kind})),
        knownNodeIds = knownNodeIds.toSortedSet(),
        visibleNodeIds = visibleNodeIds.toSortedSet()
    )
}

@Serializable enum class TerrainKind { CLEAR_GRASSLANDS, HILLS, MOUNTAINS, FOREST, DESERT, SWAMP, JUNGLE, BARREN_ADV, BROKEN_LANDS_ADV }
@Serializable enum class HydrologyKind { NONE, LAKE_RIVER, SEA_OCEAN }
@Serializable data class HexCellSnapshot(
    val q: Int, val r: Int,
    val terrain: TerrainKind,
    val hydrology: HydrologyKind = HydrologyKind.NONE,
    val road: Boolean = false,
    val trail: Boolean = false,
    val poiId: String? = null,
    val known: Boolean = false,
    val visible: Boolean = false
)
@Serializable data class HexWorldSnapshot(
    val worldId: String,
    val generatorVersion: Int,
    val geometryVersion: Int,
    val seed: ULong,
    val width: Int,
    val height: Int,
    val cells: List<HexCellSnapshot>
) {
    fun canonical() = copy(cells = cells.sortedWith(compareBy<HexCellSnapshot>({it.r},{it.q})))
}

@Serializable data class SettlementLocationSnapshot(val locationId: String, val name: String, val discovered: Boolean = false, val tags: Set<String> = emptySet())
@Serializable data class SettlementSnapshot(
    val settlementId: String,
    val packageVersion: Int,
    val locations: List<SettlementLocationSnapshot>,
    val exits: Map<String, String> = emptyMap()
) {
    fun canonical() = copy(locations = locations.sortedBy { it.locationId }, exits = exits.toSortedMap())
}

@Serializable data class NpcSnapshot(
    val npcId: String,
    val name: String,
    /** Legacy persisted locator retained for migration compatibility; new runtime code uses spatialRef. */
    val locationRef: String,
    val spatialRef: SpatialRef? = null,
    val knownFactIds: Set<String> = emptySet(),
    val reactionHistory: List<String> = emptyList(),
    val memorySummaries: List<String> = emptyList(),
    val consequenceFlags: Set<String> = emptySet(),
    /** Explicitly sourced NPC adventurer level for retainer eligibility; null means not verified. */
    val retainerLevel: Int? = null,
    val retainerClassId: String? = null
) {
    init {
        require(retainerLevel == null || retainerLevel >= 0)
        require(retainerLevel == null || retainerLevel == 0 || !retainerClassId.isNullOrBlank())
    }
    fun canonical() = copy(
        knownFactIds = knownFactIds.toSortedSet(),
        reactionHistory = reactionHistory.toList(),
        memorySummaries = memorySummaries.toList(),
        consequenceFlags = consequenceFlags.toSortedSet()
    )
}

@Serializable data class WorldEventSnapshot(
    val eventId: String,
    val dueTurn: Long,
    val kind: String,
    val targetId: String,
    val payload: String,
    val resolved: Boolean = false
)

@Serializable data class ActionLogEntry(
    val actionId: String,
    val channel: String,
    val text: String,
    val ruleEvidenceRefs: List<String> = emptyList()
)


@Serializable data class RngStateSnapshot(
    val state: String,
    val increment: String,
    val counter: Long,
    val version: String
)



@Serializable enum class ArmorLoadCategory { NONE, LIGHT, HEAVY }
@Serializable enum class EncumbranceMode { NONE, BASIC, DETAILED }
@Serializable enum class LightSourceKind { TORCH, LANTERN }
@Serializable data class LightSourceSnapshot(
    val lightId: String,
    val kind: LightSourceKind,
    val carrierId: String,
    val remainingTurns: Int,
    val lit: Boolean = true,
    val sourceItemId: String? = null,
    val fuelItemId: String? = null
) {
    init { require(lightId.isNotBlank()); require(carrierId.isNotBlank()); require(remainingTurns >= 0) }
}

@Serializable data class ExpeditionSnapshot(
    val armorLoad: ArmorLoadCategory = ArmorLoadCategory.NONE,
    val treasureWeightCoins: Int = 0,
    val equipmentWeightCoins: Int = 0,
    val significantTreasure: Boolean = false,
    val encumbranceMode: EncumbranceMode = EncumbranceMode.BASIC,
    val baseMoveMeters: Int = 36,
    val encounterMoveMeters: Int = 12,
    val immobile: Boolean = false,
    val wildernessTravelDaysSinceRest: Int = 0,
    val wildernessRestOverdue: Boolean = false,
    val forcedMarchRestRequired: Boolean = false,
    val foodlessDays: Int = 0,
    val lastRationDeficit: Int = 0,
    val lightSources: List<LightSourceSnapshot> = emptyList(),
    val oilPools: List<OilPoolSnapshot> = emptyList()
) {
    init {
        require(treasureWeightCoins >= 0)
        require(equipmentWeightCoins >= 0)
        require(baseMoveMeters >= 0)
        require(encounterMoveMeters >= 0)
        require(wildernessTravelDaysSinceRest >= 0)
        require(foodlessDays >= 0)
        require(lastRationDeficit >= 0)
    }
    fun canonical() = copy(
        lightSources = lightSources.sortedBy { it.lightId },
        oilPools = oilPools.sortedBy { it.poolId }
    )
}

@Serializable enum class CombatActionPhase { DECLARATIONS, INITIATIVE, MORALE, MOVEMENT, MISSILE, SPELL, MELEE, COMPLETE }
@Serializable data class CombatRoundSnapshot(
    val roundNumber: Int,
    val phase: CombatActionPhase = CombatActionPhase.DECLARATIONS,
    val activeSide: EncounterSide? = null,
    val winningSide: EncounterSide? = null,
    val declarations: Map<String, String> = emptyMap(),
    val partyInitiative: Int? = null,
    val oppositionInitiative: Int? = null,
    val history: List<String> = emptyList(),
    val simultaneous: Boolean = false,
    val pendingEffects: List<PendingCombatEffectSnapshot> = emptyList(),
    val completedActionKeys: Set<String> = emptySet(),
    val movedActorKeys: Set<String> = emptySet(),
    val declaredSpellMemoryIds: Map<String, String> = emptyMap()
) {
    init { require(roundNumber >= 1) }
    fun canonical() = copy(
        declarations = declarations.toSortedMap(),
        pendingEffects = pendingEffects.sortedBy { it.effectId },
        completedActionKeys = completedActionKeys.toSortedSet(),
        movedActorKeys = movedActorKeys.toSortedSet(),
        declaredSpellMemoryIds = declaredSpellMemoryIds.toSortedMap()
    )
}

@Serializable enum class EncounterPhase {
    SURPRISE_PENDING,
    DISTANCE_PENDING,
    INITIATIVE_PENDING,
    ACTIONS,
    COMBAT,
    RESOLVED
}

@Serializable enum class EncounterDistanceContext { DUNGEON, WILDERNESS, WATERBORNE }

@Serializable enum class EncounterSide { PARTY, OPPOSITION }

@Serializable enum class ChasePhase { EVASION_PENDING, CHASE_ACTIVE, ESCAPED, CAUGHT }

@Serializable data class ChaseSnapshot(
    val context: EncounterDistanceContext,
    val fleeingSide: EncounterSide,
    val pursuingSide: EncounterSide,
    val phase: ChasePhase = ChasePhase.EVASION_PENDING,
    val fleeingSpeedMeters: Int,
    val pursuingSpeedMeters: Int,
    val fleeingCount: Int = 1,
    val pursuingCount: Int = 1,
    val evasionChancePercent: Int? = null,
    val lastEvasionRoll: Int? = null,
    val distanceMeters: Int? = null,
    val chaseRounds: Int = 0,
    val chaseDays: Int = 0,
    val exhausted: Boolean = false,
    val cannotBeReacquiredUntilNextDay: Boolean = false,
    val history: List<String> = emptyList()
) {
    init {
        require(fleeingSpeedMeters >= 0)
        require(pursuingSpeedMeters >= 0)
        require(fleeingCount >= 1)
        require(pursuingCount >= 1)
    }
}

@Serializable data class EncounterSnapshot(
    val encounterId: String,
    val phase: EncounterPhase = EncounterPhase.SURPRISE_PENDING,
    val partySurprised: Boolean? = null,
    val oppositionSurprised: Boolean? = null,
    val surpriseAdvantageSide: String? = null,
    val distanceContext: EncounterDistanceContext? = null,
    val distanceMeters: Int? = null,
    val reactionTotal: Int? = null,
    val reactionDisposition: String? = null,
    val initiativeParty: Int? = null,
    val initiativeOpposition: Int? = null,
    val initiativeOutcome: String? = null,
    val lastMoraleTotal: Int? = null,
    val lastMoraleHolds: Boolean? = null,
    val chase: ChaseSnapshot? = null,
    val combatRound: CombatRoundSnapshot? = null,
    val combatants: Map<String, CombatantSnapshot> = emptyMap(),
    val history: List<String> = emptyList()
) {
    init { require(encounterId.isNotBlank()) }
    fun canonical() = copy(
        combatRound = combatRound?.canonical(),
        combatants = combatants.toSortedMap()
    )
}

@Serializable data class GameExtensions(
    val characters: Map<String, CharacterSnapshot> = emptyMap(),
    val creationDrafts: Map<String, CharacterCreationDraftSnapshot> = emptyMap(),
    /** OSE retainers, distinct from mercenary military units and party PCs. */
    val retainerContracts: Map<String, RetainerContractSnapshot> = emptyMap(),
    /** Hiring a prospective adventurer can cause a -1 reaction penalty in the same area. */
    val retainerOffendedLocations: Set<String> = emptySet(),
    val loadouts: Map<String, CharacterLoadoutSnapshot> = emptyMap(),
    val session: SessionSnapshot = SessionSnapshot(),
    val encounter: EncounterSnapshot? = null,
    val dungeon: DungeonSnapshot? = null,
    val hexWorld: HexWorldSnapshot? = null,
    val settlement: SettlementSnapshot? = null,
    val expedition: ExpeditionSnapshot = ExpeditionSnapshot(),
    val inventory: InventorySnapshot = InventorySnapshot(),
    val magic: MagicRuntimeSnapshot = MagicRuntimeSnapshot(),
    val experience: ExperienceRuntimeSnapshot = ExperienceRuntimeSnapshot(),
    val npcs: Map<String, NpcSnapshot> = emptyMap(),
    val worldEvents: List<WorldEventSnapshot> = emptyList(),
    val livingWorld: LivingWorldSnapshot = LivingWorldSnapshot(),
    val actionLog: List<ActionLogEntry> = emptyList(),
    val rngRootSeed: String = "42",
    val rngStreams: Map<String, RngStateSnapshot> = emptyMap(),
    val revision: Long = 0
) {
    fun canonical() = copy(
        characters = characters.toSortedMap().mapValues { it.value.canonical() },
        creationDrafts = creationDrafts.toSortedMap(),
        retainerContracts = retainerContracts.toSortedMap(),
        retainerOffendedLocations = retainerOffendedLocations.toSortedSet(),
        loadouts = loadouts.toSortedMap().mapValues { it.value.canonical() },
        session = session.canonical(),
        encounter = encounter?.canonical(),
        dungeon = dungeon?.canonical(),
        hexWorld = hexWorld?.canonical(),
        settlement = settlement?.canonical(),
        expedition = expedition.canonical(),
        inventory = inventory.canonical(),
        magic = magic.canonical(),
        experience = experience.canonical(),
        npcs = npcs.toSortedMap().mapValues { it.value.canonical() },
        worldEvents = worldEvents.sortedWith(compareBy<WorldEventSnapshot>({it.dueTurn},{it.eventId})),
        actionLog = actionLog.toList(),
        rngStreams = rngStreams.toSortedMap()
    )
}
