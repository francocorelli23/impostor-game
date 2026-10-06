package com.impostor.party

import com.impostor.party.data.CustomCategory
import com.impostor.party.data.CustomWordList
import com.impostor.party.data.model.Category
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.GameConfig
import com.impostor.party.data.model.HintMode
import com.impostor.party.data.model.WordEntry
import com.impostor.party.game.GameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {

    private val entry = WordEntry("Pizza", "Italian food", "Round and shared", Difficulty.EASY)
    private val category = Category("food", "Food", "🍔", listOf(entry))

    private fun config(
        players: Int,
        impostors: Int,
        hints: HintMode = HintMode.NONE,
        randomImpostors: Boolean = false,
    ) = GameConfig(
        playerCount = players,
        impostorCount = impostors,
        randomImpostors = randomImpostors,
        categoryIds = emptySet(),
        difficulty = Difficulty.EASY,
        hintMode = hints,
        hideCategoryFromImpostor = false,
        discussionSeconds = 0,
        votingSeconds = 0,
        playerNames = emptyList(),
    )

    private fun build(
        players: Int,
        impostors: Int,
        hints: HintMode = HintMode.NONE,
        randomImpostors: Boolean = false,
        seed: Int = 1,
    ) = GameRules.buildRound(
        config = config(players, impostors, hints, randomImpostors),
        category = category,
        entry = entry,
        random = Random(seed),
    )

    // ------------------------------------------------------------------ roles

    @Test
    fun `crew all share exactly one word`() {
        val round = build(players = 6, impostors = 1)
        val crew = round.roles.filterNot { it.isImpostor }
        assertEquals(5, crew.size)
        assertEquals(setOf("Pizza"), crew.map { it.word }.toSet())
    }

    @Test
    fun `impostors never receive the word`() {
        val round = build(players = 8, impostors = 2)
        val impostors = round.roles.filter { it.isImpostor }
        assertEquals(2, impostors.size)
        impostors.forEach { assertNull(it.word) }
    }

    @Test
    fun `impostor indices match the roles marked as impostor`() {
        repeat(50) { seed ->
            val round = build(players = 9, impostors = 3, seed = seed)
            val fromRoles = round.roles.filter { it.isImpostor }.map { it.index }.toSet()
            assertEquals(round.impostorIndices, fromRoles)
            assertEquals(3, round.impostorIndices.size)
        }
    }

    // ------------------------------------------------------------------ hints

    @Test
    fun `hints only reach impostors and only when enabled`() {
        build(players = 5, impostors = 1, hints = HintMode.NONE)
            .roles.forEach { assertNull(it.hint) }

        build(players = 5, impostors = 1, hints = HintMode.EASY).roles.forEach {
            if (it.isImpostor) assertEquals("Italian food", it.hint) else assertNull(it.hint)
        }

        build(players = 5, impostors = 1, hints = HintMode.VAGUE)
            .roles.filter { it.isImpostor }
            .forEach { assertEquals("Round and shared", it.hint) }
    }

    @Test
    fun `a word saved without hints gives the impostor no hint in any mode`() {
        val bare = WordEntry("Grandma's soup", null, null, Difficulty.MEDIUM, isCustom = true)
        for (mode in HintMode.entries) {
            val round = GameRules.buildRound(config(5, 1, mode), category, bare, Random(3))
            round.roles.forEach { assertNull(it.hint) }
        }
    }

    @Test
    fun `a word with only one hint shows it only in that mode`() {
        val easyOnly = WordEntry("Lake", "Water", "   ", Difficulty.MEDIUM, isCustom = true)
        fun hintsFor(mode: HintMode) = GameRules.buildRound(config(5, 1, mode), category, easyOnly, Random(4))
            .roles.filter { it.isImpostor }.map { it.hint }
        assertEquals(listOf("Water"), hintsFor(HintMode.EASY))
        assertEquals(listOf<String?>(null), hintsFor(HintMode.VAGUE))
    }

    // ----------------------------------------------------------- custom words

    @Test
    fun `custom words join their category and custom categories follow`() {
        val mine = WordEntry("Burek", null, null, Difficulty.MEDIUM, isCustom = true)
        val party = WordEntry("Karaoke", "Singing", null, Difficulty.MEDIUM, isCustom = true)
        val custom = CustomWordList(
            categories = listOf(
                CustomCategory("custom_a", "Party", "🎉"),
                CustomCategory("custom_empty", "Empty", "⭐"),
            ),
            words = mapOf("food" to listOf(mine), "custom_a" to listOf(party)),
        )
        val merged = GameRules.mergeCustomWords(listOf(category), custom)

        assertEquals(listOf("food", "custom_a"), merged.map { it.id })
        assertEquals(listOf("Pizza", "Burek"), merged[0].words.map { it.word })
        assertEquals(1, merged[0].customWordCount)
        assertFalse(merged[0].isCustom)
        assertTrue(merged[1].isCustom)
        assertEquals("Party", merged[1].name)
    }

    @Test
    fun `custom words are in play at every difficulty`() {
        val mine = WordEntry("Burek", null, null, Difficulty.MEDIUM, isCustom = true)
        for (setting in Difficulty.entries) {
            assertTrue(GameRules.isEligible(mine, GameRules.allowedDifficulties(setting)))
        }
        val hard = WordEntry("Opera", "e", "h", Difficulty.HARD)
        assertFalse(GameRules.isEligible(hard, GameRules.allowedDifficulties(Difficulty.EASY)))
    }

    @Test
    fun `hints never contain the secret word`() {
        val round = build(players = 5, impostors = 1, hints = HintMode.EASY)
        round.roles.filter { it.isImpostor }.forEach {
            assertFalse(it.hint.orEmpty().contains(round.secretWord, ignoreCase = true))
        }
    }

    // -------------------------------------------------------------- impostors

    @Test
    fun `the group may pick any impostor count up to everyone`() {
        assertEquals(3, GameRules.maxImpostorsFor(3))
        assertEquals(6, GameRules.maxImpostorsFor(6))
        assertEquals(20, GameRules.maxImpostorsFor(20))

        // Four impostors out of six is allowed, if odd.
        val round = build(players = 6, impostors = 4)
        assertEquals(4, round.impostorIndices.size)
        assertEquals(2, round.roles.count { !it.isImpostor })
    }

    @Test
    fun `the impostor count is capped at the table size`() {
        val round = build(players = 5, impostors = 99)
        assertEquals(5, round.impostorIndices.size)
        // A full-table troll round: nobody receives the word.
        assertTrue(round.roles.all { it.isImpostor && it.word == null })
    }

    @Test
    fun `an outnumbered crew is flagged but not blocked`() {
        assertFalse(GameRules.isOutnumbered(6, 2))
        assertTrue(GameRules.isOutnumbered(6, 3))
        assertTrue(GameRules.isConfigValid(config(6, 4)))
        assertEquals(2, GameRules.balancedImpostorsFor(6))
    }

    @Test
    fun `random impostors stay within one and the chosen ceiling`() {
        val rng = Random(20260825)
        val seen = HashSet<Int>()
        repeat(4_000) {
            val round = GameRules.buildRound(
                config = config(players = 8, impostors = 3, randomImpostors = true),
                category = category,
                entry = entry,
                random = rng,
            )
            val n = round.impostorIndices.size
            assertTrue("drew $n impostors", n in 1..3)
            seen.add(n)
        }
        // Over four thousand rounds every count in the range should have shown up.
        assertEquals(setOf(1, 2, 3), seen)
    }

    @Test
    fun `a random count is hidden from the reveal card`() {
        assertFalse(build(players = 8, impostors = 3, randomImpostors = true).revealsImpostorCount)
        assertTrue(build(players = 8, impostors = 3).revealsImpostorCount)
    }

    @Test
    fun `impostor draw is roughly uniform across seats`() {
        val players = 6
        val counts = IntArray(players)
        val trials = 12_000
        // One generator for the whole run - sequential seeds would not be independent.
        val rng = Random(20260825)
        repeat(trials) {
            GameRules.buildRound(
                config = config(players, 1),
                category = category,
                entry = entry,
                random = rng,
            ).impostorIndices.forEach { counts[it]++ }
        }
        val expected = trials.toDouble() / players
        counts.forEach { c ->
            assertTrue(
                "seat drawn $c times, expected around $expected",
                c > expected * 0.85 && c < expected * 1.15,
            )
        }
    }

    // --------------------------------------------------------- first speaker

    @Test
    fun `the first speaker is drawn fresh and can be anyone`() {
        val players = 6
        val counts = IntArray(players)
        val rng = Random(20260825)
        repeat(12_000) {
            GameRules.buildRound(
                config = config(players, 1),
                category = category,
                entry = entry,
                random = rng,
            ).let { counts[it.startingPlayerIndex]++ }
        }
        val expected = 12_000.0 / players
        counts.forEach { c ->
            assertTrue(
                "seat started $c times, expected around $expected",
                c > expected * 0.85 && c < expected * 1.15,
            )
        }
    }

    @Test
    fun `an impostor can be made to speak first`() {
        val rng = Random(7)
        var impostorStarted = 0
        repeat(2_000) {
            val round = GameRules.buildRound(
                config = config(5, 1),
                category = category,
                entry = entry,
                random = rng,
            )
            if (round.startingPlayerIndex in round.impostorIndices) impostorStarted++
        }
        // One impostor in five seats: roughly a fifth of rounds, and never zero.
        assertTrue("impostor started $impostorStarted times", impostorStarted in 300..500)
    }

    @Test
    fun `the first speaker name matches the seat that was drawn`() {
        val round = build(players = 5, impostors = 1, seed = 3)
        assertEquals(
            round.roles[round.startingPlayerIndex].name,
            round.startingPlayerName,
        )
    }

    // ------------------------------------------------------------------ names

    @Test
    fun `custom names are used and blanks fall back`() {
        val cfg = config(4, 1).copy(playerNames = listOf("Ana", "  ", "Marko"))
        val round = GameRules.buildRound(
            config = cfg,
            category = category,
            entry = entry,
            random = Random(3),
            defaultName = { "Igrač ${it + 1}" },
        )
        assertEquals(listOf("Ana", "Igrač 2", "Marko", "Igrač 4"), round.roles.map { it.name })
    }

    // ------------------------------------------------------------- difficulty

    @Test
    fun `difficulty settings map onto overlapping word bands`() {
        assertEquals(setOf(Difficulty.EASY), GameRules.allowedDifficulties(Difficulty.EASY))
        assertEquals(
            setOf(Difficulty.EASY, Difficulty.MEDIUM),
            GameRules.allowedDifficulties(Difficulty.MEDIUM),
        )
        assertEquals(
            setOf(Difficulty.MEDIUM, Difficulty.HARD),
            GameRules.allowedDifficulties(Difficulty.HARD),
        )
    }

    @Test
    fun `mixed draws from every word in the list`() {
        val allowed = GameRules.allowedDifficulties(Difficulty.MIXED)
        assertEquals(Difficulty.wordTags.toSet(), allowed)
        // Every tag a word can carry is in play, and nothing is left out.
        Difficulty.wordTags.forEach { assertTrue("$it excluded", it in allowed) }
        assertEquals(3, allowed.size)
    }

    @Test
    fun `mixed is a setting only and never tags a word`() {
        assertFalse(Difficulty.MIXED in Difficulty.wordTags)
        assertEquals(listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), Difficulty.wordTags)
    }

    // ---------------------------------------------------------------- outcome

    @Test
    fun `crew wins only when the ejected set is exactly the impostors`() {
        val round = build(players = 5, impostors = 1, seed = 7)
        val impostor = round.impostorIndices.first()
        val everyoneElse = (0 until 5).filter { it != impostor }

        val win = GameRules.outcome(round, everyoneElse.associateWith { impostor })
        assertTrue(win.hasVerdict)
        assertTrue(win.crewWins)
        assertEquals(setOf(impostor), win.ejected)

        val wrongTarget = everyoneElse.first()
        val loss = GameRules.outcome(
            round,
            (0 until 5).filter { it != wrongTarget }.associateWith { wrongTarget },
        )
        assertFalse(loss.crewWins)
    }

    @Test
    fun `a tie at the cut-off ejects nobody`() {
        val round = build(players = 4, impostors = 1, seed = 11)
        val votes = mapOf(0 to 1, 1 to 2, 2 to 1, 3 to 2)
        val outcome = GameRules.outcome(round, votes)
        assertTrue(outcome.tied)
        assertFalse(outcome.crewWins)
        assertTrue(outcome.ejected.isEmpty())
    }

    @Test
    fun `no votes means no verdict`() {
        val outcome = GameRules.outcome(build(players = 5, impostors = 1), emptyMap())
        assertFalse(outcome.hasVerdict)
        assertTrue(outcome.ejected.isEmpty())
    }

    @Test
    fun `config validation rejects impossible setups`() {
        assertTrue(GameRules.isConfigValid(config(6, 2)))
        assertTrue(GameRules.isConfigValid(config(6, 5)))
        assertTrue(GameRules.isConfigValid(config(6, 6)))
        assertFalse(GameRules.isConfigValid(config(6, 7)))
        assertFalse(GameRules.isConfigValid(config(2, 1)))
        assertFalse(GameRules.isConfigValid(config(25, 1)))
        assertEquals(mapOf(1 to 2), GameRules.tally(mapOf(0 to 1, 2 to 1)))
    }

    @Test
    fun `all categories is the default and means no filter`() {
        assertTrue(config(6, 1).usesAllCategories)
        assertFalse(config(6, 1).copy(categoryIds = setOf("food")).usesAllCategories)
    }
}
