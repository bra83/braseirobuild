package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.barbara.DeterministicBarbaraSupervisor
import braseiro.ose.character.CharacterCreator
import braseiro.ose.hex.HexKnowledge
import braseiro.ose.hex.HexWorldGeneratorV1
import braseiro.ose.model.*
import braseiro.ose.npc.NpcDomain
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.rules.api.*
import braseiro.ose.session.SessionEngine
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

class PlayerLifecycleLivingWorldTest {
    private class MemoryRepo : CampaignRepository {
        private val store = linkedMapOf<String, CampaignEnvelope>()
        override fun create(envelope: CampaignEnvelope) { check(store.putIfAbsent(envelope.campaignId.value,envelope)==null) }
        override fun load(campaignId: CampaignId): CampaignLoadResult =
            store[campaignId.value]?.let { CampaignLoadResult.Loaded(it) } ?: CampaignLoadResult.NotFound(campaignId)
        override fun replace(envelope: CampaignEnvelope) { check(store.containsKey(envelope.campaignId.value)); store[envelope.campaignId.value]=envelope }
        override fun commit(campaignId: CampaignId, transition: StateTransition) {
            val current=store[campaignId.value] ?: error("missing campaign")
            store[campaignId.value]=current.copy(campaignState=transition.updatedState)
        }
        override fun checkpoint(campaignId: CampaignId, checkpointId: String) { check(store.containsKey(campaignId.value)); check(checkpointId.isNotBlank()) }
        override fun listCampaigns(): List<CampaignSummary> = store.keys.sorted().map { CampaignSummary(CampaignId(it),false) }
        override fun archive(campaignId: CampaignId) { check(store.containsKey(campaignId.value)) }
    }

    @Test
    fun `character creation persistence xp level up and backup survive end to end`() {
        val creator=CharacterCreator()
        val created=creator.create(
            CharacterCreationRequest(
                profile=RuleProfile.OSE_CLASSIC_FANTASY,
                method=CreationMethod.CLASSIC,
                characterId="hero",
                name="Brena",
                rolledAttributes=Attributes(15,10,10,12,13,9),
                classIds=listOf("FIGHTER"),
                hpRolls=listOf(7)
            )
        )
        val character=assertIs<CharacterCreationResult.Created>(created).character
        assertEquals(1,character.levelByClass.getValue("FIGHTER"))
        assertTrue(character.primeModifierPercentByClass.containsKey("FIGHTER"))

        val base=Fixtures.campaign("lifecycle",RuleProfile.OSE_CLASSIC_FANTASY)
        val treasure=TreasureXpSnapshot(
            treasureId="treasure-level-2",
            profile=RuleProfile.OSE_CLASSIC_FANTASY,
            sourceId="TEST_CANONICAL_TREASURE",
            gpValue=3000,
            magical=false,
            lifecycle=TreasureLifecycle.RECOVERED
        )
        val envelope=base.copy(campaignState=base.campaignState.copy(
            party=PartyState(listOf("hero")),
            game=GameExtensions(
                characters=mapOf("hero" to character),
                session=SessionSnapshot(phase="ACTIVE"),
                experience=ExperienceRuntimeSnapshot(treasures=mapOf(treasure.treasureId to treasure)),
                rngRootSeed="777"
            )
        ))
        val repo=MemoryRepo()
        repo.create(envelope)
        val session=SessionEngine(repo,RulesRefereeBoundary(),DeterministicBarbaraSupervisor())

        val award=session.submitPlayerReaction(envelope.campaignId,"award-1","Distribuímos a experiência recuperada.","XP_AWARD")
        assertTrue(award.mechanicalMutation)
        val persistedAfterAward=assertIs<CampaignLoadResult.Loaded>(repo.load(envelope.campaignId)).envelope
        assertTrue(persistedAfterAward.campaignState.game.experience.awards.isNotEmpty())

        val level=session.submitPlayerReaction(envelope.campaignId,"level-1","Brena consolida a experiência e avança.","XP_LEVEL_UP:hero:FIGHTER")
        assertTrue(level.mechanicalMutation)
        val persisted=assertIs<CampaignLoadResult.Loaded>(repo.load(envelope.campaignId)).envelope
        val leveled=persisted.campaignState.game.characters.getValue("hero")
        assertEquals(2,leveled.levelByClass.getValue("FIGHTER"))
        assertTrue(leveled.hitPoints.maxNumerator >= character.hitPoints.maxNumerator)

        val backup=BackupCodec.exportCampaign(persisted)
        val restored=BackupCodec.importCampaign(backup)
        assertEquals(2,restored.campaignState.game.characters.getValue("hero").levelByClass.getValue("FIGHTER"))
        assertEquals(CanonicalStateHash.sha256(persisted),CanonicalStateHash.sha256(restored))
    }

