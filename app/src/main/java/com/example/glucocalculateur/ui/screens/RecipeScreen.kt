package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter

@Composable
fun RecipeScreen(
    recipes: List<RecipeWithComponents>,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recipes) { recipe ->
            RecipeItem(recipe, availableFood, onDeleteRecipe)
        }
    }
}

@Composable
fun RecipeItem(
    recipeWithComponents: RecipeWithComponents,
    availableFood: List<FoodEntity>,
    onDeleteRecipe: (RecipeWithComponents) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Calcul des glucides totaux pour 100g de la recette (approximation simple: somme des carbs / poids total * 100)
    val totalWeight = recipeWithComponents.components.sumOf { it.weightGrams }
    val totalCarbs = recipeWithComponents.components.sumOf { comp ->
        val food = availableFood.find { it.id == comp.foodId }
        if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
    }
    val carbsPer100g = if (totalWeight > 0) (totalCarbs / totalWeight) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recipeWithComponents.recipe.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${Formatter.formatDouble(carbsPer100g)} g glucides / 100g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onDeleteRecipe(recipeWithComponents) }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Composition :", style = MaterialTheme.typography.titleSmall)
                recipeWithComponents.components.forEach { comp ->
                    val food = availableFood.find { it.id == comp.foodId }
                    val itemCarbs = if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
                    Text(
                        text = "• ${food?.name ?: "Inconnu"} : ${comp.weightGrams}g (${Formatter.formatDouble(itemCarbs)} g glucides)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Poids total : ${totalWeight}g",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
