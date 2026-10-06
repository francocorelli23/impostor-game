package com.impostor.party.util

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * One place that decides whether a given interaction makes a sound, a buzz, both or
 * neither. Haptics go through Compose's platform feedback API, which means the app
 * needs no VIBRATE permission.
 */
class Feedback(
    private val sound: SoundManager?,
    private val haptics: HapticFeedback?,
    private val soundEnabled: () -> Boolean,
    private val hapticsEnabled: () -> Boolean,
) {

    fun tap() {
        sfx(Sfx.TAP)
        light()
    }

    fun select() {
        sfx(Sfx.VOTE)
        light()
    }

    /**
     * Deliberately identical for crew and impostor: a different clip for the
     * impostor would let anyone listening pick them out from across the room.
     */
    fun reveal() {
        sfx(Sfx.REVEAL)
        strong()
    }

    fun hide() {
        sfx(Sfx.HIDE)
        light()
    }

    fun tick() {
        sfx(Sfx.TICK, 0.7f)
    }

    fun win() {
        sfx(Sfx.WIN)
        strong()
    }

    fun lose() {
        sfx(Sfx.LOSE)
        strong()
    }

    private fun sfx(effect: Sfx, volume: Float = 1f) {
        if (soundEnabled()) sound?.play(effect, volume)
    }

    private fun light() {
        if (hapticsEnabled()) {
            runCatching { haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        }
    }

    private fun strong() {
        if (hapticsEnabled()) {
            runCatching { haptics?.performHapticFeedback(HapticFeedbackType.LongPress) }
        }
    }

    companion object {
        val Silent = Feedback(null, null, { false }, { false })
    }
}

val LocalFeedback = staticCompositionLocalOf { Feedback.Silent }
