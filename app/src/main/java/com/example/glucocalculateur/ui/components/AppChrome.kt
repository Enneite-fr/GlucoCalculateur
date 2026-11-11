package com.example.glucocalculateur.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.ui.theme.Dimens

/** Onglets principaux, avec leur action « créer ». */
enum class AppDestinations(val labelRes: Int, val createLabelRes: Int) {
    MEALS(R.string.tab_meals, R.string.new_meal_title),
    RECIPES(R.string.tab_recipes, R.string.new_recipe_title),
    FOOD(R.string.tab_food, R.string.add_food);

    @Composable
    fun icon(): Painter = when (this) {
        MEALS -> rememberVectorPainter(Icons.Default.Restaurant)
        RECIPES -> rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)
        FOOD -> painterResource(R.drawable.ic_carotte)
    }
}

/** En-tête commun des onglets : titre et accès aux paramètres. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onSettingsClick: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/** Barre de navigation flottante en forme de pilule (sans flou : effet sans objet sous Android 12). */
@Composable
fun FloatingBottomBar(
    current: AppDestinations,
    onNavigate: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(Dimens.bottomBarHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceS)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestinations.entries.forEach { destination ->
                val selected = destination == current
                val background by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    label = "tab-background"
                )
                val content by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tab-content"
                )
                Surface(
                    shape = CircleShape,
                    color = background,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(destination) })
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = destination.icon(),
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = content,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Rangée Exporter / Importer des paramètres. */
@Composable
fun ImportExportRow(onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f), contentPadding = PaddingValues(Dimens.spaceM)) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(Dimens.spaceS))
            Text(stringResource(R.string.export_btn))
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f), contentPadding = PaddingValues(Dimens.spaceM)) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(Modifier.width(Dimens.spaceS))
            Text(stringResource(R.string.import_btn))
        }
    }
}
