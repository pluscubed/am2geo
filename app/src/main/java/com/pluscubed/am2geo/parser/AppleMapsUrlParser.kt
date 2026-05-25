package com.pluscubed.am2geo.parser

import java.net.URI
import java.net.URLDecoder

/**
 * Pure-Kotlin parser for Apple Maps URLs. No Android imports, so it runs under
 * plain JUnit. Handles both the legacy MapLinks (`?ll=`, `?q=`, `?daddr=`, ...)
 * and the modern MapKit unified-URL params (`?coordinate=`, `?name=`,
 * `?address=`, `?place-id=`, `?map=`).
 */
object AppleMapsUrlParser {

    /**
     * Parse an Apple Maps URL into a [ParsedAppleMaps].
     *
     * Rules, applied in order:
     *  1. `daddr` present → [ParsedAppleMaps.Directions].
     *  2. `coordinate` or `ll` present → [ParsedAppleMaps.Point].
     *  3. `q` or `address` present → [ParsedAppleMaps.Search].
     *  4. Path like `/place/<name>` → [ParsedAppleMaps.Search].
     *  5. Otherwise → [ParsedAppleMaps.Unparseable].
     */
    fun parse(url: String): ParsedAppleMaps {
        val uri = runCatching { URI(url) }.getOrNull() ?: return ParsedAppleMaps.Unparseable
        val params = parseQuery(uri.rawQuery)

        params["daddr"]?.takeIf { it.isNotBlank() }?.let { daddr ->
            return ParsedAppleMaps.Directions(
                origin = params["saddr"]?.takeIf { it.isNotBlank() },
                destination = daddr,
                travelMode = parseDirflg(params["dirflg"]),
            )
        }

        val coord = parseLatLng(params["coordinate"]) ?: parseLatLng(params["ll"])
        val name = params["name"]?.takeIf { it.isNotBlank() }
        val address = params["address"]?.takeIf { it.isNotBlank() }
        val q = params["q"]?.takeIf { it.isNotBlank() }
        val zoom = params["z"]?.toFloatOrNull()

        if (coord != null) {
            return ParsedAppleMaps.Point(
                coordinate = coord,
                name = name,
                address = address,
                query = q,
                zoom = zoom,
                placeId = params["place-id"]?.takeIf { it.isNotBlank() },
            )
        }

        val searchText = q ?: address ?: name
        if (searchText != null) return ParsedAppleMaps.Search(searchText)

        // Fallback: `/place/Some-Name` style path.
        val pathName = placeNameFromPath(uri.path)
        if (pathName != null) return ParsedAppleMaps.Search(pathName)

        return ParsedAppleMaps.Unparseable
    }

    /** "lat,lng" → [LatLng] (returns null on malformed input). */
    internal fun parseLatLng(raw: String?): LatLng? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return LatLng(lat, lng)
    }

    /** Apple's `dirflg` → our [TravelMode] (d/w/r/c, case-insensitive). */
    internal fun parseDirflg(raw: String?): TravelMode? = when (raw?.trim()?.lowercase()) {
        "d" -> TravelMode.DRIVING
        "w" -> TravelMode.WALKING
        "r" -> TravelMode.TRANSIT
        "c" -> TravelMode.BICYCLING
        else -> null
    }

    /**
     * Decode the raw query string into a map. Returns an empty map for null or
     * empty input. Values are URL-decoded (handles both `+` and `%20`).
     * If a key appears more than once, the first occurrence wins.
     */
    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val key = if (eq < 0) pair else pair.substring(0, eq)
            val value = if (eq < 0) "" else pair.substring(eq + 1)
            val decodedKey = runCatching { URLDecoder.decode(key, "UTF-8") }.getOrDefault(key)
            val decodedValue =
                runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
            out.putIfAbsent(decodedKey, decodedValue)
        }
        return out
    }

    /** `/place/Eiffel-Tower` → "Eiffel Tower". */
    private fun placeNameFromPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val segments = path.trim('/').split('/')
        if (segments.size < 2 || segments[0] != "place") return null
        val raw = segments[1].takeIf { it.isNotBlank() } ?: return null
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw).replace('-', ' ')
    }
}
