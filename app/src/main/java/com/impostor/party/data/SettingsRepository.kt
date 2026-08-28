package com.impostor.party.data

import android.content.Context
import android.content.SharedPreferences
import com.impostor.party.data.model.AppLanguage
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.GameConfig
import com.impostor.party.data.model.HintMode
import com.impostor.party.data.model.ThemeMode
import com.impostor.party.game.GameRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class Settings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultPlayers: Int = 6,
    val defaultImpostors: Int = 1,
    val defaultRandomImpostors: Boolean = false,
    /** Empty means every category. */
    val defaultCategoryIds: Set<String> = emptySet(),
    val defaultDifficulty: Difficulty = Difficulty.MEDIUM,
    val defaultHintMode: HintMode = HintMode.NONE,
    val defaultHideCategoryFromImpostor: Boolean = false,
    /**
     * Player names stick around between games - the same group usually plays
     * several rounds in a row. They are only cleared when somebody asks.
     * Kept at full length even when the player count drops, so shrinking the
     * table and growing it again does not lose anyone.
     */
    val playerNames: List<String> = emptyList(),
    val defaultDiscussionSeconds: Int = 0,
    val defaultVotingSeconds: Int = 0,
) {
    /** Builds the starting configuration for the setup screen. */
    fun toConfig(): GameConfig {
        val players = defaultPlayers.coerceIn(GameRules.MIN_PLAYERS, GameRules.MAX_PLAYERS)
        return GameConfig(
            playerCount = players,
            impostorCount = GameRules.clampImpostors(players, defaultImpostors),
            randomImpostors = defaultRandomImpostors,
            categoryIds = defaultCategoryIds,
            difficulty = defaultDifficulty,
            hintMode = defaultHintMode,
            hideCategoryFromImpostor = defaultHideCategoryFromImpostor,
            discussionSeconds = defaultDiscussionSeconds,
            votingSeconds = defaultVotingSeconds,
            playerNames = playerNames,
        )
    }
}

/**
 * Preferences live in a single private SharedPreferences file on the device.
 * Nothing leaves the phone.
 */
