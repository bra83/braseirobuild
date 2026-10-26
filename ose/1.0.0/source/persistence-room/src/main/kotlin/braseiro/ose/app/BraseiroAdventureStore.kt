package braseiro.ose.app

import braseiro.ose.persistence.room.AdventureNpcEntity
import braseiro.ose.persistence.room.AdventurePackageEntity
import braseiro.ose.persistence.room.BraseiroOseDatabase
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores and manages validated braseiro.adventure/v1 adventure packages and NPC continuity
 * directly inside the canonical Room database (BraseiroOseDatabase).
 *
 * Implements the universal adventure contract:
 * - Adventures are transient content; characters, worlds, and timelines are persistent.
 * - Validation enforces schema, system compatibility, worldId matching, and start.locationId presence.
 * - GM-only secrets and background are strictly kept on the referee backend and NEVER leaked to presentation/WebView.
 * - Finishing or archiving an adventure preserves NPC continuity and past adventure history.
 */
class BraseiroAdventureStore(private val db: BraseiroOseDatabase) {

    private val lock = Any()

    companion object {
        const val SCHEMA_VERSION = "braseiro.adventure/v1"
        private val OSE_ALLOWED_SYSTEMS = setOf(
            "ose",
            "ose-classic",
            "ose-advanced",
            "old-school-essentials"
        )
        private val ID_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._:-]*$")

        fun validate(rawJson: String, expectedWorldId: String? = null): JSONObject {
            val root = runCatching { JSONObject(rawJson) }
                .getOrElse { throw IllegalArgumentException("INVALID_JSON: ${it.message}") }
            validatePackageInternal(root, expectedWorldId)
            return root
        }

