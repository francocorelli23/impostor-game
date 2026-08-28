package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.model.Round
import com.impostor.party.data.model.RoundOutcome
import com.impostor.party.game.GameRules
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.components.SecondaryButton
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

@Composable
fun ResultsScreen(
    round: Round,
    outcome: RoundOutcome?,
    votes: Map<Int, Int>,
    roundNumber: Int,
    onPlayAgain: () -> Unit,
    onNewCategory: () -> Unit,
    onChangeSettings: () -> Unit,
    onHome: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val extended = ImpostorTheme.extended
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealed = true
        when {
            outcome == null || !outcome.hasVerdict -> Unit
            outcome.crewWins -> feedback.win()
            else -> feedback.lose()
        }
    }

    val crewWon = outcome?.crewWins == true
    val hasVerdict = outcome?.hasVerdict == true
    val accent = if (crewWon) extended.crew else extended.impostor
    val multiple = round.impostorIndices.size > 1

    ScreenScaffold(title = null, onBack = onHome) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Overline(stringResource(R.string.results_round, roundNumber))
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 },
            ) {
                Column {
                    if (hasVerdict) {
                        Text(
                            text = when {
                                crewWon -> stringResource(R.string.results_crew_win)
                                multiple -> stringResource(R.string.results_impostors_win)
                                else -> stringResource(R.string.results_impostor_win)
                            },
                            style = MaterialTheme.typography.displaySmall,
                            color = accent,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (outcome?.tied == true) {
                                stringResource(R.string.results_tie)
                            } else if (crewWon) {
                                stringResource(R.string.results_crew_win_body)
                            } else {
                                stringResource(R.string.results_impostor_win_body)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = extended.muted,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.results_title),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.results_no_votes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = extended.muted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(500, delayMillis = 180)) +
                    slideInVertically(tween(500, delayMillis = 180)) { it / 4 },
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, extended.hairline, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${round.categoryEmoji} ${round.categoryName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.muted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.results_secret_word),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = round.secretWord,
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = if (round.secretWord.length > 14) 28.sp else 38.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(500, delayMillis = 360)) +
                    slideInVertically(tween(500, delayMillis = 360)) { it / 4 },
            ) {
                SectionCard(
                    title = if (multiple) {
                        stringResource(R.string.results_impostors_were)
                    } else {
                        stringResource(R.string.results_impostor_was)
                    },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        round.impostorNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = extended.impostor,
                            )
                        }
                    }
                }
            }

            if (votes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                AnimatedVisibility(
                    visible = revealed,
                    enter = fadeIn(tween(500, delayMillis = 520)),
                ) {
                    SectionCard(title = stringResource(R.string.results_votes_title)) {
                        val tally = GameRules.tally(votes)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            round.roles
                                .sortedByDescending { tally[it.index] ?: 0 }
                                .filter { (tally[it.index] ?: 0) > 0 }
                                .forEach { role ->
                                    val count = tally[role.index] ?: 0
                                    val isImpostor = role.index in round.impostorIndices
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = role.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isImpostor) {
                                                extended.impostor
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.results_votes,
                                                count,
                                                count,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.muted,
                                        )
                                    }
                                }

                            Spacer(Modifier.height(4.dp))
                            votes.entries.sortedBy { it.key }.forEach { (voter, suspect) ->
                                Text(
                                    text = stringResource(
                                        R.string.results_voted_for,
                                        round.roles.getOrNull(voter)?.name.orEmpty(),
                                        round.roles.getOrNull(suspect)?.name.orEmpty(),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.muted,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.results_play_again),
                    onClick = onPlayAgain,
                )
                SecondaryButton(
                    text = stringResource(R.string.results_new_category),
                    onClick = onNewCategory,
                )
                SecondaryButton(
                    text = stringResource(R.string.results_change_settings),
                    onClick = onChangeSettings,
                )
                QuietButton(
                    text = stringResource(R.string.results_home),
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
