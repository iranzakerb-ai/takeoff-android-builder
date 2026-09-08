package ai.takeoff.insightscompanion

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class StudioEntry(
    val id: String,
    val type: String, // "scenario" or "ai_video"
    val mode: String, // "smart", "smart_silent", "short_15s", "short_15s_silent", "cinematic", "cinematic_silent", "viral_10s", "viral_10s_silent"
    val title: String,
    val niche: String,
    val description: String,
    val targetAudience: String = "",
    val mainOffer: String = "",
    val constraints: String = "",
    val actorCount: Int = 1,
    var status: String = "processing", // "processing", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    var resultJson: String? = null,
    var errorMessage: String? = null,
    var pdfPath: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("mode", mode)
        put("title", title)
        put("niche", niche)
        put("description", description)
        put("target_audience", targetAudience)
        put("main_offer", mainOffer)
        put("constraints", constraints)
        put("actor_count", actorCount)
        put("status", status)
        put("created_at", createdAt)
        put("result_json", resultJson ?: JSONObject.NULL)
        put("error_message", errorMessage ?: JSONObject.NULL)
        put("pdf_path", pdfPath ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(json: JSONObject): StudioEntry = StudioEntry(
            id = json.optString("id"),
            type = json.optString("type"),
            mode = json.optString("mode"),
            title = json.optString("title"),
            niche = json.optString("niche"),
            description = json.optString("description"),
            targetAudience = json.optString("target_audience"),
            mainOffer = json.optString("main_offer"),
            constraints = json.optString("constraints"),
            actorCount = json.optInt("actor_count", 1),
            status = json.optString("status", "processing"),
            createdAt = json.optLong("created_at", System.currentTimeMillis()),
            resultJson = json.optString("result_json").takeIf { it.isNotBlank() && it != "null" },
            errorMessage = json.optString("error_message").takeIf { it.isNotBlank() && it != "null" },
            pdfPath = json.optString("pdf_path").takeIf { it.isNotBlank() && it != "null" },
        )
    }
}

class StudioResultStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("takeoff_studio_results", Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(): List<StudioEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val list = mutableListOf<StudioEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(StudioEntry.fromJson(obj))
        }
        return list.sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun getByType(type: String): List<StudioEntry> = getAll().filter { it.type == type }

    @Synchronized
    fun get(id: String): StudioEntry? = getAll().firstOrNull { it.id == id }

    @Synchronized
    fun save(entry: StudioEntry) {
        val existing = getAll().toMutableList()
        val index = existing.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            existing[index] = entry
        } else {
            existing.add(0, entry)
        }
        persist(existing)
    }

    @Synchronized
    fun update(id: String, mutator: (StudioEntry) -> StudioEntry): StudioEntry? {
        val existing = getAll().toMutableList()
        val index = existing.indexOfFirst { it.id == id }
        if (index < 0) return null
        val updated = mutator(existing[index])
        existing[index] = updated
        persist(existing)
        return updated
    }

    @Synchronized
    fun delete(id: String) {
        val existing = getAll().toMutableList()
        existing.removeAll { it.id == id }
        persist(existing)
    }

    private fun persist(entries: List<StudioEntry>) {
        val array = JSONArray()
        for (e in entries) {
            array.put(e.toJson())
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
