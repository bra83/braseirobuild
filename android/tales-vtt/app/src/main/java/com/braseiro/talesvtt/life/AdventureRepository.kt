package com.braseiro.talesvtt.life

import android.content.Context
import org.json.JSONObject
import java.io.File
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

  private fun atomicWrite(file: File, content: String): Boolean {
    val parent = file.parentFile ?: return false
    if (!parent.exists()) parent.mkdirs()
    val tmp = File(parent, "${file.name}.${UUID.randomUUID()}.tmp")
    val bak = File(parent, "${file.name}.bak")
    return try {
      java.io.FileOutputStream(tmp).use { fos ->
        fos.write(content.toByteArray(Charsets.UTF_8))
        fos.flush()
        fos.fd.sync()
      }
      if (file.exists()) {
        try {
          if (bak.exists()) bak.delete()
          file.copyTo(bak, overwrite = true)
        } catch (_: Exception) {}
        file.delete()
      }
      val renamed = tmp.renameTo(file)
      if (!renamed) {
        tmp.copyTo(file, overwrite = true)
        tmp.delete()
      }
      true
    } catch (e: Exception) {
      if (tmp.exists()) tmp.delete()
      false
    }
  }

  fun saveTimeline(timeline: WorldTimelineState): Boolean {
    val safeId = "${sanitizeId(timeline.worldId)}_${sanitizeId(timeline.timelineId)}"
    val file = File(timelineDir, "$safeId.json")
    return atomicWrite(file, timeline.toJson().toString())
  }

  fun loadTimeline(worldId: String, timelineId: String): WorldTimelineState? {
    val safeId = "${sanitizeId(worldId)}_${sanitizeId(timelineId)}"
    val file = File(timelineDir, "$safeId.json")
    if (!file.exists()) return null
    return try {
      val obj = JSONObject(file.readText(Charsets.UTF_8))
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
        val content = f.readText(Charsets.UTF_8)
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
    if (!file.exists()) return null
    val content = file.readText(Charsets.UTF_8)
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
    if (!file.exists()) return null
    return try {
      val obj = JSONObject(file.readText(Charsets.UTF_8))
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
        val obj = JSONObject(f.readText(Charsets.UTF_8))
        result.add(AdventureInstance.fromJson(obj))
      } catch (_: Exception) {}
    }
    return result
  }

  fun saveLegacy(legacy: CharacterLegacyRecord): Boolean {
    val compoundKey = "${sanitizeId(legacy.identityKey.systemId)}_${sanitizeId(legacy.identityKey.worldId)}_${sanitizeId(legacy.identityKey.characterId)}"
    val file = File(legacyDir, "$compoundKey.json")
    return atomicWrite(file, legacy.toJson().toString())
  }

  fun loadLegacy(identityKey: CharacterIdentityKey): CharacterLegacyRecord? {
    val compoundKey = "${sanitizeId(identityKey.systemId)}_${sanitizeId(identityKey.worldId)}_${sanitizeId(identityKey.characterId)}"
    val file = File(legacyDir, "$compoundKey.json")
    if (!file.exists()) {
      val fallback = File(legacyDir, "${sanitizeId(identityKey.characterId)}.json")
      if (!fallback.exists()) return null
      return try {
        val obj = JSONObject(fallback.readText(Charsets.UTF_8))
        CharacterLegacyRecord.fromJson(obj)
      } catch (_: Exception) {
        null
      }
    }
    return try {
      val obj = JSONObject(file.readText(Charsets.UTF_8))
      CharacterLegacyRecord.fromJson(obj)
    } catch (_: Exception) {
      null
    }
  }

  fun loadLegacy(characterId: String): CharacterLegacyRecord? {
    val safeCharId = sanitizeId(characterId)
    val files = legacyDir.listFiles { _, name -> name == "$safeCharId.json" || name.endsWith("_$safeCharId.json") } ?: emptyArray()
    for (f in files) {
      try {
        val obj = JSONObject(f.readText(Charsets.UTF_8))
        val rec = CharacterLegacyRecord.fromJson(obj)
        if (rec != null) return rec
      } catch (_: Exception) {}
    }
    return null
  }
}
