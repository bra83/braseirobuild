package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import kotlin.test.*

class RetainerCampaignFlowTest {
    private fun fixture(cha: Int = 18, level: Int? = 1): CampaignEnvelope {
        val employer=CharacterSnapshot(
            characterId="hero", name="Contratante", creationMethod="CLASSIC",
            raceId="HUMAN", classIds=listOf("FIGHTER"),
            levelByClass=mapOf("FIGHTER" to 1), xpByClass=mapOf("FIGHTER" to 0L),
            attributes=Attributes(15,10,10,12,13,cha),
            hitPoints=HitPoints(7,7), armorClassDescending=9,
            armorClassAscending=10, thac0=19, attackBonusAscending=0,
            savingThrows=SavingThrows(12,13,14,15,16)
        )
        val place=SpatialRef.Hex("campaign-world",0,0)
        val npc=NpcSnapshot("npc-candidate","Yarra","campaign-world",place,
            retainerLevel=level,retainerClassId=if(level==null||level==0)null else "FIGHTER")
        return CampaignEnvelope(
            campaignId=CampaignId("retainer-audit"),
            createdAtMetadata="test",ruleProfile=RuleProfile.OSE_CLASSIC_FANTASY,
            authorityRevision=AuthorityRevision("AUTH.CLASSIC","v1","test"),
            optionSet=OptionSet(), generatorRegistry=GeneratorRegistry(),
            assetManifestRevision=AssetManifestRevision("assets","v1","test"),
            campaignState=CampaignState(
                party=PartyState(listOf("hero")), time=TimeState(turns=25),
                position=PositionState(place), game=GameExtensions(
                    characters=mapOf("hero" to employer),
                    npcs=mapOf(npc.npcId to npc),rngRootSeed="4040",
                    session=SessionSnapshot(phase="ACTIVE",visibleNarration="A feira encontra-se agitada.")
                )
            )
        )
    }

    @Test fun recruitPersistsContractWithoutErasingWorldOrChangingTime() {
        val rng=NamedRngStreams.fromRootSeed(22u)
        var envelope=fixture()
        var accepted=false
        for(attempt in 0 until 64) {
            val resolved=assertNotNull(RuntimeClosureBindings.tryResolve(
                envelope,listOf("RETAINER_RECRUIT","hero","npc-candidate","1","1"),rng))
            assertTrue(resolved.mechanicalMutation,"a qualified offer must record the outcome")
            assertTrue(resolved.rngConsumed)
            val previous=envelope.campaignState
            envelope=envelope.copy(campaignState=resolved.updatedState)
            assertEquals(previous.time,envelope.campaignState.time)
            assertEquals(previous.position,envelope.campaignState.position)
            assertEquals(previous.game.session,envelope.campaignState.game.session)
            if("npc-candidate" in envelope.campaignState.game.retainerContracts) {accepted=true;break}
        }
        assertTrue(accepted,"repeated correctly-modified offers should eventually get acceptance")
        val contract=envelope.campaignState.game.retainerContracts.getValue("npc-candidate")
        assertTrue(contract.active)
        assertEquals("hero",contract.employerCharacterId)
        assertEquals(1,contract.dailyWageGp)
        assertEquals(1,contract.treasureHalfShares)
        assertTrue(contract.loyalty in 10..11)
        assertTrue(envelope.campaignState.game.npcs.getValue("npc-candidate")
            .reactionHistory.any { it.startsWith("RETAINER_RECRUIT:") })
        // Identity-gated repeats never reroll or create duplicate active contracts.
        val repeated=assertNotNull(RuntimeClosureBindings.tryResolve(
            envelope,listOf("RETAINER_RECRUIT","hero","npc-candidate","1","1"),rng))
        assertFalse(repeated.mechanicalMutation)
        assertEquals(envelope.campaignState,repeated.updatedState)
    }

    @Test fun refusingToInventNpcLevelNeverConsumesRngOrMutatesCampaign() {
        val rng=NamedRngStreams.fromRootSeed(81u)
        val env=fixture(level=null)
        val before=rng.snapshot()
        val result=assertNotNull(RuntimeClosureBindings.tryResolve(
            env,listOf("RETAINER_RECRUIT","hero","npc-candidate","1","1"),rng))
        assertFalse(result.mechanicalMutation)
        assertFalse(result.rngConsumed)
        assertEquals(env.campaignState,result.updatedState)
        assertEquals(before,rng.snapshot())
        assertTrue(result.feedback.contains("MISSING_EVIDENCE:NPC_RETAINER_LEVEL"))
    }

    @Test fun loyaltyFailureInDangerNeedsJudgmentButEndOfAdventureTerminatesService() {
        val base=fixture()
        val prior=RetainerContractSnapshot("npc-candidate","hero",1,1,2,hiredAtTurn=25)
        val initial=base.copy(campaignState=base.campaignState.copy(game=base.campaignState.game.copy(
            retainerContracts=mapOf("npc-candidate" to prior)
        )))
        val rng=NamedRngStreams.fromRootSeed(1234u)
        var current=initial
        val danger=assertNotNull(RuntimeClosureBindings.tryResolve(
            current,listOf("RETAINER_LOYALTY","npc-candidate","DANGER"),rng))
        assertTrue(danger.mechanicalMutation)
        current=current.copy(campaignState=danger.updatedState)
        assertTrue(current.campaignState.game.retainerContracts.getValue("npc-candidate").active)
        var departed=false
        for(i in 0 until 25) {
            val roll=assertNotNull(RuntimeClosureBindings.tryResolve(
                current,listOf("RETAINER_LOYALTY","npc-candidate","AFTER_ADVENTURE"),rng))
            if(!roll.mechanicalMutation) break
            current=current.copy(campaignState=roll.updatedState)
            if(!current.campaignState.game.retainerContracts.getValue("npc-candidate").active) {departed=true;break}
        }
        assertTrue(departed)
        assertEquals(25L,current.campaignState.time.turns)
        assertEquals("A feira encontra-se agitada.",current.campaignState.game.session.visibleNarration)
        assertTrue("RETAINER_DEPARTED" in current.campaignState.game.npcs.getValue("npc-candidate").consequenceFlags)
    }
}
