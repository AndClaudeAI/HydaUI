package com.hydaui.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.compose.runtime.Immutable
import com.hydaui.launcher.BuildConfig
import com.hydaui.launcher.ui.theme.Look
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateState {
    data object Idle : UpdateState
    data object UpToDate : UpdateState

    /** A new build is approved but Android hasn't let HydaUI install apps yet. */
    data class NeedsPermission(val versionName: String) : UpdateState
    data class Downloading(val versionName: String, val progress: Float) : UpdateState
    data class Installing(val versionName: String) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/** Shown once after the app has updated itself. */
@Immutable
data class WhatsNew(val versionName: String, val notes: String)

/**
 * Keeps HydaUI current from the repo's GitHub releases. Only the *latest* (approved) release
 * counts; per-build previews are prereleases, which GitHub's "latest" never returns.
 *
 * - Look-and-feel (hyda-config.json) is applied live, without installing anything.
 * - The APK is only fetched when the release's code fingerprint differs from ours, verified
 *   against the manifest's SHA-256, and installed with PackageInstaller. On Android 12+ an app
 *   may update itself without asking; older versions show a single confirmation.
 */
class Updater(private val context: Context) {
    private val prefs = context.getSharedPreferences("hyda_updates", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _whatsNew = MutableStateFlow<WhatsNew?>(null)
    val whatsNew: StateFlow<WhatsNew?> = _whatsNew.asStateFlow()

    /** Whether this build can update itself at all (debug builds are signed differently). */
    val enabled: Boolean get() = !BuildConfig.DEBUG

    /** Applies whichever look is newer: the one shipped in this APK or the last one downloaded. */
    fun loadLook() {
        val bundled = runCatching {
            JSONObject(context.assets.open(CONFIG_ASSET).bufferedReader().use { it.readText() })
        }.getOrNull()
        val cached = prefs.getString(KEY_LOOK, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val pick = listOfNotNull(bundled, cached).maxByOrNull { it.optInt("revision", 0) } ?: return
        Look.apply(pick)
        appliedRevision = pick.optInt("revision", 0)
    }

    /** Detects that we've just been updated and surfaces the release notes once. */
    fun noteLaunch() {
        val last = prefs.getInt(KEY_LAST_VERSION, 0)
        if (last in 1 until BuildConfig.VERSION_CODE) {
            val notes = prefs.getString(KEY_PENDING_NOTES, null).orEmpty()
            _whatsNew.value = WhatsNew(BuildConfig.VERSION_NAME, notes)
        }
        prefs.edit().putInt(KEY_LAST_VERSION, BuildConfig.VERSION_CODE).remove(KEY_PENDING_NOTES).apply()
    }

    fun dismissWhatsNew() {
        _whatsNew.value = null
    }

    suspend fun checkForUpdate(force: Boolean = false) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0) < CHECK_INTERVAL_MS) return
        if (!mutex.tryLock()) return
        try {
            withContext(Dispatchers.IO) { checkLocked() }
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun checkLocked() {
        val release = JSONObject(fetchText("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"))
        val assets = release.getJSONArray("assets").let { array ->
            (0 until array.length()).associate { i ->
                val a = array.getJSONObject(i)
                a.getString("name") to a.getString("browser_download_url")
            }
        }

        // 1. Look-and-feel: instant, no install.
        assets[CONFIG_ASSET]?.let { url ->
            val look = JSONObject(fetchText(url))
            if (look.optInt("revision", 0) > appliedRevision) {
                withContext(Dispatchers.Main) { Look.apply(look) }
                appliedRevision = look.optInt("revision", 0)
                prefs.edit().putString(KEY_LOOK, look.toString()).apply()
            }
        }

        // 2. Code: only when the approved build is newer *and* its code actually differs.
        val manifest = assets[MANIFEST_ASSET]?.let { JSONObject(fetchText(it)) }
        val apkUrl = assets[APK_ASSET]
        if (manifest == null || apkUrl == null) {
            _state.value = UpdateState.UpToDate
            return
        }
        val versionCode = manifest.getInt("versionCode")
        val versionName = manifest.optString("versionName", versionCode.toString())
        if (versionCode <= BuildConfig.VERSION_CODE || manifest.optString("codeVersion") == BuildConfig.CODE_VERSION) {
            _state.value = UpdateState.UpToDate
            return
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(versionName)
            return
        }

        val apk = download(apkUrl, versionName)
        val expected = manifest.optString("apkSha256")
        check(expected.isNotEmpty() && sha256(apk) == expected) { "Downloaded update failed verification" }

        prefs.edit().putString(KEY_PENDING_NOTES, manifest.optString("notes")).apply()
        _state.value = UpdateState.Installing(versionName)
        install(apk)
    }

    private fun download(url: String, versionName: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "HydaUI-$versionName.apk")
        val conn = open(url)
        try {
            val total = conn.contentLengthLong.takeIf { it > 0 }
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total != null) _state.value = UpdateState.Downloading(versionName, read.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }

    private fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java),
                flags,
            )
            session.commit(callback.intentSender)
        }
    }

    private var appliedRevision: Int
        get() = prefs.getInt(KEY_APPLIED_REVISION, 0)
        set(value) = prefs.edit().putInt(KEY_APPLIED_REVISION, value).apply()

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "HydaUI/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.github+json, application/json, */*")
        }

    private fun fetchText(url: String): String {
        val conn = open(url)
        try {
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode} from ${URL(url).host}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val CONFIG_ASSET = "hyda-config.json"
        const val MANIFEST_ASSET = "update.json"
        const val APK_ASSET = "HydaUI.apk"
        const val CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000L
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_LOOK = "look_json"
        const val KEY_APPLIED_REVISION = "look_revision"
        const val KEY_LAST_VERSION = "last_version_code"
        const val KEY_PENDING_NOTES = "pending_notes"
    }
}
