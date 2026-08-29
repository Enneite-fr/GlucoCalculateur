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
import com.example.glucocalculateur.ui.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeDialog(
    availableFood: List<FoodEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Pair<Long, Double>>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selectedComponents = remember { mutableStateListOf<Pair<Long, Double>>() }
    
    var showFoodPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Recette") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la recette") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ingrédients", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showFoodPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter ingrédient")
                    }
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(selectedComponents) { (foodId, weight) ->
                        val food = availableFood.find { it.id == foodId }
                        if (food != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(food.name, modifier = Modifier.weight(1f))
                                Text("${weight}g", modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { selectedComponents.remove(foodId to weight) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedComponents.isNotEmpty()) {
                        onConfirm(name, selectedComponents.toList())
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

    if (showFoodPicker) {
        FoodPickerDetailDialog(
            availableFood = availableFood,
            onDismiss = { showFoodPicker = false },
            onFoodSelected = { food, weight ->
                selectedComponents.add(food.id to weight)
                showFoodPicker = false
            }
        )
    }
}

@Composable
fun FoodPickerDetailDialog(
    availableFood: List<FoodEntity>,
    onDismiss: () -> Unit,
    onFoodSelected: (FoodEntity, Double) -> Unit
) {
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var weight by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un ingrédient") },
        text = {
            Column {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedFood?.name ?: "Choisir un aliment")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableFood.forEach { food ->
                            DropdownMenuItem(
                                text = { Text(food.name) },
                                onClick = {
                                    selectedFood = food
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                    selectedFood?.let { onFoodSelected(it, w) }
                },
                enabled = selectedFood != null && weight.isNotBlank()
            ) {
                Text("Valider")
            }
        }
    )
}
