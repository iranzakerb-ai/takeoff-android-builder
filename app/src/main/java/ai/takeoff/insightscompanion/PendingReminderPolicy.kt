package ai.takeoff.insightscompanion

import java.net.URI
import java.net.URLEncoder

data class PendingReminderCandidate(val account: String,val url: String,val shortcode: String,val targetHours: Double)
object PendingReminderPolicy {
    fun candidate(account: String, url: String, shortcode: String, dueTargetsHours: List<Double>): PendingReminderCandidate? { val cleanAccount = ManagedAccount.normalizeHandle(account); val cleanUrl = canonicalInstagramUrl(url) ?: return null; val cleanShortcode = shortcode.trim(); val target = dueTargetsHours.filter { it > 0.0 && !it.isNaN() && !it.isInfinite() }.maxOrNull(); if (cleanAccount.isBlank() || cleanShortcode.isBlank() || target == null) return null; val urlShortcode = cleanUrl.trimEnd('/').substringAfterLast('/'); if (urlShortcode != cleanShortcode) return null; return PendingReminderCandidate(cleanAccount, cleanUrl, cleanShortcode, target) }
    private fun canonicalInstagramUrl(raw: String): String? { val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null; if (!uri.scheme.equals("https", ignoreCase = true)) return null; val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null; if (host != "instagram.com") return null; val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }; if (segments.size != 2 || segments[0] !in setOf("reel", "p")) return null; val shortcode = segments[1]; if (!shortcode.matches(Regex("[A-Za-z0-9_-]+"))) return null; return "https://www.instagram.com/${segments[0]}/$shortcode/" }
    fun isRetryableHttpStatus(code: Int): Boolean = code == 408 || code == 429 || code in 500..599
    fun shouldRetryForBacklog(notified: Int, limit: Int, hasUnprocessed: Boolean): Boolean = limit > 0 && notified >= limit && hasUnprocessed
    fun dedupeKey(candidate: PendingReminderCandidate): String = "${candidate.account}|${candidate.shortcode}|${candidate.targetHours}"
    fun notificationId(candidate: PendingReminderCandidate): Int { val hash = dedupeKey(candidate).hashCode() and 0x7fffffff; val span = Int.MAX_VALUE - 2402; return 2402 + (hash % span) }
    fun requestCode(candidate: PendingReminderCandidate): Int = "${candidate.account}|${candidate.shortcode}".hashCode() and 0x7fffffff
    fun armIntentIdentityUri(candidate: PendingReminderCandidate): String { val account = URLEncoder.encode(candidate.account, Charsets.UTF_8.name()); val shortcode = URLEncoder.encode(candidate.shortcode, Charsets.UTF_8.name()); return "takeoff://owner-insights/reminder/$account/$shortcode" }
}