    @Test
    fun `world npc weather and ration deficit advance automatically when canonical time advances`() {
        val base=Fixtures.campaign("living-world",RuleProfile.OSE_CLASSIC_FANTASY)
        val npc=NpcDomain.createStable(
            "npc-innkeeper","Estalajadeiro",
            SpatialRef.Settlement("SETTLEMENT_001","INN"),
            setOf("RUMOR_1")
        )
        val event=WorldEventSnapshot(
            eventId="event-rumor",
            dueTurn=6,
            kind="RUMOR",
            targetId=npc.npcId,
            payload="O rumor mudou enquanto a companhia estava fora."
        )
        val envelope=base.copy(campaignState=base.campaignState.copy(
            resources=ResourceState(),
            game=GameExtensions(
                session=SessionSnapshot(phase="ACTIVE"),
                npcs=mapOf(npc.npcId to npc),
                worldEvents=listOf(event),
                rngRootSeed="991"
            )
        ))
        val repo=MemoryRepo()
        repo.create(envelope)
        val session=SessionEngine(repo,RulesRefereeBoundary(),DeterministicBarbaraSupervisor())

        val result=session.submitPlayerReaction(envelope.campaignId,"rest-day","Descansamos um dia inteiro.","WILD_REST_DAY")
        assertTrue(result.mechanicalMutation)

        val persisted=assertIs<CampaignLoadResult.Loaded>(repo.load(envelope.campaignId)).envelope
        val state=persisted.campaignState
        assertEquals(144,state.time.turns)
        assertEquals(1,state.time.days)
        assertEquals(4,state.game.livingWorld.pulseCount)
        assertEquals(144,state.game.livingWorld.lastProcessedTurn)
        assertNotEquals(WeatherKind.CLEAR,state.game.livingWorld.weather.kind)
        assertEquals(1,state.game.livingWorld.weather.changedAtDay)

        val livingNpc=state.game.npcs.getValue(npc.npcId)
        assertTrue(livingNpc.reactionHistory.any { it.startsWith("WORLD_EVENT:event-rumor") })
        assertTrue(livingNpc.reactionHistory.any { it.startsWith("WORLD_PULSE:") })
        assertTrue(livingNpc.memorySummaries.any { it.contains("rumor",ignoreCase=true) })
        assertTrue("LIVING_WORLD_ACTIVE" in livingNpc.consequenceFlags)
        assertTrue(state.game.worldEvents.single().resolved)

        assertTrue(state.game.expedition.foodlessDays >= 1)
        assertTrue(state.game.expedition.lastRationDeficit >= 1)
        assertTrue(state.resources.resourceFacts.any { it.startsWith("ration_deficit:") && !it.endsWith(":0") })
    }

    @Test
    fun `fresh hexcrawl reveals only the starting hex`() {
        val world=HexWorldGeneratorV1.generate("world-fog",1234u,RuleProfile.OSE_CLASSIC_FANTASY)
        val revealed=HexKnowledge.reveal(world,3,2,0)
        assertEquals(1,revealed.cells.count { it.known })
        assertEquals(1,revealed.cells.count { it.visible })
        assertTrue(revealed.cells.first { it.q==3 && it.r==2 }.known)
    }
}
