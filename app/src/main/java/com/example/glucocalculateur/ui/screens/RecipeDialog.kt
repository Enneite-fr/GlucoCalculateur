package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.components.FoodSelectionList
import com.example.glucocalculateur.ui.components.SortOption
import com.example.glucocalculateur.ui.components.WeightInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDialog(
    recipe: RecipeWithComponents? = null,
    availableFood: List<FoodEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Pair<Long, Double>>) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(recipe?.recipe?.name ?: "") }
    val selectedComponents = remember { 
        mutableStateListOf<Pair<Long, Double>>().apply {
            recipe?.components?.forEach { comp ->
                add(comp.foodId to comp.weightGrams)
            }
        }
    }
    
    var showFoodPicker by rememberSaveable { mutableStateOf(false) }
    var componentToEditIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recipe == null) stringResource(id = R.string.new_recipe_title) else stringResource(id = R.string.edit_recipe_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.recipe_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.ingredients_title), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showFoodPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_ingredient_desc))
                    }
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    itemsIndexed(selectedComponents) { index, (foodId, weight) ->
                        val food = availableFood.find { it.id == foodId }
                        if (food != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { componentToEditIndex = index }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(food.name, modifier = Modifier.weight(1f))
                                Text("${Formatter.formatDouble(weight)}g", modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { selectedComponents.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
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
                Text(if (recipe == null) stringResource(R.string.add) else stringResource(R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (componentToEditIndex != null && componentToEditIndex!! in selectedComponents.indices) {
        val index = componentToEditIndex!!
        val (foodId, currentWeight) = selectedComponents[index]
        val foodName = availableFood.find { it.id == foodId }?.name ?: stringResource(id = R.string.unknown_food)

        WeightInputDialog(
            title = foodName,
            initialWeight = currentWeight,
            onDismiss = { componentToEditIndex = null },
            onConfirm = { newWeight ->
                selectedComponents[index] = foodId to newWeight
                componentToEditIndex = null
            }
        )
    }

    if (showFoodPicker) {
        FoodPickerDialog(
            availableFood = availableFood,
            onDismiss = { showFoodPicker = false },
            onFoodSelected = { food, weight ->
                selectedComponents.add(food.id to weight)
                showFoodPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerDialog(
    availableFood: List<FoodEntity>,
    onDismiss: () -> Unit,
    onFoodSelected: (FoodEntity, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }
    var foodToWeight by remember { mutableStateOf<FoodEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.add_ingredient_dialog_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.cancel))
                        }
                    }
                )
                
                FoodSelectionList(
                    foods = availableFood,
                    onFoodClick = { foodToWeight = it },
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    selectedSort = currentSort,
                    onSortSelected = { currentSort = it },
                    showSettings = false
                )
            }
        }
    }

    if (foodToWeight != null) {
        WeightInputDialog(
            title = foodToWeight?.name ?: "",
            onDismiss = { foodToWeight = null },
            onConfirm = { weight ->
                foodToWeight?.let { onFoodSelected(it, weight) }
                foodToWeight = null
            }
        )
    }
}
