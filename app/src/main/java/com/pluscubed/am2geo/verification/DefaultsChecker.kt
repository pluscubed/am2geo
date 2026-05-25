package com.pluscubed.am2geo.verification

import android.content.Context
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build

/** Whether am2geo is currently set up to receive Apple Maps links. */
enum class DefaultsState {
    /** Both required domains are enabled and the master toggle is on. */
    Configured,

    /** At least one domain is missing, or the user has the master toggle off. */
    NotConfigured,

    /** Running on Android < 12, where we can't query this state. */
    Unknown,
}

/**
 * Reads the live link-handling state for am2geo's domains. Backed by
 * [DomainVerificationManager] on API 31+; returns [DefaultsState.Unknown] on
 * older Android because the API doesn't exist there.
 *
 * Mirrors what `adb shell pm get-app-links --user 0 com.pluscubed.am2geo`
 * reports — those two checks are the most direct surface for whether tapping
 * a maps.apple.com link will route through us or get punted to a browser.
 */
object DefaultsChecker {

    private val REQUIRED_HOSTS = setOf("maps.apple.com", "maps.apple")

    fun check(context: Context): DefaultsState {
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return DefaultsState.Unknown

        val manager = context.getSystemService(DomainVerificationManager::class.java)
            ?: return DefaultsState.Unknown

        val userState = try {
            manager.getDomainVerificationUserState(context.packageName)
        } catch (_: Exception) {
            // Some OEMs throw IllegalArgumentException for packages they
            // don't track; treat as "we can't tell, fall back to setup UI".
            return DefaultsState.Unknown
        } ?: return DefaultsState.Unknown

        if (!userState.isLinkHandlingAllowed) return DefaultsState.NotConfigured

        val hostStates = userState.hostToStateMap
        val allEnabled = REQUIRED_HOSTS.all { host ->
            val s = hostStates[host] ?: return@all false
            s == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                s == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
        }
        return if (allEnabled) DefaultsState.Configured else DefaultsState.NotConfigured
    }
}
