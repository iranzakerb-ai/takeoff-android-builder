package ai.takeoff.insightscompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ManagedAccount(val label: String, val handle: String) {
    val normalizedHandle: String = normalizeHandle(handle)
    companion object { fun normalizeHandle(value: String): String = value.trim().removePrefix("@").lowercase() }
}

class ManagedAccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
    fun all(): List<ManagedAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null)
        val parsed = runCatching { val arr = JSONArray(raw ?: "[]"); buildList { for (i in 0 until arr.length()) { val obj = arr.optJSONObject(i) ?: continue; val handle = ManagedAccount.normalizeHandle(obj.optString("handle")); if (handle.isBlank()) continue; val label = obj.optString("label").trim().ifBlank { "@$handle" }; add(ManagedAccount(label, handle)) } } }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) return parsed.distinctBy { it.normalizedHandle }
        val legacy = ManagedAccount.normalizeHandle(prefs.getString("account", "").orEmpty())
        return if (legacy.isBlank()) emptyList() else listOf(ManagedAccount("پیج اصلی", legacy))
    }
    fun selected(): ManagedAccount? { val selected = ManagedAccount.normalizeHandle(prefs.getString(KEY_SELECTED, "").orEmpty()); return all().firstOrNull { it.normalizedHandle == selected } ?: all().firstOrNull() }
    fun upsert(label: String, handle: String): ManagedAccount { val normalized = ManagedAccount.normalizeHandle(handle); require(normalized.matches(Regex("[a-z0-9._]{1,30}"))) { "invalid_instagram_handle" }; val cleanLabel = label.trim().ifBlank { "@$normalized" }; val next = all().filterNot { it.normalizedHandle == normalized }.toMutableList(); val account = ManagedAccount(cleanLabel, normalized); next.add(account); persist(next); select(normalized); return account }
    fun remove(handle: String): Boolean { val normalized = ManagedAccount.normalizeHandle(handle); val before = all(); val next = before.filterNot { it.normalizedHandle == normalized }; if (next.size == before.size) return false; persist(next); val replacement = next.firstOrNull(); prefs.edit().putString(KEY_SELECTED, replacement?.normalizedHandle.orEmpty()).putString("account", replacement?.normalizedHandle.orEmpty()).apply(); return true }
    fun select(handle: String): ManagedAccount? { val normalized = ManagedAccount.normalizeHandle(handle); val account = all().firstOrNull { it.normalizedHandle == normalized } ?: return null; prefs.edit().putString(KEY_SELECTED, account.normalizedHandle).putString("account", account.normalizedHandle).putString("account_label", account.label).apply(); return account }
    private fun persist(accounts: List<ManagedAccount>) { val arr = JSONArray(); accounts.forEach { arr.put(JSONObject().put("label", it.label).put("handle", it.normalizedHandle)) }; prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply() }
    companion object { private const val KEY_ACCOUNTS = "managed_accounts_v1"; private const val KEY_SELECTED = "selected_managed_account_v1" }
}
