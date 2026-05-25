package com.pluscubed.am2geo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pluscubed.am2geo.net.ShortLinkResolver
import com.pluscubed.am2geo.parser.AppleMapsUrlParser
import com.pluscubed.am2geo.parser.GoogleMapsUrlBuilder
import com.pluscubed.am2geo.parser.ParsedAppleMaps
import kotlinx.coroutines.launch

/**
 * No-UI activity that owns the VIEW intent-filters for Apple Maps URLs. It
 * parses the URL (resolving the `maps.apple/p/...` short-link form over HTTP
 * if needed), builds a `geo:` / Google Maps URL, hands the intent off, and
 * finishes.
 *
 * Uses a translucent theme (not `Theme.NoDisplay`) so we can suspend on the
 * short-link HEAD request without violating the "must finish before onResume"
 * rule that `NoDisplay` enforces.
 */
class RedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val source = intent?.data
        if (source == null) {
            finish()
            return
        }

        if (isShortLink(source)) {
            lifecycleScope.launch {
                val resolved = ShortLinkResolver.resolve(source.toString())
                dispatch(resolved ?: source.toString(), originalUri = source)
                finish()
            }
        } else {
            dispatch(source.toString(), originalUri = source)
            finish()
        }
    }

    /** True for `https://maps.apple/p/<token>` style short links. */
    private fun isShortLink(uri: Uri): Boolean = uri.host?.equals("maps.apple", ignoreCase = true) == true

    /**
     * Parse [url], build a target URI, and start the maps intent. If anything
     * fails we hand the *original* URI to a chooser so the user at least gets
     * a browser fallback rather than a dead-end.
     */
    private fun dispatch(url: String, originalUri: Uri) {
        val parsed = AppleMapsUrlParser.parse(url)
        if (parsed is ParsedAppleMaps.Unparseable) {
            Log.w(TAG, "Unparseable Apple Maps URL: $url")
            fallbackChooser(originalUri)
            return
        }
        val target = GoogleMapsUrlBuilder.build(parsed)
        if (target == null) {
            fallbackChooser(originalUri)
            return
        }

        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(viewIntent)
        } catch (_: ActivityNotFoundException) {
            // No maps app installed: fall back to the universal Google Maps
            // search URL, which any browser handles.
            val webFallback = webFallbackFor(parsed)
            if (webFallback != null) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webFallback))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: ActivityNotFoundException) { /* fall through */ }
            }
            fallbackChooser(originalUri)
        }
    }

    /**
     * Universal https URL Google Maps / any browser can open. Uses the same
     * "real POI → search by name+address, otherwise → search by coordinate"
     * heuristic as the geo: builder so the fallback lands on the business
     * listing when we have one and on the exact pin otherwise.
     */
    private fun webFallbackFor(parsed: ParsedAppleMaps): String? {
        return when (parsed) {
            is ParsedAppleMaps.Point -> {
                val isRealPoi =
                    !parsed.address.isNullOrBlank() || !parsed.placeId.isNullOrBlank()
                val query = if (isRealPoi) {
                    listOfNotNull(
                        parsed.name?.takeIf { it.isNotBlank() },
                        parsed.address?.takeIf { it.isNotBlank() },
                    ).joinToString(", ")
                } else {
                    parsed.coordinate?.let { "${it.lat},${it.lng}" } ?: parsed.name ?: return null
                }
                if (query.isBlank()) return null
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
            }
            is ParsedAppleMaps.Search ->
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(parsed.query)}"
            is ParsedAppleMaps.Directions -> GoogleMapsUrlBuilder.build(parsed)
            ParsedAppleMaps.Unparseable -> null
        }
    }

    /** Last-ditch fallback: show a chooser for the original Apple Maps URL. */
    private fun fallbackChooser(originalUri: Uri) {
        val view = Intent(Intent.ACTION_VIEW, originalUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(Intent.createChooser(view, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.redirect_failed, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "am2geo.redirect"
    }
}
