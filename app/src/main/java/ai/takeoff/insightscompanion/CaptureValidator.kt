package ai.takeoff.insightscompanion

object CaptureValidator {
    data class Result(val accepted: Boolean, val pageHint: String, val reason: String)
    private val reelContextMarkers = listOf("reel insights", "summary", "views over time", "what impacts your views", "how long people watched your reel", "top sources of views", "actions after viewing", "interactions", "when people liked your reel", "who viewed your reel", "audience details", "آمار ریل", "خلاصه", "تعامل", "مخاطب")
    private val summarySignature = setOf("views", "viewers", "reach", "avg_watch_time_seconds", "follows")
    private val impactSignature = setOf("skip_rate", "share_rate", "like_rate", "save_rate", "repost_rate", "comment_rate")
    private val sourceSignature = setOf("source_reels_tab_rate", "source_explore_rate", "source_feed_rate", "source_stories_rate", "source_profile_rate")
    private val engagementSignature = setOf("profile_visits", "follows", "likes", "comments", "reposts", "shares", "saves")
    private val audienceSignature = setOf("audience_followers_rate", "audience_nonfollowers_rate", "age_13_17_rate", "age_18_24_rate", "age_25_34_rate", "age_35_44_rate", "age_45_54_rate", "age_55_64_rate", "age_65_plus_rate")
    private val retentionSignature = setOf("avg_watch_time_seconds", "total_watch_time_seconds", "completion_rate", "hold_3s_rate", "replay_rate")
    fun validate(rawText: String, metrics: Map<String, Double>): Result {
        val text = normalize(rawText); val page = pageHint(text, metrics); val hasExplicitContext = reelContextMarkers.any { text.contains(it) }; val structuralContext = hasStrongMetricSignature(metrics)
        if (!hasExplicitContext && !structuralContext) return Result(false, page, "reel_insights_context_missing")
        val keys = metrics.keys
        return when (page) {
            "summary" -> { val hasDistributionCore = "views" in keys || "reach" in keys; if (hasDistributionCore && keys.count { it in summarySignature } >= 3) Result(true, page, if (hasExplicitContext) "summary_supported" else "summary_supported_by_metric_signature") else Result(false, page, "summary_needs_distribution_plus_two_metrics") }
            "impact_rates" -> acceptSection(page, keys.count { it in impactSignature }, hasExplicitContext, 2)
            "sources" -> acceptSection(page, keys.count { it in sourceSignature }, hasExplicitContext, 2)
            "engagement" -> acceptSection(page, keys.count { it in engagementSignature }, hasExplicitContext, 2)
            "audience" -> acceptSection(page, keys.count { it in audienceSignature }, hasExplicitContext, 2)
            "retention" -> { val supported = keys.count { it in retentionSignature } + keys.count { it in sourceSignature } + keys.count { it in impactSignature }; if (supported >= 2) Result(true, page, "retention_context_with_labeled_metrics") else Result(false, page, "retention_chart_has_no_labeled_metrics") }
            else -> if (metrics.size >= 2) Result(true, page, "details_supported") else Result(false, page, "detail_needs_two_labeled_metrics")
        }
    }
    private fun acceptSection(page: String, supported: Int, explicit: Boolean, minimum: Int): Result = if (supported >= minimum) Result(true, page, if (explicit) "${page}_supported" else "${page}_supported_by_metric_signature") else Result(false, page, "${page}_needs_${minimum}_labeled_metrics")
    fun pageHint(textRaw: String, metrics: Map<String, Double> = emptyMap()): String { val text = normalize(textRaw); val keys = metrics.keys; return when { text.contains("summary") -> "summary"; text.contains("what impacts your views") || keys.count { it in impactSignature } >= 2 -> "impact_rates"; text.contains("top sources of views") || keys.count { it in sourceSignature } >= 2 -> "sources"; text.contains("actions after viewing") || text.contains("interactions") || text.contains("when people liked your reel") -> "engagement"; text.contains("who viewed your reel") || text.contains("audience details") || keys.count { it in audienceSignature } >= 2 -> "audience"; text.contains("how long people watched your reel") || text.contains("retention") -> "retention"; hasSummarySignature(metrics) -> "summary"; keys.count { it in engagementSignature } >= 3 -> "engagement"; keys.count { it in retentionSignature } >= 2 -> "retention"; else -> "details" } }
    private fun hasStrongMetricSignature(metrics: Map<String, Double>): Boolean { val keys = metrics.keys; return hasSummarySignature(metrics) || keys.count { it in impactSignature } >= 3 || keys.count { it in sourceSignature } >= 3 || keys.count { it in engagementSignature } >= 3 || keys.count { it in audienceSignature } >= 3 || keys.count { it in retentionSignature } >= 2 }
    private fun hasSummarySignature(metrics: Map<String, Double>): Boolean { val keys = metrics.keys; val hasDistributionCore = "views" in keys || "reach" in keys; return hasDistributionCore && keys.count { it in summarySignature } >= 3 }
    private fun normalize(raw: String): String = raw.lowercase().replace(Regex("\\s+"), " ").trim()
}