class SettingsRepository(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    /**
     * The language tag the word list should be read in. SYSTEM follows the device,
     * falling back to English for any locale the game has not been translated into.
     */
    fun effectiveLanguageTag(): String = when (val language = _settings.value.language) {
        AppLanguage.SYSTEM -> {
            val device = Locale.getDefault().language
            if (device in WordRepository.SUPPORTED) device else "en"
        }
        else -> language.key
    }

    private fun read(): Settings {
        val defaults = Settings()
        return Settings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, defaults.soundEnabled),
            musicEnabled = prefs.getBoolean(KEY_MUSIC, defaults.musicEnabled),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, defaults.vibrationEnabled),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, defaults.keepScreenOn),
            themeMode = ThemeMode.fromKey(prefs.getString(KEY_THEME, null)),
            language = AppLanguage.fromKey(prefs.getString(KEY_LANGUAGE, null)),
            defaultPlayers = prefs.getInt(KEY_PLAYERS, defaults.defaultPlayers)
                .coerceIn(GameRules.MIN_PLAYERS, GameRules.MAX_PLAYERS),
            defaultImpostors = prefs.getInt(KEY_IMPOSTORS, defaults.defaultImpostors)
                .coerceAtLeast(1),
            defaultRandomImpostors = prefs.getBoolean(KEY_RANDOM_IMPOSTORS, defaults.defaultRandomImpostors),
            defaultCategoryIds = prefs.getStringSet(KEY_CATEGORIES, null)?.toSet() ?: emptySet(),
            defaultDifficulty = Difficulty.fromKey(prefs.getString(KEY_DIFFICULTY, null)),
            defaultHintMode = HintMode.fromKey(prefs.getString(KEY_HINT, null)),
            defaultHideCategoryFromImpostor = prefs.getBoolean(
                KEY_HIDE_CATEGORY,
                defaults.defaultHideCategoryFromImpostor,
            ),
            defaultDiscussionSeconds = prefs.getInt(KEY_DISCUSSION, defaults.defaultDiscussionSeconds)
                .coerceAtLeast(0),
            defaultVotingSeconds = prefs.getInt(KEY_VOTING, defaults.defaultVotingSeconds)
                .coerceAtLeast(0),
            playerNames = readNames(),
        )
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    /** Stores the configuration the player just started a game with as the new default. */
    fun rememberConfig(config: GameConfig) {
        update {
            it.copy(
                defaultPlayers = config.playerCount,
                defaultImpostors = config.impostorCount,
                defaultRandomImpostors = config.randomImpostors,
                defaultCategoryIds = config.categoryIds,
                defaultDifficulty = config.difficulty,
                defaultHintMode = config.hintMode,
                defaultHideCategoryFromImpostor = config.hideCategoryFromImpostor,
                defaultDiscussionSeconds = config.discussionSeconds,
                defaultVotingSeconds = config.votingSeconds,
                // playerNames deliberately untouched here: by the time a round
                // starts every blank seat has been filled with a generated label,
                // so this is not the place to learn what anyone actually typed.
                // The name editor persists them directly instead.
            )
        }
    }

    fun resetToDefaults() {
        // The chosen language survives a reset - it is how the player reads the menu.
        val defaults = Settings(language = _settings.value.language)
        write(defaults)
        _settings.value = defaults
    }

    private fun write(s: Settings) {
        prefs.edit()
            .putBoolean(KEY_SOUND, s.soundEnabled)
            .putBoolean(KEY_MUSIC, s.musicEnabled)
            .putBoolean(KEY_VIBRATION, s.vibrationEnabled)
            .putBoolean(KEY_KEEP_SCREEN_ON, s.keepScreenOn)
            .putString(KEY_THEME, s.themeMode.key)
            .putString(KEY_LANGUAGE, s.language.key)
            .putInt(KEY_PLAYERS, s.defaultPlayers)
            .putInt(KEY_IMPOSTORS, s.defaultImpostors)
            .putBoolean(KEY_RANDOM_IMPOSTORS, s.defaultRandomImpostors)
            .putStringSet(KEY_CATEGORIES, s.defaultCategoryIds)
            .putString(KEY_DIFFICULTY, s.defaultDifficulty.key)
            .putString(KEY_HINT, s.defaultHintMode.key)
            .putBoolean(KEY_HIDE_CATEGORY, s.defaultHideCategoryFromImpostor)
            .putInt(KEY_DISCUSSION, s.defaultDiscussionSeconds)
            .putInt(KEY_VOTING, s.defaultVotingSeconds)
            .putString(KEY_NAMES, s.playerNames.joinToString(NAME_SEPARATOR))
            .apply()
    }

    private fun readNames(): List<String> {
        val raw = prefs.getString(KEY_NAMES, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(NAME_SEPARATOR).map { it.trim() }.dropLastWhile { it.isEmpty() }
    }

    companion object {
        const val FILE_NAME = "impostor_settings"

        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PLAYERS = "default_players"
        private const val KEY_IMPOSTORS = "default_impostors"
        private const val KEY_RANDOM_IMPOSTORS = "default_random_impostors"
        private const val KEY_CATEGORIES = "default_categories"
        private const val KEY_DIFFICULTY = "default_difficulty"
        private const val KEY_HINT = "default_hint"
        private const val KEY_HIDE_CATEGORY = "default_hide_category"
        private const val KEY_DISCUSSION = "default_discussion_seconds"
        private const val KEY_VOTING = "default_voting_seconds"
        private const val KEY_NAMES = "player_names"

        /** ASCII record separator - cannot appear in a typed name. */
        private const val NAME_SEPARATOR = "\u001E"
    }
}
