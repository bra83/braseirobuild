package com.braseiro.talesvtt.updater

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Installs an already verified APK using PackageInstaller instead of an ACTION_VIEW chooser. */
class BraseiroPackageInstall(private val context: Context) {
  fun hasInstallPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

  fun openInstallPermissionSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }
  }

  /** Rechecks the cached APK SHA-256 after settings return, then commits a real install session. */
  suspend fun commitVerifiedApk(apk: File, expectedSha256: String): Int = withContext(Dispatchers.IO) {
    require(hasInstallPermission()) { "Autorize instalar desta fonte e tente novamente." }
    val directory = File(context.cacheDir, "braseiro_updates").canonicalFile
    require(apk.canonicalFile.parentFile == directory) { "APK fora do cache verificado." }
    require(ApkIntegrity.matches(apk, expectedSha256)) { "APK inválido ou modificado depois do download." }

    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(context.packageName)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
      }
    }
    val sessionId = installer.createSession(params)
    try {
      installer.openSession(sessionId).use { session ->
        apk.inputStream().use { input ->
          session.openWrite("base.apk", 0, apk.length()).use { output ->
            input.copyTo(output, 64 * 1024)
            session.fsync(output)
          }
        }
        // Explicit mutable pending activity is required for OS status & confirmation extras.
        val callback = Intent(context, UpdateInstallResultActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
          (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pending = PendingIntent.getActivity(context, sessionId, callback, flags)
        session.commit(pending.intentSender)
      }
      sessionId
    } catch (e: Exception) {
      runCatching { installer.abandonSession(sessionId) }
      throw e
    }
  }
}

/** Pure file-integrity helper that is also exercised in local unit tests. */
internal object ApkIntegrity {
  fun matches(file: File, expectedSha: String): Boolean {
    if (!file.isFile || file.length() == 0L || !expectedSha.matches(Regex("(?i)[0-9a-f]{64}"))) return false
    return runCatching {
      val md = MessageDigest.getInstance("SHA-256")
      file.inputStream().buffered().use { input ->
        val chunk = ByteArray(64 * 1024)
        while (true) {
          val n = input.read(chunk)
          if (n == -1) break
          md.update(chunk, 0, n)
        }
      }
      md.digest().joinToString("") { "%02x".format(it) }.equals(expectedSha, ignoreCase = true)
    }.getOrDefault(false)
  }
}

/** Receives INSTALL_SESSION status and launches the *system* confirmation UI. */
class UpdateInstallResultActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
      ?: PackageInstaller.STATUS_FAILURE
    when (status) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        @Suppress("DEPRECATION")
        val confirmation: Intent? =
          if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
          else intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
        if (confirmation == null) {
          Toast.makeText(this, "O Android não retornou a confirmação da instalação.", Toast.LENGTH_LONG).show()
        } else {
          runCatching { startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(this, "Erro ao abrir a instalação: ${it.message}", Toast.LENGTH_LONG).show() }
        }
      }
      PackageInstaller.STATUS_SUCCESS ->
        Toast.makeText(this, "Braseiro atualizado.", Toast.LENGTH_LONG).show()
      else -> {
        val info = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        Toast.makeText(this, "Instalação recusada pelo Android ($status): $info", Toast.LENGTH_LONG).show()
      }
    }
    finish()
  }
}
