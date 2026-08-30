package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.components.FoodSelectionList
import com.example.glucocalculateur.ui.components.SortOption

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoodScreen(
    foods: List<FoodEntity>,
    onDeleteFood: (FoodEntity) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val language by viewModel.language.collectAsState()
    
    key(language) {
        FoodScreenContent(
            foods = foods,
            onDeleteFood = onDeleteFood,
            onSettingsClick = onSettingsClick,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodScreenContent(
    foods: List<FoodEntity>,
    onDeleteFood: (FoodEntity) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel
) {
    var foodToDelete by remember { mutableStateOf<FoodEntity?>(null) }
    var foodToEdit by remember { mutableStateOf<FoodEntity?>(null) }
    var impact by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }

    LaunchedEffect(foodToDelete) {
        foodToDelete?.let {
            impact = viewModel.getFoodImpact(it)
        } ?: run {
            impact = null
        }
    }

    if (foodToEdit != null) {
        FoodDialog(
            food = foodToEdit,
            onDismiss = { foodToEdit = null },
            onConfirm = { name, carbs ->
                foodToEdit?.let { viewModel.updateFood(it.id, name, carbs) }
                foodToEdit = null
            }
        )
    }

    if (foodToDelete != null) {
        AlertDialog(
            onDismissRequest = { foodToDelete = null },
            title = { Text(stringResource(id = com.example.glucocalculateur.R.string.delete_food_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete_confirm_msg, foodToDelete?.name ?: ""))
                    impact?.let { (recipesCount, mealsCount) ->
                        if ((recipesCount > 0 || mealsCount > 0)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = com.example.glucocalculateur.R.string.cascade_delete_warning, recipesCount, mealsCount),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        foodToDelete?.let { onDeleteFood(it) }
                        foodToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToDelete = null }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.cancel))
                }
            }
        )
    }

    FoodSelectionList(
        foods = foods,
        onFoodClick = { }, // On ne fait rien au clic simple dans l'écran principal
        onFoodLongClick = { foodToEdit = it },
        onDeleteFood = { foodToDelete = it },
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        selectedSort = currentSort,
        onSortSelected = { currentSort = it },
        onSettingsClick = onSettingsClick
    )
}
