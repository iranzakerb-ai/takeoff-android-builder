package ai.takeoff.insightscompanion

import org.json.JSONObject

/**
 * Legacy compatibility facade.
 *
 * The production share flow is ViralJobClient + Vercel-compatible hosting. Keeping
 * this facade avoids accidental reintroduction of the former streaming endpoint.
 */
@Deprecated("Use ViralJobClient with PayloadClient.PRODUCTION_ENDPOINT")
object ViralStreamClient {
    val PRODUCTION_ENDPOINT: String = PayloadClient.PRODUCTION_ENDPOINT

    data class StreamResult(val httpCode: Int, val errorBody: String = "")

    fun analyze(
        reelUrl: String,
        niche: String,
        onEvent: (JSONObject) -> Unit,
    ): StreamResult {
        val result = ViralJobClient.analyze(
            endpoint = PayloadClient.PRODUCTION_ENDPOINT,
            reelUrl = reelUrl,
            niche = niche,
            onEvent = onEvent,
        )
        return StreamResult(result.httpCode, result.errorBody)
    }
}
