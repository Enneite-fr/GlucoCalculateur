package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.CarbCalculator
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
            items(filteredAndSortedFoods, key = { it.id }) { food ->
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
    val foodMap = remember(availableFood) { availableFood.associateBy { it.id } }

    val filteredAndSortedRecipes = remember(recipes, foodMap, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) {
            recipes
        } else {
            recipes.filter { it.recipe.name.contains(searchQuery, ignoreCase = true) }
        }

        val recipesWithCarbs = filtered.map { recipeWithComponents ->
            val carbsPer100g = CarbCalculator.calculateRecipeCarbsPer100g(recipeWithComponents.components, foodMap)
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
            items(filteredAndSortedRecipes, key = { it.recipe.id }) { recipe ->
                RecipeSelectionItem(
                    recipeWithComponents = recipe,
                    foodMap = foodMap,
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
    foodMap: Map<Long, FoodEntity>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    val totalWeight = remember(recipeWithComponents.components) {
        CarbCalculator.calculateRecipeTotalWeight(recipeWithComponents.components)
    }
    val carbsPer100g = remember(recipeWithComponents.components, foodMap) {
        CarbCalculator.calculateRecipeCarbsPer100g(recipeWithComponents.components, foodMap)
    }

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
                    val food = foodMap[comp.foodId]
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
    var weight by rememberSaveable { mutableStateOf("") }

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
