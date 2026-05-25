package com.pluscubed.am2geo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pluscubed.am2geo.ui.HomeScreen
import com.pluscubed.am2geo.ui.theme.Am2geoTheme
import com.pluscubed.am2geo.verification.DefaultsChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be installed before super.onCreate(): the SplashScreen API
        // swaps the launcher Theme.Am2geo.Starting for postSplashScreenTheme
        // (= Theme.Am2geo) at this point, so the window background stays
        // continuous from splash → Compose surface.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Am2geoTheme {
                // Recompute the link-handler state every time the activity
                // resumes — that's how we react to the user going to Settings,
                // toggling the domains, and coming back.
                val context = LocalContext.current
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                var state by remember { mutableStateOf(DefaultsChecker.check(context)) }
                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            state = DefaultsChecker.check(context)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        state = state,
                        onOpenDefaultsSettings = ::openDefaultsSettings,
                        onTestUrl = ::launchTestUrl,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    /**
     * Open the system page where the user can mark this app as the default
     * handler for `maps.apple.com` / `maps.apple` links. On API 31+ we can deep
     * link straight to "Open by default"; before that we open the app's info
     * page and the user navigates the rest of the way.
     */
    private fun openDefaultsSettings() {
        val packageUri = Uri.parse("package:$packageName")
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, packageUri)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some OEM ROMs don't expose the per-app open-by-default screen;
            // fall back to the generic app info page.
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.home_settings_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Send a URL through the real redirect path. We explicitly target
     * [RedirectActivity] (rather than firing an implicit ACTION_VIEW) so the
     * test exercises our code even when am2geo isn't yet the default handler.
     */
    private fun launchTestUrl(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, R.string.home_test_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setClass(this, RedirectActivity::class.java)
        startActivity(intent)
    }
}
