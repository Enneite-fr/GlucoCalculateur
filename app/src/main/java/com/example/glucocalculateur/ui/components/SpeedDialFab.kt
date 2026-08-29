package com.example.glucocalculateur.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

enum class SpeedDialAction {
    MEAL, RECIPE, FOOD
}

@Composable
fun SpeedDialFab(
    onActionClick: (SpeedDialAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { 
                        expanded = false
                        onActionClick(SpeedDialAction.FOOD) 
                    }
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.tab_food), modifier = Modifier.padding(horizontal = 8.dp))
                }
                SmallFloatingActionButton(
                    onClick = { 
                        expanded = false
                        onActionClick(SpeedDialAction.RECIPE) 
                    }
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.tab_recipes), modifier = Modifier.padding(horizontal = 8.dp))
                }
                SmallFloatingActionButton(
                    onClick = { 
                        expanded = false
                        onActionClick(SpeedDialAction.MEAL) 
                    }
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.tab_meals), modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Menu else Icons.Default.Add,
                contentDescription = stringResource(id = com.example.glucocalculateur.R.string.add)
            )
        }
    }
}
