package com.impostor.party.data.model

/**
 * How hard the secret words are allowed to be.
 *
 * [EASY], [MEDIUM] and [HARD] are also the tags carried by individual words in
 * the word list. [MIXED] never appears on a word - it exists only as a setting,
 * meaning "draw from the whole list".
 */
enum class Difficulty(val key: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard"),
    MIXED("mixed");

    companion object {
        /** The three tags a word can actually carry. */
        val wordTags = listOf(EASY, MEDIUM, HARD)

        fun fromKey(key: String?): Difficulty =
            entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

/** How much help, if any, an impostor gets. */
enum class HintMode(val key: String) {
    NONE("none"),
    EASY("easy"),
    VAGUE("vague");

    companion object {
        fun fromKey(key: String?): HintMode =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Interface and word-list language. SYSTEM follows the device; the other entries
 * override it for this app only.
 */
enum class AppLanguage(val key: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    CROATIAN("hr");

    companion object {
        /** Every language the word list ships in, in menu order. */
        val selectable = listOf(SYSTEM, ENGLISH, CROATIAN)

        fun fromKey(key: String?): AppLanguage =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

data class WordEntry(
    val word: String,
    /** Null when nobody wrote one - the impostor then simply gets no hint. */
    val easyHint: String?,
    /** Null when nobody wrote one - the impostor then simply gets no hint. */
    val vagueHint: String?,
    val difficulty: Difficulty,
    /** Added by the players rather than shipped with the app. Eligible at every difficulty. */
    val isCustom: Boolean = false,
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val words: List<WordEntry>,
    /** Created by the players rather than shipped with the app. */
    val isCustom: Boolean = false,
) {
    /** How many of [words] the players added themselves. */
    val customWordCount: Int get() = words.count { it.isCustom }
}

data class GameConfig(
    val playerCount: Int,
    /** When [randomImpostors] is on this is the ceiling, not the exact count. */
    val impostorCount: Int,
    /** Draw a fresh, secret impostor count of 1..impostorCount every round. */
    val randomImpostors: Boolean,
    /** Categories in play. Empty means every category. */
    val categoryIds: Set<String>,
    val difficulty: Difficulty,
    val hintMode: HintMode,
    /** Hide even the category from the impostors, for a much harder game. */
    val hideCategoryFromImpostor: Boolean,
    /** 0 means the discussion is untimed. */
    val discussionSeconds: Int,
    /** 0 means voting is untimed. */
    val votingSeconds: Int,
    val playerNames: List<String>,
) {
    val isTimedDiscussion: Boolean get() = discussionSeconds > 0
    val isTimedVoting: Boolean get() = votingSeconds > 0
    val usesAllCategories: Boolean get() = categoryIds.isEmpty()
}

data class PlayerRole(
    val index: Int,
    val name: String,
    val isImpostor: Boolean,
    /** Null for impostors - they never receive the word. */
    val word: String?,
    /** Only ever set for impostors, and only when hints are switched on. */
    val hint: String?,
)

data class Round(
    val config: GameConfig,
    val categoryId: String,
    val categoryName: String,
    val categoryEmoji: String,
    val secretWord: String,
    val impostorIndices: Set<Int>,
    val roles: List<PlayerRole>,
    /**
     * Who gives the first clue. Drawn fresh every round from every seat, the
     * impostors included - going first is a disadvantage, so it should not
     * always land on the same person.
     */
    val startingPlayerIndex: Int,
) {
    val impostorNames: List<String>
        get() = impostorIndices.sorted().mapNotNull { roles.getOrNull(it)?.name }

    val startingPlayerName: String
        get() = roles.getOrNull(startingPlayerIndex)?.name.orEmpty()

    /** True when the impostors were not even told which category the word is from. */
    val categoryHiddenFromImpostor: Boolean get() = config.hideCategoryFromImpostor

    /**
     * With a random count the impostors must not learn how many of them there are,
     * so the reveal card stays deliberately vague.
     */
    val revealsImpostorCount: Boolean get() = !config.randomImpostors
}

/** The verdict for a finished round. */
data class RoundOutcome(
    val hasVerdict: Boolean,
    val crewWins: Boolean,
    val ejected: Set<Int>,
    val tied: Boolean,
)
