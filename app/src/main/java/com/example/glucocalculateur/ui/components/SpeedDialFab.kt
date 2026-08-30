package com.example.glucocalculateur.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R

enum class SpeedDialAction {
    MEAL, RECIPE, FOOD
}

@Composable
fun SpeedDialFab(
    onActionClick: (SpeedDialAction) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // On utilise un Box sans padding pour l'overlay afin qu'il couvre tout l'écran
    Box(modifier = Modifier.fillMaxSize()) {
        // Overlay pour bloquer les interactions et fermer le menu
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // Pas d'effet visuel au clic sur l'overlay
                        onClick = { expanded = false }
                    )
            )
        }

        // Contenu du FAB et des options, décalé pour être au-dessus de la barre de navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 104.dp, end = 16.dp), // 16 (padding bar) + 72 (height bar) + 16 (gap) = 104dp
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp) // Réduit de 16.dp à 8.dp
                ) {
                    // Ajouter Aliment
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            onActionClick(SpeedDialAction.FOOD) 
                        },
                        icon = { Icon(painterResource(id = R.drawable.ic_carotte), contentDescription = null) },
                        text = { Text(stringResource(id = R.string.tab_food)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    // Ajouter Recette
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            onActionClick(SpeedDialAction.RECIPE) 
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        text = { Text(stringResource(id = R.string.tab_recipes)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    // Ajouter Repas
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            onActionClick(SpeedDialAction.MEAL) 
                        },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        text = { Text(stringResource(id = R.string.tab_meals)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (expanded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
