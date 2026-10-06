package com.impostor.party.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.CustomWordList
import com.impostor.party.data.CustomWordStore
import com.impostor.party.data.model.Category
import com.impostor.party.data.model.WordEntry
import com.impostor.party.ui.components.AppDialog
import com.impostor.party.ui.components.AppTextField
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

private const val MAX_WORD_LENGTH = 40
private const val MAX_HINT_LENGTH = 60
private const val MAX_CATEGORY_LENGTH = 24
private const val MAX_EMOJI_CODE_POINTS = 8

/** Everything the editor can change. The caller binds these to the current language. */
class CustomWordActions(
    val addCategory: (name: String, emoji: String) -> String,
    val updateCategory: (id: String, name: String, emoji: String) -> Unit,
    val deleteCategory: (id: String) -> Unit,
    val addWord: (categoryId: String, word: String, easyHint: String?, vagueHint: String?) -> Unit,
    val updateWord: (
        categoryId: String,
        original: String,
        word: String,
        easyHint: String?,
        vagueHint: String?,
    ) -> Unit,
    val deleteWord: (categoryId: String, word: String) -> Unit,
)

/**
 * The players' own words. Words can be added to any bundled category or to a
 * category of their own. Both hints are optional - a word without one gives the
 * impostor no hint in that mode.
 */
@Composable
fun CustomWordsScreen(
    bundled: List<Category>,
    custom: CustomWordList,
    languageName: String,
    actions: CustomWordActions,
    onBack: () -> Unit,
) {
    var openCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    // Hoisted so coming back from a category lands where the list was left.
    val listState = rememberLazyListState()

    /** True when another category already uses this name (case and spacing ignored). */
    fun nameTaken(name: String, exceptId: String?): Boolean {
        val key = CustomWordStore.matchKey(name)
        return bundled.any { it.id != exceptId && CustomWordStore.matchKey(it.name) == key } ||
            custom.categories.any { it.id != exceptId && CustomWordStore.matchKey(it.name) == key }
    }

    val openId = openCategoryId
    if (openId != null) {
        val bundledCategory = bundled.firstOrNull { it.id == openId }
        val ownCategory = custom.categories.firstOrNull { it.id == openId }
        // A freshly created category can take a frame to arrive; until it does the
        // list below simply stays up.
        if (bundledCategory != null || ownCategory != null) {
            CategoryWordsScreen(
                categoryId = openId,
                name = bundledCategory?.name ?: ownCategory!!.name,
                emoji = bundledCategory?.emoji ?: ownCategory!!.emoji,
                builtInWords = bundledCategory?.words.orEmpty(),
                isOwnCategory = ownCategory != null,
                yourWords = custom.wordsIn(openId),
                nameTaken = { nameTaken(it, openId) },
                actions = actions,
                onClose = { openCategoryId = null },
            )
            return
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.custom_title),
        subtitle = stringResource(R.string.custom_subtitle, languageName),
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (custom.categories.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.custom_section_yours)) }
                    items(custom.categories, key = { it.id }) { category ->
                        CategoryRow(
                            emoji = category.emoji,
                            name = category.name,
                            detail = stringResource(
                                R.string.custom_your_word_count,
                                custom.wordsIn(category.id).size,
                            ),
                            onClick = { openCategoryId = category.id },
                        )
                    }
                }
                item { SectionLabel(stringResource(R.string.custom_section_builtin)) }
                items(bundled, key = { it.id }) { category ->
                    val yours = custom.wordsIn(category.id).size
                    CategoryRow(
                        emoji = category.emoji,
                        name = category.name,
                        detail = if (yours > 0) {
                            stringResource(R.string.custom_builtin_plus_yours, category.words.size, yours)
                        } else {
                            stringResource(R.string.category_word_count, category.words.size)
                        },
                        onClick = { openCategoryId = category.id },
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.custom_new_category),
                    onClick = { showNewCategory = true },
                )
            }
        }
    }

    if (showNewCategory) {
        CategoryDialog(
            isNew = true,
            initialName = "",
            initialEmoji = "",
            nameTaken = { nameTaken(it, null) },
            onSave = { name, emoji ->
                showNewCategory = false
                openCategoryId = actions.addCategory(name, emoji)
            },
            onDismiss = { showNewCategory = false },
        )
    }
}

