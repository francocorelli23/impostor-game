package com.impostor.party.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.impostor.party.R
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.theme.ImpostorTheme

private const val MAX_NAME_LENGTH = 16

@Composable
fun NameEditor(
    playerCount: Int,
    names: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val draft = remember(names, playerCount) {
        mutableStateListOf<String>().apply {
            for (i in 0 until playerCount) add(names.getOrNull(i).orEmpty())
        }
    }

    // Names for seats beyond the current player count are held aside rather than
    // dropped, so going from eight players down to four and back up again does
    // not lose the last four. Clearing wipes these too.
    val retained = remember(names, playerCount) {
        mutableStateListOf<String>().apply {
            if (names.size > playerCount) addAll(names.subList(playerCount, names.size))
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.setup_player_names),
        subtitle = stringResource(R.string.setup_names_hint),
        onBack = onDismiss,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(playerCount) { index ->
                    val placeholder = stringResource(R.string.player_default_name, index + 1)
                    OutlinedTextField(
                        value = draft.getOrElse(index) { "" },
                        onValueChange = { new ->
                            if (index < draft.size) {
                                draft[index] = new.take(MAX_NAME_LENGTH)
                            }
                        },
                        singleLine = true,
                        label = { Text(placeholder) },
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = if (index == playerCount - 1) ImeAction.Done else ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedBorderColor = ImpostorTheme.extended.hairline,
                            focusedLabelColor = ImpostorTheme.extended.muted,
                            unfocusedLabelColor = ImpostorTheme.extended.muted,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.done),
                    onClick = {
                        onConfirm((draft + retained).dropLastWhile { it.isBlank() })
                    },
                )
                Spacer(Modifier.height(4.dp))
                QuietButton(
                    text = stringResource(R.string.setup_names_clear),
                    onClick = {
                        for (i in draft.indices) draft[i] = ""
                        retained.clear()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
