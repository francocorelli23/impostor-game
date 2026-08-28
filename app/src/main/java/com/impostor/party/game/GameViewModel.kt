package com.impostor.party.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.impostor.party.ImpostorApplication
import com.impostor.party.data.SettingsRepository
import com.impostor.party.data.WordRepository
import com.impostor.party.data.model.GameConfig
import com.impostor.party.data.model.Round
import com.impostor.party.data.model.RoundOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Phase { IDLE, PREPARING, REVEAL, DISCUSSION, VOTING, RESULTS }

/** SELECTING: the name grid. SHOWING: one player is looking at their role. */
enum class RevealStep { SELECTING, SHOWING, ALL_DONE }

enum class VoteStep { HANDOFF, CHOOSING, RECORDED, READY_TO_REVEAL }

data class GameUiState(
    val phase: Phase = Phase.IDLE,
    val round: Round? = null,

    /** Who is looking right now, or null while the name grid is up. */
    val activeReveal: Int? = null,
    val revealedIndices: Set<Int> = emptySet(),
    val revealStep: RevealStep = RevealStep.SELECTING,

    val discussionRemaining: Int = 0,
    val discussionRunning: Boolean = false,
    val discussionExpired: Boolean = false,

    val voteIndex: Int = 0,
    val voteStep: VoteStep = VoteStep.HANDOFF,
    val votingRemaining: Int = 0,
    val selectedSuspect: Int? = null,
    val votes: Map<Int, Int> = emptyMap(),
    val votingSkipped: Boolean = false,

    val outcome: RoundOutcome? = null,
    val roundNumber: Int = 1,
    val failedToLoadWords: Boolean = false,
) {
    val currentVoter get() = round?.roles?.getOrNull(voteIndex)
    val revealedCount get() = revealedIndices.size
}

/**
 * Owns the whole life of a round. Everything is in memory and in this ViewModel,
 * so nothing about a game is ever written to disk.
 */
