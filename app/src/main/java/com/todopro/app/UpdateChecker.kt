package com.todopro.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
    private const val KEY_SNOOZED_VERSION = "snoozed_update_version"

    fun checkForUpdate(context: Context, currentVersion: String) {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "GitHub API Response: $responseCode")

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    val latestTag = json.getString("tag_name").trimStart('v')
                    val releaseUrl = json.getString("html_url")
                    val releaseName = json.optString("name", "Version $latestTag")

                    var apkDownloadUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk")) {
                                apkDownloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }

                    val currentClean = currentVersion.trimStart('v').trim()
                    val latestClean = latestTag.trim()

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val snoozedVersion = prefs.getString(KEY_SNOOZED_VERSION, "") ?: ""

                    Log.d(TAG, "Current: '$currentClean' | Latest: '$latestClean' | Snoozed: '$snoozedVersion'")

                    if (isNewerVersion(latestClean, currentClean) && latestClean != snoozedVersion) {
                        (context as? androidx.appcompat.app.AppCompatActivity)?.runOnUiThread {
                            showUpdateDialog(context, releaseName, latestClean, apkDownloadUrl, releaseUrl)
                        }
                    }
                } else {
                    Log.w(TAG, "GitHub API Fehler: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Update-Check fehlgeschlagen: ${e.message}")
            }
        }.start()
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.trim().toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.trim().toIntOrNull() ?: 0 }
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
            .setMessage("Neue Version verfügbar:\n\n📦 $releaseName\n\nAktuell installiert: ${getCurrentVersion(activity)}\n\nMöchtest du jetzt aktualisieren?")
            .setPositiveButton("⬇️ Jetzt installieren") { _, _ ->
                if (apkUrl != null) {
                    downloadAndInstallApk(activity, apkUrl, version)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
                    activity.startActivity(intent)
                }
            }
            .setNegativeButton("Später") { _, _ ->
                snoozeVersion(context, version)
            }
            .setCancelable(false)
            .show()
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) { "?" }
    }

    private fun snoozeVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SNOOZED_VERSION, version).apply()
    }

    fun clearSnooze(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SNOOZED_VERSION).apply()
    }

    private fun downloadAndInstallApk(context: Context, apkUrl: String, version: String) {
        try {
            val fileName = "TodoPro-v$version.apk"

            // Alte APK löschen falls vorhanden
            val oldFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (oldFile.exists()) oldFile.delete()

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("TodoPro Update")
                setDescription("Version $version wird heruntergeladen...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
                // Wichtig: Auch über mobile Daten erlauben
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Log.d(TAG, "Download gestartet: ID=$downloadId, URL=$apkUrl")

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx.unregisterReceiver(this)

                        // Download-Status prüfen
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                            cursor.close()

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                Log.d(TAG, "Download erfolgreich, starte Installation...")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    installApkAndRestart(ctx, fileName)
                                }, 500)
                            } else {
                                Log.e(TAG, "Download fehlgeschlagen: Status=$status")
                            }
                        } else {
                            cursor.close()
                        }
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

    private fun installApkAndRestart(context: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (!file.exists()) {
                Log.e(TAG, "APK-Datei nicht gefunden: $fileName")
                return
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            // Nach der Installation: App neu starten
            // Wir registrieren einen PackageReplaced-Receiver der die App neu startet
            val restartIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Snooze löschen damit nach Neustart kein Dialog mehr kommt
            clearSnooze(context)

            context.startActivity(installIntent)

            // App nach kurzer Verzögerung schließen (Android öffnet sie nach Installation selbst)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    (context as? androidx.appcompat.app.AppCompatActivity)?.finishAffinity()
                } catch (e: Exception) {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }, 1000)

        } catch (e: Exception) {
            Log.e(TAG, "Installation fehlgeschlagen: ${e.message}")
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
