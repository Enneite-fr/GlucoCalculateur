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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.components.RecipeSelectionList
import com.example.glucocalculateur.ui.components.SortOption

@Composable
fun RecipeScreen(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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

@Composable
private fun RecipeScreenContent(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GlucoCalculateurViewModel
) {
    var recipeToDelete by remember { mutableStateOf<RecipeWithComponents?>(null) }
    var recipeToEdit by remember { mutableStateOf<RecipeWithComponents?>(null) }
    var mealsCount by remember { mutableIntStateOf(0) }
    
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }

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
            title = { Text(stringResource(id = R.string.delete_recipe_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(id = R.string.delete_confirm_msg, recipeToDelete?.recipe?.name ?: ""))
                    if (mealsCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.cascade_delete_recipe_warning, mealsCount),
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
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
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
