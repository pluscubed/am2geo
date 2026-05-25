package com.pluscubed.am2geo

import com.pluscubed.am2geo.parser.GoogleMapsUrlBuilder
import com.pluscubed.am2geo.parser.LatLng
import com.pluscubed.am2geo.parser.ParsedAppleMaps
import com.pluscubed.am2geo.parser.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleMapsUrlBuilderTest {

    @Test
    fun `real POI with name and address uses location-biased search`() {
        // Replays the Dining Yamamoto example from the live `maps.apple/p/...`
        // short link. address+place-id are Apple's "this is a real catalogued
        // place" signal, so we hand Google a search query and let it surface
        // the actual restaurant card.
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.773722, -122.416037),
                name = "Dining Yamamoto",
                address = "167 11th St, San Francisco, CA 94103, United States",
                query = null,
                zoom = null,
                placeId = "I54A31A3DE65065C6",
            )
        )
        assertEquals(
            "geo:37.773722,-122.416037" +
                "?q=Dining%20Yamamoto%2C%20167%2011th%20St%2C%20San%20Francisco%2C%20CA%2094103%2C%20United%20States",
            out,
        )
    }

    @Test
    fun `real POI with placeId but no address still searches by name`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.7749, -122.4194),
                name = "Twin Peaks",
                address = null,
                query = null,
                zoom = null,
                placeId = "I1234",
            )
        )
        assertEquals("geo:37.7749,-122.4194?q=Twin%20Peaks", out)
    }

    @Test
    fun `Marked Location-style drop pin keeps exact coords with label`() {
        // Apple "drop a pin" share — coordinate + name "Marked Location", no
        // address, no place-id. Searching for "Marked Location" lands on
        // random nearby businesses (verified on-device), so we drop a labeled
        // pin at the exact coordinate the friend shared.
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.767102, -122.411464),
                name = "Marked Location",
                address = null,
                query = null,
                zoom = null,
                placeId = null,
            )
        )
        assertEquals(
            "geo:37.767102,-122.411464?q=37.767102,-122.411464(Marked%20Location)",
            out,
        )
    }

    @Test
    fun `point with coordinate only has no label`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(LatLng(37.7749, -122.4194), null, null, null, null)
        )
        assertEquals("geo:37.7749,-122.4194?q=37.7749,-122.4194", out)
    }

    @Test
    fun `point with coordinate only and zoom appends z`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(LatLng(37.7749, -122.4194), null, null, null, 15f)
        )
        assertEquals("geo:37.7749,-122.4194?q=37.7749,-122.4194&z=15", out)
    }

    @Test
    fun `point without coordinate falls back to address search`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(
                coordinate = null,
                name = null,
                address = "1 Infinite Loop, Cupertino, CA",
                query = null,
                zoom = null,
            )
        )
        assertEquals("geo:0,0?q=1%20Infinite%20Loop%2C%20Cupertino%2C%20CA", out)
    }

    @Test
    fun `point without coordinate or address still searches by name`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Point(
                coordinate = null,
                name = "Apple Park",
                address = null,
                query = null,
                zoom = null,
            )
        )
        assertEquals("geo:0,0?q=Apple%20Park", out)
    }

    @Test
    fun `search builds geo zero-zero`() {
        val out = GoogleMapsUrlBuilder.build(ParsedAppleMaps.Search("coffee shops near me"))
        assertEquals("geo:0,0?q=coffee%20shops%20near%20me", out)
    }

    @Test
    fun `directions destination only walking`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Directions(
                origin = null,
                destination = "Apple Park",
                travelMode = TravelMode.WALKING,
            )
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1" +
                "&destination=Apple%20Park&travelmode=walking",
            out,
        )
    }

    @Test
    fun `directions with origin transit`() {
        val out = GoogleMapsUrlBuilder.build(
            ParsedAppleMaps.Directions(
                origin = "37.77,-122.42",
                destination = "37.33,-122.03",
                travelMode = TravelMode.TRANSIT,
            )
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1" +
                "&destination=37.33%2C-122.03" +
                "&origin=37.77%2C-122.42" +
                "&travelmode=transit",
            out,
        )
    }

    @Test
    fun `unparseable returns null`() {
        assertNull(GoogleMapsUrlBuilder.build(ParsedAppleMaps.Unparseable))
    }
}
