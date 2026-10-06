package com.impostor.party.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impostor.party.BuildConfig
import com.impostor.party.ImpostorApplication
import com.impostor.party.R
import com.impostor.party.data.CustomWordList
import com.impostor.party.data.model.AppLanguage
import com.impostor.party.data.model.Category
import com.impostor.party.game.GameViewModel
import com.impostor.party.game.Phase
import com.impostor.party.ui.components.AppDialog
import com.impostor.party.ui.screens.CategoryPicker
import com.impostor.party.ui.screens.CustomWordActions
import com.impostor.party.ui.screens.CustomWordsScreen
import com.impostor.party.ui.screens.DiscussionScreen
import com.impostor.party.ui.screens.HomeScreen
import com.impostor.party.ui.screens.HowToPlayScreen
import com.impostor.party.ui.screens.ResultsScreen
import com.impostor.party.ui.screens.RevealScreen
import com.impostor.party.ui.screens.SettingsScreen
import com.impostor.party.ui.screens.SetupScreen
import com.impostor.party.ui.screens.VotingScreen
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.Feedback
import com.impostor.party.util.LocalFeedback
import java.util.Locale

enum class Route { HOME, SETUP, GAME, HOW_TO_PLAY, SETTINGS, CUSTOM_WORDS }

@Composable
fun ImpostorApp() {
    val application = LocalContext.current.applicationContext as ImpostorApplication
    val container = application.container
    val settings by container.settings.settings.collectAsState()
    val haptics = LocalHapticFeedback.current

    val feedback = remember(haptics) {
        Feedback(
            sound = container.sound,
            haptics = haptics,
            soundEnabled = { container.settings.settings.value.soundEnabled },
            hapticsEnabled = { container.settings.settings.value.vibrationEnabled },
        )
    }

    LaunchedEffect(settings.musicEnabled) {
        container.sound.setMusicEnabled(settings.musicEnabled)
    }

    // An explicit language choice is applied by handing the whole tree a Context
    // configured for that locale, so every stringResource below resolves against
    // values-<lang>/strings.xml with no appcompat dependency and no restart.
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val override = when (settings.language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ENGLISH -> "en"
        AppLanguage.CROATIAN -> "hr"
    }
    val localizedContext = remember(baseContext, baseConfiguration, override) {
        if (override == null) {
            baseContext
        } else {
            val configuration = Configuration(baseConfiguration)
            configuration.setLocale(Locale.forLanguageTag(override))
            baseContext.createConfigurationContext(configuration)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalFeedback provides feedback,
    ) {
        ImpostorTheme(themeMode = settings.themeMode) {
            AppContent(
                application = application,
                versionName = BuildConfig.VERSION_NAME,
            )
        }
    }
}

@Composable
private fun AppContent(
    application: ImpostorApplication,
    versionName: String,
) {
    val container = application.container
    val settings by container.settings.settings.collectAsState()
    val resourceContext = LocalContext.current

    val languageTag = remember(settings.language) { container.settings.effectiveLanguageTag() }
    // Re-read whenever the players edit their own words, so changes show up at once.
    val customLists by container.customWords.lists.collectAsState()
    val categories by produceState(initialValue = emptyList<Category>(), languageTag, customLists) {
        value = container.words.categories(languageTag)
    }
    val bundledCategories by produceState(initialValue = emptyList<Category>(), languageTag) {
        value = container.words.bundledCategories(languageTag)
    }
    val customWordActions = remember(languageTag) {
        val store = container.customWords
        CustomWordActions(
            addCategory = { name, emoji -> store.addCategory(languageTag, name, emoji) },
            updateCategory = { id, name, emoji -> store.updateCategory(languageTag, id, name, emoji) },
            deleteCategory = { id -> store.deleteCategory(languageTag, id) },
            addWord = { categoryId, word, easy, vague ->
                store.addWord(languageTag, categoryId, word, easy, vague)
            },
            updateWord = { categoryId, original, word, easy, vague ->
                store.updateWord(languageTag, categoryId, original, word, easy, vague)
            },
            deleteWord = { categoryId, word -> store.deleteWord(languageTag, categoryId, word) },
        )
    }

    val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(application))
    val gameState by viewModel.state.collectAsState()

    val backStack = remember { mutableStateListOf(Route.HOME) }
    val route = backStack.last()

    var config by remember { mutableStateOf(container.settings.settings.value.toConfig()) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showNextCategoryPicker by remember { mutableStateOf(false) }

    /** Seat labels fall back to a translated "Player N". */
    val defaultName: (Int) -> String = { index ->
        resourceContext.getString(R.string.player_default_name, index + 1)
    }

    fun navigateTo(target: Route) {
        if (backStack.last() != target) backStack.add(target)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
    }

    fun goHome() {
        viewModel.endGame()
        backStack.clear()
        backStack.add(Route.HOME)
    }

    // Keep the screen awake only while a round is actually in progress.
    val view = LocalView.current
    val inGame = route == Route.GAME && gameState.phase != Phase.IDLE
    DisposableEffect(settings.keepScreenOn, inGame) {
        view.keepScreenOn = settings.keepScreenOn && inGame
        onDispose { view.keepScreenOn = false }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val offset = if (forward) 1 else -1
                (
                    slideInHorizontally(tween(320)) { offset * it / 6 } + fadeIn(tween(280))
                    ) togetherWith (
                    slideOutHorizontally(tween(320)) { -offset * it / 6 } + fadeOut(tween(220))
                    )
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.HOME -> HomeScreen(
                    language = settings.language,
                    onLanguageChange = { chosen ->
                        container.settings.update { it.copy(language = chosen) }
                    },
                    onNewGame = {
                        config = container.settings.settings.value.toConfig()
                        navigateTo(Route.SETUP)
                    },
                    onHowToPlay = { navigateTo(Route.HOW_TO_PLAY) },
                    onCustomWords = { navigateTo(Route.CUSTOM_WORDS) },
                    onSettings = { navigateTo(Route.SETTINGS) },
                )

                Route.CUSTOM_WORDS -> CustomWordsScreen(
                    bundled = bundledCategories,
                    custom = customLists[languageTag] ?: CustomWordList(),
                    languageName = stringResource(
                        if (languageTag == "hr") R.string.language_croatian else R.string.language_english
                    ),
                    actions = customWordActions,
                    onBack = { pop() },
                )

                Route.SETUP -> SetupScreen(
                    config = config,
                    categories = categories,
                    onConfigChange = { updated ->
                        // Names outlive the game they were typed for, so they are
                        // written through to settings the moment they change.
                        if (updated.playerNames != config.playerNames) {
                            container.settings.update { it.copy(playerNames = updated.playerNames) }
                        }
                        config = updated
                    },
                    onStart = {
                        viewModel.startGame(config, defaultName)
                        navigateTo(Route.GAME)
                    },
                    onBack = { pop() },
                )

                Route.HOW_TO_PLAY -> HowToPlayScreen(
                    onBack = { pop() },
                    onStart = {
                        config = container.settings.settings.value.toConfig()
                        backStack.clear()
                        backStack.add(Route.HOME)
                        backStack.add(Route.SETUP)
                    },
                )

                Route.SETTINGS -> SettingsScreen(
                    settings = settings,
                    categories = categories,
                    versionName = versionName,
                    onChange = { updated -> container.settings.update { updated } },
                    onResetDefaults = { container.settings.resetToDefaults() },
                    onClearWordHistory = { container.words.clearHistory() },
                    onBack = { pop() },
                )

                Route.GAME -> GameRoute(
                    viewModel = viewModel,
                    onQuitRequested = {
                        if (gameState.phase == Phase.RESULTS) {
                            goHome()
                        } else {
                            showQuitDialog = true
                        }
                    },
                    onChangeSettings = {
                        gameState.round?.let { config = it.config }
                        viewModel.endGame()
                        backStack.clear()
                        backStack.add(Route.HOME)
                        backStack.add(Route.SETUP)
                    },
                    onNewCategory = { showNextCategoryPicker = true },
                    onHome = { goHome() },
                )
            }
        }

        if (showNextCategoryPicker) {
            CategoryPicker(
                categories = categories,
                selectedIds = gameState.round?.config?.categoryIds ?: emptySet(),
                onConfirm = { ids ->
                    showNextCategoryPicker = false
                    viewModel.startGameWithCategories(ids)
                },
                onDismiss = { showNextCategoryPicker = false },
            )
        }
    }

    if (showQuitDialog) {
        AppDialog(
            title = stringResource(R.string.quit_title),
            body = stringResource(R.string.quit_body),
            confirmText = stringResource(R.string.quit_confirm),
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                showQuitDialog = false
                goHome()
            },
            onDismiss = { showQuitDialog = false },
        )
    }

    // System back: never leaves the app from a nested screen by accident.
    BackHandler(enabled = backStack.size > 1 || showNextCategoryPicker) {
        when {
            showNextCategoryPicker -> {
                showNextCategoryPicker = false
            }

            route == Route.GAME && gameState.phase != Phase.RESULTS -> {
                showQuitDialog = true
            }

            route == Route.GAME -> goHome()
            else -> pop()
        }
    }
}

