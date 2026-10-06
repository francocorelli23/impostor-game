package com.impostor.party.data

import android.content.Context
import android.util.AtomicFile
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.WordEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A category the players made themselves. Its words live in [CustomWordList.words]. */
data class CustomCategory(
    val id: String,
    val name: String,
    val emoji: String,
)

/** Everything the players added for one word-list language. */
data class CustomWordList(
    val categories: List<CustomCategory> = emptyList(),
    /** Keyed by category id - either a bundled category's id or one of [categories]. */
    val words: Map<String, List<WordEntry>> = emptyMap(),
) {
    fun wordsIn(categoryId: String): List<WordEntry> = words[categoryId].orEmpty()
}

/**
 * The players' own words and categories.
 *
 * Kept per word-list language, exactly like the bundled lists, in one small JSON
 * file in the app's private storage. Hints are optional: a word saved without one
 * simply gives the impostor no hint, whatever the hint setting.
 */
class CustomWordStore(context: Context) {

    private val file = AtomicFile(File(context.applicationContext.filesDir, FILE_NAME))

    private val _lists = MutableStateFlow(read())

    /** Language tag -> that language's custom words. */
    val lists: StateFlow<Map<String, CustomWordList>> = _lists.asStateFlow()

    fun forLanguage(language: String): CustomWordList = _lists.value[language] ?: CustomWordList()

    // ------------------------------------------------------------------ categories

    /** Creates a category and returns its id. */
    fun addCategory(language: String, name: String, emoji: String): String {
        val id = CUSTOM_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").take(12)
        edit(language) {
            it.copy(categories = it.categories + CustomCategory(id, normalise(name), cleanEmoji(emoji)))
        }
        return id
    }

    fun updateCategory(language: String, id: String, name: String, emoji: String) {
        edit(language) { list ->
            list.copy(
                categories = list.categories.map {
                    if (it.id == id) it.copy(name = normalise(name), emoji = cleanEmoji(emoji)) else it
                }
            )
        }
    }

    fun deleteCategory(language: String, id: String) {
        edit(language) { list ->
            list.copy(
                categories = list.categories.filterNot { it.id == id },
                words = list.words - id,
            )
        }
    }

    // ----------------------------------------------------------------------- words

    fun addWord(language: String, categoryId: String, word: String, easyHint: String?, vagueHint: String?) {
        val entry = entry(word, easyHint, vagueHint) ?: return
        edit(language) { list ->
            list.copy(words = list.words + (categoryId to list.wordsIn(categoryId) + entry))
        }
    }

    /** Replaces the word currently saved as [original] in [categoryId]. */
    fun updateWord(
        language: String,
        categoryId: String,
        original: String,
        word: String,
        easyHint: String?,
        vagueHint: String?,
    ) {
        val entry = entry(word, easyHint, vagueHint) ?: return
        edit(language) { list ->
            val updated = list.wordsIn(categoryId).map { if (it.word == original) entry else it }
            list.copy(words = list.words + (categoryId to updated))
        }
    }

    fun deleteWord(language: String, categoryId: String, word: String) {
        edit(language) { list ->
            val remaining = list.wordsIn(categoryId).filterNot { it.word == word }
            list.copy(
                words = if (remaining.isEmpty()) list.words - categoryId else list.words + (categoryId to remaining)
            )
        }
    }

    // --------------------------------------------------------------------- storage

    @Synchronized
    private fun edit(language: String, transform: (CustomWordList) -> CustomWordList) {
        val current = _lists.value
        val next = current + (language to transform(current[language] ?: CustomWordList()))
        write(next)
        _lists.value = next
    }

    private fun read(): Map<String, CustomWordList> = try {
        parse(String(file.readFully(), Charsets.UTF_8))
    } catch (t: Throwable) {
        // Missing on first run, and a damaged file must never stop the game.
        emptyMap()
    }

