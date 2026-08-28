package com.impostor.party.game

import com.impostor.party.data.model.Category
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.GameConfig
import com.impostor.party.data.model.HintMode
import com.impostor.party.data.model.PlayerRole
import com.impostor.party.data.model.Round
import com.impostor.party.data.model.RoundOutcome
import com.impostor.party.data.model.WordEntry
import kotlin.random.Random

/**
 * Pure game logic. No Android types in here, which keeps it unit-testable and
 * makes the fairness of the randomisation easy to reason about.
 */
object GameRules {

    const val MIN_PLAYERS = 3
    const val MAX_PLAYERS = 20

    /**
     * The only hard rule is that somebody has to know the word, so at most
     * players - 1 impostors. Everything past half is the group's business.
     */
    fun maxImpostorsFor(playerCount: Int): Int =
        (playerCount).coerceAtLeast(1)

    /** Past this the impostors outnumber the crew - allowed, but worth flagging. */
    fun balancedImpostorsFor(playerCount: Int): Int =
        ((playerCount - 1) / 2).coerceAtLeast(1)

    fun clampImpostors(playerCount: Int, impostors: Int): Int =
        impostors.coerceIn(1, maxImpostorsFor(playerCount))

    /** True when the impostors would outnumber or match the crew. */
    fun isOutnumbered(playerCount: Int, impostors: Int): Boolean =
        impostors >= playerCount - impostors

    fun isConfigValid(config: GameConfig): Boolean =
        config.playerCount in MIN_PLAYERS..MAX_PLAYERS &&
            config.impostorCount in 1..maxImpostorsFor(config.playerCount)

    /**
     * Which word tags a difficulty setting draws from. The bands overlap so the
     * middle settings still have a decent pool, and MIXED opens the whole list.
     */
    fun allowedDifficulties(setting: Difficulty): Set<Difficulty> = when (setting) {
        Difficulty.EASY -> setOf(Difficulty.EASY)
        Difficulty.MEDIUM -> setOf(Difficulty.EASY, Difficulty.MEDIUM)
        Difficulty.HARD -> setOf(Difficulty.MEDIUM, Difficulty.HARD)
        Difficulty.MIXED -> Difficulty.wordTags.toSet()
    }

    fun defaultPlayerName(index: Int): String = "Player ${index + 1}"

    /** Fills in blanks so every seat always has a usable label. */
    fun resolveNames(
        config: GameConfig,
        defaultName: (Int) -> String = ::defaultPlayerName,
    ): List<String> =
        (0 until config.playerCount).map { i ->
            config.playerNames.getOrNull(i)?.trim()?.takeIf { it.isNotEmpty() }
                ?: defaultName(i)
        }

    /**
     * Builds a round: one secret word shared by every crew member, and a fairly
     * drawn, non-overlapping set of impostors.
     */
    fun buildRound(
        config: GameConfig,
        category: Category,
        entry: WordEntry,
        random: Random,
        defaultName: (Int) -> String = ::defaultPlayerName,
    ): Round {
        val ceiling = clampImpostors(config.playerCount, config.impostorCount)
        // With the random option the count itself is hidden information: anywhere
        // from one impostor up to the ceiling the group set.
        val impostorCount = if (config.randomImpostors) random.nextInt(1, ceiling + 1) else ceiling
        val names = resolveNames(config, defaultName)

        // A full shuffle of the seats, then take the first N. Every seat has exactly
        // the same chance of being an impostor, and a seat can only be drawn once.
        val impostors = (0 until config.playerCount)
            .shuffled(random)
            .take(impostorCount)
            .toSet()

        val hint = when (config.hintMode) {
            HintMode.NONE -> null
            HintMode.EASY -> entry.easyHint
            HintMode.VAGUE -> entry.vagueHint
        }

        val roles = (0 until config.playerCount).map { index ->
            val isImpostor = index in impostors
            PlayerRole(
                index = index,
                name = names[index],
                isImpostor = isImpostor,
                word = if (isImpostor) null else entry.word,
                hint = if (isImpostor) hint else null,
            )
        }

        // Drawn independently of the impostor shuffle, across every seat, so the
        // impostor is just as likely to have to speak first as anyone else.
        val startingPlayer = random.nextInt(config.playerCount)

        return Round(
            config = config.copy(impostorCount = ceiling, playerNames = names),
            categoryId = category.id,
            categoryName = category.name,
            categoryEmoji = category.emoji,
            secretWord = entry.word,
            impostorIndices = impostors,
            roles = roles,
            startingPlayerIndex = startingPlayer,
        )
    }

    /**
     * Decides the round.
     *
     * The N most-voted players are ejected, where N is however many impostors this
     * round actually had. If the vote is tied at the cut-off there is no clear
     * ejection, nobody goes, and the impostors survive. The crew only wins by
     * ejecting exactly the impostors.
     */
    fun outcome(round: Round, votes: Map<Int, Int>): RoundOutcome {
        if (votes.isEmpty()) {
            return RoundOutcome(hasVerdict = false, crewWins = false, ejected = emptySet(), tied = false)
        }

        val counts = tally(votes)
        val k = round.impostorIndices.size
        val ranked = counts.entries.sortedByDescending { it.value }

        if (ranked.size <= k) {
            val ejected = ranked.map { it.key }.toSet()
            return RoundOutcome(
                hasVerdict = true,
                crewWins = ejected == round.impostorIndices,
                ejected = ejected,
                tied = false,
            )
        }

        if (ranked[k - 1].value == ranked[k].value) {
            // Ambiguous - the group could not agree, so nobody is ejected.
            return RoundOutcome(hasVerdict = true, crewWins = false, ejected = emptySet(), tied = true)
        }

        val ejected = ranked.take(k).map { it.key }.toSet()
        return RoundOutcome(
            hasVerdict = true,
            crewWins = ejected == round.impostorIndices,
            ejected = ejected,
            tied = false,
        )
    }

    /** Vote counts per player index. */
    fun tally(votes: Map<Int, Int>): Map<Int, Int> {
        val result = HashMap<Int, Int>()
        for (suspect in votes.values) {
            result[suspect] = (result[suspect] ?: 0) + 1
        }
        return result
    }
}
