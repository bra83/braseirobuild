package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.barbara.DeterministicBarbaraSupervisor
import braseiro.ose.character.CharacterCreator
import braseiro.ose.dungeon.DungeonGeneratorV1
import braseiro.ose.hex.GeographicCoherenceValidator
import braseiro.ose.hex.HexWorldGeneratorV1
import braseiro.ose.model.*
import braseiro.ose.npc.NpcDomain
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.rules.api.*
import braseiro.ose.session.SessionEngine
import braseiro.ose.settlement.SettlementPackages
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

class FinalClosureStressTest {
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
    fun `40 heavy sixty-day campaigns keep time world npc weather persistence and player reaction authority coherent`() {
        repeat(40) { campaignIndex ->
            val profile=if(campaignIndex%2==0) RuleProfile.OSE_CLASSIC_FANTASY else RuleProfile.OSE_ADVANCED_FANTASY
            val settlement=SettlementPackages.canonicalVillage("town-${campaignIndex}")
            val npcs=(0 until 30).associate { i ->
                val id="c${campaignIndex}-npc-${i}"
                id to NpcDomain.createStable(
                    id,
                    "NPC ${i}",
                    SpatialRef.Settlement(settlement.settlementId, if(i%2==0)"INN" else "GATE"),
                    setOf("public-${i}")
                )
            }
            val events=(0 until 120).map { i ->
                val target="c${campaignIndex}-npc-${i%30}"
                WorldEventSnapshot(
                    eventId="c${campaignIndex}-event-${i}",
                    dueTurn=((i%120)+1L)*36L,
                    kind="WORLD_STRESS",
                    targetId=target,
                    payload="Evento persistente ${i} da campanha ${campaignIndex}"
                )
            }
            val base=Fixtures.campaign("closure-${campaignIndex}",profile)
            val envelope=base.copy(campaignState=base.campaignState.copy(
                party=PartyState(),
                time=TimeState(),
                position=PositionState(SpatialRef.Hex("world-${campaignIndex}",0,0)),
                resources=ResourceState(listOf("rations:0","torch:0")),
                game=GameExtensions(
                    session=SessionSnapshot(phase="ACTIVE"),
                    settlement=settlement,
                    npcs=npcs,
                    worldEvents=events,
                    rngRootSeed=(9000+campaignIndex).toString()
                )
            ))
            val repo=MemoryRepo()
            repo.create(envelope)
            val session=SessionEngine(repo,RulesRefereeBoundary(),DeterministicBarbaraSupervisor())

            repeat(60) { day ->
                val id="day-${day}"
                val result=session.submitPlayerReaction(
                    envelope.campaignId,
                    id,
                    "A companhia descansa durante um dia completo.",
                    "WILD_REST_DAY"
                )
                assertTrue(result.mechanicalMutation)
                assertTrue(result.narration.isNotBlank())
                assertTrue(result.feedback.contains("Dia inteiro de descanso concluído"))
                if(day%10==0){
                    val before=CanonicalStateHash.sha256(session.loadSession(envelope.campaignId))
                    session.gmHelp(envelope.campaignId,"Qual é a situação observável atual?")
                    assertEquals(before,CanonicalStateHash.sha256(session.loadSession(envelope.campaignId)))
                }
            }

            val final=session.loadSession(envelope.campaignId)
            val state=final.campaignState
            assertEquals(60L,state.time.days)
            assertEquals(144L*60L,state.time.turns)
            assertEquals(24L*60L,state.time.hours)
            assertEquals(240L,state.game.livingWorld.pulseCount)
            assertEquals(144L*60L,state.game.livingWorld.lastProcessedTurn)
            assertEquals(60L,state.game.livingWorld.weather.sequence)
            assertEquals(60L,state.game.livingWorld.weather.changedAtDay)
            assertEquals(60,state.game.expedition.foodlessDays)
            assertTrue(state.game.expedition.lastRationDeficit>=1)
            assertTrue(state.resources.resourceFacts.any { it.startsWith("ration_deficit:") && !it.endsWith(":0") })
            assertEquals(60,state.game.actionLog.count { it.channel=="PLAYER_REACTION" })
            assertEquals(60,state.game.session.journal.size)
            assertEquals(60,state.game.session.reactionReceipts.size)
            assertTrue(state.game.worldEvents.all { it.resolved })

            state.game.npcs.values.forEach { npc ->
                assertTrue("LIVING_WORLD_ACTIVE" in npc.consequenceFlags)
                assertTrue(npc.reactionHistory.any { it.startsWith("WORLD_PULSE:") })
                assertTrue(npc.reactionHistory.any { it.startsWith("NPC_MOVE:") })
                assertTrue(npc.reactionHistory.size<=80)
                assertTrue(npc.memorySummaries.size<=20)
                assertIs<SpatialRef.Settlement>(npc.spatialRef)
            }

            val beforeRetry=CanonicalStateHash.sha256(final)
            val retry=session.submitPlayerReaction(
                envelope.campaignId,
                "day-0",
                "A companhia descansa durante um dia completo.",
                "WILD_REST_DAY"
            )
            assertTrue(retry.mechanicalMutation)
            assertEquals(beforeRetry,CanonicalStateHash.sha256(session.loadSession(envelope.campaignId)))

            val encoded=BackupCodec.exportCampaign(session.loadSession(envelope.campaignId))
            val restored=BackupCodec.importCampaign(encoded)
            assertEquals(CanonicalStateHash.sha256(session.loadSession(envelope.campaignId)),CanonicalStateHash.sha256(restored))
        }
    }

