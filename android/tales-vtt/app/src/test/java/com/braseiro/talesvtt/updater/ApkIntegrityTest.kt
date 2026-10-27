package com.braseiro.talesvtt.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkIntegrityTest {
  @Test fun realSha256Matches() {
    val f = File.createTempFile("braseiro-integrity", ".bin")
    try {
      f.writeText("hello")
      assertTrue(ApkIntegrity.matches(f,"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
    } finally { f.delete() }
  }

  @Test fun missingOrChangedApkIsRejected() {
    val f = File.createTempFile("braseiro-integrity", ".bin")
    try {
      f.writeText("hello")
      assertFalse(ApkIntegrity.matches(f,"a".repeat(64)))
      f.delete()
      assertFalse(ApkIntegrity.matches(f,"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
    } finally { f.delete() }
  }
}
