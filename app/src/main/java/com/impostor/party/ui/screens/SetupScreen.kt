package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.model.Category
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.GameConfig
import com.impostor.party.data.model.HintMode
import com.impostor.party.game.GameRules
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.components.SegmentedSelector
import com.impostor.party.ui.components.SettingRow
import com.impostor.party.ui.components.Stepper
import com.impostor.party.ui.components.SwitchRow
import com.impostor.party.ui.theme.ImpostorTheme

val DISCUSSION_TIMER_OPTIONS = listOf(0, 60, 120, 180, 300)
val VOTING_TIMER_OPTIONS = listOf(0, 30, 60, 120)

@Composable
fun SetupScreen(
    config: GameConfig,
    categories: List<Category>,
    onConfigChange: (GameConfig) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }

    if (showCategoryPicker) {
        CategoryPicker(
            categories = categories,
            selectedIds = config.categoryIds,
            onConfirm = {
                onConfigChange(config.copy(categoryIds = it))
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
        return
    }

    if (showNameEditor) {
        NameEditor(
            playerCount = config.playerCount,
            names = config.playerNames,
            onConfirm = {
                onConfigChange(config.copy(playerNames = it))
                showNameEditor = false
            },
            onDismiss = { showNameEditor = false },
        )
        return
    }

    val maxImpostors = GameRules.maxImpostorsFor(config.playerCount)
    val crewCount = config.playerCount - config.impostorCount
    val outnumbered = GameRules.isOutnumbered(config.playerCount, config.impostorCount)

    ScreenScaffold(title = stringResource(R.string.setup_title), onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionCard(
                    title = stringResource(R.string.setup_players),
                    subtitle = stringResource(R.string.setup_players_help),
                ) {
                    Stepper(
                        value = config.playerCount,
                        min = GameRules.MIN_PLAYERS,
                        max = GameRules.MAX_PLAYERS,
                        onChange = { players ->
                            onConfigChange(
                                config.copy(
                                    playerCount = players,
                                    impostorCount = GameRules.clampImpostors(players, config.impostorCount),
                                )
                            )
                        },
                    )
                }

                SectionCard(
                    title = stringResource(R.string.setup_impostors),
                    subtitle = if (config.randomImpostors) {
                        stringResource(R.string.setup_impostors_random_help, config.impostorCount)
                    } else {
                        stringResource(R.string.setup_impostors_help)
                    },
                ) {
                    Stepper(
                        value = config.impostorCount,
                        min = 1,
                        max = maxImpostors,
                        onChange = { onConfigChange(config.copy(impostorCount = it)) },
                    )
                    Spacer(Modifier.height(6.dp))
                    SwitchRow(
                        title = stringResource(R.string.setup_random_impostors),
                        subtitle = stringResource(R.string.setup_random_impostors_sub),
                        checked = config.randomImpostors,
                        onCheckedChange = { onConfigChange(config.copy(randomImpostors = it)) },
                    )
                    AnimatedVisibility(
                        visible = outnumbered,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.setup_impostor_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = ImpostorTheme.extended.accent,
                            )
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.setup_categories)) {
                    SettingRow(
                        title = categoryTitle(config, categories),
                        subtitle = categorySubtitle(config, categories),
                        onClick = { showCategoryPicker = true },
                        trailing = {
                            Text(text = categoryEmoji(config, categories), fontSize = 24.sp)
                        },
                    )
                }

                SectionCard(title = stringResource(R.string.setup_difficulty)) {
                    SegmentedSelector(
                        options = listOf(
                            stringResource(R.string.setup_difficulty_easy),
                            stringResource(R.string.setup_difficulty_medium),
                            stringResource(R.string.setup_difficulty_hard),
                            stringResource(R.string.setup_difficulty_mixed),
                        ),
                        selectedIndex = Difficulty.entries.indexOf(config.difficulty),
                        onSelect = { onConfigChange(config.copy(difficulty = Difficulty.entries[it])) },
                    )
                }

                SectionCard(
                    title = stringResource(R.string.setup_hints),
                    subtitle = when (config.hintMode) {
                        HintMode.NONE -> stringResource(R.string.setup_hint_help_none)
                        HintMode.EASY -> stringResource(R.string.setup_hint_help_easy)
                        HintMode.VAGUE -> stringResource(R.string.setup_hint_help_vague)
                    },
                ) {
                    SegmentedSelector(
                        options = listOf(
                            stringResource(R.string.setup_hint_none),
                            stringResource(R.string.setup_hint_easy),
                            stringResource(R.string.setup_hint_vague),
                        ),
                        selectedIndex = HintMode.entries.indexOf(config.hintMode),
                        onSelect = { onConfigChange(config.copy(hintMode = HintMode.entries[it])) },
                    )
                    Spacer(Modifier.height(6.dp))
                    SwitchRow(
                        title = stringResource(R.string.setup_hide_category),
                        subtitle = stringResource(R.string.setup_hide_category_sub),
                        checked = config.hideCategoryFromImpostor,
                        onCheckedChange = {
                            onConfigChange(config.copy(hideCategoryFromImpostor = it))
                        },
                    )
                }

                SectionCard(title = stringResource(R.string.setup_round_timer)) {
                    SegmentedSelector(
                        options = DISCUSSION_TIMER_OPTIONS.map { timerLabel(it) },
                        selectedIndex = DISCUSSION_TIMER_OPTIONS
                            .indexOf(config.discussionSeconds)
                            .coerceAtLeast(0),
                        onSelect = {
                            onConfigChange(config.copy(discussionSeconds = DISCUSSION_TIMER_OPTIONS[it]))
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    Overline(stringResource(R.string.setup_voting_timer))
                    Spacer(Modifier.height(8.dp))
                    SegmentedSelector(
                        options = VOTING_TIMER_OPTIONS.map { timerLabel(it) },
                        selectedIndex = VOTING_TIMER_OPTIONS
                            .indexOf(config.votingSeconds)
                            .coerceAtLeast(0),
                        onSelect = {
                            onConfigChange(config.copy(votingSeconds = VOTING_TIMER_OPTIONS[it]))
                        },
                    )
                }

                SectionCard(title = stringResource(R.string.setup_player_names)) {
                    SettingRow(
                        title = stringResource(R.string.setup_edit_names),
                        subtitle = namesSummary(config),
                        onClick = { showNameEditor = true },
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Overline(
                        text = when {
                            config.randomImpostors ->
                                stringResource(R.string.setup_summary_random, config.impostorCount)
                            config.impostorCount == 1 ->
                                stringResource(R.string.setup_summary, crewCount, config.impostorCount)
                            else ->
                                stringResource(
                                    R.string.setup_summary_plural,
                                    crewCount,
                                    config.impostorCount,
                                )
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    text = stringResource(R.string.setup_start),
                    onClick = onStart,
                    enabled = GameRules.isConfigValid(config),
                )
            }
        }
    }
}

@Composable
private fun timerLabel(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.off)
    seconds % 60 == 0 -> stringResource(R.string.minutes_short, seconds / 60)
    else -> stringResource(R.string.seconds_short, seconds)
}

@Composable
private fun categoryTitle(config: GameConfig, categories: List<Category>): String = when {
    config.usesAllCategories -> stringResource(R.string.category_all)
    config.categoryIds.size == 1 ->
        categories.firstOrNull { it.id in config.categoryIds }?.name
            ?: stringResource(R.string.category_all)
    else -> stringResource(R.string.category_count_selected, config.categoryIds.size)
}

@Composable
private fun categorySubtitle(config: GameConfig, categories: List<Category>): String? = when {
    config.usesAllCategories -> stringResource(R.string.category_all_detail)
    config.categoryIds.size == 1 ->
        categories.firstOrNull { it.id in config.categoryIds }
            ?.let { stringResource(R.string.category_word_count, it.words.size) }
    else -> {
        val words = categories.filter { it.id in config.categoryIds }.sumOf { it.words.size }
        stringResource(R.string.category_word_count, words)
    }
}

@Composable
private fun categoryEmoji(config: GameConfig, categories: List<Category>): String = when {
    config.usesAllCategories -> "🎲"
    config.categoryIds.size == 1 ->
        categories.firstOrNull { it.id in config.categoryIds }?.emoji ?: "🎲"
    else -> categories.firstOrNull { it.id in config.categoryIds }?.emoji ?: "🎲"
}

@Composable
private fun namesSummary(config: GameConfig): String {
    val filled = config.playerNames.count { it.isNotBlank() }
    return if (filled == 0) {
        stringResource(R.string.setup_names_default)
    } else {
        stringResource(R.string.setup_names_custom, filled)
    }
}
