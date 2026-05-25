package com.pluscubed.am2geo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pluscubed.am2geo.R
import com.pluscubed.am2geo.ui.theme.LocalStatusPalette
import com.pluscubed.am2geo.verification.DefaultsState

/**
 * Home screen. Built on Material 3 components and dynamic colour. The
 * personality comes from typography (Instrument Serif italic headline + a
 * mono technical readout) and the at-a-glance status hero — a Material
 * Icon tinted with a semantic accent — not from custom drawing.
 *
 * Three states (mirrored from [DefaultsState]):
 *  - [DefaultsState.Configured]    → green check, "Ready", domains ON.
 *  - [DefaultsState.NotConfigured] → red cross, "Not yet", CTA filled red.
 *  - [DefaultsState.Unknown]       → amber settings, manual setup, test field is the verification path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: DefaultsState,
    onOpenDefaultsSettings: () -> Unit,
    onTestUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Have Scaffold fold IME + cutout + system bars into innerPadding so
        // we don't double-count with a separate imePadding() further down.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        // Two stacked sections: a scrollable upper section (hero + domains +
        // action) that takes the remaining height, and the TestCard pinned to
        // the very bottom of the visible (IME-aware) area. This way the test
        // input sits right above the keyboard with no Scaffold-background gap.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                StatusHero(state = state)

                DomainCard(state = state)

                ActionGroup(state = state, onClick = onOpenDefaultsSettings)
            }

            Spacer(Modifier.height(32.dp))

            TestCard(onSubmit = onTestUrl)

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status hero — big Material icon in a tinted circular Surface, with serif
// headline below.

@Composable
private fun StatusHero(state: DefaultsState) {
    val palette = LocalStatusPalette.current
    val (icon, accent, onAccent) = when (state) {
        DefaultsState.Configured -> Triple(Icons.Filled.Check, palette.configured, palette.onConfigured)
        DefaultsState.NotConfigured -> Triple(Icons.Filled.Close, palette.notConfigured, palette.onNotConfigured)
        DefaultsState.Unknown -> Triple(Icons.Filled.Settings, palette.unknown, palette.onUnknown)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(144.dp),
            // Soft squircle — reads as round at a glance, but the slight
            // flat sides feel more M3 Expressive than a perfect circle.
            shape = RoundedCornerShape(percent = 42),
            color = accent,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(state.headlineRes()),
                    tint = onAccent,
                    modifier = Modifier.size(80.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn() togetherWith fadeOut())
            },
            label = "status-headline",
        ) { current ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(current.headlineRes()),
                    style = MaterialTheme.typography.headlineLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(current.subRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Domain list — uses M3 ListItem + OutlinedCard so it feels native.

@Composable
private fun DomainCard(state: DefaultsState) {
    val palette = LocalStatusPalette.current
    val accent = when (state) {
        DefaultsState.Configured -> palette.configured
        DefaultsState.NotConfigured -> palette.notConfigured
        DefaultsState.Unknown -> palette.unknown
    }
    val trailingLabel = when (state) {
        DefaultsState.Configured -> stringResource(R.string.status_on)
        DefaultsState.NotConfigured -> stringResource(R.string.status_off)
        DefaultsState.Unknown -> stringResource(R.string.status_manual_short)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        DomainRow("maps.apple.com", trailingLabel, accent)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DomainRow("maps.apple", trailingLabel, accent)
    }
}

@Composable
private fun DomainRow(domain: String, trailingLabel: String, accent: Color) {
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = accent, shape = CircleShape),
            )
        },
        headlineContent = {
            Text(
                text = domain,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailingContent = {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Action group — primary call when not configured, secondary text when ready,
// plus a supporting caption when we have one to soften the "where do I go in
// settings?" cliff.

@Composable
private fun ActionGroup(state: DefaultsState, onClick: () -> Unit) {
    val palette = LocalStatusPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state) {
            DefaultsState.Configured -> {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_manage))
                }
            }
            DefaultsState.NotConfigured -> {
                FilledStatusButton(
                    label = stringResource(R.string.action_enable),
                    container = palette.notConfigured,
                    content = palette.onNotConfigured,
                    onClick = onClick,
                )
                Text(
                    text = stringResource(R.string.status_caption_not_configured),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            DefaultsState.Unknown -> {
                FilledStatusButton(
                    label = stringResource(R.string.action_enable),
                    container = palette.unknown,
                    content = palette.onUnknown,
                    onClick = onClick,
                )
                Text(
                    text = stringResource(R.string.status_caption_manual),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FilledStatusButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
    ) {
        Text(text = label)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Test card — paste-in field with submit icon button. Slightly more
// prominent when state == Unknown, since that's the only way to verify.

@Composable
private fun TestCard(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_test_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.home_test_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (text.isNotBlank()) {
                                onSubmit(text.trim())
                                keyboard?.hide()
                            }
                        },
                    ),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSubmit(text.trim())
                            keyboard?.hide()
                        }
                    },
                    enabled = text.isNotBlank(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.home_test_button),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State-to-resource mapping

private fun DefaultsState.headlineRes(): Int = when (this) {
    DefaultsState.Configured -> R.string.status_headline_configured
    DefaultsState.NotConfigured -> R.string.status_headline_not_configured
    DefaultsState.Unknown -> R.string.status_headline_unknown
}

private fun DefaultsState.subRes(): Int = when (this) {
    DefaultsState.Configured -> R.string.status_sub_configured
    DefaultsState.NotConfigured -> R.string.status_sub_not_configured
    DefaultsState.Unknown -> R.string.status_sub_unknown
}
