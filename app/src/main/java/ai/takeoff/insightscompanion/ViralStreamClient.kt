package ai.takeoff.insightscompanion

import org.json.JSONObject

/**
 * Legacy compatibility facade.
 *
 * Public viral-learning traffic must use the durable viral runtime. Keeping this
 * facade avoids accidental fallback to the Owner/edge endpoint from old call sites.
 */
@Deprecated("Use ViralJobClient with PayloadClient.VIRAL_PRODUCTION_ENDPOINT")
object ViralStreamClient {
    const val PRODUCTION_ENDPOINT = PayloadClient.VIRAL_PRODUCTION_ENDPOINT

    data class StreamResult(val httpCode: Int, val errorBody: String = "")

    fun analyze(
        reelUrl: String,
        niche: String,
        onEvent: (JSONObject) -> Unit,
    ): StreamResult {
        val result = ViralJobClient.analyze(
            endpoint = PayloadClient.VIRAL_PRODUCTION_ENDPOINT,
            reelUrl = reelUrl,
            niche = niche,
            onEvent = onEvent,
        )
        return StreamResult(result.httpCode, result.errorBody)
    }
}
