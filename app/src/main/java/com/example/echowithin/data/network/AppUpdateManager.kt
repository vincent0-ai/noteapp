package com.example.echowithin.data.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.echowithin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionName: String,
    val apkUrl: String,
    val changelog: String
)

class AppUpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val manifestUrl = "https://echowithin.xyz/static/update-manifest.json"

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(manifestUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext UpdateInfo(false, "", "", "")

            val bodyString = response.body?.string().orEmpty()
            if (bodyString.isBlank()) return@withContext UpdateInfo(false, "", "", "")

            val json = JSONObject(bodyString)
            val serverVersionCode = json.getInt("versionCode")
            val serverVersionName = json.getString("versionName")
            val apkUrl = json.getString("apkUrl")
            val changelog = json.optString("changelog", "")

            val currentVersionCode = BuildConfig.VERSION_CODE
            val hasUpdate = serverVersionCode > currentVersionCode

            UpdateInfo(hasUpdate, serverVersionName, apkUrl, changelog)
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(false, "", "", "")
        }
    }

    suspend fun downloadAndInstallApk(apkUrl: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()
            
            val downloadDir = context.externalCacheDir ?: context.cacheDir
            val destinationFile = File(downloadDir, "update-latest.apk")
            if (destinationFile.exists()) destinationFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesCopied = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesCopied += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalBytesCopied.toFloat() / contentLength.toFloat())
                        }
                    }
                    output.flush()
                }
            }

            triggerInstall(destinationFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun triggerInstall(file: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        context.startActivity(intent)
    }
}
