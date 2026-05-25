package com.pluscubed.am2geo

import com.pluscubed.am2geo.parser.AppleMapsUrlParser
import com.pluscubed.am2geo.parser.LatLng
import com.pluscubed.am2geo.parser.ParsedAppleMaps
import com.pluscubed.am2geo.parser.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMapsUrlParserTest {

    @Test
    fun `legacy ll only`() {
        val parsed = AppleMapsUrlParser.parse("https://maps.apple.com/?ll=37.7749,-122.4194")
        assertEquals(
            ParsedAppleMaps.Point(LatLng(37.7749, -122.4194), null, null, null, null),
            parsed,
        )
    }

    @Test
    fun `legacy ll with query and zoom`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/?ll=37.7749,-122.4194&q=Twin+Peaks&z=15"
        )
        assertEquals(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.7749, -122.4194),
                name = null,
                address = null,
                query = "Twin Peaks",
                zoom = 15f,
            ),
            parsed,
        )
    }

    @Test
    fun `address only becomes search`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/?address=1+Infinite+Loop,+Cupertino,+CA"
        )
        assertEquals(ParsedAppleMaps.Search("1 Infinite Loop, Cupertino, CA"), parsed)
    }

    @Test
    fun `modern coordinate with name`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/place?coordinate=37.767102,-122.411464" +
                "&name=Marked%20Location&map=explore"
        )
        assertEquals(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.767102, -122.411464),
                name = "Marked Location",
                address = null,
                query = null,
                zoom = null,
            ),
            parsed,
        )
    }

    @Test
    fun `modern place with coordinate name address and place-id`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/place" +
                "?address=1426%20Montana%20Ave,%20Unit%201,%20Santa%20Monica,%20CA%2090403,%20United%20States" +
                "&coordinate=34.032787,-118.494265" +
                "&name=La%20La%20Land" +
                "&place-id=IEB6B88C01B939EF7" +
                "&map=explore"
        )
        assertEquals(
            ParsedAppleMaps.Point(
                coordinate = LatLng(34.032787, -118.494265),
                name = "La La Land",
                address = "1426 Montana Ave, Unit 1, Santa Monica, CA 90403, United States",
                query = null,
                zoom = null,
                placeId = "IEB6B88C01B939EF7",
            ),
            parsed,
        )
    }

    @Test
    fun `directions with origin and walking flag`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/?daddr=37.33,-122.03&saddr=37.77,-122.42&dirflg=w"
        )
        assertEquals(
            ParsedAppleMaps.Directions(
                origin = "37.77,-122.42",
                destination = "37.33,-122.03",
                travelMode = TravelMode.WALKING,
            ),
            parsed,
        )
    }

    @Test
    fun `directions destination only, transit`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/?daddr=Apple+Park&dirflg=r"
        )
        assertEquals(
            ParsedAppleMaps.Directions(
                origin = null,
                destination = "Apple Park",
                travelMode = TravelMode.TRANSIT,
            ),
            parsed,
        )
    }

    @Test
    fun `legacy http scheme parses too`() {
        val parsed = AppleMapsUrlParser.parse("http://maps.apple.com/?ll=0,0")
        assertEquals(
            ParsedAppleMaps.Point(LatLng(0.0, 0.0), null, null, null, null),
            parsed,
        )
    }

    @Test
    fun `query only becomes search`() {
        val parsed = AppleMapsUrlParser.parse(
            "https://maps.apple.com/?q=coffee&sll=37.7749,-122.4194"
        )
        assertEquals(ParsedAppleMaps.Search("coffee"), parsed)
    }

    @Test
    fun `garbage URL is unparseable`() {
        // No query, no recognizable path.
        val parsed = AppleMapsUrlParser.parse("https://maps.apple.com/")
        assertEquals(ParsedAppleMaps.Unparseable, parsed)
    }

    @Test
    fun `malformed url returns unparseable`() {
        assertEquals(ParsedAppleMaps.Unparseable, AppleMapsUrlParser.parse("not a url"))
    }

    @Test
    fun `dirflg case-insensitive`() {
        assertEquals(TravelMode.DRIVING, AppleMapsUrlParser.parseDirflg("D"))
        assertEquals(TravelMode.BICYCLING, AppleMapsUrlParser.parseDirflg(" c "))
        assertNull(AppleMapsUrlParser.parseDirflg(null))
        assertNull(AppleMapsUrlParser.parseDirflg("x"))
    }

    @Test
    fun `parseLatLng rejects out of range`() {
        assertNull(AppleMapsUrlParser.parseLatLng("100,0"))
        assertNull(AppleMapsUrlParser.parseLatLng("0,200"))
        assertNull(AppleMapsUrlParser.parseLatLng("a,b"))
        assertNull(AppleMapsUrlParser.parseLatLng(""))
        assertTrue(AppleMapsUrlParser.parseLatLng("0,0") == LatLng(0.0, 0.0))
    }
}
