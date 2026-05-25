package com.pluscubed.am2geo.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Result of issuing a HEAD against a single URL. */
data class HttpHeadResult(
    /** HTTP status code, or 0 if the request failed before getting one. */
    val code: Int,
    /** The `Location` header, if any (may be a relative URL). */
    val location: String?,
)

/** Issues HEAD requests. Abstracted so tests can inject canned responses. */
interface HttpEngine {
    suspend fun head(url: String): HttpHeadResult
}

/**
 * Real engine backed by [HttpURLConnection]. We disable automatic redirect
 * following so [ShortLinkResolver] can cap hops, capture each `Location`
 * header itself, and stop as soon as the chain terminates.
 */
object DefaultHttpEngine : HttpEngine {
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000
    private const val USER_AGENT = "Mozilla/5.0 (Android) am2geo"

    override suspend fun head(url: String): HttpHeadResult = withContext(Dispatchers.IO) {
        val connection = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "HEAD"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
            }
        } catch (_: Exception) {
            return@withContext HttpHeadResult(code = 0, location = null)
        }
        try {
            HttpHeadResult(
                code = connection.responseCode,
                location = connection.getHeaderField("Location"),
            )
        } catch (_: Exception) {
            HttpHeadResult(code = 0, location = null)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Follows HTTP redirects manually so a `maps.apple/p/<token>` short link can be
 * expanded into the canonical `maps.apple.com/place?...` URL we know how to
 * parse.
 *
 * We don't lean on [HttpURLConnection.setInstanceFollowRedirects]'s built-in
 * chain because we want to (a) cap hops, (b) keep the resolved URL even if
 * Apple's server eventually 200s, and (c) issue HEAD rather than GET to keep
 * latency low.
 */
object ShortLinkResolver {

    private const val DEFAULT_MAX_HOPS = 3

    /**
     * Resolve [url] by following redirects up to [maxHops] times via [engine].
     * Returns the final URL on success (including [url] itself if the server
     * already returns 2xx), or null on any network/timeout/parse failure.
     */
    suspend fun resolve(
        url: String,
        maxHops: Int = DEFAULT_MAX_HOPS,
        engine: HttpEngine = DefaultHttpEngine,
    ): String? {
        var current = url
        repeat(maxHops) {
            val result = engine.head(current)
            when (result.code) {
                in 200..299 -> return current
                in 300..399 -> {
                    val location = result.location ?: return null
                    current = resolveRelative(current, location) ?: return null
                }
                else -> return null
            }
        }
        // Exhausted hop budget without reaching a 2xx — return what we have.
        return current
    }

    /** Resolve a possibly-relative `Location` header against the request URL. */
    private fun resolveRelative(base: String, location: String): String? = try {
        URI(base).resolve(location).toString()
    } catch (_: Exception) {
        null
    }
}
