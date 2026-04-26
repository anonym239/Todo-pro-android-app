package com.todopro.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_USER = "anonym239"
    private const val GITHUB_REPO = "Todo-pro-android-app"
    private const val TAG = "UpdateChecker"
    private const val PREFS_NAME = "todopro_prefs"
    private const val KEY_SKIPPED_VERSION = "skipped_update_version"

    // Prüft ob ein Update verfügbar ist (läuft im Hintergrund-Thread)
    fun checkForUpdate(context: Context, currentVersion: String) {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    val latestTag = json.getString("tag_name").trimStart('v')
                    val releaseUrl = json.getString("html_url")
                    val releaseName = json.getString("name")

                    // APK Download-URL aus Assets holen
                    var apkDownloadUrl: String? = null
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    val currentClean = currentVersion.trimStart('v')

                    // Bereits gesehene/installierte Version aus Prefs lesen
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val skippedVersion = prefs.getString(KEY_SKIPPED_VERSION, "") ?: ""

                    Log.d(TAG, "Current: $currentClean | Latest: $latestTag | Skipped: $skippedVersion")

                    // Nur zeigen wenn: neuere Version UND nicht bereits für diese Version gefragt
                    if (isNewerVersion(latestTag, currentClean) && latestTag != skippedVersion) {
                        (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                            showUpdateDialog(context, releaseName, latestTag, apkDownloadUrl, releaseUrl)
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Update-Check fehlgeschlagen: ${e.message}")
            }
        }.start()
    }

    // Vergleicht Versionen: "1.0.5" > "1.0.3" → true
    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun showUpdateDialog(
        context: Context,
        releaseName: String,
        version: String,
        apkUrl: String?,
        releaseUrl: String
    ) {
        val activity = context as? androidx.appcompat.app.AppCompatActivity ?: return

        AlertDialog.Builder(activity)
            .setTitle("🔄 Update verfügbar!")
            .setMessage("Eine neue Version ist verfügbar:\n\n📦 $releaseName\n\nMöchtest du jetzt aktualisieren?")
            .setPositiveButton("⬇️ Jetzt installieren") { _, _ ->
                // Version als "gesehen" markieren damit Dialog nicht nochmal kommt
                markVersionAsSeen(context, version)
                if (apkUrl != null) {
                    downloadAndInstallApk(activity, apkUrl, version)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                    activity.startActivity(intent)
                }
            }
            .setNegativeButton("Später", null)
            .setCancelable(true)
            .show()
    }

    // Merkt sich die Version damit der Dialog nicht nochmal erscheint
    private fun markVersionAsSeen(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIPPED_VERSION, version)
            .apply()
    }

    private fun downloadAndInstallApk(context: Context, apkUrl: String, version: String) {
        try {
            val fileName = "TodoPro-v$version.apk"
            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("TodoPro Update")
                setDescription("Version $version wird heruntergeladen...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // Warte auf Download-Abschluss
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx.unregisterReceiver(this)
                        installApk(ctx, fileName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download fehlgeschlagen: ${e.message}")
        }
    }

    private fun installApk(context: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (!file.exists()) return

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Installation fehlgeschlagen: ${e.message}")
        }
    }

    // Wird nach erfolgreicher Installation aufgerufen - startet App neu
    // (Todos bleiben erhalten da sie in SharedPreferences gespeichert sind)
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        // Aktuellen Prozess beenden
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
