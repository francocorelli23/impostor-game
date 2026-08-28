package com.impostor.party.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.Settings
import com.impostor.party.data.model.AppLanguage
import com.impostor.party.data.model.Category
import com.impostor.party.data.model.Difficulty
import com.impostor.party.data.model.HintMode
import com.impostor.party.data.model.ThemeMode
import com.impostor.party.game.GameRules
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.components.SegmentedSelector
import com.impostor.party.ui.components.SettingRow
import com.impostor.party.ui.components.Stepper
import com.impostor.party.ui.components.SwitchRow
import com.impostor.party.ui.theme.ImpostorTheme

@Composable
fun SettingsScreen(
    settings: Settings,
    categories: List<Category>,
    versionName: String,
    onChange: (Settings) -> Unit,
    onResetDefaults: () -> Unit,
    onClearWordHistory: () -> Unit,
    onBack: () -> Unit,
) {
    var showCategoryPicker by remember { mutableStateOf(false) }
    var historyCleared by remember { mutableStateOf(false) }

    if (showCategoryPicker) {
        CategoryPicker(
            categories = categories,
            selectedIds = settings.defaultCategoryIds,
            onConfirm = {
                onChange(settings.copy(defaultCategoryIds = it))
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
        return
    }

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(
                title = stringResource(R.string.settings_section_language),
                subtitle = stringResource(R.string.settings_language_body),
            ) {
                SegmentedSelector(
                    options = listOf(
                        stringResource(R.string.language_system_short),
                        stringResource(R.string.language_english),
                        stringResource(R.string.language_croatian),
                    ),
                    selectedIndex = AppLanguage.selectable.indexOf(settings.language)
                        .coerceAtLeast(0),
                    onSelect = { onChange(settings.copy(language = AppLanguage.selectable[it])) },
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_feedback)) {
                SwitchRow(
                    title = stringResource(R.string.settings_sound),
                    checked = settings.soundEnabled,
                    onCheckedChange = { onChange(settings.copy(soundEnabled = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_music),
                    checked = settings.musicEnabled,
                    onCheckedChange = { onChange(settings.copy(musicEnabled = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_vibration),
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { onChange(settings.copy(vibrationEnabled = it)) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_keep_screen_on),
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onChange(settings.copy(keepScreenOn = it)) },
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_appearance)) {
                Overline(stringResource(R.string.settings_theme))
                Spacer(Modifier.height(10.dp))
                SegmentedSelector(
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    ),
                    selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                    onSelect = { onChange(settings.copy(themeMode = ThemeMode.entries[it])) },
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_defaults)) {
                Overline(stringResource(R.string.settings_default_players))
                Spacer(Modifier.height(8.dp))
                Stepper(
                    value = settings.defaultPlayers,
                    min = GameRules.MIN_PLAYERS,
                    max = GameRules.MAX_PLAYERS,
                    onChange = { players ->
                        onChange(
                            settings.copy(
                                defaultPlayers = players,
                                defaultImpostors = GameRules.clampImpostors(
                                    players,
                                    settings.defaultImpostors,
                                ),
                            )
                        )
                    },
                )
                Spacer(Modifier.height(18.dp))
                Overline(stringResource(R.string.settings_default_impostors))
                Spacer(Modifier.height(8.dp))
                Stepper(
                    value = settings.defaultImpostors,
                    min = 1,
                    max = GameRules.maxImpostorsFor(settings.defaultPlayers),
                    onChange = { onChange(settings.copy(defaultImpostors = it)) },
                )
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = stringResource(R.string.setup_random_impostors),
                    subtitle = stringResource(R.string.setup_random_impostors_sub),
                    checked = settings.defaultRandomImpostors,
                    onCheckedChange = { onChange(settings.copy(defaultRandomImpostors = it)) },
                )
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = stringResource(R.string.settings_default_categories),
                    subtitle = defaultCategorySummary(settings, categories),
                    onClick = { showCategoryPicker = true },
                    trailing = {
                        Text(
                            text = if (settings.defaultCategoryIds.isEmpty()) {
                                "🎲"
                            } else {
                                categories.firstOrNull { it.id in settings.defaultCategoryIds }
                                    ?.emoji ?: "🎲"
                            },
                            fontSize = 22.sp,
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))
                Overline(stringResource(R.string.settings_default_difficulty))
                Spacer(Modifier.height(8.dp))
                SegmentedSelector(
                    options = listOf(
                        stringResource(R.string.setup_difficulty_easy),
                        stringResource(R.string.setup_difficulty_medium),
                        stringResource(R.string.setup_difficulty_hard),
                        stringResource(R.string.setup_difficulty_mixed),
                    ),
                    selectedIndex = Difficulty.entries.indexOf(settings.defaultDifficulty),
                    onSelect = {
                        onChange(settings.copy(defaultDifficulty = Difficulty.entries[it]))
                    },
                )
                Spacer(Modifier.height(18.dp))
                Overline(stringResource(R.string.settings_default_hint))
                Spacer(Modifier.height(8.dp))
                SegmentedSelector(
                    options = listOf(
                        stringResource(R.string.setup_hint_none),
                        stringResource(R.string.setup_hint_easy),
                        stringResource(R.string.setup_hint_vague),
                    ),
                    selectedIndex = HintMode.entries.indexOf(settings.defaultHintMode),
                    onSelect = { onChange(settings.copy(defaultHintMode = HintMode.entries[it])) },
                )
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = stringResource(R.string.setup_hide_category),
                    subtitle = stringResource(R.string.setup_hide_category_sub),
                    checked = settings.defaultHideCategoryFromImpostor,
                    onCheckedChange = {
                        onChange(settings.copy(defaultHideCategoryFromImpostor = it))
                    },
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_timers)) {
                Overline(stringResource(R.string.setup_round_timer))
                Spacer(Modifier.height(8.dp))
                SegmentedSelector(
                    options = DISCUSSION_TIMER_OPTIONS.map { timerOptionLabel(it) },
                    selectedIndex = DISCUSSION_TIMER_OPTIONS
                        .indexOf(settings.defaultDiscussionSeconds)
                        .coerceAtLeast(0),
                    onSelect = {
                        onChange(
                            settings.copy(defaultDiscussionSeconds = DISCUSSION_TIMER_OPTIONS[it])
                        )
                    },
                )
                Spacer(Modifier.height(18.dp))
                Overline(stringResource(R.string.setup_voting_timer))
                Spacer(Modifier.height(8.dp))
                SegmentedSelector(
                    options = VOTING_TIMER_OPTIONS.map { timerOptionLabel(it) },
                    selectedIndex = VOTING_TIMER_OPTIONS
                        .indexOf(settings.defaultVotingSeconds)
                        .coerceAtLeast(0),
                    onSelect = {
                        onChange(settings.copy(defaultVotingSeconds = VOTING_TIMER_OPTIONS[it]))
                    },
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_privacy)) {
                Text(
                    text = stringResource(R.string.settings_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImpostorTheme.extended.muted,
                )
            }

            Column(Modifier.fillMaxWidth()) {
                QuietButton(
                    text = if (historyCleared) {
                        stringResource(R.string.settings_word_history_cleared)
                    } else {
                        stringResource(R.string.settings_reset_word_history)
                    },
                    onClick = {
                        onClearWordHistory()
                        historyCleared = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                QuietButton(
                    text = stringResource(R.string.settings_reset_defaults),
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = ImpostorTheme.extended.muted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun defaultCategorySummary(settings: Settings, categories: List<Category>): String = when {
    settings.defaultCategoryIds.isEmpty() -> stringResource(R.string.category_all)
    settings.defaultCategoryIds.size == 1 ->
        categories.firstOrNull { it.id in settings.defaultCategoryIds }?.name
            ?: stringResource(R.string.category_all)
    else -> stringResource(R.string.category_count_selected, settings.defaultCategoryIds.size)
}

@Composable
private fun timerOptionLabel(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.off)
    seconds % 60 == 0 -> stringResource(R.string.minutes_short, seconds / 60)
    else -> stringResource(R.string.seconds_short, seconds)
}
