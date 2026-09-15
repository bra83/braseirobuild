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
}
