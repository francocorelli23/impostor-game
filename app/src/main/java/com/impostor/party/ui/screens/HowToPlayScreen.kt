package com.impostor.party.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.impostor.party.R
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.components.SectionCard
import com.impostor.party.ui.theme.ImpostorTheme

@Composable
fun HowToPlayScreen(
    onBack: () -> Unit,
    onStart: (() -> Unit)? = null,
) {
    ScreenScaffold(
        title = stringResource(R.string.htp_title),
        subtitle = stringResource(R.string.htp_subtitle),
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Step(1, stringResource(R.string.htp_step1_title), stringResource(R.string.htp_step1_body))
            Step(2, stringResource(R.string.htp_step2_title), stringResource(R.string.htp_step2_body))
            Step(3, stringResource(R.string.htp_step3_title), stringResource(R.string.htp_step3_body))
            Step(4, stringResource(R.string.htp_step4_title), stringResource(R.string.htp_step4_body))
            Step(5, stringResource(R.string.htp_step5_title), stringResource(R.string.htp_step5_body))

            Spacer(Modifier.height(4.dp))

            SectionCard(title = stringResource(R.string.htp_tip_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Bullet(stringResource(R.string.htp_tip_1))
                    Bullet(stringResource(R.string.htp_tip_2))
                    Bullet(stringResource(R.string.htp_tip_3))
                    Bullet(stringResource(R.string.htp_tip_4))
                }
            }

            if (onStart != null) {
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = stringResource(R.string.home_new_game),
                    onClick = onStart,
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ImpostorTheme.extended.elevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.padding(start = 8.dp))
        Column(Modifier.padding(start = 8.dp, top = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = ImpostorTheme.extended.muted,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(ImpostorTheme.extended.muted)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ImpostorTheme.extended.muted,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
