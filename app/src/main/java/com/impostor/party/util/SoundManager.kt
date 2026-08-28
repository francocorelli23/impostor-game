package com.impostor.party.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.impostor.party.R

enum class Sfx {
    TAP,
    REVEAL,
    IMPOSTOR,
    HIDE,
    VOTE,
    TICK,
    WIN,
    LOSE,
}

/**
 * Short effects go through SoundPool (low latency, tiny memory footprint) and the
 * optional ambient loop through MediaPlayer. Every clip is bundled in res/raw, so
 * audio works with the device in flight mode.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(attributes)
        .build()

    private val loaded = HashMap<Sfx, Int>()
    private val ready = HashSet<Int>()

    private var music: MediaPlayer? = null
    private var musicShouldPlay = false
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready.add(sampleId)
        }
        load(Sfx.TAP, R.raw.sfx_tap)
        load(Sfx.REVEAL, R.raw.sfx_reveal)
        load(Sfx.IMPOSTOR, R.raw.sfx_impostor)
        load(Sfx.HIDE, R.raw.sfx_hide)
        load(Sfx.VOTE, R.raw.sfx_vote)
        load(Sfx.TICK, R.raw.sfx_tick)
        load(Sfx.WIN, R.raw.sfx_win)
        load(Sfx.LOSE, R.raw.sfx_lose)
    }

    private fun load(sfx: Sfx, resId: Int) {
        try {
            loaded[sfx] = pool.load(appContext, resId, 1)
        } catch (t: Throwable) {
            // Audio is a nicety; a device that cannot decode a clip still plays the game.
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (id !in ready) return
        try {
            pool.play(id, volume, volume, 1, 0, 1f)
        } catch (t: Throwable) {
            // ignored on purpose
        }
    }

    // ------------------------------------------------------------------ music

    fun setMusicEnabled(enabled: Boolean) {
        musicShouldPlay = enabled
        if (enabled) startMusic() else stopMusic()
    }

    private fun startMusic() {
        if (released || music != null) return
        try {
            music = MediaPlayer.create(appContext, R.raw.music_ambient)?.apply {
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                start()
            }
        } catch (t: Throwable) {
            music = null
        }
    }

    private fun stopMusic() {
        music?.let {
            try {
                it.stop()
            } catch (t: Throwable) {
                // ignored
            }
            it.release()
        }
        music = null
    }

    /** Called when the app leaves the foreground. */
    fun pauseMusic() {
        music?.let { if (it.isPlaying) it.pause() }
    }

    /** Called when the app comes back, honouring the current preference. */
    fun resumeMusic() {
        if (!musicShouldPlay) return
        val current = music
        if (current == null) startMusic() else if (!current.isPlaying) current.start()
    }

    fun release() {
        if (released) return
        released = true
        stopMusic()
        pool.release()
    }

    private companion object {
        const val MUSIC_VOLUME = 0.35f
    }
}
