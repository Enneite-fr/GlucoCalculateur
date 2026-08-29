package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import java.util.Calendar

@Composable
fun AddMealDialog(
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Triple<Long?, Long?, Double>>) -> Unit
) {
    val defaultName = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 6..9 -> "Petit Déjeuner"
            hour in 10..13 -> "Déjeuner"
            hour in 18..21 -> "Diner"
            else -> "Collation"
        }
    }
    var name by remember { mutableStateOf(defaultName) }
    val selectedItems = remember { mutableStateListOf<Triple<Long?, Long?, Double>>() }
    
    var showItemPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Repas") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du repas") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Composition", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showItemPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter élément")
                    }
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(selectedItems) { item ->
                        val (foodId, recipeId, weight) = item
                        val label = if (foodId != null) {
                            availableFood.find { it.id == foodId }?.name ?: "Aliment inconnu"
                        } else {
                            availableRecipes.find { it.recipe.id == recipeId }?.recipe?.name ?: "Recette inconnue"
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            Text("${weight}g", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { selectedItems.remove(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedItems.isNotEmpty()) {
                        onConfirm(name, selectedItems.toList())
                    }
                }
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showItemPicker) {
        ItemPickerDetailDialog(
            availableFood = availableFood,
            availableRecipes = availableRecipes,
            onDismiss = { showItemPicker = false },
            onItemSelected = { foodId, recipeId, weight ->
                selectedItems.add(Triple(foodId, recipeId, weight))
                showItemPicker = false
            }
        )
    }
}

@Composable
fun ItemPickerDetailDialog(
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDismiss: () -> Unit,
    onItemSelected: (Long?, Long?, Double) -> Unit
) {
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var selectedRecipe by remember { mutableStateOf<RecipeWithComponents?>(null) }
    var weight by remember { mutableStateOf("") }
    var expandedFood by remember { mutableStateOf(false) }
    var expandedRecipe by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter au repas") },
        text = {
            Column {
                Text("Choisir un aliment :", style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(
                        onClick = { 
                            expandedFood = true
                            selectedRecipe = null 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedFood?.name ?: "Choisir un aliment")
                    }
                    DropdownMenu(
                        expanded = expandedFood,
                        onDismissRequest = { expandedFood = false }
                    ) {
                        availableFood.forEach { food ->
                            DropdownMenuItem(
                                text = { Text(food.name) },
                                onClick = {
                                    selectedFood = food
                                    expandedFood = false
                                }
                            )
                        }
                    }
                }
                
                Text("OU choisir une recette :", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(
                        onClick = { 
                            expandedRecipe = true
                            selectedFood = null 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedRecipe?.recipe?.name ?: "Choisir une recette")
                    }
                    DropdownMenu(
                        expanded = expandedRecipe,
                        onDismissRequest = { expandedRecipe = false }
                    ) {
                        availableRecipes.forEach { recipe ->
                            DropdownMenuItem(
                                text = { Text(recipe.recipe.name) },
                                onClick = {
                                    selectedRecipe = recipe
                                    expandedRecipe = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = weight,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() || char == ',' || char == '.' }) {
                            weight = Formatter.validateNumericInput(it)
                        }
                    },
                    label = { Text("Poids (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = Formatter.parseDouble(weight) ?: 0.0
                    onItemSelected(selectedFood?.id, selectedRecipe?.recipe?.id, w)
                },
                enabled = (selectedFood != null || selectedRecipe != null) && weight.isNotBlank()
            ) {
                Text("Valider")
            }
        }
    )
}
