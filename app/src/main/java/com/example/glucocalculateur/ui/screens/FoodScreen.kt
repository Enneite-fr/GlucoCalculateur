package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.components.FoodSelectionList
import com.example.glucocalculateur.ui.components.SortOption

@Composable
fun FoodScreen(
    foods: List<FoodEntity>,
    onDeleteFood: (FoodEntity) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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

@Composable
private fun FoodScreenContent(
    foods: List<FoodEntity>,
    onDeleteFood: (FoodEntity) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GlucoCalculateurViewModel
) {
    var foodToDelete by remember { mutableStateOf<FoodEntity?>(null) }
    var foodToEdit by remember { mutableStateOf<FoodEntity?>(null) }
    var impact by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }

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
            title = { Text(stringResource(id = R.string.delete_food_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(id = R.string.delete_confirm_msg, foodToDelete?.name ?: ""))
                    impact?.let { (recipesCount, mealsCount) ->
                        if ((recipesCount > 0 || mealsCount > 0)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.cascade_delete_warning, recipesCount, mealsCount),
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
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
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
