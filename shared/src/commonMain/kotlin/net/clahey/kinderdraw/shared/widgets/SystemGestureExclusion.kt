package net.clahey.kinderdraw.shared.widgets

import androidx.compose.ui.Modifier

/**
 * Registers this composable's on-screen bounds as excluded from the
 * platform's system gesture navigation, for as long as it stays composed —
 * see the Widgets LLD's System Gesture Coexistence. A no-op on platforms
 * with no equivalent concept.
 */
internal expect fun Modifier.excludeFromSystemGestures(): Modifier
