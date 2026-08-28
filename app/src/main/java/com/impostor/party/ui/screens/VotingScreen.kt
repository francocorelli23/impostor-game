package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impostor.party.R
import com.impostor.party.data.model.PlayerRole
import com.impostor.party.data.model.Round
import com.impostor.party.game.VoteStep
import com.impostor.party.ui.components.LogoMark
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ProgressRail
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.formatClock
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

@Composable
fun VotingScreen(
    round: Round,
    voteIndex: Int,
    step: VoteStep,
    selectedSuspect: Int?,
    remaining: Int,
    onVoterReady: () -> Unit,
    onSelectSuspect: (Int) -> Unit,
    onLockIn: () -> Unit,
    onSkip: () -> Unit,
    onReveal: () -> Unit,
    onQuit: () -> Unit,
) {
    val voter = round.roles.getOrNull(voteIndex)
    val timed = round.config.isTimedVoting

    ScreenScaffold(title = null, onBack = onQuit) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Overline(stringResource(R.string.voting_title))
            Spacer(Modifier.height(10.dp))

            if (step != VoteStep.READY_TO_REVEAL) {
                ProgressRail(progress = voteIndex.toFloat() / round.roles.size.toFloat())
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.voting_progress, voteIndex + 1, round.roles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = ImpostorTheme.extended.muted,
                )
            }

            if (timed && step != VoteStep.READY_TO_REVEAL) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatClock(remaining),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (remaining in 1..10) {
                        ImpostorTheme.extended.impostor
                    } else {
                        ImpostorTheme.extended.muted
                    },
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.96f)) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "voting",
                ) { current ->
                    when (current) {
                        VoteStep.HANDOFF -> VoterHandoff(voter?.name.orEmpty())

                        VoteStep.CHOOSING -> SuspectGrid(
                            roles = round.roles,
                            voterIndex = voteIndex,
                            selected = selectedSuspect,
                            impostorCount = round.impostorIndices.size,
                            onSelect = onSelectSuspect,
                        )

                        VoteStep.RECORDED -> Text(
                            text = stringResource(R.string.voting_hidden),
                            style = MaterialTheme.typography.headlineSmall,
                            color = ImpostorTheme.extended.muted,
                        )

                        VoteStep.READY_TO_REVEAL -> AllVotesIn()
                    }
                }
            }

            Column(Modifier.padding(bottom = 16.dp)) {
                when (step) {
                    VoteStep.HANDOFF -> PrimaryButton(
                        text = voter?.name?.let { stringResource(R.string.reveal_ready, it) }
                            ?: stringResource(R.string.reveal_ready_generic),
                        onClick = onVoterReady,
                    )

                    VoteStep.CHOOSING -> PrimaryButton(
                        text = stringResource(R.string.voting_lock_in),
                        onClick = onLockIn,
                        enabled = selectedSuspect != null,
                    )

                    VoteStep.RECORDED -> Spacer(Modifier.height(1.dp))

                    VoteStep.READY_TO_REVEAL -> PrimaryButton(
                        text = stringResource(R.string.voting_reveal),
                        onClick = onReveal,
                    )
                }
                if (step != VoteStep.READY_TO_REVEAL) {
                    QuietButton(
                        text = stringResource(R.string.voting_skip),
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.voting_skip_explain),
                        style = MaterialTheme.typography.bodySmall,
                        color = ImpostorTheme.extended.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun VoterHandoff(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LogoMark(
            ringColor = ImpostorTheme.extended.muted,
            accentColor = ImpostorTheme.extended.muted,
            size = 52.dp,
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = stringResource(R.string.voting_pass_to),
            style = MaterialTheme.typography.bodyLarge,
            color = ImpostorTheme.extended.muted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AllVotesIn() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LogoMark(
            ringColor = MaterialTheme.colorScheme.onBackground,
            accentColor = ImpostorTheme.extended.impostor,
            size = 80.dp,
            animateIn = true,
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.voting_all_in),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SuspectGrid(
    roles: List<PlayerRole>,
    voterIndex: Int,
    selected: Int?,
    impostorCount: Int,
    onSelect: (Int) -> Unit,
) {
    val candidates = roles.filter { it.index != voterIndex }
    Column(Modifier.fillMaxSize()) {
        Text(
            text = if (impostorCount > 1) {
                stringResource(R.string.voting_prompt_plural)
            } else {
                stringResource(R.string.voting_prompt)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(candidates, key = { it.index }) { candidate ->
                SuspectChip(
                    name = candidate.name,
                    selected = selected == candidate.index,
                    onClick = { onSelect(candidate.index) },
                )
            }
        }
    }
}

@Composable
private fun SuspectChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "suspectScale",
    )
    val border by animateColorAsState(
        targetValue = if (selected) {
            ImpostorTheme.extended.impostor
        } else {
            ImpostorTheme.extended.hairline
        },
        animationSpec = tween(160),
        label = "suspectBorder",
    )
    val background by animateColorAsState(
        targetValue = if (selected) {
            ImpostorTheme.extended.impostorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(160),
        label = "suspectBg",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable {
                feedback.select()
                onClick()
            }
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
