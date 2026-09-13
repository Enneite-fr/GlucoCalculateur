package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.components.FoodSelectionList
import com.example.glucocalculateur.ui.components.RecipeSelectionList
import com.example.glucocalculateur.ui.components.SortOption
import com.example.glucocalculateur.ui.components.WeightInputDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDialog(
    mealWithItems: MealWithItems? = null,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, List<Triple<Long?, Long?, Double>>) -> Unit
) {
    val initialCalendar = remember { 
        Calendar.getInstance().apply {
            mealWithItems?.meal?.dateTimestamp?.let { timeInMillis = it }
        }
    }
    
    val defaultName = if (mealWithItems == null) {
        val hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
        when {
            hour in 6..9 -> stringResource(id = R.string.breakfast)
            hour in 10..13 -> stringResource(id = R.string.lunch)
            hour in 18..21 -> stringResource(id = R.string.dinner)
            else -> stringResource(id = R.string.snack)
        }
    } else {
        mealWithItems.meal.name
    }
    
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var selectedTimestamp by rememberSaveable { mutableLongStateOf(initialCalendar.timeInMillis) }
    
    val selectedItems = remember { 
        mutableStateListOf<Triple<Long?, Long?, Double>>().apply {
            mealWithItems?.items?.forEach { item ->
                add(Triple(item.foodId, item.recipeId, item.weightGrams))
            }
        }
    }
    
    var showItemPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var itemToEditIndex by remember { mutableStateOf<Int?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateCal = Calendar.getInstance().apply { 
                        timeInMillis = datePickerState.selectedDateMillis ?: selectedTimestamp 
                    }
                    val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                    currentCal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                    currentCal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                    currentCal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                    selectedTimestamp = currentCal.timeInMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(id = R.string.validate_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = currentCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentCal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    selectedTimestamp = cal.timeInMillis
                    showTimePicker = false
                }) {
                    Text(stringResource(id = R.string.validate_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mealWithItems == null) stringResource(id = R.string.new_meal_title) else stringResource(id = R.string.edit_meal_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.meal_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedTimestamp))
                        Text(dateStr)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(selectedTimestamp))
                        Text(timeStr)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.composition_title), style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { showItemPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_item_desc))
                    }
                }
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(selectedItems) { index, item ->
                        val (foodId, recipeId, weight) = item
                        val label = if (foodId != null) {
                            availableFood.find { it.id == foodId }?.name ?: stringResource(id = R.string.unknown_food)
                        } else {
                            availableRecipes.find { it.recipe.id == recipeId }?.recipe?.name ?: stringResource(id = R.string.unknown_recipe)
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { itemToEditIndex = index }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            Text("${Formatter.formatDouble(weight)}g", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { selectedItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
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
                        onConfirm(name, selectedTimestamp, selectedItems.toList())
                    }
                }
            ) {
                Text(if (mealWithItems == null) stringResource(R.string.add) else stringResource(R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (itemToEditIndex != null && itemToEditIndex!! in selectedItems.indices) {
        val index = itemToEditIndex!!
        val (foodId, recipeId, currentWeight) = selectedItems[index]
        val label = if (foodId != null) {
            availableFood.find { it.id == foodId }?.name ?: stringResource(id = R.string.unknown_food)
        } else {
            availableRecipes.find { it.recipe.id == recipeId }?.recipe?.name ?: stringResource(id = R.string.unknown_recipe)
        }

        WeightInputDialog(
            title = label,
            initialWeight = currentWeight,
            onDismiss = { itemToEditIndex = null },
            onConfirm = { newWeight ->
                selectedItems[index] = Triple(foodId, recipeId, newWeight)
                itemToEditIndex = null
            }
        )
    }

    if (showItemPicker) {
        ItemPickerDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPickerDialog(
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDismiss: () -> Unit,
    onItemSelected: (Long?, Long?, Double) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_food), stringResource(R.string.tab_recipes))

    var foodSearchQuery by remember { mutableStateOf("") }
    var foodSort by remember { mutableStateOf(SortOption.NAME_ASC) }
    
    var recipeSearchQuery by remember { mutableStateOf("") }
    var recipeSort by remember { mutableStateOf(SortOption.NAME_ASC) }

    var foodToWeight by remember { mutableStateOf<FoodEntity?>(null) }
    var recipeToWeight by remember { mutableStateOf<RecipeWithComponents?>(null) }

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
                    title = { Text(stringResource(id = R.string.add_to_meal_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.cancel))
                        }
                    }
                )
                
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> FoodSelectionList(
                        foods = availableFood,
                        onFoodClick = { foodToWeight = it },
                        searchQuery = foodSearchQuery,
                        onSearchQueryChanged = { foodSearchQuery = it },
                        selectedSort = foodSort,
                        onSortSelected = { foodSort = it },
                        showSettings = false
                    )
                    1 -> RecipeSelectionList(
                        recipes = availableRecipes,
                        availableFood = availableFood,
                        onRecipeClick = { recipeToWeight = it },
                        searchQuery = recipeSearchQuery,
                        onSearchQueryChanged = { recipeSearchQuery = it },
                        selectedSort = recipeSort,
                        onSortSelected = { recipeSort = it },
                        showSettings = false
                    )
                }
            }
        }
    }

    if (foodToWeight != null) {
        WeightInputDialog(
            title = foodToWeight?.name ?: "",
            onDismiss = { foodToWeight = null },
            onConfirm = { weight ->
                onItemSelected(foodToWeight?.id, null, weight)
                foodToWeight = null
            }
        )
    }

    if (recipeToWeight != null) {
        WeightInputDialog(
            title = recipeToWeight?.recipe?.name ?: "",
            onDismiss = { recipeToWeight = null },
            onConfirm = { weight ->
                onItemSelected(null, recipeToWeight?.recipe?.id, weight)
                recipeToWeight = null
            }
        )
    }
}
