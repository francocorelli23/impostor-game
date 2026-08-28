package com.impostor.party.data

import android.content.Context
import android.content.SharedPreferences
import com.impostor.party.data.model.Category
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.WordEntry
import com.impostor.party.game.GameRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random

/**
 * Reads the bundled word lists from assets and hands out secret words.
 *
 * Everything is local: one JSON file per language ships inside the APK, is parsed
 * with the platform's own org.json (no third-party parser, no reflection) and is
 * then kept in memory for the lifetime of the process.
 */
class WordRepository(
    private val context: Context,
    private val prefs: SharedPreferences,
) {

    data class Pick(val category: Category, val entry: WordEntry)

    private val cache = HashMap<String, List<Category>>()

    suspend fun categories(language: String): List<Category> =
        withContext(Dispatchers.IO) { loadBlocking(language) }

    @Synchronized
    private fun loadBlocking(language: String): List<Category> {
        val tag = if (language in SUPPORTED) language else FALLBACK
        cache[tag]?.let { return it }
        val parsed = try {
            parse(context.assets.open("$ASSET_DIR/$tag.json").bufferedReader().use { it.readText() })
        } catch (t: Throwable) {
            // A corrupt or missing asset must never crash the game. Fall back to
            // English, and to an empty list only if that is broken too.
            if (tag != FALLBACK) return loadBlocking(FALLBACK) else emptyList()
        }
        cache[tag] = parsed
        return parsed
    }

    private fun parse(text: String): List<Category> {
        val root = JSONObject(text)
        val cats = root.getJSONArray("categories")
        val result = ArrayList<Category>(cats.length())
        for (i in 0 until cats.length()) {
            val c = cats.getJSONObject(i)
            val wordsArray = c.getJSONArray("words")
            val words = ArrayList<WordEntry>(wordsArray.length())
            for (j in 0 until wordsArray.length()) {
                val w = wordsArray.getJSONObject(j)
                words.add(
                    WordEntry(
                        word = w.getString("w"),
                        easyHint = w.getString("e"),
                        vagueHint = w.getString("h"),
                        difficulty = Difficulty.fromKey(w.optString("d")),
                    )
                )
            }
            if (words.isNotEmpty()) {
                result.add(
                    Category(
                        id = c.getString("id"),
                        name = c.getString("name"),
                        emoji = c.optString("emoji"),
                        words = words,
                    )
                )
            }
        }
        return result
    }

    /**
     * Picks one word for a round.
     *
     * [categoryIds] may name one category, several, or none at all - none meaning
     * every category is in play. A category is drawn first and a word second, so a
     * large category does not crowd out a small one.
     *
     * Words used in the last [HISTORY_SIZE] rounds are skipped when there is
     * anything else to choose from, so the same word does not keep coming back.
     */
    suspend fun pickWord(
        categoryIds: Set<String>,
        difficulty: Difficulty,
        random: Random,
        language: String,
    ): Pick? {
        val all = categories(language)
        if (all.isEmpty()) return null

        val eligible = if (categoryIds.isEmpty()) {
            all
        } else {
            all.filter { it.id in categoryIds }.ifEmpty { all }
        }

        val allowed = GameRules.allowedDifficulties(difficulty)
        val recent = recentKeys().toHashSet()

        // Prefer a category that still has an unused word at this difficulty.
        val withFresh = eligible.filter { category ->
            category.words.any { it.difficulty in allowed && key(category.id, it.word) !in recent }
        }
        val pool = withFresh.ifEmpty { eligible }
        val category = pool[random.nextInt(pool.size)]

        var words = category.words.filter { it.difficulty in allowed }
        if (words.isEmpty()) words = category.words
        val fresh = words.filter { key(category.id, it.word) !in recent }
        val source = fresh.ifEmpty { words }

        val entry = source[random.nextInt(source.size)]
        remember(key(category.id, entry.word))
        return Pick(category, entry)
    }

    private fun key(categoryId: String, word: String) = "$categoryId|$word"

    private fun recentKeys(): List<String> {
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        return raw.split(RECENT_SEPARATOR).filter { it.isNotEmpty() }
    }

    @Synchronized
    private fun remember(key: String) {
        val list = recentKeys().toMutableList()
        list.remove(key)
        list.add(key)
        while (list.size > HISTORY_SIZE) list.removeAt(0)
        prefs.edit().putString(KEY_RECENT, list.joinToString(RECENT_SEPARATOR)).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_RECENT).apply()
    }

    companion object {
        private const val ASSET_DIR = "words"
        private const val FALLBACK = "en"
        val SUPPORTED = setOf("en", "hr")

        private const val KEY_RECENT = "recent_words"

        /** ASCII record separator - cannot appear inside a category id or a word. */
        private const val RECENT_SEPARATOR = "\u001E"
        private const val HISTORY_SIZE = 80
    }
}