@Composable
private fun GameRoute(
    viewModel: GameViewModel,
    onQuitRequested: () -> Unit,
    onChangeSettings: () -> Unit,
    onNewCategory: () -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val round = state.round

    when {
        round == null || state.phase == Phase.PREPARING || state.phase == Phase.IDLE -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }
        }

        state.phase == Phase.REVEAL -> RevealScreen(
            round = round,
            activeReveal = state.activeReveal,
            revealedIndices = state.revealedIndices,
            step = state.revealStep,
            onSelectPlayer = viewModel::selectPlayerForReveal,
            onHide = viewModel::hideCurrentReveal,
            onStartDiscussion = viewModel::startDiscussion,
            onQuit = onQuitRequested,
        )

        state.phase == Phase.DISCUSSION -> DiscussionScreen(
            round = round,
            remaining = state.discussionRemaining,
            running = state.discussionRunning,
            expired = state.discussionExpired,
            onToggleTimer = viewModel::toggleDiscussionTimer,
            onAddTime = { viewModel.addDiscussionTime() },
            onGoToVoting = viewModel::goToVoting,
            onQuit = onQuitRequested,
        )

        state.phase == Phase.VOTING -> VotingScreen(
            round = round,
            voteIndex = state.voteIndex,
            step = state.voteStep,
            selectedSuspect = state.selectedSuspect,
            remaining = state.votingRemaining,
            onVoterReady = viewModel::onVoterReady,
            onSelectSuspect = viewModel::selectSuspect,
            onLockIn = viewModel::lockInVote,
            onSkip = viewModel::skipVoting,
            onReveal = viewModel::revealResults,
            onQuit = onQuitRequested,
        )

        else -> ResultsScreen(
            round = round,
            outcome = state.outcome,
            votes = state.votes,
            roundNumber = state.roundNumber,
            onPlayAgain = viewModel::playAgain,
            onNewCategory = onNewCategory,
            onChangeSettings = onChangeSettings,
            onHome = onHome,
        )
    }
}
