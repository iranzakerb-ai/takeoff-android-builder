package ai.takeoff.insightscompanion

import org.json.JSONObject

/**
 * Legacy compatibility facade.
 *
 * The production share flow is ViralJobClient + Vercel. Keeping this facade avoids
 * accidental reintroduction of the former Render streaming endpoint by old call sites.
 */
@Deprecated("Use ViralJobClient with PayloadClient.PRODUCTION_ENDPOINT")
object ViralStreamClient {
    const val PRODUCTION_ENDPOINT = PayloadClient.PRODUCTION_ENDPOINT

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
