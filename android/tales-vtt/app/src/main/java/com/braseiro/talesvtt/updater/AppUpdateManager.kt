package com.braseiro.talesvtt.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateStatus {
  UP_TO_DATE,
  UPDATE_AVAILABLE,
  NETWORK_ERROR,
  INVALID_MANIFEST
}

data class UpdateCheckResult(
  val status: UpdateStatus,
  val currentVersionCode: Int,
  val remoteVersionCode: Int? = null,
  val remoteVersionName: String? = null,
  val downloadUrl: String? = null,
  val sha256: String? = null,
  val changelog: String? = null,
  val errorMessage: String? = null,
  val checkedAtMillis: Long = System.currentTimeMillis()
)

class AppUpdateManager(
  val currentVersionCode: Int = 33,
  val currentVersionName: String = "1.3.26"
) {

  companion object {
    const val PRIMARY_MANIFEST_URL = "https://raw.githubusercontent.com/bra83/braseirobuild/main/releases/tales/release-manifest.json"
    const val FALLBACK_MANIFEST_URL = "https://raw.githubusercontent.com/bra83/braseirobuild/main/release-manifest.json"
  }

  fun parseManifest(jsonString: String): UpdateCheckResult {
    return try {
      val json = JSONObject(jsonString)
      if (!json.has("versionCode") || !json.has("versionName")) {
        return UpdateCheckResult(
          status = UpdateStatus.INVALID_MANIFEST,
          currentVersionCode = currentVersionCode,
          errorMessage = "Manifesto não contém os campos obrigatórios 'versionCode' e 'versionName'."
        )
      }

      val remoteCode = json.getInt("versionCode")
      val remoteName = json.getString("versionName")
      val downloadUrl = json.optString("downloadUrl", "")
      val sha256 = json.optString("sha256", "")
      val changelog = json.optString("changelog", "")

      if (remoteCode > currentVersionCode) {
        if (!isValidDownloadUrl(downloadUrl) || !sha256.matches(Regex("(?i)[0-9a-f]{64}"))) {
          return UpdateCheckResult(UpdateStatus.INVALID_MANIFEST, currentVersionCode,
            errorMessage = "Atualização sem URL HTTPS segura e SHA-256 válido.")
        }
        UpdateCheckResult(
          status = UpdateStatus.UPDATE_AVAILABLE,
          currentVersionCode = currentVersionCode,
          remoteVersionCode = remoteCode,
          remoteVersionName = remoteName,
          downloadUrl = downloadUrl,
          sha256 = sha256,
          changelog = changelog
        )
      } else {
        // Equal or lower (downgrade blocked)
        UpdateCheckResult(
          status = UpdateStatus.UP_TO_DATE,
          currentVersionCode = currentVersionCode,
          remoteVersionCode = remoteCode,
          remoteVersionName = remoteName,
          changelog = changelog
        )
      }
    } catch (e: Exception) {
      UpdateCheckResult(
        status = UpdateStatus.INVALID_MANIFEST,
        currentVersionCode = currentVersionCode,
        errorMessage = "Erro ao processar manifesto: ${e.message}"
      )
    }
  }

  suspend fun checkForUpdates(manifestUrl: String = PRIMARY_MANIFEST_URL): UpdateCheckResult =
    withContext(Dispatchers.IO) {
      val urlsToTry = if (manifestUrl == PRIMARY_MANIFEST_URL) {
        listOf(PRIMARY_MANIFEST_URL, FALLBACK_MANIFEST_URL)
      } else {
        listOf(manifestUrl)
      }

      var lastErrorResult: UpdateCheckResult? = null

      for (targetUrl in urlsToTry) {
        var connection: HttpURLConnection? = null
        try {
          val url = URL(targetUrl)
          connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "BraseiroTalesVTT/${currentVersionName}")
          }

          val code = connection.responseCode
          if (code in 200..299) {
            val content = connection.inputStream.bufferedReader().use { it.readText() }
            val parsed = parseManifest(content)
            if (parsed.status != UpdateStatus.INVALID_MANIFEST) {
              return@withContext parsed
            } else {
              lastErrorResult = parsed
            }
          } else {
            lastErrorResult = UpdateCheckResult(
              status = UpdateStatus.NETWORK_ERROR,
              currentVersionCode = currentVersionCode,
              errorMessage = "Servidor retornou código HTTP $code em $targetUrl."
            )
          }
        } catch (e: Exception) {
          lastErrorResult = UpdateCheckResult(
            status = UpdateStatus.NETWORK_ERROR,
            currentVersionCode = currentVersionCode,
            errorMessage = "Falha de rede ao consultar atualizações em $targetUrl: ${e.message ?: "Conexão indisponível"}"
          )
        } finally {
          connection?.disconnect()
        }
      }

      lastErrorResult ?: UpdateCheckResult(
        status = UpdateStatus.NETWORK_ERROR,
        currentVersionCode = currentVersionCode,
        errorMessage = "Nenhum endpoint de atualização respondeu com sucesso."
      )
    }

  /** Downloads and verifies binary SHA-256, APK package, increasing version and signing identity. */
  suspend fun downloadVerifiedApk(context: Context, result: UpdateCheckResult): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        require(result.status == UpdateStatus.UPDATE_AVAILABLE) { "Nenhuma atualização disponível." }
        val downloadUrl = result.downloadUrl.orEmpty()
        val expectedSha = result.sha256.orEmpty()
        require(isValidDownloadUrl(downloadUrl) && expectedSha.matches(Regex("(?i)[0-9a-f]{64}"))) {
          "Endereço de download ou SHA-256 inválido."
        }
        val dir = File(context.cacheDir, "braseiro_updates").apply { mkdirs() }
        val file = File(dir, "tales-update-${result.remoteVersionCode}.apk")
        val partial = File(dir, "${file.name}.part")
        partial.delete()
        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
          connectTimeout = 15000
          readTimeout = 30000
          instanceFollowRedirects = true
          setRequestProperty("User-Agent", "BraseiroTalesVTT/$currentVersionName")
        }
        try {
          check(connection.responseCode == HttpURLConnection.HTTP_OK) {
            "Servidor de APK retornou HTTP ${connection.responseCode}."
          }
          require(connection.url.protocol == "https") { "Redirecionamento para endereço não seguro." }
          val md = MessageDigest.getInstance("SHA-256")
          val limit = 500L * 1024 * 1024
          var received = 0L
          connection.inputStream.use { input ->
            FileOutputStream(partial).use { output ->
              val buf = ByteArray(64 * 1024)
              while (true) {
                val n = input.read(buf)
                if (n < 0) break
                received += n
                require(received <= limit) { "APK ultrapassou o limite de segurança." }
                md.update(buf, 0, n)
                output.write(buf, 0, n)
              }
              output.fd.sync()
            }
          }
          require(received > 0L && md.digest().joinToString("") { "%02x".format(it) }
            .equals(expectedSha, ignoreCase = true)) { "SHA-256 do APK não confere." }
          val pm = context.packageManager
          val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
          @Suppress("DEPRECATION")
          val archive = pm.getPackageArchiveInfo(partial.absolutePath, flags)
            ?: error("Arquivo recebido não é um APK Android válido.")
          require(archive.packageName == context.packageName) { "APK de outro aplicativo bloqueado." }
          val remoteCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode
            else archive.versionCode.toLong()
          require(remoteCode > currentVersionCode && remoteCode == result.remoteVersionCode?.toLong()) {
            "VersionCode incompatível ou downgrade bloqueado."
          }
          @Suppress("DEPRECATION")
          val installed = pm.getPackageInfo(context.packageName, flags)
          val remoteSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            archive.signingInfo?.apkContentsSigners?.map { it.toByteArray().toList() }.orEmpty()
            else archive.signatures?.map { it.toByteArray().toList() }.orEmpty()
          val installedSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            installed.signingInfo?.apkContentsSigners?.map { it.toByteArray().toList() }.orEmpty()
            else installed.signatures?.map { it.toByteArray().toList() }.orEmpty()
          require(remoteSignatures.isNotEmpty() && remoteSignatures == installedSignatures) {
            "Assinatura do APK diferente da versão instalada."
          }
          if (file.exists()) file.delete()
          check(partial.renameTo(file)) { "Não foi possível finalizar o APK validado." }
          file
        } finally {
          connection.disconnect()
          partial.delete()
        }
      }
    }

  fun createInstallIntent(context: Context, apkFile: File): Intent = Intent(Intent.ACTION_VIEW).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apkFile)
    setDataAndType(uri, "application/vnd.android.package-archive")
  }

  fun isValidDownloadUrl(url: String): Boolean = runCatching {
    val parsed = URL(url)
    parsed.protocol == "https" && parsed.host.isNotBlank() && parsed.userInfo == null
  }.getOrDefault(false)

  /** Legacy helper: not used by the secure install flow. */
  fun createDownloadIntent(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
  }
}