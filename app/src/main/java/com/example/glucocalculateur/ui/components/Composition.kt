package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.ui.formatCarbsPer100g
import com.example.glucocalculateur.ui.formatGrams
import com.example.glucocalculateur.ui.theme.Dimens

/**
 * Ligne de composition : libellé, poids, glucides et suppression.
 * Le clic (si fourni) permet de modifier le poids.
 */
@Composable
fun CompositionRow(
    label: String,
    weightGrams: Double,
    carbs: Double?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClickLabel = stringResource(R.string.edit_weight), role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(
                start = if (onClick != null) Dimens.spaceS else 0.dp,
                top = Dimens.spaceXs,
                bottom = Dimens.spaceXs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                formatGrams(weightGrams),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = carbs?.let { formatGrams(it) } ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.spaceS)
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.remove_item, label))
            }
        } else {
            Spacer(Modifier.width(Dimens.spaceS))
        }
    }
}

/** Glucides d'une ligne de composition, ou `null` si sa source est introuvable. */
fun lineCarbs(carbsPer100g: Double?, weightGrams: Double): Double? =
    carbsPer100g?.let { CarbCalculator.componentCarbs(it, weightGrams) }

/**
 * Bandeau de total, toujours visible en bas des éditeurs : glucides, poids, glucides / 100 g.
 * Remonte au-dessus du clavier.
 */
@Composable
fun CarbTotalBar(totalCarbs: Double, totalWeight: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.total_carbs_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    formatGrams(totalCarbs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.total_weight_prefix, formatGrams(totalWeight)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    formatCarbsPer100g(CarbCalculator.carbsPer100g(totalCarbs, totalWeight)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
