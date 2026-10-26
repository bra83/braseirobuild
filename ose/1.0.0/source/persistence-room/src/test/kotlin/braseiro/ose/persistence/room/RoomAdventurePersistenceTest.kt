package braseiro.ose.persistence.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import braseiro.ose.app.BraseiroAdventureStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
class RoomAdventurePersistenceTest {

    private lateinit var context: Context
    private lateinit var db: BraseiroOseDatabase
    private lateinit var store: BraseiroAdventureStore

    private val classicAdventureJson = """
        {
          "schema": "braseiro.adventure/v1",
          "id": "cripta-teste-classic",
          "system": "ose-classic",
          "worldId": "mundo-primario",
          "title": "A Cripta de Teste",
          "start": { "locationId": "loc-entrada" },
          "story": {
            "premise": "Explorar a tumba esquecida.",
            "background": "Construída pelo antigo culto sombrio (SEGREDO)."
          },
          "locations": [
            { "id": "loc-entrada", "name": "Entrada de Pedra", "type": "ruin", "description": "Arco de pedra coberto de hera" },
            { "id": "loc-camara", "name": "Câmara Funerária", "type": "tomb", "description": "Sarcófagos de alabastro" }
          ],
          "npcs": [
            { "id": "npc-garrick", "name": "Garrick", "role": "guia", "personality": "cauteloso" }
          ],
          "enemies": [
            { "id": "esqueleto", "rulesRef": { "source": "Classic Rules Tome p. 120" } }
          ],
          "encounters": [
            { "id": "enc-01", "locationId": "loc-camara", "enemies": ["esqueleto"] }
          ],
          "secrets": [
            { "id": "sec-01", "visibility": "gm_only", "truth": "O tesouro real jaz sob o terceiro degrau" }
          ]
        }
    """.trimIndent()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, BraseiroOseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = BraseiroAdventureStore(db)
    }

    @After
    fun cleanup() {
        if (db.isOpen) db.close()
    }

    @Test
    fun importValidAdventureToRoomPersistence() {
        val res = store.importJson(classicAdventureJson, scope = "camp-01", expectedWorldId = "mundo-primario")
        assertEquals(true, res.getBoolean("ok"))
        assertEquals("cripta-teste-classic", res.getString("adventureId"))
        assertEquals("mundo-primario", res.getString("worldId"))

        // Public summary for UI must NOT leak background or secrets
        val pub = store.getActiveAdventureSummary("camp-01")
        assertEquals(true, pub.getBoolean("active"))
        assertEquals("A Cripta de Teste", pub.getString("title"))
        assertEquals("loc-entrada", pub.getString("startLocationId"))
        assertFalse(pub.has("background"), "Public summary must NEVER contain background")
        assertFalse(pub.has("secrets"), "Public summary must NEVER contain secrets")

        // Master prompt for Gemini Barbara must contain the private block and secrets
        val gmPrompt = store.masterContextPrompt("camp-01")
        assertTrue(gmPrompt.contains("<DADOS_DE_FICCAO_PRIVADOS_NAO_EXECUTAVEIS>"))
        assertTrue(gmPrompt.contains("Construída pelo antigo culto sombrio (SEGREDO)"))
        assertTrue(gmPrompt.contains("O tesouro real jaz sob o terceiro degrau"))
        assertTrue(gmPrompt.contains("Garrick"))
    }

    @Test
    fun rejectsIncompatibleWorld() {
        try {
            store.importJson(classicAdventureJson, scope = "camp-01", expectedWorldId = "outro-mundo-diferente")
            fail("Must reject when worldId does not match expected session worldId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("INCOMPATIBLE_WORLD"))
        }
    }

    @Test
    fun rejectsMismatchedStartLocation() {
        val badStart = classicAdventureJson.replace("\"locationId\": \"loc-entrada\"", "\"locationId\": \"loc-fantasma\"")
        try {
            store.importJson(badStart, scope = "camp-01")
            fail("Must reject when start.locationId does not exist in locations[]")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("START_LOCATION_NOT_FOUND"))
        }
    }

    @Test
    fun isolatesAdventuresByScope() {
        store.importJson(classicAdventureJson, scope = "scope-alfa")

        val pubAlfa = store.getActiveAdventureSummary("scope-alfa")
        assertTrue(pubAlfa.getBoolean("active"))

        val pubBeta = store.getActiveAdventureSummary("scope-beta")
        assertFalse(pubBeta.getBoolean("active"))
    }

    @Test
    fun finishesAndArchivesAdventurePreservingNpcContinuity() {
        store.importJson(classicAdventureJson, scope = "camp-01")
        val finishRes = store.finish(scope = "camp-01")
        assertEquals(true, finishRes.getBoolean("ok"))
        assertEquals("cripta-teste-classic", finishRes.getString("archivedAdventureId"))

        // Active summary is now empty
        val pub = store.getActiveAdventureSummary("camp-01")
        assertFalse(pub.getBoolean("active"))

        // NPCs remain preserved in Room ledger for future adventures
        val npcs = db.adventureDao().listNpcs("camp-01")
        assertEquals(1, npcs.size)
        assertEquals("npc-garrick", npcs[0].npcId)
    }

    @Test
    fun durablePersistenceAcrossDatabaseCrashAndReopen3x() {
        val dbFile = File(context.filesDir, "test_durable_adventures.db")
        if (dbFile.exists()) dbFile.delete()

        // Pass 1: Import adventure into file-backed DB
        var fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        var fileStore = BraseiroAdventureStore(fileDb)
        fileStore.importJson(classicAdventureJson, scope = "camp-crash-test")
        fileDb.close()

        // Pass 2: Reopen 1st time
        fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        fileStore = BraseiroAdventureStore(fileDb)
        var pub = fileStore.getActiveAdventureSummary("camp-crash-test")
        assertTrue(pub.getBoolean("active"))
        assertEquals("cripta-teste-classic", pub.getString("id"))
        fileDb.close()

        // Pass 3: Reopen 2nd time
        fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        fileStore = BraseiroAdventureStore(fileDb)
        pub = fileStore.getActiveAdventureSummary("camp-crash-test")
        assertTrue(pub.getBoolean("active"))
        assertEquals("A Cripta de Teste", pub.getString("title"))
        fileDb.close()

        // Pass 4: Reopen 3rd time
        fileDb = Room.databaseBuilder(context, BraseiroOseDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        fileStore = BraseiroAdventureStore(fileDb)
        val gmPrompt = fileStore.masterContextPrompt("camp-crash-test")
        assertTrue(gmPrompt.contains("O tesouro real jaz sob o terceiro degrau"))
        fileDb.close()

        dbFile.delete()
    }
}
