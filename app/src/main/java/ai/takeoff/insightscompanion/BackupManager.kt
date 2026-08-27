package ai.takeoff.insightscompanion

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BackupManager {
    private const val ALIAS = "takeoff_companion_backup_v1"

    fun create(context: Context): File {
        val accounts = ManagedAccountStore(context).all()
        val insight = LocalInsightStore(context)
        val root = JSONObject()
            .put("format", 1)
            .put("created_at", System.currentTimeMillis())
            .put("accounts", JSONArray().apply { accounts.forEach { put(JSONObject().put("label", it.label).put("handle", it.normalizedHandle)) } })
            .put("reels", JSONArray().apply {
                insight.reels().forEach { r ->
                    put(JSONObject().put("account_id", r.accountId).put("shortcode", r.shortcode).put("url", r.url)
                        .put("scenario_id", r.scenarioId).put("first_seen_at", r.firstSeenAt).put("last_capture_at", r.lastCaptureAt)
                        .put("execution_fidelity", r.executionFidelity).put("state", r.state)
                        .put("metrics", JSONObject(r.metrics)))
                }
            })
            .put("planned_scenarios", JSONArray().apply {
                insight.scenarios().forEach { s -> put(JSONObject().put("scenario_id",s.scenarioId).put("title",s.title).put("account_id",s.accountId).put("created_at",s.createdAt)) }
            })
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(root.toString().toByteArray(Charsets.UTF_8))
        val envelope = JSONObject()
            .put("v",1)
            .put("iv", android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP))
            .put("data", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "takeoff-${System.currentTimeMillis()}.tobak")
        file.writeText(envelope.toString())
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
        return file
    }

    fun latest(context: Context): File? = File(context.filesDir, "backups").listFiles()?.maxByOrNull { it.lastModified() }

    fun verify(context: Context, file: File): Boolean = runCatching {
        val env = JSONObject(file.readText())
        val iv = android.util.Base64.decode(env.getString("iv"), android.util.Base64.NO_WRAP)
        val data = android.util.Base64.decode(env.getString("data"), android.util.Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        JSONObject(String(cipher.doFinal(data), Charsets.UTF_8)).optInt("format") == 1
    }.getOrDefault(false)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
}
