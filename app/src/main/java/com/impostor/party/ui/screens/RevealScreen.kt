package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.model.PlayerRole
import com.impostor.party.data.model.Round
import com.impostor.party.game.RevealStep
import com.impostor.party.ui.components.LogoMark
import com.impostor.party.ui.components.Overline
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ProgressRail
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

/**
 * The phone travels in whatever order the group likes. Whoever is holding it taps
 * their own name; names that have already been looked at are dimmed and refuse
 * further taps, so nobody can see a role twice and nobody is forced to wait a turn.
 */
@Composable
fun RevealScreen(
    round: Round,
    activeReveal: Int?,
    revealedIndices: Set<Int>,
    step: RevealStep,
    onSelectPlayer: (Int) -> Unit,
    onHide: () -> Unit,
    onStartDiscussion: () -> Unit,
    onQuit: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val active = activeReveal?.let { round.roles.getOrNull(it) }

    LaunchedEffect(step, activeReveal) {
        if (step == RevealStep.SHOWING && active != null) {
            feedback.reveal()
        }
    }

    ScreenScaffold(title = null, onBack = onQuit) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            if (step != RevealStep.ALL_DONE) {
                Overline(
                    stringResource(
                        R.string.reveal_progress,
                        revealedIndices.size,
                        round.roles.size,
                    )
                )
                Spacer(Modifier.height(10.dp))
                ProgressRail(
                    progress = revealedIndices.size.toFloat() / round.roles.size.toFloat()
                )
                Spacer(Modifier.height(18.dp))
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = step to activeReveal,
                    transitionSpec = {
                        (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.03f))
                    },
                    label = "reveal",
                ) { (currentStep, currentIndex) ->
                    // The role is read from the animated state, never the outer scope,
                    // so the outgoing card cannot re-compose with someone else's word
                    // while it fades away.
                    when (currentStep) {
                        RevealStep.ALL_DONE -> AllDoneCard()

                        RevealStep.SELECTING -> NameGrid(
                            roles = round.roles,
                            revealed = revealedIndices,
                            onSelect = onSelectPlayer,
                        )

                        RevealStep.SHOWING -> {
                            val shown = currentIndex?.let { round.roles.getOrNull(it) }
                            if (shown == null) {
                                Spacer(Modifier.height(1.dp))
                            } else {
                                // Apps targeting API 36 can be rotated on large screens
                                // whatever the manifest asks for, so a tall role card has
                                // to survive a short window. The name grid above scrolls
                                // itself; this branch has no lazy content, so a plain
                                // scroll container is safe here.
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    RoleCard(round, shown)
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.padding(bottom = 20.dp)) {
                when (step) {
                    RevealStep.SELECTING -> Text(
                        text = stringResource(R.string.reveal_tap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = ImpostorTheme.extended.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    RevealStep.SHOWING -> PrimaryButton(
                        text = stringResource(R.string.reveal_hide),
                        onClick = onHide,
                    )

                    RevealStep.ALL_DONE -> PrimaryButton(
                        text = stringResource(R.string.reveal_start_discussion),
                        onClick = onStartDiscussion,
                    )
                }
            }
        }
    }
}

@Composable
private fun NameGrid(
    roles: List<PlayerRole>,
    revealed: Set<Int>,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.reveal_pick_your_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(roles, key = { it.index }) { role ->
                NameChip(
                    name = role.name,
                    done = role.index in revealed,
                    onClick = { onSelect(role.index) },
                )
            }
        }
    }
}

@Composable
private fun NameChip(
    name: String,
    done: Boolean,
    onClick: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val doneLabel = stringResource(R.string.reveal_already_seen)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (done) 0.42f else 1f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, ImpostorTheme.extended.hairline, RoundedCornerShape(18.dp))
            .clickable(
                enabled = !done,
                role = Role.Button,
                onClickLabel = if (done) doneLabel else null,
            ) {
                feedback.tap()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = doneLabel,
                tint = ImpostorTheme.extended.crew,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RoleCard(round: Round, role: PlayerRole) {
    val extended = ImpostorTheme.extended
    val impostor = role.isImpostor
    // With the category hidden the impostor is told nothing at all about the word.
    val showCategory = !impostor || !round.categoryHiddenFromImpostor

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (impostor) extended.impostorContainer else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (impostor) extended.impostor else extended.hairline,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (showCategory) {
                "${round.categoryEmoji} ${round.categoryName}"
            } else {
                stringResource(R.string.reveal_category_hidden)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (impostor) extended.impostor else extended.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        if (impostor) {
            Text(
                text = if (round.revealsImpostorCount && round.impostorIndices.size > 1) {
                    stringResource(R.string.reveal_impostor_plural, round.impostorIndices.size)
                } else {
                    stringResource(R.string.reveal_you_are_impostor)
                },
                style = MaterialTheme.typography.displaySmall,
                color = extended.impostor,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.reveal_impostor_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.muted,
                textAlign = TextAlign.Center,
            )
            val hint = role.hint
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(28.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Overline(stringResource(R.string.reveal_hint_label))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.reveal_you_are_crew),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = role.word.orEmpty(),
                style = MaterialTheme.typography.displayMedium,
                fontSize = if ((role.word?.length ?: 0) > 14) 30.sp else 40.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AllDoneCard() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LogoMark(
            ringColor = MaterialTheme.colorScheme.onBackground,
            accentColor = ImpostorTheme.extended.impostor,
            size = 84.dp,
            animateIn = true,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.reveal_all_done_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.reveal_all_done_body),
            style = MaterialTheme.typography.bodyLarge,
            color = ImpostorTheme.extended.muted,
            textAlign = TextAlign.Center,
        )
    }
}
