package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter

@Composable
fun FoodSelectionList(
    foods: List<FoodEntity>,
    onFoodClick: (FoodEntity) -> Unit,
    onFoodLongClick: ((FoodEntity) -> Unit)? = null,
    onDeleteFood: ((FoodEntity) -> Unit)? = null,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    showSettings: Boolean = true,
    onSettingsClick: () -> Unit = {}
) {
    val filteredAndSortedFoods = remember(foods, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) {
            foods
        } else {
            foods.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        when (selectedSort) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOption.CARBS_ASC -> filtered.sortedBy { it.carbsPer100g }
            SortOption.CARBS_DESC -> filtered.sortedByDescending { it.carbsPer100g }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FilterBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
            onSettingsClick = onSettingsClick,
            showSettings = showSettings
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAndSortedFoods) { food ->
                FoodSelectionItem(
                    food = food,
                    onClick = { onFoodClick(food) },
                    onLongClick = onFoodLongClick?.let { { it(food) } },
                    onDelete = onDeleteFood?.let { { it(food) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoodSelectionItem(
    food: FoodEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(id = R.string.carbs_per_100g_format, Formatter.formatDouble(food.carbsPer100g)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeSelectionList(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onRecipeClick: (RecipeWithComponents) -> Unit,
    onRecipeLongClick: ((RecipeWithComponents) -> Unit)? = null,
    onDeleteRecipe: ((RecipeWithComponents) -> Unit)? = null,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    showSettings: Boolean = true,
    onSettingsClick: () -> Unit = {}
) {
    val filteredAndSortedRecipes = remember(recipes, availableFood, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) {
            recipes
        } else {
            recipes.filter { it.recipe.name.contains(searchQuery, ignoreCase = true) }
        }

        val recipesWithCarbs = filtered.map { recipeWithComponents ->
            val totalWeight = recipeWithComponents.components.sumOf { it.weightGrams }
            val totalCarbs = recipeWithComponents.components.sumOf { comp ->
                val food = availableFood.find { it.id == comp.foodId }
                if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
            }
            val carbsPer100g = if (totalWeight > 0) (totalCarbs / totalWeight) * 100.0 else 0.0
            recipeWithComponents to carbsPer100g
        }

        when (selectedSort) {
            SortOption.NAME_ASC -> recipesWithCarbs.sortedBy { it.first.recipe.name.lowercase() }
            SortOption.NAME_DESC -> recipesWithCarbs.sortedByDescending { it.first.recipe.name.lowercase() }
            SortOption.CARBS_ASC -> recipesWithCarbs.sortedBy { it.second }
            SortOption.CARBS_DESC -> recipesWithCarbs.sortedByDescending { it.second }
        }.map { it.first }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FilterBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
            onSettingsClick = onSettingsClick,
            showSettings = showSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAndSortedRecipes) { recipe ->
                RecipeSelectionItem(
                    recipeWithComponents = recipe,
                    availableFood = availableFood,
                    onClick = { onRecipeClick(recipe) },
                    onLongClick = onRecipeLongClick?.let { { it(recipe) } },
                    onDelete = onDeleteRecipe?.let { { it(recipe) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeSelectionItem(
    recipeWithComponents: RecipeWithComponents,
    availableFood: List<FoodEntity>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    val totalWeight = recipeWithComponents.components.sumOf { it.weightGrams }
    val totalCarbs = recipeWithComponents.components.sumOf { comp ->
        val food = availableFood.find { it.id == comp.foodId }
        if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
    }
    val carbsPer100g = if (totalWeight > 0) (totalCarbs / totalWeight) * 100.0 else 0.0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .combinedClickable(
                onClick = { 
                    expanded = !expanded
                    onClick()
                },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recipeWithComponents.recipe.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = R.string.carbs_per_100g_format, Formatter.formatDouble(carbsPer100g)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.delete)
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(id = R.string.composition_title), style = MaterialTheme.typography.titleSmall)
                recipeWithComponents.components.forEach { comp ->
                    val food = availableFood.find { it.id == comp.foodId }
                    val itemCarbs = if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
                    Text(
                        text = "• ${food?.name ?: stringResource(id = R.string.unknown_food)} : ${Formatter.formatDouble(comp.weightGrams)}g (${Formatter.formatDouble(itemCarbs)} ${stringResource(id = R.string.carbs_unit)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = stringResource(id = R.string.total_weight_prefix, Formatter.formatDouble(totalWeight)) + "g",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeightInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { 
                    if (it.all { char -> (char.isDigit() || char == ',' || char == '.') }) {
                        weight = Formatter.validateNumericInput(it)
                    }
                },
                label = { Text(stringResource(id = R.string.weight_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = Formatter.parseDouble(weight) ?: 0.0
                    if (w > 0) {
                        onConfirm(w)
                    }
                },
                enabled = weight.isNotBlank() && (Formatter.parseDouble(weight) ?: 0.0) > 0
            ) {
                Text(stringResource(id = R.string.validate_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
