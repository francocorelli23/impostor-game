package com.impostor.party.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.impostor.party.ui.theme.ImpostorTheme

/**
 * The app's one dialog: a bordered surface card with the same buttons as every
 * screen, instead of Material's stock grey container.
 *
 * A dialog lives in its own window, and that window's composition re-provides
 * [LocalContext] and [LocalConfiguration] from the activity - which would quietly
 * drop the in-app language override and show the device's language instead. Both
 * are captured here and handed back in, so text inside follows the chosen language.
 */
@Composable
fun AppDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    body: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
        ) {
            val extended = ImpostorTheme.extended
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, extended.hairline, RoundedCornerShape(28.dp))
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (body != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.muted,
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(18.dp))
                    content()
                }
                Spacer(Modifier.height(22.dp))
                PrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    container = if (destructive) extended.impostor else null,
                    contentColor = if (destructive) MaterialTheme.colorScheme.onError else null,
                )
                Spacer(Modifier.height(4.dp))
                QuietButton(
                    text = dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Single-line text input in the app's style: inset background, hairline border, ink focus. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val extended = ImpostorTheme.extended
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        isError = isError,
        supportingText = if (errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            focusedContainerColor = colors.background,
            unfocusedContainerColor = colors.background,
            errorContainerColor = colors.background,
            cursorColor = colors.onBackground,
            errorCursorColor = extended.impostor,
            focusedBorderColor = colors.onBackground,
            unfocusedBorderColor = extended.hairline,
            errorBorderColor = extended.impostor,
            focusedLabelColor = extended.muted,
            unfocusedLabelColor = extended.muted,
            errorLabelColor = extended.impostor,
            focusedPlaceholderColor = extended.muted,
            unfocusedPlaceholderColor = extended.muted,
            errorSupportingTextColor = extended.impostor,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
