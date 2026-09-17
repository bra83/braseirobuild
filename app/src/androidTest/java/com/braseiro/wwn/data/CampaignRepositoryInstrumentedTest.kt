package com.braseiro.wwn.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampaignRepositoryInstrumentedTest {
    private lateinit var db: BraseiroDatabase
    private lateinit var repository: CampaignRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BraseiroDatabase::class.java).build()
        repository = CampaignRepository(db)
    }

    @After fun teardown() = db.close()

    private fun create() = repository.createCampaign(
        campaignId = "c1",
        displayName = "Fixture",
        stateJson = "{\"revision\":0}",
        masterSeed = "seed",
        initialSnapshotId = "s0",
        auditTimeMs = 1L,
    )

    @Test fun createLoadRoundTrip() {
        create()
        val load = repository.loadCampaign("c1")
        assertTrue(load is CampaignLoadResult.Ok)
        assertEquals(0L, (load as CampaignLoadResult.Ok).revision)
    }

    @Test fun revisionConflictDoesNotMutate() {
        create()
        runCatching {
            repository.commit(
                CommitRequest("c1", 9L, "e1", "test", "{}", "{\"revision\":1}"),
                2L,
            )
        }.onSuccess { throw AssertionError("Expected revision conflict") }
        assertEquals(0L, (repository.loadCampaign("c1") as CampaignLoadResult.Ok).revision)
    }

    @Test fun duplicateEventFailureRollsBackWholeCommit() {
        create()
        repository.commit(
            CommitRequest("c1", 0L, "e1", "test", "{}", "{\"revision\":1}"),
            2L,
        )
        runCatching {
            repository.commit(
                CommitRequest("c1", 1L, "e1", "test", "{}", "{\"revision\":2}"),
                3L,
            )
        }.onSuccess { throw AssertionError("Expected duplicate event failure") }
        val load = repository.loadCampaign("c1") as CampaignLoadResult.Ok
        assertEquals(1L, load.revision)
        assertEquals(1, db.dao().countEvents("c1"))
    }

    @Test fun campaignStateAndActiveSelectionSurviveDatabaseCloseAndReopen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "braseiro_wwn_reopen_gate.db"
        context.deleteDatabase(dbName)
        val campaignId = "reopen-campaign"
        val characterId = "pc:reopen-gate"
        val persistedState = """{
            "revision":1,
            "characterState":{"characterId":"$characterId","name":"Persistente"},
            "charactersState":{"$characterId":{"characterId":"$characterId","name":"Persistente"}},
            "partyState":{"activeGroupId":"group:default","groups":{"group:default":{"memberIds":["$characterId"]}}}
        }""".trimIndent().replace("\n", "")

        var fileDb = Room.databaseBuilder(context, BraseiroDatabase::class.java, dbName).build()
        try {
            val firstRepository = CampaignRepository(fileDb)
            firstRepository.createCampaign(
                campaignId = campaignId,
                displayName = "Reopen fixture",
                stateJson = "{\"revision\":0}",
                masterSeed = "reopen-seed",
                initialSnapshotId = "reopen-s0",
                auditTimeMs = 10L,
            )
            firstRepository.setActiveCampaignId(campaignId)
            firstRepository.commit(
                CommitRequest(
                    campaignId = campaignId,
                    expectedRevision = 0L,
                    eventId = "character-created:reopen",
                    eventKind = "RANDOM_CHARACTER_CREATED",
                    eventPayloadJson = "{\"characterId\":\"$characterId\"}",
                    newStateJson = persistedState,
                ),
                auditTimeMs = 11L,
            )
        } finally {
            fileDb.close()
        }

        fileDb = Room.databaseBuilder(context, BraseiroDatabase::class.java, dbName).build()
        try {
            val reopenedRepository = CampaignRepository(fileDb)
            assertEquals(campaignId, reopenedRepository.getActiveCampaignId())
            val reopened = reopenedRepository.loadCampaign(campaignId)
            assertTrue(reopened is CampaignLoadResult.Ok)
            val ok = reopened as CampaignLoadResult.Ok
            assertEquals(1L, ok.revision)
            assertTrue(ok.stateJson.contains("\"characterId\":\"$characterId\""))
            assertTrue(ok.stateJson.contains("\"memberIds\":[\"$characterId\"]"))
            assertEquals(1, fileDb.dao().countEvents(campaignId))
        } finally {
            fileDb.close()
            context.deleteDatabase(dbName)
        }
    }
}