    @Test
    fun `character creation remains profile isolated across a large deterministic corpus`() {
        val creator=CharacterCreator()
        repeat(256) { i ->
            val classic=creator.create(CharacterCreationRequest(
                profile=RuleProfile.OSE_CLASSIC_FANTASY,
                method=CreationMethod.CLASSIC,
                characterId="classic-${i}",
                name="Classic ${i}",
                rolledAttributes=Attributes(15,9,9,13,12,10),
                classIds=listOf("FIGHTER"),
                hpRolls=listOf((i%8)+1)
            ))
            val c=assertIs<CharacterCreationResult.Created>(classic)
            assertEquals(listOf("FIGHTER"),c.character.classIds)
            assertEquals(1,c.character.levelByClass.getValue("FIGHTER"))
            assertEquals(EvidenceStatus.CANONICAL_PROCEDURE,c.trace.status)

            val advanced=creator.create(CharacterCreationRequest(
                profile=RuleProfile.OSE_ADVANCED_FANTASY,
                method=CreationMethod.ADVANCED_BASIC,
                characterId="advanced-${i}",
                name="Advanced ${i}",
                rolledAttributes=Attributes(13,15,12,13,12,10),
                classIds=listOf("MAGIC_USER"),
                hpRolls=listOf((i%4)+1)
            ))
            val a=assertIs<CharacterCreationResult.Created>(advanced)
            assertEquals(listOf("MAGIC_USER"),a.character.classIds)
            assertEquals(1,a.character.levelByClass.getValue("MAGIC_USER"))
            assertEquals(EvidenceStatus.CANONICAL_PROCEDURE,a.trace.status)
        }
    }

    @Test
    fun `large procedural corpus stays deterministic coherent reachable and profile clean`() {
        RuleProfile.entries.forEach { profile ->
            repeat(256) { seed ->
                val a=HexWorldGeneratorV1.generate("world-${profile}-${seed}",seed.toULong(),profile)
                val b=HexWorldGeneratorV1.generate("world-${profile}-${seed}",seed.toULong(),profile)
                assertEquals(a,b)
                val report=GeographicCoherenceValidator.validate(a,profile)
                assertTrue(report.pass,"seed=${seed} profile=${profile} errors=${report.errors}")
                if(profile==RuleProfile.OSE_CLASSIC_FANTASY){
                    assertFalse(a.cells.any { it.terrain==TerrainKind.BARREN_ADV || it.terrain==TerrainKind.BROKEN_LANDS_ADV })
                }
            }
        }
        repeat(256) { seed ->
            val a=DungeonGeneratorV1.generate("dungeon-${seed}",seed.toULong(),24)
            val b=DungeonGeneratorV1.generate("dungeon-${seed}",seed.toULong(),24)
            assertEquals(a,b)
            assertTrue(DungeonGeneratorV1.isReachable(a))
            assertEquals(setOf(a.entranceNodeId),a.knownNodeIds)
            assertEquals(setOf(a.entranceNodeId),a.visibleNodeIds)
        }
    }
}
