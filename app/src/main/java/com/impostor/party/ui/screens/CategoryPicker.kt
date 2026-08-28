package com.impostor.party.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.data.model.Category
import com.impostor.party.ui.components.PrimaryButton
import com.impostor.party.ui.components.QuietButton
import com.impostor.party.ui.components.ScreenScaffold
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

/**
 * Pick every category, a handful, or exactly one.
 *
 * An empty selection is the canonical way of saying "all of them", so adding a
 * category to the word list later automatically joins the default pool.
 */
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedIds: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    var selection by remember(selectedIds) { mutableStateOf(selectedIds) }
    val allSelected = selection.isEmpty() || selection.size >= categories.size

    /** Collapses a full selection back to the "all categories" sentinel. */
    fun normalise(next: Set<String>): Set<String> =
        if (categories.isNotEmpty() && next.size >= categories.size) emptySet() else next

    val subtitle = when {
        allSelected -> stringResource(R.string.category_all_selected)
        selection.size == 1 -> {
            val only = categories.firstOrNull { it.id in selection }
            only?.name ?: stringResource(R.string.category_count_selected, 1)
        }
        else -> stringResource(R.string.category_count_selected, selection.size)
    }

    ScreenScaffold(
        title = stringResource(R.string.setup_categories),
        subtitle = subtitle,
        onBack = onDismiss,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    CategoryCell(
                        emoji = "🎲",
                        name = stringResource(R.string.category_all),
                        detail = stringResource(R.string.category_all_detail),
                        selected = allSelected,
                        wide = true,
                        onClick = { selection = emptySet() },
                    )
                }
                items(categories, key = { it.id }) { category ->
                    CategoryCell(
                        emoji = category.emoji,
                        name = category.name,
                        detail = stringResource(R.string.category_word_count, category.words.size),
                        selected = !allSelected && category.id in selection,
                        wide = false,
                        onClick = {
                            val base = if (allSelected) emptySet() else selection
                            selection = normalise(
                                if (category.id in base) base - category.id else base + category.id
                            )
                        },
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.done),
                    onClick = { onConfirm(normalise(selection)) },
                )
                QuietButton(
                    text = stringResource(R.string.category_select_all),
                    onClick = { selection = emptySet() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CategoryCell(
    emoji: String,
    name: String,
    detail: String?,
    selected: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val border by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            ImpostorTheme.extended.hairline
        },
        animationSpec = tween(180),
        label = "categoryBorder",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (wide) 92.dp else 118.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .clickable(role = Role.Checkbox) {
                feedback.select()
                onClick()
            }
            .padding(16.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 28.sp)
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = ImpostorTheme.extended.muted,
                )
            }
        }
    }
}
