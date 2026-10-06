package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impostor.party.R
import com.impostor.party.data.model.Round
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ProgressRail
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SecondaryButton
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.components.formatClock
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

@Composable
fun DiscussionScreen(
    round: Round,
    remaining: Int,
    running: Boolean,
    expired: Boolean,
    onToggleTimer: () -> Unit,
    onAddTime: () -> Unit,
    onGoToVoting: () -> Unit,
    onQuit: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val timed = round.config.isTimedDiscussion

    LaunchedEffect(remaining, running) {
        if (running && remaining in 1..10) feedback.tick()
    }
    LaunchedEffect(expired) {
        if (expired) feedback.lose()
    }

    val timerColor by animateColorAsState(
        targetValue = when {
            expired -> ImpostorTheme.extended.impostor
            remaining in 1..10 -> ImpostorTheme.extended.accent
            else -> MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(300),
        label = "timerColor",
    )

    ScreenScaffold(
        title = stringResource(R.string.discussion_title),
        onBack = onQuit,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            SectionCard {
                // Hiding the category from the impostor only works if it stays
                // hidden here too - otherwise the discussion screen hands it back.
                Text(
                    text = if (round.categoryHiddenFromImpostor) {
                        stringResource(R.string.reveal_category_hidden)
                    } else {
                        "${round.categoryEmoji} ${round.categoryName}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (round.categoryHiddenFromImpostor) {
                        ImpostorTheme.extended.muted
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.discussion_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImpostorTheme.extended.muted,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(
                        R.string.discussion_first_speaker,
                        round.startingPlayerName,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (timed) {
                        Text(
                            text = formatClock(remaining),
                            style = MaterialTheme.typography.displayLarge,
                            color = timerColor,
                        )
                        Spacer(Modifier.height(18.dp))
                        ProgressRail(
                            progress = remaining.toFloat() /
                                round.config.discussionSeconds.toFloat().coerceAtLeast(1f),
                            color = timerColor,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                        AnimatedVisibility(visible = expired, enter = fadeIn()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(18.dp))
                                Overline(
                                    text = stringResource(R.string.discussion_time_up),
                                    color = ImpostorTheme.extended.impostor,
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SecondaryButton(
                                text = if (running) {
                                    stringResource(R.string.discussion_pause)
                                } else {
                                    stringResource(R.string.discussion_resume)
                                },
                                onClick = onToggleTimer,
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = stringResource(R.string.discussion_add_time),
                                onClick = onAddTime,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.discussion_no_timer),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ImpostorTheme.extended.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            PrimaryButton(
                text = stringResource(R.string.discussion_go_to_vote),
                onClick = onGoToVoting,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }
}
