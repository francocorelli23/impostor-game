package com.impostor.party.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.model.AppLanguage
import com.impostor.party.ui.components.LogoMark
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SecondaryButton
import com.impostor.party.ui.components.SegmentedSelector
import com.impostor.party.ui.theme.Display
import com.impostor.party.ui.theme.ImpostorTheme

@Composable
fun HomeScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onNewGame: () -> Unit,
    onHowToPlay: () -> Unit,
    onCustomWords: () -> Unit,
    onSettings: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    ScreenScaffold(title = null, onBack = null) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            LogoMark(
                ringColor = MaterialTheme.colorScheme.onBackground,
                accentColor = ImpostorTheme.extended.impostor,
                size = 100.dp,
                animateIn = true,
                breathe = true,
                contentDescription = stringResource(R.string.cd_app_mark),
            )

            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(700, delayMillis = 240)) +
                    slideInVertically(tween(700, delayMillis = 240)) { it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = Display,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ImpostorTheme.extended.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(600, delayMillis = 520)) +
                    slideInVertically(tween(600, delayMillis = 520)) { it / 4 },
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.home_new_game),
                        onClick = onNewGame,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.home_how_to_play),
                        onClick = onHowToPlay,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.home_custom_words),
                        onClick = onCustomWords,
                    )
                    SecondaryButton(
                        text = stringResource(R.string.home_settings),
                        onClick = onSettings,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(600, delayMillis = 700)),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SegmentedSelector(
                        options = listOf(
                            stringResource(R.string.language_system_short),
                            stringResource(R.string.language_english),
                            stringResource(R.string.language_croatian),
                        ),
                        selectedIndex = AppLanguage.selectable.indexOf(language).coerceAtLeast(0),
                        onSelect = { onLanguageChange(AppLanguage.selectable[it]) },
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.home_offline_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = ImpostorTheme.extended.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