/**
 * One category: the player's own words (tap to edit) above everything that is
 * already in it, so nobody adds a word twice. Own categories can also be renamed
 * or deleted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryWordsScreen(
    categoryId: String,
    name: String,
    emoji: String,
    builtInWords: List<WordEntry>,
    isOwnCategory: Boolean,
    yourWords: List<WordEntry>,
    nameTaken: (String) -> Boolean,
    actions: CustomWordActions,
    onClose: () -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<WordEntry?>(null) }
    var deletingWord by remember { mutableStateOf<WordEntry?>(null) }
    var editingCategory by remember { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val title = "$emoji $name".trim()

    if (editorOpen) {
        val original = editingWord
        WordEditorScreen(
            categoryTitle = title,
            initial = original,
            // Everything already in the category, except the word being edited.
            existingWords = (builtInWords + yourWords)
                .map { it.word }
                .filterNot { original != null && it == original.word },
            onSave = { word, easy, vague ->
                if (original == null) {
                    actions.addWord(categoryId, word, easy, vague)
                } else {
                    actions.updateWord(categoryId, original.word, word, easy, vague)
                }
                editorOpen = false
                editingWord = null
            },
            onClose = {
                editorOpen = false
                editingWord = null
            },
        )
        return
    }

    BackHandler(onBack = onClose)

    ScreenScaffold(
        title = title,
        subtitle = if (isOwnCategory) {
            stringResource(R.string.custom_own_category_note)
        } else {
            stringResource(R.string.custom_builtin_note, builtInWords.size)
        },
        onBack = onClose,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SectionLabel(stringResource(R.string.custom_your_words_header, yourWords.size))
                }
                if (yourWords.isEmpty()) {
                    item {
                        Text(
                            text = if (isOwnCategory) {
                                stringResource(R.string.custom_category_empty)
                            } else {
                                stringResource(R.string.custom_no_words_builtin)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImpostorTheme.extended.muted,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                items(yourWords) { entry ->
                    WordRow(
                        entry = entry,
                        onEdit = {
                            editingWord = entry
                            editorOpen = true
                        },
                        onDelete = { deletingWord = entry },
                    )
                }

                if (builtInWords.isNotEmpty()) {
                    item {
                        SectionLabel(stringResource(R.string.custom_builtin_words_header, builtInWords.size))
                    }
                    item {
                        SectionCard {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                builtInWords.forEach { WordChip(it.word) }
                            }
                        }
                    }
                }

                if (isOwnCategory) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            QuietButton(
                                text = stringResource(R.string.custom_rename_category),
                                onClick = { editingCategory = true },
                            )
                            QuietButton(
                                text = stringResource(R.string.custom_delete_category),
                                onClick = { deletingCategory = true },
                                color = ImpostorTheme.extended.impostor,
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.custom_add_word),
                    onClick = {
                        editingWord = null
                        editorOpen = true
                    },
                )
            }
        }
    }

    deletingWord?.let { entry ->
        AppDialog(
            title = stringResource(R.string.custom_delete_word_title, entry.word),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                actions.deleteWord(categoryId, entry.word)
                deletingWord = null
            },
            onDismiss = { deletingWord = null },
        )
    }

    if (editingCategory) {
        CategoryDialog(
            isNew = false,
            initialName = name,
            initialEmoji = emoji,
            nameTaken = nameTaken,
            onSave = { newName, newEmoji ->
                actions.updateCategory(categoryId, newName, newEmoji)
                editingCategory = false
            },
            onDismiss = { editingCategory = false },
        )
    }

    if (deletingCategory) {
        AppDialog(
            title = stringResource(R.string.custom_delete_category_title, name),
            body = stringResource(R.string.custom_delete_category_body),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                deletingCategory = false
                onClose()
                actions.deleteCategory(categoryId)
            },
            onDismiss = { deletingCategory = false },
        )
    }
}

/**
 * Adding or editing one word, full screen. Below the fields, the words already in
 * the category narrow down as you type, and an exact match blocks saving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordEditorScreen(
    categoryTitle: String,
    initial: WordEntry?,
    existingWords: List<String>,
    onSave: (word: String, easyHint: String?, vagueHint: String?) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    var word by remember { mutableStateOf(initial?.word.orEmpty()) }
    var easy by remember { mutableStateOf(initial?.easyHint.orEmpty()) }
    var vague by remember { mutableStateOf(initial?.vagueHint.orEmpty()) }

    val cleanWord = CustomWordStore.normalise(word)
    val typedKey = CustomWordStore.matchKey(word)
    val taken = typedKey.isNotEmpty() && existingWords.any { CustomWordStore.matchKey(it) == typedKey }
    val canSave = cleanWord.isNotEmpty() && !taken
    val shown = if (typedKey.isEmpty()) {
        existingWords
    } else {
        existingWords.filter { CustomWordStore.matchKey(it).contains(typedKey) }
    }

    ScreenScaffold(
        title = stringResource(if (initial == null) R.string.custom_new_word else R.string.custom_edit_word),
        subtitle = categoryTitle,
        onBack = onClose,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionCard {
                        AppTextField(
                            value = word,
                            onValueChange = { word = it.take(MAX_WORD_LENGTH) },
                            label = stringResource(R.string.custom_word),
                            isError = taken,
                            errorText = if (taken) stringResource(R.string.custom_word_exists) else null,
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = easy,
                            onValueChange = { easy = it.take(MAX_HINT_LENGTH) },
                            label = stringResource(R.string.custom_easy_hint),
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = vague,
                            onValueChange = { vague = it.take(MAX_HINT_LENGTH) },
                            label = stringResource(R.string.custom_vague_hint),
                            imeAction = ImeAction.Done,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.custom_hints_optional),
                            style = MaterialTheme.typography.bodySmall,
                            color = ImpostorTheme.extended.muted,
                        )
                    }
                }

                if (existingWords.isNotEmpty()) {
                    item {
                        SectionLabel(
                            if (typedKey.isEmpty()) {
                                stringResource(R.string.custom_already_here, existingWords.size)
                            } else {
                                stringResource(R.string.custom_similar_words)
                            }
                        )
                    }
                    item {
                        if (shown.isEmpty()) {
                            Text(
                                text = stringResource(R.string.custom_no_similar),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ImpostorTheme.extended.crew,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                shown.forEach {
                                    WordChip(it, highlighted = CustomWordStore.matchKey(it) == typedKey)
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.save),
                    enabled = canSave,
                    onClick = {
                        onSave(
                            cleanWord,
                            CustomWordStore.normalise(easy).ifEmpty { null },
                            CustomWordStore.normalise(vague).ifEmpty { null },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    isNew: Boolean,
    initialName: String,
    initialEmoji: String,
    nameTaken: (String) -> Boolean,
    onSave: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    val cleanName = CustomWordStore.normalise(name)
    val taken = cleanName.isNotEmpty() && nameTaken(cleanName)

    AppDialog(
        title = stringResource(if (isNew) R.string.custom_new_category else R.string.custom_edit_category),
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel),
        confirmEnabled = cleanName.isNotEmpty() && !taken,
        onConfirm = { onSave(cleanName, emoji) },
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it.take(MAX_CATEGORY_LENGTH) },
            label = stringResource(R.string.custom_category_name),
            isError = taken,
            errorText = if (taken) stringResource(R.string.custom_category_exists) else null,
            capitalization = KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = emoji,
            onValueChange = { typed ->
                // Counted in code points so an emoji is never cut in half.
                if (typed.codePointCount(0, typed.length) <= MAX_EMOJI_CODE_POINTS) emoji = typed
            },
            label = stringResource(R.string.custom_category_emoji),
            placeholder = CustomWordStore.DEFAULT_EMOJI,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.None,
        )
    }
}

// ------------------------------------------------------------------ list pieces

@Composable
private fun SectionLabel(text: String) {
    Overline(text, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

/** A word that is already in the category. Highlighted when it matches what is being typed. */
@Composable
private fun WordChip(word: String, highlighted: Boolean = false) {
    val extended = ImpostorTheme.extended
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) extended.impostorContainer else extended.elevated)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlighted) extended.impostor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ListCard(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val feedback = LocalFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, ImpostorTheme.extended.hairline, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) {
                feedback.tap()
                onClick()
            }
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun CategoryRow(emoji: String, name: String, detail: String, onClick: () -> Unit) {
    ListCard(onClick = onClick) {
        Text(text = emoji.ifBlank { CustomWordStore.DEFAULT_EMOJI }, fontSize = 26.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = ImpostorTheme.extended.muted,
            )
        }
        Text(
            text = "›",
            fontSize = 26.sp,
            color = ImpostorTheme.extended.muted,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun WordRow(entry: WordEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val hints = listOfNotNull(
        entry.easyHint?.let { stringResource(R.string.custom_hint_easy_short, it) },
        entry.vagueHint?.let { stringResource(R.string.custom_hint_vague_short, it) },
    )
    ListCard(onClick = onEdit) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.word,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hints.isEmpty()) stringResource(R.string.custom_no_hints) else hints.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = ImpostorTheme.extended.muted,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete_word),
                tint = ImpostorTheme.extended.muted,
            )
        }
    }
}