        fun validatePackageInternal(pkg: JSONObject, expectedWorldId: String? = null) {
            val schema = pkg.optString("schema")
            require(schema == SCHEMA_VERSION) { "INVALID_SCHEMA: expected $SCHEMA_VERSION, got $schema" }

            val id = pkg.optString("id")
            require(id.isNotBlank() && ID_REGEX.matches(id)) { "INVALID_ADVENTURE_ID: $id" }

            val system = pkg.optString("system")
            require(system in OSE_ALLOWED_SYSTEMS) {
                "INCOMPATIBLE_SYSTEM: expected one of $OSE_ALLOWED_SYSTEMS, got $system"
            }

            val worldId = pkg.optString("worldId")
            require(worldId.isNotBlank() && ID_REGEX.matches(worldId)) { "INVALID_WORLD_ID: $worldId" }
            if (expectedWorldId != null && expectedWorldId.isNotBlank()) {
                require(worldId == expectedWorldId) {
                    "INCOMPATIBLE_WORLD: adventure belongs to world '$worldId', expected '$expectedWorldId'"
                }
            }

            val title = pkg.optString("title")
            require(title.isNotBlank()) { "EMPTY_ADVENTURE_TITLE" }

            val start = pkg.optJSONObject("start") ?: throw IllegalArgumentException("MISSING_START_SECTION")
            val startLocationId = start.optString("locationId")
            require(startLocationId.isNotBlank() && ID_REGEX.matches(startLocationId)) {
                "INVALID_START_LOCATION_ID: $startLocationId"
            }

            val story = pkg.optJSONObject("story") ?: throw IllegalArgumentException("MISSING_STORY_SECTION")
            require(story.optString("premise").isNotBlank()) { "EMPTY_STORY_PREMISE" }
            require(story.optString("background").isNotBlank()) { "EMPTY_STORY_BACKGROUND" }

            val locations = pkg.optJSONArray("locations")
            require(locations != null && locations.length() > 0) { "EMPTY_LOCATIONS_ARRAY" }
            val locationIds = mutableSetOf<String>()
            for (i in 0 until locations.length()) {
                val loc = locations.getJSONObject(i)
                val locId = loc.optString("id")
                require(locId.isNotBlank() && ID_REGEX.matches(locId)) { "INVALID_LOCATION_ID: $locId" }
                require(locationIds.add(locId)) { "DUPLICATE_LOCATION_ID: $locId" }
            }
            require(startLocationId in locationIds) {
                "START_LOCATION_NOT_FOUND: start.locationId '$startLocationId' does not match any locations[].id"
            }

            val npcs = pkg.optJSONArray("npcs")
            val npcIds = mutableSetOf<String>()
            if (npcs != null) {
                for (i in 0 until npcs.length()) {
                    val npc = npcs.getJSONObject(i)
                    val npcId = npc.optString("id")
                    require(npcId.isNotBlank() && ID_REGEX.matches(npcId)) { "INVALID_NPC_ID: $npcId" }
                    require(npcIds.add(npcId)) { "DUPLICATE_NPC_ID: $npcId" }
                }
            }

            val enemies = pkg.optJSONArray("enemies")
            val enemyIds = mutableSetOf<String>()
            if (enemies != null) {
                for (i in 0 until enemies.length()) {
                    val enemy = enemies.getJSONObject(i)
                    val enemyId = enemy.optString("id")
                    require(enemyId.isNotBlank() && ID_REGEX.matches(enemyId)) { "INVALID_ENEMY_ID: $enemyId" }
                    require(enemyIds.add(enemyId)) { "DUPLICATE_ENEMY_ID: $enemyId" }
                    val rulesRef = enemy.optJSONObject("rulesRef")
                        ?: throw IllegalArgumentException("ENEMY_MISSING_RULES_REF: $enemyId")
                    require(rulesRef.optString("source").isNotBlank()) {
                        "ENEMY_MISSING_RULES_SOURCE: $enemyId"
                    }
                }
            }

            val encounters = pkg.optJSONArray("encounters")
            if (encounters != null) {
                for (i in 0 until encounters.length()) {
                    val enc = encounters.getJSONObject(i)
                    val encId = enc.optString("id")
                    require(encId.isNotBlank() && ID_REGEX.matches(encId)) { "INVALID_ENCOUNTER_ID: $encId" }
                    val encLoc = enc.optString("locationId")
                    if (encLoc.isNotBlank()) {
                        require(encLoc in locationIds) { "ENCOUNTER_UNKNOWN_LOCATION: $encId -> $encLoc" }
                    }
                    val encEnemies = enc.optJSONArray("enemies")
                    if (encEnemies != null) {
                        for (j in 0 until encEnemies.length()) {
                            val eid = encEnemies.getString(j)
                            require(eid in enemyIds) { "ENCOUNTER_UNKNOWN_ENEMY: $encId -> $eid" }
                        }
                    }
                }
            }

            val secrets = pkg.optJSONArray("secrets")
            if (secrets != null) {
                for (i in 0 until secrets.length()) {
                    val sec = secrets.getJSONObject(i)
                    val secId = sec.optString("id")
                    require(secId.isNotBlank() && ID_REGEX.matches(secId)) { "INVALID_SECRET_ID: $secId" }
                    require(sec.optString("visibility") == "gm_only") {
                        "SECRET_VISIBILITY_MUST_BE_GM_ONLY: $secId"
                    }
                    require(sec.optString("truth").isNotBlank()) {
                        "EMPTY_SECRET_TRUTH: $secId"
                    }
                }
            }

            require(pkg.toString().length <= 2_000_000) { "ADVENTURE_SIZE_EXCEEDS_2MB" }
        }
    }

    /**
     * Validates and imports an adventure package into the Room database under the specified scope.
     * Transactionally archives any currently active adventure to history.
     */
    fun importJson(
        rawJson: String,
        scope: String = "default",
        expectedWorldId: String? = null,
        timelineId: String = "canonical"
    ): JSONObject = synchronized(lock) {
        val root = runCatching { JSONObject(rawJson) }
            .getOrElse { throw IllegalArgumentException("INVALID_JSON: ${it.message}") }

        validatePackageInternal(root, expectedWorldId)

        val adventureId = root.getString("id")
        val worldId = root.getString("worldId")
        val title = root.getString("title")
        val now = System.currentTimeMillis()
        val packageKey = "$scope:$worldId:$timelineId"

        db.runInTransaction {
            val dao = db.adventureDao()
            val existing = dao.findActivePackage(scope, worldId, timelineId)

            val history = if (existing != null && existing.activeJson.isNotBlank()) {
                val arr = runCatching { JSONArray(existing.historyJson) }.getOrDefault(JSONArray())
                val prior = runCatching { JSONObject(existing.activeJson) }.getOrNull()
                if (prior != null) {
                    arr.put(
                        JSONObject()
                            .put("id", prior.optString("id"))
                            .put("title", prior.optString("title"))
                            .put("archivedAt", now)
                    )
                }
                arr.toString()
            } else {
                existing?.historyJson ?: "[]"
            }

            // Upsert adventure package entity
            dao.savePackage(
                AdventurePackageEntity(
                    packageKey = packageKey,
                    scope = scope,
                    worldId = worldId,
                    timelineId = timelineId,
                    activeJson = root.toString(),
                    historyJson = history,
                    updatedAt = now
                )
            )

            // Upsert NPCs in ledger to preserve cross-adventure continuity
            val npcs = root.optJSONArray("npcs")
            if (npcs != null) {
                for (i in 0 until npcs.length()) {
                    val npc = npcs.getJSONObject(i)
                    val npcId = npc.getString("id")
                    dao.saveNpc(
                        AdventureNpcEntity(
                            scope = scope,
                            npcId = npcId,
                            dataJson = npc.toString()
                        )
                    )
                }
            }
        }

        return JSONObject()
            .put("ok", true)
            .put("adventureId", adventureId)
            .put("title", title)
            .put("worldId", worldId)
            .put("startLocationId", root.getJSONObject("start").getString("locationId"))
            .put("status", "ACTIVE")
    }

    /**
     * Finishes and archives the active adventure for the given scope, preserving NPC ledger.
     */
    fun finish(
        scope: String = "default",
        worldId: String = "world",
        timelineId: String = "canonical"
    ): JSONObject = synchronized(lock) {
        val packageKey = "$scope:$worldId:$timelineId"
        val now = System.currentTimeMillis()
        var archivedId: String? = null

        db.runInTransaction {
            val dao = db.adventureDao()
            val existing = dao.findActivePackage(scope, worldId, timelineId)
                ?: dao.findPackageByScope(scope)
            if (existing != null && existing.activeJson.isNotBlank()) {
                val prior = runCatching { JSONObject(existing.activeJson) }.getOrNull()
                archivedId = prior?.optString("id")
                val history = runCatching { JSONArray(existing.historyJson) }.getOrDefault(JSONArray())
                if (prior != null) {
                    history.put(
                        JSONObject()
                            .put("id", prior.optString("id"))
                            .put("title", prior.optString("title"))
                            .put("finishedAt", now)
                    )
                }
                dao.savePackage(
                    existing.copy(
                        activeJson = "",
                        historyJson = history.toString(),
                        updatedAt = now
                    )
                )
            }
        }

        val hadActiveAdventure = archivedId != null
        return JSONObject()
            .put("ok", hadActiveAdventure)
            .put("archivedAdventureId", archivedId ?: "none")
            .put("status", if (hadActiveAdventure) "FINISHED" else "NO_ACTIVE_ADVENTURE")
    }

    /**
     * Returns ONLY player-safe public adventure summary for presentation in the UI/WebView.
     * Strictly omits secrets, background, and GM fiction to prevent presentation leakage.
     */
    fun getActiveAdventureSummary(scope: String = "default"): JSONObject = synchronized(lock) {
        val dao = db.adventureDao()
        val existing = dao.findPackageByScope(scope)
        if (existing == null || existing.activeJson.isBlank()) {
            return JSONObject().put("active", false).put("message", "Nenhuma aventura importada ativa.")
        }
        val root = runCatching { JSONObject(existing.activeJson) }.getOrNull()
            ?: return JSONObject().put("active", false).put("message", "Aventura ativa corrompida.")

        val publicLocations = JSONArray()
        val locs = root.optJSONArray("locations")
        if (locs != null) {
            for (i in 0 until locs.length()) {
                val loc = locs.getJSONObject(i)
                publicLocations.put(
                    JSONObject()
                        .put("id", loc.optString("id"))
                        .put("name", loc.optString("name"))
                        .put("type", loc.optString("type"))
                        .put("description", loc.optString("description"))
                )
            }
        }

        return JSONObject()
            .put("active", true)
            .put("id", root.optString("id"))
            .put("system", root.optString("system"))
            .put("worldId", root.optString("worldId"))
            .put("title", root.optString("title"))
            .put("premise", root.optJSONObject("story")?.optString("premise") ?: "")
            .put("startLocationId", root.optJSONObject("start")?.optString("locationId") ?: "")
            .put("locations", publicLocations)
            .put("status", "ACTIVE")
    }

    /**
     * Internal referee master context. Contains GM background and secrets.
     * Strictly NOT for presentation/WebView transmission.
     */
    fun masterContext(scope: String = "default"): JSONObject = synchronized(lock) {
        val dao = db.adventureDao()
        val existing = dao.findPackageByScope(scope)
        if (existing == null || existing.activeJson.isBlank()) {
            return JSONObject().put("active", false)
        }
        val root = runCatching { JSONObject(existing.activeJson) }.getOrNull()
            ?: return JSONObject().put("active", false)

        val npcs = dao.listNpcs(scope)
        val npcsArr = JSONArray()
        for (n in npcs) {
            runCatching { JSONObject(n.dataJson) }.getOrNull()?.let { npcsArr.put(it) }
        }

        return JSONObject()
            .put("active", true)
            .put("adventure", root)
            .put("persistentNpcLedger", npcsArr)
    }

    /**
     * Formats the master context into a secure, non-executable block for the Gemini Barbara Supervisor.
     */
    fun masterContextPrompt(scope: String = "default"): String = synchronized(lock) {
        val ctx = masterContext(scope)
        if (!ctx.optBoolean("active", false)) {
            return ""
        }
        val adv = ctx.optJSONObject("adventure") ?: return ""
        val sb = StringBuilder()
        sb.append("\n<DADOS_DE_FICCAO_PRIVADOS_NAO_EXECUTAVEIS>\n")
        sb.append("Aventura Ativa: ${adv.optString("title")} (${adv.optString("system")})\n")
        sb.append("Mundo: ${adv.optString("worldId")}\n")

        val story = adv.optJSONObject("story")
        if (story != null) {
            sb.append("Premissa Pública: ${story.optString("premise")}\n")
            sb.append("Segredos/História de Fundo do Mestre (Privado): ${story.optString("background")}\n")
        }

        val secrets = adv.optJSONArray("secrets")
        if (secrets != null && secrets.length() > 0) {
            sb.append("\nSEGREDOS DO ÁRBITRO (NÃO REVELE DIRETAMENTE SEM INVESTIGAÇÃO/EVIDÊNCIA):\n")
            for (i in 0 until secrets.length()) {
                val sec = secrets.getJSONObject(i)
                sb.append("- [${sec.optString("id")}]: ${sec.optString("truth")}\n")
            }
        }

        val npcs = ctx.optJSONArray("persistentNpcLedger")
        if (npcs != null && npcs.length() > 0) {
            sb.append("\nNPCS REGISTRADOS E CONTINUIDADE:\n")
            for (i in 0 until npcs.length()) {
                val npc = npcs.getJSONObject(i)
                sb.append("- ${npc.optString("name")} (${npc.optString("role")}): ${npc.optString("personality")}\n")
            }
        }
        sb.append("</DADOS_DE_FICCAO_PRIVADOS_NAO_EXECUTAVEIS>\n")
        return sb.toString()
    }
}