    private fun write(lists: Map<String, CustomWordList>) {
        val bytes = serialise(lists).toByteArray(Charsets.UTF_8)
        val out = try {
            file.startWrite()
        } catch (t: Throwable) {
            return
        }
        try {
            out.write(bytes)
            file.finishWrite(out)
        } catch (t: Throwable) {
            file.failWrite(out)
        }
    }

    companion object {
        private const val FILE_NAME = "custom_words.json"
        const val CUSTOM_ID_PREFIX = "custom_"
        const val DEFAULT_EMOJI = "⭐"

        /**
         * Trims, collapses runs of whitespace and drops control characters - the
         * word-history key uses one as its separator, so none may get in.
         */
        fun normalise(text: String): String =
            text.filterNot { it.isISOControl() }.trim().replace(WHITESPACE, " ")

        /** Comparison key for spotting duplicates: same text, ignoring case and spacing. */
        fun matchKey(text: String): String = normalise(text).lowercase()

        private fun cleanEmoji(emoji: String): String = normalise(emoji).ifEmpty { DEFAULT_EMOJI }

        private fun entry(word: String, easyHint: String?, vagueHint: String?): WordEntry? {
            val cleanWord = normalise(word)
            if (cleanWord.isEmpty()) return null
            return WordEntry(
                word = cleanWord,
                easyHint = easyHint?.let(::normalise)?.ifEmpty { null },
                vagueHint = vagueHint?.let(::normalise)?.ifEmpty { null },
                // Custom words are eligible at every difficulty; the tag is nominal.
                difficulty = Difficulty.MEDIUM,
                isCustom = true,
            )
        }

        private val WHITESPACE = Regex("\\s+")

        private fun JSONObject.optText(key: String): String? =
            if (!has(key) || isNull(key)) null else optString(key).trim().ifEmpty { null }

        private fun parse(text: String): Map<String, CustomWordList> {
            val root = JSONObject(text)
            val languages = root.optJSONObject("languages") ?: return emptyMap()
            val result = HashMap<String, CustomWordList>()
            for (language in languages.keys()) {
                val node = languages.optJSONObject(language) ?: continue

                val categories = ArrayList<CustomCategory>()
                val cats = node.optJSONArray("categories") ?: JSONArray()
                for (i in 0 until cats.length()) {
                    val c = cats.optJSONObject(i) ?: continue
                    val id = c.optText("id") ?: continue
                    val name = c.optText("name") ?: continue
                    categories.add(CustomCategory(id, name, c.optText("emoji") ?: DEFAULT_EMOJI))
                }

                val words = HashMap<String, List<WordEntry>>()
                val wordNode = node.optJSONObject("words") ?: JSONObject()
                for (categoryId in wordNode.keys()) {
                    val array = wordNode.optJSONArray(categoryId) ?: continue
                    val entries = ArrayList<WordEntry>(array.length())
                    for (j in 0 until array.length()) {
                        val w = array.optJSONObject(j) ?: continue
                        entries.add(entry(w.optText("w") ?: continue, w.optText("e"), w.optText("h")) ?: continue)
                    }
                    if (entries.isNotEmpty()) words[categoryId] = entries
                }

                result[language] = CustomWordList(categories, words)
            }
            return result
        }

        private fun serialise(lists: Map<String, CustomWordList>): String {
            val languages = JSONObject()
            for ((language, list) in lists) {
                val cats = JSONArray()
                for (c in list.categories) {
                    cats.put(JSONObject().put("id", c.id).put("name", c.name).put("emoji", c.emoji))
                }
                val words = JSONObject()
                for ((categoryId, entries) in list.words) {
                    if (entries.isEmpty()) continue
                    val array = JSONArray()
                    for (e in entries) {
                        val w = JSONObject().put("w", e.word)
                        e.easyHint?.let { w.put("e", it) }
                        e.vagueHint?.let { w.put("h", it) }
                        array.put(w)
                    }
                    words.put(categoryId, array)
                }
                languages.put(language, JSONObject().put("categories", cats).put("words", words))
            }
            return JSONObject().put("version", 1).put("languages", languages).toString()
        }
    }
}
