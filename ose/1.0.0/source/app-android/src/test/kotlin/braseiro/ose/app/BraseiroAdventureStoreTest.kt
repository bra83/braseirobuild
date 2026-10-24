package braseiro.ose.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class BraseiroAdventureStoreTest {

    private val validClassicJson = """
        {
          "schema": "braseiro.adventure/v1",
          "id": "cripta-teste-classic",
          "system": "ose-classic",
          "worldId": "mundo-teste",
          "title": "A Cripta de Teste",
          "start": { "locationId": "entrada" },
          "story": {
            "premise": "Explorar a cripta de teste",
            "background": "Construída para testes"
          },
          "locations": [
            { "id": "entrada", "name": "Entrada" }
          ],
          "enemies": [
            {
              "id": "esqueleto",
              "rulesRef": { "source": "Classic Rules Tome p. 120" }
            }
          ],
          "secrets": [
            {
              "id": "passagem-secreta",
              "visibility": "gm_only",
              "truth": "Atrás do altar de pedra"
            }
          ]
        }
    """.trimIndent()

    private val validAdvancedJson = """
        {
          "schema": "braseiro.adventure/v1",
          "id": "enclave-teste-adv",
          "system": "ose-advanced",
          "worldId": "mundo-teste",
          "title": "O Enclave de Teste",
          "start": { "locationId": "patio" },
          "story": {
            "premise": "Explorar o enclave avançado",
            "background": "Construído para testes avançados"
          },
          "locations": [
            { "id": "patio", "name": "Pátio" }
          ]
        }
    """.trimIndent()

    @Test
    fun validatesValidClassicAdventure() {
        val obj = BraseiroAdventureStore.validate(validClassicJson)
        assertEquals("cripta-teste-classic", obj.getString("id"))
        assertEquals("ose-classic", obj.getString("system"))
    }

    @Test
    fun validatesValidAdvancedAdventure() {
        val obj = BraseiroAdventureStore.validate(validAdvancedJson)
        assertEquals("enclave-teste-adv", obj.getString("id"))
        assertEquals("ose-advanced", obj.getString("system"))
    }

    @Test
    fun rejectsIncompatibleSystem() {
        val foreignSystem = validClassicJson.replace("\"system\": \"ose-classic\"", "\"system\": \"dnd5e-2024\"")
        try {
            BraseiroAdventureStore.validate(foreignSystem)
            fail("Should reject incompatible system")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
            assert(e.message!!.contains("INCOMPATIBLE_SYSTEM"))
        }
    }

    @Test
    fun rejectsEnemyWithoutRulesSource() {
        val missingSource = validClassicJson.replace("\"source\": \"Classic Rules Tome p. 120\"", "\"source\": \"\"")
        try {
            BraseiroAdventureStore.validate(missingSource)
            fail("Should reject enemy without rules source")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
            assert(e.message!!.contains("ENEMY_MISSING_RULES_SOURCE"))
        }
    }

    @Test
    fun rejectsSecretNonGmOnly() {
        val publicSecret = validClassicJson.replace("\"visibility\": \"gm_only\"", "\"visibility\": \"public\"")
        try {
            BraseiroAdventureStore.validate(publicSecret)
            fail("Should reject secret not marked gm_only")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
            assert(e.message!!.contains("SECRET_VISIBILITY_MUST_BE_GM_ONLY"))
        }
    }

    @Test
    fun rejectsEmptyLocations() {
        val emptyLoc = validClassicJson.replace(
            "\"locations\": [\n    { \"id\": \"entrada\", \"name\": \"Entrada\" }\n  ]",
            "\"locations\": []"
        )
        try {
            BraseiroAdventureStore.validate(emptyLoc)
            fail("Should reject empty locations")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
            assert(e.message!!.contains("EMPTY_LOCATIONS_ARRAY"))
        }
    }
}
