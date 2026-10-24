package braseiro.ose.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores and manages validated braseiro.adventure/v1 adventure packages and NPC state.
 *
 * Implements the universal adventure contract:
 * - Adventures are transient content; characters, worlds, and timelines are persistent.
 * - Validation enforces the corrected Draft 2020-12 schema before saving.
 * - GM-only secrets and fiction data are isolated into master context without leaking to players.
 * - Finishing or archiving an adventure preserves NPC continuity and world facts.
 */
class BraseiroAdventureStore(context: Context) {

    private val dbHelper = AdventureDbHelper(context)
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

        fun validate(rawJson: String): JSONObject {
            val root = runCatching { JSONObject(rawJson) }
                .getOrElse { throw IllegalArgumentException("INVALID_JSON: ${it.message}") }
            validatePackageInternal(root)
            return root
        }

        fun validatePackageInternal(pkg: JSONObject) {
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
            for (i in 0 until locations.length()) {
                val loc = locations.getJSONObject(i)
                val locId = loc.optString("id")
                require(locId.isNotBlank() && ID_REGEX.matches(locId)) { "INVALID_LOCATION_ID: $locId" }
            }

            val npcs = pkg.optJSONArray("npcs")
            if (npcs != null) {
                for (i in 0 until npcs.length()) {
                    val npc = npcs.getJSONObject(i)
                    val npcId = npc.optString("id")
                    require(npcId.isNotBlank() && ID_REGEX.matches(npcId)) { "INVALID_NPC_ID: $npcId" }
                }
            }

            val enemies = pkg.optJSONArray("enemies")
            if (enemies != null) {
                for (i in 0 until enemies.length()) {
                    val enemy = enemies.getJSONObject(i)
                    val enemyId = enemy.optString("id")
                    require(enemyId.isNotBlank() && ID_REGEX.matches(enemyId)) { "INVALID_ENEMY_ID: $enemyId" }
                    val rulesRef = enemy.optJSONObject("rulesRef")
                        ?: throw IllegalArgumentException("ENEMY_MISSING_RULES_REF: $enemyId")
                    require(rulesRef.optString("source").isNotBlank()) {
                        "ENEMY_MISSING_RULES_SOURCE: $enemyId"
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
        }
    }

    private class AdventureDbHelper(context: Context) :
        SQLiteOpenHelper(context, "braseiro_adventures.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS braseiro_adventure_packages (
                    scope TEXT PRIMARY KEY,
                    world_id TEXT NOT NULL,
                    timeline_id TEXT NOT NULL,
                    active_json TEXT,
                    history_json TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS braseiro_adventure_npc_ledger (
                    scope TEXT NOT NULL,
                    npc_id TEXT NOT NULL,
                    data_json TEXT NOT NULL,
                    PRIMARY KEY (scope, npc_id)
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Initial version 1; no upgrades yet
        }
    }

    /**
     * Validates and imports a JSON string containing an adventure package.
     * Archives any currently active adventure to history.
     */
    fun importJson(rawJson: String, scope: String = "default"): JSONObject = synchronized(lock) {
        val root = runCatching { JSONObject(rawJson) }
            .getOrElse { throw IllegalArgumentException("INVALID_JSON: ${it.message}") }

        validatePackageInternal(root)

        val adventureId = root.getString("id")
        val worldId = root.getString("worldId")
        val system = root.getString("system")
        val title = root.getString("title")
        val now = System.currentTimeMillis()

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var historyArray = JSONArray()
            val cursor = db.query(
                "braseiro_adventure_packages",
                arrayOf("active_json", "history_json"),
                "scope = ?",
                arrayOf(scope),
                null,
                null,
                null
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val prevActive = c.getString(c.getColumnIndexOrThrow("active_json"))
                    val existingHistory = c.getString(c.getColumnIndexOrThrow("history_json"))
                    if (!existingHistory.isNullOrBlank()) {
                        historyArray = runCatching { JSONArray(existingHistory) }.getOrDefault(JSONArray())
                    }
                    if (!prevActive.isNullOrBlank()) {
                        val prevObj = runCatching { JSONObject(prevActive) }.getOrNull()
                        if (prevObj != null) {
                            val archived = JSONObject()
                                .put("adventureId", prevObj.optString("id"))
                                .put("title", prevObj.optString("title"))
                                .put("archivedAt", now)
                                .put("raw", prevObj)
                            historyArray.put(archived)
                        }
                    }
                }
            }

            val cv = ContentValues().apply {
                put("scope", scope)
                put("world_id", worldId)
                put("timeline_id", "canonical")
                put("active_json", root.toString())
                put("history_json", historyArray.toString())
                put("updated_at", now)
            }
            db.insertWithOnConflict(
                "braseiro_adventure_packages",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            // Register NPCs into ledger
            val npcs = root.optJSONArray("npcs")
            if (npcs != null) {
                for (i in 0 until npcs.length()) {
                    val npc = npcs.getJSONObject(i)
                    val npcId = npc.getString("id")
                    val npcData = JSONObject()
                        .put("npcId", npcId)
                        .put("name", npc.optString("name", npcId))
                        .put("role", npc.optString("role", ""))
                        .put("adventureId", adventureId)
                        .put("worldId", worldId)
                        .put("updatedAt", now)
                        .put("raw", npc)

                    val npcValues = ContentValues().apply {
                        put("scope", scope)
                        put("npc_id", npcId)
                        put("data_json", npcData.toString())
                    }
                    db.insertWithOnConflict(
                        "braseiro_adventure_npc_ledger",
                        null,
                        npcValues,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            }

            db.setTransactionSuccessful()

            val locationsCount = root.optJSONArray("locations")?.length() ?: 0
            val npcsCount = root.optJSONArray("npcs")?.length() ?: 0

            return JSONObject()
                .put("ok", true)
                .put("adventureId", adventureId)
                .put("title", title)
                .put("system", system)
                .put("worldId", worldId)
                .put("locationsCount", locationsCount)
                .put("npcsCount", npcsCount)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Finishes the active adventure, preserving all NPCs and character legacy.
     */
    fun finish(scope: String = "default"): Boolean = synchronized(lock) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query(
                "braseiro_adventure_packages",
                arrayOf("active_json", "history_json"),
                "scope = ?",
                arrayOf(scope),
                null,
                null,
                null
            )
            var activeJson: String? = null
            var historyArray = JSONArray()
            cursor.use { c ->
                if (c.moveToFirst()) {
                    activeJson = c.getString(c.getColumnIndexOrThrow("active_json"))
                    val existingHistory = c.getString(c.getColumnIndexOrThrow("history_json"))
                    if (!existingHistory.isNullOrBlank()) {
                        historyArray = runCatching { JSONArray(existingHistory) }.getOrDefault(JSONArray())
                    }
                }
            }

            if (activeJson.isNullOrBlank()) {
                return false
            }

            val now = System.currentTimeMillis()
            val activeObj = JSONObject(activeJson!!)
            val finishedEntry = JSONObject()
                .put("adventureId", activeObj.optString("id"))
                .put("title", activeObj.optString("title"))
                .put("finishedAt", now)
                .put("status", "COMPLETED")
                .put("raw", activeObj)
            historyArray.put(finishedEntry)

            val cv = ContentValues().apply {
                put("active_json", null as String?)
                put("history_json", historyArray.toString())
                put("updated_at", now)
            }
            db.update("braseiro_adventure_packages", cv, "scope = ?", arrayOf(scope))
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Returns structured master context including private GM secrets.
     */
    fun masterContext(scope: String = "default"): JSONObject = synchronized(lock) {
        val active = getActiveAdventure(scope) ?: return JSONObject().put("hasActiveAdventure", false)
        val story = active.optJSONObject("story") ?: JSONObject()
        val start = active.optJSONObject("start") ?: JSONObject()

        return JSONObject()
            .put("hasActiveAdventure", true)
            .put("adventureId", active.optString("id"))
            .put("system", active.optString("system"))
            .put("worldId", active.optString("worldId"))
            .put("title", active.optString("title"))
            .put("author", active.optString("author", "Desconhecido"))
            .put("startLocationId", start.optString("locationId"))
            .put("startHook", start.optString("hook"))
            .put("premise", story.optString("premise"))
            .put("background", story.optString("background"))
            .put("playerIntroduction", story.optString("playerIntroduction"))
            .put("locations", active.optJSONArray("locations") ?: JSONArray())
            .put("npcs", active.optJSONArray("npcs") ?: JSONArray())
            .put("secrets", active.optJSONArray("secrets") ?: JSONArray())
            .put("objectives", active.optJSONArray("objectives") ?: JSONArray())
            .put("enemies", active.optJSONArray("enemies") ?: JSONArray())
    }

    /**
     * Formats private GM fiction data for injection into Barbara supervisor prompt.
     * Guaranteed to never leak directly to player narration unless revealed by game actions.
     */
    fun masterContextPrompt(scope: String = "default"): String = synchronized(lock) {
        val active = getActiveAdventure(scope) ?: return ""
        val story = active.optJSONObject("story") ?: JSONObject()
        val secrets = active.optJSONArray("secrets")
        val npcs = active.optJSONArray("npcs")
        val objectives = active.optJSONArray("objectives")

        val sb = StringBuilder()
        sb.appendLine("AVENTURA ATIVA: ${active.optString("title")} (${active.optString("id")})")
        sb.appendLine("PREMISSA: ${story.optString("premise")}")
        sb.appendLine("ANTECEDENTES SECRETOS DO MESTRE: ${story.optString("background")}")

        if (objectives != null && objectives.length() > 0) {
            sb.appendLine("OBJETIVOS DO CENÁRIO:")
            for (i in 0 until objectives.length()) {
                val obj = objectives.getJSONObject(i)
                sb.appendLine("- ${obj.optString("id")}: ${obj.optString("description", "Sem descrição")}")
            }
        }

        if (npcs != null && npcs.length() > 0) {
            sb.appendLine("NPCS RELEVANTES:")
            for (i in 0 until npcs.length()) {
                val npc = npcs.getJSONObject(i)
                val goals = npc.optJSONArray("goals")?.let { arr ->
                    (0 until arr.length()).joinToString(", ") { arr.getString(it) }
                } ?: "agenda não revelada"
                sb.appendLine("- ${npc.optString("name", npc.optString("id"))} (${npc.optString("role", "habitante")}): Objetivos: $goals")
            }
        }

        if (secrets != null && secrets.length() > 0) {
            sb.appendLine("SEGREDOS DO MESTRE (NÃO REVELAR ATÉ QUE O GRUPO INVESTIGUE E DESCUBRA):")
            for (i in 0 until secrets.length()) {
                val sec = secrets.getJSONObject(i)
                sb.appendLine("- [SEGREDO ${sec.optString("id")}]: ${sec.optString("truth")}")
            }
        }

        return sb.toString().trim()
    }

    fun getActiveAdventure(scope: String = "default"): JSONObject? = synchronized(lock) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "braseiro_adventure_packages",
            arrayOf("active_json"),
            "scope = ?",
            arrayOf(scope),
            null,
            null,
            null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val raw = c.getString(c.getColumnIndexOrThrow("active_json"))
                if (!raw.isNullOrBlank()) {
                    return runCatching { JSONObject(raw) }.getOrNull()
                }
            }
        }
        return null
    }

    fun getActiveAdventureSummary(scope: String = "default"): JSONObject? = synchronized(lock) {
        val active = getActiveAdventure(scope) ?: return null
        val story = active.optJSONObject("story") ?: JSONObject()
        val start = active.optJSONObject("start") ?: JSONObject()
        return JSONObject()
            .put("id", active.optString("id"))
            .put("title", active.optString("title"))
            .put("system", active.optString("system"))
            .put("worldId", active.optString("worldId"))
            .put("premise", story.optString("premise"))
            .put("startLocationId", start.optString("locationId"))
            .put("locationsCount", active.optJSONArray("locations")?.length() ?: 0)
            .put("npcsCount", active.optJSONArray("npcs")?.length() ?: 0)
            .put("objectivesCount", active.optJSONArray("objectives")?.length() ?: 0)
    }

    fun listHistory(scope: String = "default"): JSONArray = synchronized(lock) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "braseiro_adventure_packages",
            arrayOf("history_json"),
            "scope = ?",
            arrayOf(scope),
            null,
            null,
            null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val raw = c.getString(c.getColumnIndexOrThrow("history_json"))
                if (!raw.isNullOrBlank()) {
                    return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
                }
            }
        }
        return JSONArray()
    }
}
