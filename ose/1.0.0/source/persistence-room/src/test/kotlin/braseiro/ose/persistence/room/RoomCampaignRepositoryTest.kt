package braseiro.ose.persistence.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import braseiro.ose.model.*
import braseiro.ose.persistence.api.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RoomCampaignRepositoryTest {
    private lateinit var db: BraseiroOseDatabase
    private lateinit var repository: RoomCampaignRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BraseiroOseDatabase::class.java).allowMainThreadQueries().build()
        repository = RoomCampaignRepository(db)
    }

    @After fun close() { if (db.isOpen) db.close() }

    private fun fixture(id: String = "c1") = CampaignEnvelope(
        1,
        CampaignId(id),
        "2026-09-01T00:00:00Z",
        RuleProfile.OSE_CLASSIC_FANTASY,
        AuthorityRevision("AUTH.CLASSIC", "v1", "h"),
        OptionSet(),
        GeneratorRegistry(),
        AssetManifestRevision("manifest", "v1", "h"),
        CampaignState(PartyState(), TimeState(), PositionState(SpatialRef.Hex("world", 0, 0)))
    )

    @Test fun `create save load equality`() {
        val source = fixture()
        repository.create(source)
        val result = repository.load(source.campaignId)
        assertIs<CampaignLoadResult.Loaded>(result)
        assertEquals(source, result.envelope)
    }

    @Test fun `sequential commits preserve latest state and audit`() {
        val source = fixture()
        repository.create(source)
        repository.commit(source.campaignId, StateTransition("t1", source.campaignState.copy(time = TimeState(turns = 1))))
        repository.commit(source.campaignId, StateTransition("t2", source.campaignState.copy(time = TimeState(turns = 2))))
        val result = repository.load(source.campaignId) as CampaignLoadResult.Loaded
        assertEquals(2, result.envelope.campaignState.time.turns)
        assertEquals(2, db.campaignDao().auditCount(source.campaignId.value))
    }

    @Test fun `injected failure rolls back audit and update`() {
        val source = fixture()
        repository.create(source)
        val failing = RoomCampaignRepository(db, commitFailureInjector = { error("injected commit failure") })
        runCatching { failing.commit(source.campaignId, StateTransition("x", source.campaignState.copy(time = TimeState(turns = 1)))) }
        val result = repository.load(source.campaignId) as CampaignLoadResult.Loaded
        assertEquals(0, result.envelope.campaignState.time.turns)
        assertEquals(0, db.campaignDao().auditCount(source.campaignId.value))
    }

    @Test fun `read failure is typed and never creates blank campaign`() {
        val source = fixture()
        repository.create(source)
        val failing = RoomCampaignRepository(db, readFailureInjector = { error("injected read failure") })
        assertIs<CampaignLoadResult.ReadFailure>(failing.load(source.campaignId))
        val intact = repository.load(source.campaignId)
        assertIs<CampaignLoadResult.Loaded>(intact)
        assertEquals(source, intact.envelope)
        assertEquals(1, repository.listCampaigns().size)
    }

    @Test fun `file backed close reopen preserves last committed campaign`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "braseiro-recovery-test.db"
        if (db.isOpen) db.close()
        context.deleteDatabase(dbName)
        var fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbName).allowMainThreadQueries().build()
        var repo = RoomCampaignRepository(fileDb)
        val source = fixture("recover")
        repo.create(source)
        repo.commit(source.campaignId, StateTransition("commit-before-close", source.campaignState.copy(time = TimeState(turns = 5))))
        fileDb.close()
        fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbName).allowMainThreadQueries().build()
        repo = RoomCampaignRepository(fileDb)
        val loaded = repo.load(source.campaignId) as CampaignLoadResult.Loaded
        assertEquals(5, loaded.envelope.campaignState.time.turns)
        fileDb.close()
        context.deleteDatabase(dbName)
    }


    @Test fun `legacy runtime repair id may be followed by multiple new startup repairs without wiping save`() {
        val original = fixture("historical-ose")
        repository.create(original)
        val id = original.campaignId
        // This is the previously saved audit ID on the user's phone.
        repository.commit(id, StateTransition("runtime-repair-v2",
            original.campaignState.copy(time = TimeState(turns = 7))))
        val uniqueRepairA = "runtime-repair-v3-${java.util.UUID.randomUUID()}"
        repository.commit(id, StateTransition(uniqueRepairA,
            original.campaignState.copy(time = TimeState(turns = 8))))
        // Normal campaign progress must still persist between reopen repairs.
        repository.commit(id, StateTransition("player-progress-after-startup",
            original.campaignState.copy(time = TimeState(turns = 9))))
        val uniqueRepairB = "runtime-repair-v3-${java.util.UUID.randomUUID()}"
        repository.commit(id, StateTransition(uniqueRepairB,
            original.campaignState.copy(time = TimeState(turns = 10))))
        assertTrue(uniqueRepairA != uniqueRepairB)
        val loaded = repository.load(id) as CampaignLoadResult.Loaded
        assertEquals(10, loaded.envelope.campaignState.time.turns)
        assertEquals(original.campaignId, loaded.envelope.campaignId)
        assertEquals(4, db.campaignDao().auditCount(id.value))
        assertTrue(db.campaignDao().findTransitionAudit(id.value,"runtime-repair-v2") != null)
    }

    @Test fun `duplicate transition ids with different payloads are still rejected`() {
        val original = fixture("reject-collisions")
        repository.create(original)
        repository.commit(original.campaignId, StateTransition("user-action-1",
            original.campaignState.copy(time = TimeState(turns = 2))))
        val collision = runCatching {
            repository.commit(original.campaignId, StateTransition("user-action-1",
                original.campaignState.copy(time = TimeState(turns = 3))))
        }.exceptionOrNull()
        assertTrue(collision is IllegalArgumentException)
        assertTrue(collision?.message?.contains("TRANSITION_ID_COLLISION") == true)
        val saved = repository.load(original.campaignId) as CampaignLoadResult.Loaded
        assertEquals(2, saved.envelope.campaignState.time.turns)
        assertEquals(1, db.campaignDao().auditCount(original.campaignId.value))
    }

    @Test fun `missing remains not found`() {
        assertIs<CampaignLoadResult.NotFound>(repository.load(CampaignId("missing")))
        assertTrue(repository.listCampaigns().isEmpty())
    }
}
