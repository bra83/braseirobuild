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
        val retainerSheet=employer.copy(
            characterId="npc-candidate",name="Yarra",
            attributes=employer.attributes.copy(cha=9),
            primeModifierPercentByClass=mapOf("FIGHTER" to 0)
        ).canonical()
        return CampaignEnvelope(
            campaignId=CampaignId("retainer-audit"),
            createdAtMetadata="test",ruleProfile=RuleProfile.OSE_CLASSIC_FANTASY,
            authorityRevision=AuthorityRevision("AUTH.CLASSIC","v1","test"),
            optionSet=OptionSet(), generatorRegistry=GeneratorRegistry(),
            assetManifestRevision=AssetManifestRevision("assets","v1","test"),
            campaignState=CampaignState(
                party=PartyState(listOf("hero")), time=TimeState(turns=25),
                position=PositionState(place), game=GameExtensions(
                    characters=mapOf("hero" to employer, "npc-candidate" to retainerSheet),
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
        val recipient=envelope.campaignState.game.experience.recipients.getValue("npc-candidate")
        assertTrue(recipient.retainer)
        assertTrue(recipient.awardEligible)
        assertEquals("FIGHTER",recipient.classIds.single())
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
        // Departed NPC survives; only future award eligibility changes.
        val recipient=current.campaignState.game.experience.recipients["npc-candidate"]
        if(recipient!=null){ assertTrue(recipient.surviving); assertFalse(recipient.awardEligible) }
    }

    @Test fun dailyWageSettlementTransfersCoinsAndRejectsReplayOrInsufficientFunds() {
        for(profile in RuleProfile.entries) {
            val old=fixture()
            val gold=InventoryItemSnapshot("hero-gold","hero","STARTING_GOLD_GP",profile,
                "OSE.EQUIPMENT.GP",InventoryItemKind.COIN,quantity=40,unitWeightCoins=1)
            val contract=RetainerContractSnapshot("npc-candidate","hero",3,1,8,
                hiredAtDay=2,wagePaidThroughDay=2)
            var env=old.copy(ruleProfile=profile,campaignState=old.campaignState.copy(
                time=TimeState(days=5,turns=25),game=old.campaignState.game.copy(
                    retainerContracts=mapOf(contract.npcId to contract),
                    inventory=InventorySnapshot(items=mapOf(gold.itemId to gold)))))
            val rng=NamedRngStreams.fromRootSeed(551u)
            val beforeRng=rng.snapshot()
            val result=assertNotNull(RuntimeClosureBindings.tryResolve(env,
                listOf("RETAINER_PAY_DAILY","hero","npc-candidate"),rng))
            assertTrue(result.mechanicalMutation)
            assertFalse(result.rngConsumed)
            assertEquals(beforeRng,rng.snapshot())
            assertEquals(env.campaignState.time,result.updatedState.time)
            val after=result.updatedState.game
            assertEquals(31,after.inventory.items.getValue("hero-gold").quantity)
            assertEquals(9,after.inventory.items.getValue("RETAINER_WAGES:npc-candidate:GP").quantity)
            val ledger=after.retainerContracts.getValue("npc-candidate")
            assertEquals(5L,ledger.wagePaidThroughDay)
            assertEquals(9L,ledger.totalWagesPaidGp)
            assertEquals(1,ledger.wageReceipts.size)
            assertEquals(3L,ledger.wageReceipts.single().daysPaid)
            env=env.copy(campaignState=result.updatedState)
            val replay=assertNotNull(RuntimeClosureBindings.tryResolve(env,
                listOf("RETAINER_PAY_DAILY","hero","npc-candidate"),rng))
            assertFalse(replay.mechanicalMutation)
            assertEquals(env.campaignState,replay.updatedState)
            val lowGold=after.inventory.copy(items=after.inventory.items.mapValues { (id,item) ->
                if(id=="hero-gold") item.copy(quantity=1) else item
            })
            val poor=env.copy(campaignState=env.campaignState.copy(
                time=TimeState(days=10),game=env.campaignState.game.copy(inventory=lowGold)))
            val blocked=assertNotNull(RuntimeClosureBindings.tryResolve(poor,
                listOf("RETAINER_PAY_DAILY","hero","npc-candidate"),rng))
            assertFalse(blocked.mechanicalMutation)
            assertEquals(poor.campaignState,blocked.updatedState)
            assertTrue(blocked.feedback.contains("não possui po suficientes"))
            assertEquals(beforeRng,rng.snapshot())
            val reversed=env.copy(campaignState=env.campaignState.copy(time=TimeState(days=4)))
            val invalid=assertNotNull(RuntimeClosureBindings.tryResolve(reversed,
                listOf("RETAINER_PAY_DAILY","hero","npc-candidate"),rng))
            assertFalse(invalid.mechanicalMutation)
            assertEquals(reversed.campaignState,invalid.updatedState)
            assertEquals(beforeRng,rng.snapshot())
        }
    }

    @Test fun legacySavesRequireExplicitBaselineAndNeverInventDebt() {
        val old=fixture()
        val legacy=RetainerContractSnapshot("npc-candidate","hero",3,1,8,hiredAtTurn=25)
        var env=old.copy(campaignState=old.campaignState.copy(time=TimeState(days=12),
            game=old.campaignState.game.copy(retainerContracts=mapOf(legacy.npcId to legacy))))
        val rng=NamedRngStreams.fromRootSeed(77u)
        val denied=assertNotNull(RuntimeClosureBindings.tryResolve(env,
            listOf("RETAINER_PAY_DAILY","hero","npc-candidate"),rng))
        assertFalse(denied.mechanicalMutation)
        assertTrue(denied.feedback.contains("LEGACY_WAGE_BASELINE"))
        val baseline=assertNotNull(RuntimeClosureBindings.tryResolve(env,
            listOf("RETAINER_WAGE_BASELINE","hero","npc-candidate"),rng))
        assertTrue(baseline.mechanicalMutation)
        assertEquals(env.campaignState.game.inventory,baseline.updatedState.game.inventory)
        val migrated=baseline.updatedState.game.retainerContracts.getValue("npc-candidate")
        assertEquals(12L,migrated.hiredAtDay)
        assertEquals(12L,migrated.wagePaidThroughDay)
        assertEquals(0L,migrated.totalWagesPaidGp)
        assertTrue(migrated.wageReceipts.isEmpty())
        env=env.copy(campaignState=baseline.updatedState)
        val repeat=assertNotNull(RuntimeClosureBindings.tryResolve(env,
            listOf("RETAINER_WAGE_BASELINE","hero","npc-candidate"),rng))
        assertFalse(repeat.mechanicalMutation)
    }
}