class GameViewModel(
    application: Application,
    private val words: WordRepository,
    private val settings: SettingsRepository,
    private val random: Random = Random.Default,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    /** Supplied by the UI so fallback seat labels follow the chosen language. */
    private var defaultName: (Int) -> String = GameRules::defaultPlayerName

    // ---------------------------------------------------------------- round setup

    fun startGame(config: GameConfig, defaultName: (Int) -> String = this.defaultName) {
        this.defaultName = defaultName
        timerJob?.cancel()
        val safeConfig = config.copy(
            playerCount = config.playerCount.coerceIn(GameRules.MIN_PLAYERS, GameRules.MAX_PLAYERS),
            impostorCount = GameRules.clampImpostors(config.playerCount, config.impostorCount),
        )
        settings.rememberConfig(safeConfig)
        _state.value = GameUiState(phase = Phase.PREPARING, roundNumber = _state.value.roundNumber)

        viewModelScope.launch {
            val pick = words.pickWord(
                categoryIds = safeConfig.categoryIds,
                difficulty = safeConfig.difficulty,
                random = random,
                language = settings.effectiveLanguageTag(),
            )
            if (pick == null) {
                _state.value = _state.value.copy(phase = Phase.IDLE, failedToLoadWords = true)
                return@launch
            }
            val round = GameRules.buildRound(
                config = safeConfig,
                category = pick.category,
                entry = pick.entry,
                random = random,
                defaultName = this@GameViewModel.defaultName,
            )
            _state.value = GameUiState(
                phase = Phase.REVEAL,
                round = round,
                revealStep = RevealStep.SELECTING,
                roundNumber = _state.value.roundNumber,
            )
        }
    }

    /** Same players and settings, brand new word and brand new impostors. */
    fun playAgain() {
        val config = _state.value.round?.config ?: return
        _state.value = _state.value.copy(roundNumber = _state.value.roundNumber + 1)
        startGame(config)
    }

    fun startGameWithCategories(categoryIds: Set<String>) {
        val config = _state.value.round?.config ?: return
        _state.value = _state.value.copy(roundNumber = _state.value.roundNumber + 1)
        startGame(config.copy(categoryIds = categoryIds))
    }

    fun endGame() {
        timerJob?.cancel()
        _state.value = GameUiState()
    }

    // ---------------------------------------------------------------- reveal phase

    /**
     * Any player may take the phone at any point and tap their own name. A name
     * that has already been looked at is refused, so a role can never be shown twice.
     */
    fun selectPlayerForReveal(index: Int) {
        val s = _state.value
        val round = s.round ?: return
        if (s.revealStep != RevealStep.SELECTING) return
        if (index !in round.roles.indices) return
        if (index in s.revealedIndices) return
        _state.value = s.copy(activeReveal = index, revealStep = RevealStep.SHOWING)
    }

    fun hideCurrentReveal() {
        val s = _state.value
        val round = s.round ?: return
        val active = s.activeReveal ?: return
        if (s.revealStep != RevealStep.SHOWING) return

        val revealed = s.revealedIndices + active
        _state.value = s.copy(
            activeReveal = null,
            revealedIndices = revealed,
            revealStep = if (revealed.size >= round.roles.size) {
                RevealStep.ALL_DONE
            } else {
                RevealStep.SELECTING
            },
        )
    }

    fun startDiscussion() {
        val round = _state.value.round ?: return
        val seconds = round.config.discussionSeconds
        _state.value = _state.value.copy(
            phase = Phase.DISCUSSION,
            discussionRemaining = seconds,
            discussionRunning = seconds > 0,
            discussionExpired = false,
        )
        if (seconds > 0) runDiscussionTimer()
    }

    // ------------------------------------------------------------ discussion phase

    private fun runDiscussionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val s = _state.value
                if (s.phase != Phase.DISCUSSION || !s.discussionRunning) return@launch
                val remaining = s.discussionRemaining - 1
                if (remaining <= 0) {
                    _state.value = s.copy(
                        discussionRemaining = 0,
                        discussionRunning = false,
                        discussionExpired = true,
                    )
                    return@launch
                }
                _state.value = s.copy(discussionRemaining = remaining)
            }
        }
    }

    fun toggleDiscussionTimer() {
        val s = _state.value
        if (s.discussionRemaining <= 0) return
        if (s.discussionRunning) {
            timerJob?.cancel()
            _state.value = s.copy(discussionRunning = false)
        } else {
            _state.value = s.copy(discussionRunning = true, discussionExpired = false)
            runDiscussionTimer()
        }
    }

    fun addDiscussionTime(seconds: Int = 30) {
        val s = _state.value
        if (s.phase != Phase.DISCUSSION) return
        val wasRunning = s.discussionRunning
        _state.value = s.copy(
            discussionRemaining = s.discussionRemaining + seconds,
            discussionExpired = false,
            discussionRunning = true,
        )
        if (!wasRunning) runDiscussionTimer()
    }

    // ---------------------------------------------------------------- voting phase

    fun goToVoting() {
        val round = _state.value.round ?: return
        timerJob?.cancel()
        val seconds = round.config.votingSeconds
        _state.value = _state.value.copy(
            phase = Phase.VOTING,
            discussionRunning = false,
            voteIndex = 0,
            voteStep = VoteStep.HANDOFF,
            votes = emptyMap(),
            selectedSuspect = null,
            votingSkipped = false,
            votingRemaining = seconds,
        )
        if (seconds > 0) runVotingTimer()
    }

    private fun runVotingTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val s = _state.value
                if (s.phase != Phase.VOTING) return@launch
                val remaining = s.votingRemaining - 1
                if (remaining <= 0) {
                    _state.value = s.copy(votingRemaining = 0)
                    finishVoting(skipped = false)
                    return@launch
                }
                _state.value = s.copy(votingRemaining = remaining)
            }
        }
    }

    fun onVoterReady() {
        if (_state.value.voteStep != VoteStep.HANDOFF) return
        _state.value = _state.value.copy(voteStep = VoteStep.CHOOSING, selectedSuspect = null)
    }

    fun selectSuspect(index: Int) {
        val s = _state.value
        if (s.voteStep != VoteStep.CHOOSING) return
        if (index == s.voteIndex) return // you cannot vote for yourself
        _state.value = s.copy(selectedSuspect = if (s.selectedSuspect == index) null else index)
    }

    fun lockInVote() {
        val s = _state.value
        val round = s.round ?: return
        val suspect = s.selectedSuspect ?: return
        if (s.voteStep != VoteStep.CHOOSING) return

        val votes = s.votes + (s.voteIndex to suspect)
        val next = s.voteIndex + 1
        _state.value = if (next >= round.roles.size) {
            s.copy(votes = votes, voteStep = VoteStep.READY_TO_REVEAL, selectedSuspect = null)
        } else {
            s.copy(
                votes = votes,
                voteIndex = next,
                voteStep = VoteStep.HANDOFF,
                selectedSuspect = null,
            )
        }
    }

    fun skipVoting() = finishVoting(skipped = true)

    fun revealResults() = finishVoting(skipped = false)

    private fun finishVoting(skipped: Boolean) {
        timerJob?.cancel()
        val s = _state.value
        val round = s.round ?: return
        val votes = if (skipped) emptyMap() else s.votes
        _state.value = s.copy(
            phase = Phase.RESULTS,
            votes = votes,
            votingSkipped = skipped,
            outcome = GameRules.outcome(round, votes),
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    class Factory(private val app: ImpostorApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(app, app.container.words, app.container.settings) as T
        }
    }
}
