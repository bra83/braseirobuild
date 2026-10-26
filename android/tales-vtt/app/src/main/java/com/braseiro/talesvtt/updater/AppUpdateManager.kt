package com.braseiro.talesvtt.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
  val currentVersionCode: Int = 27,
  val currentVersionName: String = "1.3.20"
) {

  companion object {
    const val PRIMARY_MANIFEST_URL = "https://raw.githubusercontent.com/bra83/braseirobuild/main/release-manifest.json"
    const val FALLBACK_MANIFEST_URL = "https://raw.githubusercontent.com/bra83/braseirobuild/build/tales-1.3.19-final/release-manifest.json"
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

  fun createInstallIntent(context: Context, apkFile: File): Intent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
      val uri = Uri.fromFile(apkFile)
      setDataAndType(uri, "application/vnd.android.package-archive")
    }
    return intent
  }

  fun isValidDownloadUrl(url: String): Boolean {
    return url.isNotBlank() && (url.startsWith("https://") || url.startsWith("http://"))
  }

  fun createDownloadIntent(url: String): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }
}
