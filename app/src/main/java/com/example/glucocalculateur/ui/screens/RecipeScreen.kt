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
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.components.RecipeSelectionList
import com.example.glucocalculateur.ui.components.SortOption

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeScreen(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val language by viewModel.language.collectAsState()
    
    key(language) {
        RecipeScreenContent(
            recipes = recipes,
            availableFood = availableFood,
            onDeleteRecipe = onDeleteRecipe,
            onSettingsClick = onSettingsClick,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeScreenContent(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel
) {
    var recipeToDelete by remember { mutableStateOf<RecipeWithComponents?>(null) }
    var recipeToEdit by remember { mutableStateOf<RecipeWithComponents?>(null) }
    var mealsCount by remember { mutableIntStateOf(0) }
    
    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }

    LaunchedEffect(recipeToDelete) {
        recipeToDelete?.let {
            mealsCount = viewModel.getRecipeImpact(it.recipe)
        } ?: run {
            mealsCount = 0
        }
    }

    if (recipeToEdit != null) {
        RecipeDialog(
            recipe = recipeToEdit,
            availableFood = availableFood,
            onDismiss = { recipeToEdit = null },
            onConfirm = { name, components ->
                recipeToEdit?.let { viewModel.updateRecipe(it.recipe.id, name, components) }
                recipeToEdit = null
            }
        )
    }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text(stringResource(id = com.example.glucocalculateur.R.string.delete_recipe_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete_confirm_msg, recipeToDelete?.recipe?.name ?: ""))
                    if (mealsCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = com.example.glucocalculateur.R.string.cascade_delete_recipe_warning, mealsCount),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        recipeToDelete?.let { onDeleteRecipe(it) }
                        recipeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.cancel))
                }
            }
        )
    }

    RecipeSelectionList(
        recipes = recipes,
        availableFood = availableFood,
        onRecipeClick = { }, // On ne fait rien au clic simple
        onRecipeLongClick = { recipeToEdit = it },
        onDeleteRecipe = { recipeToDelete = it },
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        selectedSort = currentSort,
        onSortSelected = { currentSort = it },
        onSettingsClick = onSettingsClick
    )
}
