package ai.takeoff.insightscompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object PayloadMergePolicy {
    fun merge(target: JSONObject, incoming: JSONObject) {
        val targetObserved = target.optDouble("observed_at", 0.0); val incomingObserved = incoming.optDouble("observed_at", 0.0); val incomingIsNewest = targetObserved <= 0.0 || incomingObserved >= targetObserved
        val targetMetrics = target.optJSONObject("metrics") ?: JSONObject().also { target.put("metrics", it) }; val incomingMetrics = incoming.optJSONObject("metrics") ?: JSONObject(); val metricKeys = incomingMetrics.keys(); while (metricKeys.hasNext()) { val key = metricKeys.next(); if (!incomingMetrics.isNull(key) && (incomingIsNewest || !targetMetrics.has(key))) targetMetrics.put(key, incomingMetrics.get(key)) }
        target.put("observed_at", maxOf(targetObserved, incomingObserved))
        if (incomingIsNewest && incoming.has("operator_reviewed")) { val reviewed = incoming.optBoolean("operator_reviewed", false); target.put("operator_reviewed", reviewed); if (incoming.has("execution_fidelity")) target.put("execution_fidelity", incoming.optString("execution_fidelity", "unknown")); if (incoming.has("scenario_id")) target.put("scenario_id", incoming.optString("scenario_id")) else if (reviewed) target.remove("scenario_id") }
        val oldOcr = target.optJSONObject("ocr") ?: JSONObject().also { target.put("ocr", it) }; val newOcr = incoming.optJSONObject("ocr"); if (newOcr != null) { val oldHints = oldOcr.optString("page_hint").split(',').map { it.trim() }.filter { it.isNotBlank() }; val newHints = newOcr.optString("page_hint").split(',').map { it.trim() }.filter { it.isNotBlank() }; val hints = (oldHints + newHints).distinct(); if (hints.isNotEmpty()) oldOcr.put("page_hint", hints.joinToString(",")); if (incomingIsNewest && newOcr.has("engine")) oldOcr.put("engine", newOcr.get("engine")); if (incomingIsNewest && newOcr.has("raw_text_sha256")) oldOcr.put("raw_text_sha256", newOcr.get("raw_text_sha256")) }
    }
}

class PayloadQueue(context: Context) {
    companion object { private val PROCESS_LOCK = Any(); private const val MERGE_WINDOW_SECONDS = 15 * 60.0 }
    private val secrets = SecretStore(context)
    fun enqueue(payload: JSONObject): String = synchronized(PROCESS_LOCK) { val arr = readArray(); val mergeIndex = findMergeTarget(arr, payload); if (mergeIndex >= 0) { val existing = arr.getJSONObject(mergeIndex); PayloadMergePolicy.merge(existing, payload); writeArray(arr); return@synchronized existing.optString("capture_id") }; val id = payload.optString("capture_id").ifBlank { java.util.UUID.randomUUID().toString() }; payload.put("capture_id", id); arr.put(payload); writeArray(arr); id }
    fun all(): List<JSONObject> = synchronized(PROCESS_LOCK) { val arr = readArray(); (0 until arr.length()).mapNotNull { arr.optJSONObject(it) } }
    fun remove(captureId: String) = synchronized(PROCESS_LOCK) { val arr = readArray(); val next = JSONArray(); for (i in 0 until arr.length()) { val obj = arr.optJSONObject(i) ?: continue; if (obj.optString("capture_id") != captureId) next.put(obj) }; writeArray(next) }
    fun size(): Int = synchronized(PROCESS_LOCK) { readArray().length() }
    private fun findMergeTarget(arr: JSONArray, incoming: JSONObject): Int { val account = incoming.optString("account_id"); val shortcode = incoming.optString("shortcode"); val url = incoming.optString("url"); val observed = incoming.optDouble("observed_at", 0.0); for (i in arr.length() - 1 downTo 0) { val candidate = arr.optJSONObject(i) ?: continue; if (candidate.optString("account_id") != account) continue; val sameReel = if (shortcode.isNotBlank()) candidate.optString("shortcode") == shortcode else url.isNotBlank() && candidate.optString("url") == url; if (!sameReel) continue; val previous = candidate.optDouble("observed_at", 0.0); if (previous > 0.0 && observed > 0.0 && kotlin.math.abs(observed - previous) <= MERGE_WINDOW_SECONDS) return i }; return -1 }
    private fun readArray(): JSONArray = runCatching { JSONArray(secrets.get("payload_queue") ?: "[]") }.getOrElse { JSONArray() }
    private fun writeArray(arr: JSONArray) = secrets.put("payload_queue", arr.toString())
}
