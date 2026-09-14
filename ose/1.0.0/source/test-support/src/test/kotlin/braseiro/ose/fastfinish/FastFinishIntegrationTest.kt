package braseiro.ose.fastfinish

import braseiro.ose.backup.BackupCodec
import braseiro.ose.barbara.BarbaraSupervisorPort
import braseiro.ose.character.CharacterCreator
import braseiro.ose.dungeon.DungeonExploration
import braseiro.ose.dungeon.DungeonGeneratorV1
import braseiro.ose.hex.GeographicCoherenceValidator
import braseiro.ose.hex.HexKnowledge
import braseiro.ose.hex.HexWorldGeneratorV1
import braseiro.ose.map.HexGeometryV1
import braseiro.ose.model.*
import braseiro.ose.npc.NpcDomain
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.rules.advanced.AdvancedCharacterRules
import braseiro.ose.rules.api.*
import braseiro.ose.rules.classic.ClassicCharacterRules
import braseiro.ose.rules.shared.SharedConfirmedRules
import braseiro.ose.session.SessionEngine
import braseiro.ose.settlement.SettlementPackages
import braseiro.ose.settlement.SettlementService
import braseiro.ose.world.WorldEventScheduler
import kotlin.test.*

class FastFinishIntegrationTest {
    private val attrs = Attributes(15,12,12,14,13,10)
    private val creator = CharacterCreator(ClassicCharacterRules(), AdvancedCharacterRules())

    @Test fun `wave3 classic and advanced profiles are isolated and legal`() {
        val classic = creator.create(CharacterCreationRequest(RuleProfile.OSE_CLASSIC_FANTASY,CreationMethod.CLASSIC,"c1","Alda",attrs,listOf("FIGHTER"),hpRolls=listOf(6)))
        val cc = assertIs<CharacterCreationResult.Created>(classic).character
        assertEquals(7, cc.hitPoints.maxNumerator) // d8 roll 6 + CON 13 modifier +1
        assertEquals("HUMAN", cc.raceId); assertEquals(19,cc.thac0); assertEquals(12,cc.savingThrows.deathPoison)
        assertTrue(cc.evidenceRefs.isNotEmpty())

        val basic = creator.create(CharacterCreationRequest(RuleProfile.OSE_ADVANCED_FANTASY,CreationMethod.ADVANCED_BASIC,"a1","Borin",attrs,listOf("FIGHTER"),hpRolls=listOf(5)))
        assertIs<CharacterCreationResult.Created>(basic)
        val adv = creator.create(CharacterCreationRequest(RuleProfile.OSE_ADVANCED_FANTASY,CreationMethod.ADVANCED_METHOD,"a2","Elian",attrs,listOf("FIGHTER"),raceId="ELF",hpRolls=listOf(5)))
        val ac = assertIs<CharacterCreationResult.Created>(adv).character
        assertEquals(15, ac.attributes.dex); assertEquals(12, ac.attributes.con)
        val mismatch = creator.create(CharacterCreationRequest(RuleProfile.OSE_CLASSIC_FANTASY,CreationMethod.ADVANCED_BASIC,"x","X",attrs,listOf("FIGHTER"),hpRolls=listOf(1)))
        assertIs<CharacterCreationResult.Rejected>(mismatch)
        val illegal = creator.create(CharacterCreationRequest(RuleProfile.OSE_ADVANCED_FANTASY,CreationMethod.ADVANCED_METHOD,"x2","X2",attrs,listOf("MAGIC_USER"),raceId="DWARF",hpRolls=listOf(1)))
        assertEquals(CreationFailureCode.RACE_CLASS_ILLEGAL, assertIs<CharacterCreationResult.Rejected>(illegal).code)
    }

    @Test fun `wave3 shared mechanics follow proven d20 and damage contracts`() {
        assertTrue(SharedConfirmedRules.attack(RuleProfile.OSE_CLASSIC_FANTASY,AttackInput(20,AttackArmorMode.DESCENDING,19,0,0)).hit)
        assertFalse(SharedConfirmedRules.attack(RuleProfile.OSE_CLASSIC_FANTASY,AttackInput(1,AttackArmorMode.DESCENDING,19,0,9)).hit)
        assertTrue(SharedConfirmedRules.savingThrow(RuleProfile.OSE_CLASSIC_FANTASY,12,12).success)
        assertTrue(SharedConfirmedRules.abilityCheck(RuleProfile.OSE_CLASSIC_FANTASY,1,3,99).success)
        assertFalse(SharedConfirmedRules.abilityCheck(RuleProfile.OSE_CLASSIC_FANTASY,20,18,-99).success)
        assertEquals(1,SharedConfirmedRules.basicPlayerDamage(RuleProfile.OSE_CLASSIC_FANTASY,1,-3).damage)
    }

    @Test fun `wave4 player reaction commits before Barbara and gm help is zero delta`() {
        val repo=MemoryRepo(baseCampaign())
        var observedTurn=-1L
        val barbara=object:BarbaraSupervisorPort{
            override fun narrate(committed:CampaignEnvelope,playerReaction:String,mechanicalFeedback:String,trace:RuleTrace):String { observedTurn=committed.campaignState.time.turns; return "Narrado após commit" }
            override fun help(readOnly:CampaignEnvelope,question:String)="Ajuda sem delta"
        }
        val engine=SessionEngine(repo,RulesRefereeBoundary(),barbara)
        val before=repo.current.campaignState.canonical()
        val result=engine.submitPlayerReaction(baseCampaign().campaignId,"WAIT_TURN")
        assertEquals(1,observedTurn); assertTrue(result.mechanicalMutation)
        assertEquals(1,repo.current.campaignState.time.turns)
        val afterAction=repo.current.campaignState.canonical()
        engine.gmHelp(baseCampaign().campaignId,"como funciona?")
        assertEquals(afterAction,repo.current.campaignState.canonical())
        assertNotEquals(before,afterAction)
        assertEquals("PLAYER_REACTION",repo.current.campaignState.game.actionLog.last().channel)
    }

