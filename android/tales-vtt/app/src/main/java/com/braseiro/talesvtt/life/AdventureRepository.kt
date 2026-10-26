package com.braseiro.talesvtt.life

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import android.util.AtomicFile
import java.util.UUID

class AdventureRepository(private val context: Context) {

  private val packagesDir: File by lazy {
    File(context.filesDir, "adventures/packages").apply { mkdirs() }
  }

  private val instancesDir: File by lazy {
    File(context.filesDir, "adventures/instances").apply { mkdirs() }
  }

  private val legacyDir: File by lazy {
    File(context.filesDir, "adventures/legacy").apply { mkdirs() }
  }

  private val timelineDir: File by lazy {
    File(context.filesDir, "adventures/timeline").apply { mkdirs() }
  }

  fun sanitizeId(id: String): String = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")

  /** Android AtomicFile preserves the previous valid file until finishWrite succeeds. */
  private fun atomicWrite(file: File, content: String): Boolean {
    val parent = file.parentFile ?: return false
    if (!parent.exists() && !parent.mkdirs()) return false
    val atomic = AtomicFile(file)
    var output: FileOutputStream? = null
    return try {
      output = atomic.startWrite()
      output.write(content.toByteArray(Charsets.UTF_8))
      output.fd.sync()
      atomic.finishWrite(output)
      true
    } catch (_: Exception) {
      output?.let { runCatching { atomic.failWrite(it) } }
      false
    }
  }

  /** openRead restores a surviving .bak after a crash during replacement. */
  private fun atomicRead(file: File): String? = runCatching {
    AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
  }.getOrNull()

  private fun legacyKey(key: CharacterIdentityKey): String = listOf(
    key.systemId, key.worldId, key.timelineId, key.characterId
  ).joinToString("_") { sanitizeId(it) }

  fun saveTimeline(timeline: WorldTimelineState): Boolean {
    val safeId = "${sanitizeId(timeline.worldId)}_${sanitizeId(timeline.timelineId)}"
    val file = File(timelineDir, "$safeId.json")
    return atomicWrite(file, timeline.toJson().toString())
  }

  fun loadTimeline(worldId: String, timelineId: String): WorldTimelineState? {
    val safeId = "${sanitizeId(worldId)}_${sanitizeId(timelineId)}"
    val file = File(timelineDir, "$safeId.json")
    val raw = atomicRead(file) ?: return null
    return try {
      val obj = JSONObject(raw)
      WorldTimelineState.fromJson(obj)
    } catch (_: Exception) {
      null
    }
  }

  fun savePackage(pkg: AdventurePackage): Boolean {
    val safeId = sanitizeId(pkg.id)
    val file = File(packagesDir, "$safeId.json")
    return atomicWrite(file, pkg.rawJson)
  }

  fun listPackages(): List<AdventurePackage> {
    val result = mutableListOf<AdventurePackage>()
    val files = packagesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
    for (f in files) {
      try {
        val content = atomicRead(f) ?: continue
        val res = AdventurePackageValidator.parseAndValidate(content)
        if (res is AdventurePackageValidator.ValidationResult.Valid) {
          result.add(res.adventurePackage)
        }
      } catch (_: Exception) {}
    }
    return result
  }

  fun getPackage(id: String): AdventurePackage? {
    val safeId = sanitizeId(id)
    val file = File(packagesDir, "$safeId.json")
    val content = atomicRead(file) ?: return null
    val res = AdventurePackageValidator.parseAndValidate(content)
    return (res as? AdventurePackageValidator.ValidationResult.Valid)?.adventurePackage
  }

  fun createInstance(
    pkg: AdventurePackage,
    timelineId: String = "timeline-default"
  ): AdventureInstance {
    val instanceId = "inst-${UUID.randomUUID().toString().take(8)}"
    val instance = AdventureInstance(
      instanceId = instanceId,
      packageId = pkg.id,
      worldId = pkg.worldId,
      timelineId = timelineId,
      startedAtTimestamp = System.currentTimeMillis(),
      currentLocationId = pkg.start.locationId
    )
    saveInstance(instance)
    return instance
  }

  fun saveInstance(instance: AdventureInstance): Boolean {
    val safeId = sanitizeId(instance.instanceId)
    val file = File(instancesDir, "$safeId.json")
    return atomicWrite(file, instance.toJson().toString())
  }

  fun loadInstance(instanceId: String): AdventureInstance? {
    val safeId = sanitizeId(instanceId)
    val file = File(instancesDir, "$safeId.json")
    val raw = atomicRead(file) ?: return null
    return try {
      val obj = JSONObject(raw)
      AdventureInstance.fromJson(obj)
    } catch (_: Exception) {
      null
    }
  }

  fun listInstances(): List<AdventureInstance> {
    val result = mutableListOf<AdventureInstance>()
    val files = instancesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
    for (f in files) {
      try {
        val obj = JSONObject(atomicRead(f) ?: continue)
        result.add(AdventureInstance.fromJson(obj))
      } catch (_: Exception) {}
    }
    return result
  }

  fun saveLegacy(legacy: CharacterLegacyRecord): Boolean {
    // Always include timelineId. Previous 3-component filenames remain read-only migration sources.
    return atomicWrite(File(legacyDir, "${legacyKey(legacy.identityKey)}.json"), legacy.toJson().toString())
  }

  fun loadLegacy(identityKey: CharacterIdentityKey): CharacterLegacyRecord? {
    val canonical = File(legacyDir, "${legacyKey(identityKey)}.json")
    val olderThreePartKey = listOf(identityKey.systemId, identityKey.worldId, identityKey.characterId)
      .joinToString("_") { sanitizeId(it) }
    val candidates = listOf(canonical, File(legacyDir, "$olderThreePartKey.json"),
      File(legacyDir, "${sanitizeId(identityKey.characterId)}.json"))
    for (file in candidates) {
      val raw = atomicRead(file) ?: continue
      val record = runCatching { CharacterLegacyRecord.fromJson(JSONObject(raw)) }.getOrNull()
      if (record?.identityKey != identityKey) continue // Never load another timeline's legacy.
      if (file != canonical) saveLegacy(record) // Non-destructive migration.
      return record
    }
    return null
  }

  /** Compatibility lookup: refuse ambiguous IDs used in multiple timelines/worlds. */
  fun loadLegacy(characterId: String): CharacterLegacyRecord? {
    val matches = mutableListOf<CharacterLegacyRecord>()
    val files = legacyDir.listFiles { _, name -> name.endsWith(".json") } ?: return null
    for (file in files) {
      val raw = atomicRead(file) ?: continue
      val record = runCatching { CharacterLegacyRecord.fromJson(JSONObject(raw)) }.getOrNull()
      if (record?.identityKey?.characterId == characterId &&
        matches.none { it.identityKey == record.identityKey }) matches.add(record)
    }
    return matches.singleOrNull()
  }
}