package com.pluscubed.am2geo.parser

import java.net.URLEncoder

/**
 * Turns a [ParsedAppleMaps] into a URI string suitable for `ACTION_VIEW`.
 *
 * The `geo:` URI scheme has two semantics for the `q=` parameter and the
 * difference matters:
 *
 *  - `geo:lat,lng?q=lat,lng(label)` — drop a pin at exactly lat,lng. The label
 *    is just decoration; Google Maps won't resolve it to a POI / business
 *    listing even if it matches a real place.
 *  - `geo:lat,lng?q=<text>` — search for `<text>` biased to the lat,lng. This
 *    is what lets Google Maps geocode the query and surface the real business
 *    card (hours, reviews, photos, …).
 *
 * Apple's `/place` URLs hand us a name and address that point at a real
 * business, so we use the search form when we have a label to feed it,
 * falling back to the pin form for coordinate-only links.
 *
 * We treat `address` or `place-id` as Apple's signal that this is a real,
 * catalogued POI — and only then do we use the search form. Otherwise (a
 * "drop a pin" share, where Apple's URL is `coordinate + name=Marked
 * Location`), we fall back to a labeled pin so the user lands at the exact
 * spot the friend shared rather than at whatever business Google happens to
 * find nearby.
 *
 * Output shapes:
 *  - Point(coord, has address or placeId) → `geo:lat,lng?q=<name, address>`.
 *  - Point(coord, name only, no POI)       → `geo:lat,lng?q=lat,lng(name)`.
 *  - Point(coord, nothing else)            → `geo:lat,lng?q=lat,lng` (+ `&z=`).
 *  - Point(no coord, text)                 → `geo:0,0?q=<text>`.
 *  - Search(q)                             → `geo:0,0?q=<q>`.
 *  - Directions                            → `https://www.google.com/maps/dir/?api=1&...`.
 *  - Unparseable                           → null.
 *
 * `geo:` is preferred so the user's chosen maps app handles the intent.
 * Directions use the Google Maps universal URL because `geo:` can't express
 * travel mode; the Google Maps app intercepts it on Android.
 */
object GoogleMapsUrlBuilder {

    fun build(parsed: ParsedAppleMaps): String? = when (parsed) {
        is ParsedAppleMaps.Point -> buildPoint(parsed)
        is ParsedAppleMaps.Directions -> buildDirections(parsed)
        is ParsedAppleMaps.Search -> "geo:0,0?q=${encode(parsed.query)}"
        ParsedAppleMaps.Unparseable -> null
    }

    private fun buildPoint(p: ParsedAppleMaps.Point): String? {
        val coord = p.coordinate
        if (coord != null) {
            val latLng = "${formatCoord(coord.lat)},${formatCoord(coord.lng)}"
            val poiSearch = poiSearchText(p)
            return when {
                poiSearch != null -> "geo:$latLng?q=${encode(poiSearch)}"
                !p.name.isNullOrBlank() ->
                    // Coordinate plus a name but no address / place-id —
                    // probably a dropped pin. Keep the user at the exact
                    // coordinate; label it so they know what it is.
                    "geo:$latLng?q=$latLng(${encode(p.name)})"
                else -> {
                    val zoomSuffix = p.zoom?.let { "&z=${formatZoom(it)}" } ?: ""
                    "geo:$latLng?q=$latLng$zoomSuffix"
                }
            }
        }
        // No coordinate; fall back to a text-only search.
        val text = poiSearchText(p) ?: p.name ?: return null
        return "geo:0,0?q=${encode(text)}"
    }

    /**
     * Search text for a Point we believe is a real POI — `address` or
     * `place-id` present. Joins `name` and `address` when both are available
     * so Google's geocoder gets the most specific query we can give it.
     * Returns null when this doesn't look like a real POI (so the caller can
     * drop a pin instead of searching for fake nearby matches).
     */
    private fun poiSearchText(p: ParsedAppleMaps.Point): String? {
        val isRealPoi = !p.address.isNullOrBlank() || !p.placeId.isNullOrBlank()
        if (!isRealPoi) return null
        val parts = listOfNotNull(
            p.name?.takeIf { it.isNotBlank() },
            p.address?.takeIf { it.isNotBlank() },
        )
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun buildDirections(d: ParsedAppleMaps.Directions): String {
        val sb = StringBuilder("https://www.google.com/maps/dir/?api=1")
        sb.append("&destination=").append(encode(d.destination))
        d.origin?.let { sb.append("&origin=").append(encode(it)) }
        d.travelMode?.let { sb.append("&travelmode=").append(it.googleValue) }
        return sb.toString()
    }

    /** URL-encode using `%20` for spaces (not `+`) — friendlier for `geo:` URIs. */
    private fun encode(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** Strip a trailing `.0` so `37.7749` survives but `15.0` becomes `15`. */
    private fun formatCoord(v: Double): String {
        val s = v.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    private fun formatZoom(z: Float): String {
        val s = z.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }
}
