package com.pluscubed.am2geo

import com.pluscubed.am2geo.net.HttpEngine
import com.pluscubed.am2geo.net.HttpHeadResult
import com.pluscubed.am2geo.net.ShortLinkResolver
import com.pluscubed.am2geo.parser.AppleMapsUrlParser
import com.pluscubed.am2geo.parser.LatLng
import com.pluscubed.am2geo.parser.ParsedAppleMaps
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortLinkResolverTest {

    /**
     * Fake engine that returns canned responses keyed by URL and records the
     * order in which URLs were requested so tests can assert the hop sequence.
     */
    private class FakeHttpEngine(
        private val responses: Map<String, HttpHeadResult>,
    ) : HttpEngine {
        val requestLog = mutableListOf<String>()
        override suspend fun head(url: String): HttpHeadResult {
            requestLog += url
            return responses[url] ?: HttpHeadResult(code = 0, location = null)
        }
    }

    /**
     * Snapshot of the exact response Apple returned for this short link when
     * this test was written (`curl -sI -A "Mozilla/5.0 (Android) am2geo"
     * 'https://maps.apple/p/YJ_1aw5REgsBR6'`):
     *
     *   HTTP/1.1 301 Moved Permanently
     *   Server: Apple
     *   Date: Wed, 20 May 2026 18:21:54 GMT
     *   Content-Type: text/plain
     *   Content-Length: 0
     *   cache-control: max-age=2592000, public
     *   location: https://maps.apple.com/place?address=167%2011th%20St,...
     *
     * And the resolved Apple URL responds 200 directly.
     */
    @Test
    fun `resolves YJ_1aw5REgsBR6 short link via single 301 hop`() = runBlocking {
        val shortUrl = "https://maps.apple/p/YJ_1aw5REgsBR6"
        val resolvedUrl = "https://maps.apple.com/place" +
            "?address=167%2011th%20St,%20San%20Francisco,%20CA%2094103,%20United%20States" +
            "&coordinate=37.773722,-122.416037" +
            "&name=Dining%20Yamamoto" +
            "&place-id=I54A31A3DE65065C6" +
            "&map=explore"

        val engine = FakeHttpEngine(mapOf(
            shortUrl to HttpHeadResult(code = 301, location = resolvedUrl),
            resolvedUrl to HttpHeadResult(code = 200, location = null),
        ))

        val out = ShortLinkResolver.resolve(shortUrl, engine = engine)

        assertEquals(resolvedUrl, out)
        assertEquals(listOf(shortUrl, resolvedUrl), engine.requestLog)
    }

    @Test
    fun `resolved Dining Yamamoto URL parses to expected Point`() = runBlocking {
        val shortUrl = "https://maps.apple/p/YJ_1aw5REgsBR6"
        val resolvedUrl = "https://maps.apple.com/place" +
            "?address=167%2011th%20St,%20San%20Francisco,%20CA%2094103,%20United%20States" +
            "&coordinate=37.773722,-122.416037" +
            "&name=Dining%20Yamamoto" +
            "&place-id=I54A31A3DE65065C6" +
            "&map=explore"

        val engine = FakeHttpEngine(mapOf(
            shortUrl to HttpHeadResult(code = 301, location = resolvedUrl),
            resolvedUrl to HttpHeadResult(code = 200, location = null),
        ))

        val resolved = ShortLinkResolver.resolve(shortUrl, engine = engine)
        val parsed = AppleMapsUrlParser.parse(resolved!!)

        assertEquals(
            ParsedAppleMaps.Point(
                coordinate = LatLng(37.773722, -122.416037),
                name = "Dining Yamamoto",
                address = "167 11th St, San Francisco, CA 94103, United States",
                query = null,
                zoom = null,
                placeId = "I54A31A3DE65065C6",
            ),
            parsed,
        )
    }

    @Test
    fun `follows multi-hop chain within the cap`() = runBlocking {
        val a = "https://example.test/a"
        val b = "https://example.test/b"
        val c = "https://example.test/c"
        val engine = FakeHttpEngine(mapOf(
            a to HttpHeadResult(code = 302, location = b),
            b to HttpHeadResult(code = 302, location = c),
            c to HttpHeadResult(code = 200, location = null),
        ))

        val out = ShortLinkResolver.resolve(a, maxHops = 3, engine = engine)

        assertEquals(c, out)
        assertEquals(listOf(a, b, c), engine.requestLog)
    }

    @Test
    fun `returns last url when hop budget is exhausted`() = runBlocking {
        val a = "https://example.test/a"
        val b = "https://example.test/b"
        val c = "https://example.test/c"
        val engine = FakeHttpEngine(mapOf(
            a to HttpHeadResult(code = 302, location = b),
            b to HttpHeadResult(code = 302, location = c),
            // No entry for c — request will return code=0 (failure) — but we
            // bail before then because maxHops=2 stops after two hops.
        ))

        val out = ShortLinkResolver.resolve(a, maxHops = 2, engine = engine)

        // We followed a→b (hop 1) and b→c (hop 2); the budget ran out before
        // we could fetch c. The resolver returns the URL it would have fetched
        // next so the caller still has something usable.
        assertEquals(c, out)
        assertEquals(listOf(a, b), engine.requestLog)
    }

    @Test
    fun `null Location on 3xx returns null`() = runBlocking {
        val engine = FakeHttpEngine(mapOf(
            "https://example.test/a" to HttpHeadResult(code = 302, location = null),
        ))
        val out = ShortLinkResolver.resolve("https://example.test/a", engine = engine)
        assertNull(out)
    }

    @Test
    fun `network failure code 0 returns null`() = runBlocking {
        val engine = FakeHttpEngine(emptyMap())  // unmapped URL → code=0
        val out = ShortLinkResolver.resolve("https://example.test/a", engine = engine)
        assertNull(out)
    }

    @Test
    fun `5xx returns null`() = runBlocking {
        val engine = FakeHttpEngine(mapOf(
            "https://example.test/a" to HttpHeadResult(code = 500, location = null),
        ))
        assertNull(ShortLinkResolver.resolve("https://example.test/a", engine = engine))
    }

    @Test
    fun `200 on first hit returns the input url`() = runBlocking {
        val engine = FakeHttpEngine(mapOf(
            "https://example.test/a" to HttpHeadResult(code = 200, location = null),
        ))
        val out = ShortLinkResolver.resolve("https://example.test/a", engine = engine)
        assertEquals("https://example.test/a", out)
    }

    @Test
    fun `relative Location is resolved against base`() = runBlocking {
        val a = "https://example.test/foo/a"
        val resolvedB = "https://example.test/foo/b"
        val engine = FakeHttpEngine(mapOf(
            a to HttpHeadResult(code = 301, location = "b"),     // relative
            resolvedB to HttpHeadResult(code = 200, location = null),
        ))
        val out = ShortLinkResolver.resolve(a, engine = engine)
        assertEquals(resolvedB, out)
        assertEquals(listOf(a, resolvedB), engine.requestLog)
    }
}
