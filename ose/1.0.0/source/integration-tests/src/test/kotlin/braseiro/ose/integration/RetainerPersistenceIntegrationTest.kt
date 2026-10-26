package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.model.*
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

/** New retainer fields must survive JSON backup/restore without rewriting older world fields. */
class RetainerPersistenceIntegrationTest {
    @Test fun retainerContractLedgerAndWorldPersistThroughCanonicalBackup() {
        for(profile in RuleProfile.entries) {
            val original=Fixtures.campaign("retainer-persistence",profile)
            val npc=NpcSnapshot("npc-retainer","Yarra","world-1",
                SpatialRef.Hex("world-1",0,0),retainerLevel=1,retainerClassId="FIGHTER",
                reactionHistory=listOf("RETAINER_RECRUIT:ACCEPTED:10"),
                consequenceFlags=setOf("RETAINER_ACTIVE"))
            val contract=RetainerContractSnapshot(
                npcId="npc-retainer",employerCharacterId="char-a",
                dailyWageGp=1,treasureHalfShares=1,loyalty=8,hiredAtTurn=23L,
                hiredAtDay=2L,wagePaidThroughDay=5L,totalWagesPaidGp=3L,
                wageReceipts=listOf(RetainerWageReceipt("wage:2:5",2L,5L,3L,3L)))
            val ledger=XpRecipientSnapshot(
                recipientId="npc-retainer",profile=profile,
                retainer=true,awardEligible=true,classIds=listOf("FIGHTER"),
                primeModifierPercentByClass=mapOf("FIGHTER" to 0),
                xpByClass=mapOf("FIGHTER" to 25L))
            val state=original.campaignState.copy(
                time=TimeState(turns=23L,days=5L),
                game=GameExtensions(
                    npcs=mapOf(npc.npcId to npc),
                    retainerContracts=mapOf(npc.npcId to contract),
                    retainerOffendedLocations=setOf("old-town"),
                    experience=ExperienceRuntimeSnapshot(recipients=mapOf(npc.npcId to ledger)),
                    session=SessionSnapshot(phase="ACTIVE",visibleNarration="O grupo cruza as colinas."),
                    revision=31L
                ).canonical()
            )
            val envelope=original.copy(campaignState=state)
            val backup=BackupCodec.exportCampaign(envelope)
            val restored=BackupCodec.importCampaign(backup)
            assertEquals(CanonicalStateHash.sha256(envelope),CanonicalStateHash.sha256(restored))
            assertEquals(envelope.campaignState.time,restored.campaignState.time)
            assertEquals(contract,restored.campaignState.game.retainerContracts.getValue("npc-retainer"))
            assertEquals(npc,restored.campaignState.game.npcs.getValue("npc-retainer"))
            assertTrue("old-town" in restored.campaignState.game.retainerOffendedLocations)
            val after=restored.campaignState.game.experience.recipients.getValue("npc-retainer")
            assertTrue(after.retainer)
            assertTrue(after.awardEligible)
            assertEquals(25L,after.xpByClass.getValue("FIGHTER"))
        }
    }
}
