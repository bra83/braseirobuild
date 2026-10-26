package com.braseiro.talesvtt.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class AppUpdateManagerTest {

  private val manager = AppUpdateManager(currentVersionCode = 27, currentVersionName = "1.3.20")

  @Test
  fun testManifestWithNewerVersionTriggersUpdateAvailable() {
    val manifestJson = """
      {
        "versionCode": 28,
        "versionName": "1.3.21",
        "downloadUrl": "https://github.com/bra83/braseirobuild/releases/download/v1.3.21/app-release.apk",
        "sha256": "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
        "changelog": "Novos módulos de aventura e otimizações de áudio."
      }
    """.trimIndent()

    val result = manager.parseManifest(manifestJson)
    assertEquals(UpdateStatus.UPDATE_AVAILABLE, result.status)
    assertEquals(28, result.remoteVersionCode)
    assertEquals("1.3.21", result.remoteVersionName)
    assertNotNull(result.downloadUrl)
    assertEquals("E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", result.sha256)
  }

  @Test
  fun testManifestWithSameVersionReportsUpToDate() {
    val manifestJson = """
      {
        "versionCode": 27,
        "versionName": "1.3.20",
        "downloadUrl": "https://example.com/app.apk"
      }
    """.trimIndent()

    val result = manager.parseManifest(manifestJson)
    assertEquals(UpdateStatus.UP_TO_DATE, result.status)
    assertEquals(27, result.remoteVersionCode)
  }

  @Test
  fun testManifestWithOlderVersionBlocksDowngrade() {
    val manifestJson = """
      {
        "versionCode": 26,
        "versionName": "1.3.19"
      }
    """.trimIndent()

    val result = manager.parseManifest(manifestJson)
    assertEquals(UpdateStatus.UP_TO_DATE, result.status)
  }

  @Test
  fun testMalformedManifestReturnsInvalidManifestStatus() {
    val badJson = "{ invalid_json: true "
    val result = manager.parseManifest(badJson)
    assertEquals(UpdateStatus.INVALID_MANIFEST, result.status)
    assertTrue(result.errorMessage?.isNotEmpty() == true)
  }

  @Test
  fun testMissingRequiredFieldsReturnsInvalidManifestStatus() {
    val missingCodeJson = """{"versionName": "1.3.19"}"""
    val result = manager.parseManifest(missingCodeJson)
    assertEquals(UpdateStatus.INVALID_MANIFEST, result.status)
  }

  @Test
  fun testDownloadUrlContract() {
    val validUrl = "https://example.com/download.apk"
    assertTrue(manager.isValidDownloadUrl(validUrl))
    assertFalse(manager.isValidDownloadUrl("ftp://insecure.apk"))
    assertFalse(manager.isValidDownloadUrl(""))
  }
}
