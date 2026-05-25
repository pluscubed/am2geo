package com.pluscubed.am2geo.parser

/** Latitude/longitude pair, in decimal degrees. */
data class LatLng(val lat: Double, val lng: Double)

/** Travel mode mirrored from Apple Maps' `dirflg` to Google Maps' `travelmode`. */
enum class TravelMode(val googleValue: String) {
    DRIVING("driving"),
    WALKING("walking"),
    TRANSIT("transit"),
    BICYCLING("bicycling"),
}

/**
 * Structured result of parsing an Apple Maps URL — broken into the small set
 * of shapes that map cleanly onto a Google Maps intent.
 */
sealed class ParsedAppleMaps {
    /**
     * A place / point on the map. Either a coordinate, an address, or both.
     *
     * [placeId] is Apple's opaque place identifier (the `place-id` query
     * param). We can't translate it directly to a Google place ID, but its
     * presence is a useful signal that Apple considers this a real, catalogued
     * POI — versus a "drop a pin" share where the only fields are coordinate
     * + name "Marked Location". The builder uses that distinction to decide
     * whether to fire a location-biased search or drop a plain pin.
     */
    data class Point(
        val coordinate: LatLng?,
        val name: String?,
        val address: String?,
        val query: String?,
        val zoom: Float?,
        val placeId: String? = null,
    ) : ParsedAppleMaps()

    /** Directions from `saddr` (optional) to `daddr`, with an optional travel mode. */
    data class Directions(
        val origin: String?,
        val destination: String,
        val travelMode: TravelMode?,
    ) : ParsedAppleMaps()

    /** A free-text search query. */
    data class Search(val query: String) : ParsedAppleMaps()

    /** URL didn't carry enough information to dispatch to a maps app. */
    object Unparseable : ParsedAppleMaps()
}