    @Test fun `wave5 dungeon corpus deterministic reachable and secrets are not leaked`() {
        repeat(128){ i ->
            val a=DungeonGeneratorV1.generate("D$i",i.toULong(),18); val b=DungeonGeneratorV1.generate("D$i",i.toULong(),18)
            assertEquals(a,b); assertTrue(DungeonGeneratorV1.isReachable(a)); assertEquals(setOf(a.entranceNodeId),a.knownNodeIds)
        }
        val d=DungeonGeneratorV1.generate("D",42u,10)
        val firstEdge=d.edges.first{!it.secret && !d.nodes.first{n->n.nodeId==it.to}.secret}
        val state=baseCampaign().campaignState.copy(position=PositionState(SpatialRef.Dungeon("D",firstEdge.from)),game=baseCampaign().campaignState.game.copy(dungeon=d))
        val moved=assertIs<braseiro.ose.dungeon.DungeonMoveResult.Moved>(DungeonExploration.move(state,firstEdge.to))
        assertEquals(1,moved.state.time.turns)
    }

    @Test fun `wave6 geometry v1 and coherence pass deterministic corpus`() {
        assertEquals(1,HexGeometryV1.VERSION); assertEquals(112,HexGeometryV1.COLUMN_STEP_X); assertEquals(96,HexGeometryV1.ROW_STEP_Y); assertEquals(56,HexGeometryV1.ODD_ROW_OFFSET_X)
        val p0=HexGeometryV1.projectAxial(0,0); val p1=HexGeometryV1.projectAxial(0,1)
        assertEquals(56,p1.x-p0.x);assertEquals(96,p1.y-p0.y)
        for(profile in RuleProfile.entries) repeat(128){i->
            val a=HexWorldGeneratorV1.generate("W",i.toULong(),profile); val b=HexWorldGeneratorV1.generate("W",i.toULong(),profile)
            assertEquals(a,b); val report=GeographicCoherenceValidator.validate(a,profile); assertTrue(report.pass,"seed=$i profile=$profile errors=${report.errors}")
            if(profile==RuleProfile.OSE_CLASSIC_FANTASY) assertFalse(a.cells.any{it.terrain==TerrainKind.BARREN_ADV||it.terrain==TerrainKind.BROKEN_LANDS_ADV})
        }
        val w=HexWorldGeneratorV1.generate("W",7u,RuleProfile.OSE_CLASSIC_FANTASY)
        val revealed=HexKnowledge.reveal(w,0,0,1); assertTrue(HexKnowledge.inspect(revealed,0,0)!=null)
    }

    @Test fun `wave7 settlement is authored state driven and transitions canonical position`() {
        var state=baseCampaign().campaignState.copy(game=baseCampaign().campaignState.game.copy(settlement=SettlementPackages.canonicalVillage()))
        assertFails{SettlementService.enter(state,"TEMPLE")}
        state=SettlementService.discover(state,"TEMPLE"); state=SettlementService.enter(state,"TEMPLE")
        assertIs<SpatialRef.Settlement>(state.position.primary); assertEquals("TEMPLE",(state.position.primary as SpatialRef.Settlement).anchorId)
    }

    @Test fun `wave9 hundreds npc knowledge remains bounded and scheduler requires time advance`() {
        val npcs=(0 until 500).associate { i -> "N$i" to NpcDomain.createStable("N$i","NPC $i","INN",setOf("fact-$i")) }
        var game=GameExtensions(npcs=npcs,worldEvents=listOf(WorldEventSnapshot("E1",3,"MISSION","N42","missão avançou")))
        assertEquals(emptySet(),NpcDomain.projectPlausibleKnowledge(game,"N42",setOf("fact-41","secret-world")))
        val zero=WorldEventScheduler.advance(game,2,2);assertEquals(game,zero)
        game=WorldEventScheduler.advance(game,2,3);assertTrue(game.worldEvents.single().resolved);assertTrue(game.npcs.getValue("N42").memorySummaries.contains("missão avançou"))
    }

    @Test fun `wave10 backup checksum roundtrip preserves canonical campaign`() {
        val c=baseCampaign(); val encoded=BackupCodec.exportCampaign(c); val restored=BackupCodec.importCampaign(encoded)
        assertEquals(c.canonicalMechanical(),restored.canonicalMechanical())
        val tampered=encoded.replace("test","corrupt")
        assertFails{BackupCodec.importCampaign(tampered)}
    }

    private fun baseCampaign()=CampaignEnvelope(1,CampaignId("test"),"fixture",RuleProfile.OSE_CLASSIC_FANTASY,AuthorityRevision("classic","F3","hash"),OptionSet(),GeneratorRegistry(),AssetManifestRevision("manifest","v1","hash"),CampaignState(PartyState(),TimeState(),PositionState(SpatialRef.Scene("S","scene")),game=GameExtensions()))

    private class MemoryRepo(initial:CampaignEnvelope):CampaignRepository{
        var current=initial
        override fun create(envelope:CampaignEnvelope){current=envelope}
        override fun load(campaignId:CampaignId)=CampaignLoadResult.Loaded(current)
        override fun commit(campaignId:CampaignId,transition:StateTransition){current=current.copy(campaignState=transition.updatedState)}
        override fun checkpoint(campaignId:CampaignId,checkpointId:String){}
        override fun listCampaigns()=listOf(CampaignSummary(current.campaignId,false))
        override fun archive(campaignId:CampaignId){}
    }
}
