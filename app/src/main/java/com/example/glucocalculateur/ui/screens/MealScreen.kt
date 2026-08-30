package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.Formatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(
    meals: List<MealWithItems>,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDeleteMeal: (MealWithItems) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val language by viewModel.language.collectAsState()
    
    // On utilise key pour forcer la recréation du state et du composant lors d'un changement de langue
    key(language) {
        MealScreenContent(
            meals = meals,
            availableFood = availableFood,
            availableRecipes = availableRecipes,
            onDeleteMeal = onDeleteMeal,
            onSettingsClick = onSettingsClick,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MealScreenContent(
    meals: List<MealWithItems>,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDeleteMeal: (MealWithItems) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.example.glucocalculateur.ui.GlucoCalculateurViewModel
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var mealToDelete by remember { mutableStateOf<MealWithItems?>(null) }
    var mealToEdit by remember { mutableStateOf<MealWithItems?>(null) }
    var showFullCalendar by remember { mutableStateOf(false) }

    if (showFullCalendar) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showFullCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis = pickerState.selectedDateMillis
                    showFullCalendar = false
                }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.validate_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullCalendar = false }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (mealToEdit != null) {
        MealDialog(
            mealWithItems = mealToEdit,
            availableFood = availableFood,
            availableRecipes = availableRecipes,
            onDismiss = { mealToEdit = null },
            onConfirm = { name, timestamp, items ->
                mealToEdit?.let { viewModel.updateMeal(it.meal.id, name, timestamp, items) }
                mealToEdit = null
            }
        )
    }

    if (mealToDelete != null) {
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text(stringResource(id = com.example.glucocalculateur.R.string.delete_meal_confirm_title)) },
            text = { Text(stringResource(id = com.example.glucocalculateur.R.string.delete_confirm_msg, mealToDelete?.meal?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        mealToDelete?.let { onDeleteMeal(it) }
                        mealToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.cancel))
                }
            }
        )
    }

    // Groupement des repas par jour
    val groupedMeals = remember(meals) {
        meals.groupBy { meal ->
            Calendar.getInstance().apply {
                timeInMillis = meal.meal.dateTimestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedMap(reverseOrder())
    }

    // Synchronisation : défilement vers la date sélectionnée
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { selectedMillis ->
            val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedMillis
            }
            
            // On cherche la clé correspondante (en local)
            val targetDateMillis = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Trouver l'index dans la LazyColumn
            var index = 0
            for (date in groupedMeals.keys) {
                if (date == targetDateMillis) {
                    scope.launch {
                        listState.animateScrollToItem(index)
                    }
                    break
                }
                index += 1 + (groupedMeals[date]?.size ?: 0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de contrôle supérieure (Paramètres)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .height(72.dp), // Hauteur cohérente avec FilterBar
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = com.example.glucocalculateur.R.string.tab_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(id = com.example.glucocalculateur.R.string.my_meals_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Calendrier compact et flottant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalCalendar(
                        selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                        onDateSelected = { datePickerState.selectedDateMillis = it },
                        onOpenFullCalendar = { showFullCalendar = true }
                    )
                }

                // Moitié inférieure : Liste des repas
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedMeals.forEach { (dateMillis, mealsForDate) ->
                        stickyHeader {
                            val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
                            val dateLabel = dateFormat.format(Date(dateMillis)).replaceFirstChar { it.uppercase() }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(mealsForDate) { meal ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                MealItem(
                                    mealWithItems = meal,
                                    availableFood = availableFood,
                                    availableRecipes = availableRecipes,
                                    onEdit = { mealToEdit = meal },
                                    onDelete = { mealToDelete = meal }
                                )
                            }
                        }
                    }
                    
                    if (meals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = com.example.glucocalculateur.R.string.no_meals_saved),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalCalendar(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onOpenFullCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }
    
    val dateList = remember(selectedDateMillis) {
        (-3..3).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }.timeInMillis
        }
    }

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(onClick = onOpenFullCalendar) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dateList.forEach { dateMillis ->
                    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    val isSelected = isSameDay(dateMillis, selectedDateMillis)
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(dateCal.time)
                        .take(1).uppercase()
                    val dayNumber = dateCal.get(Calendar.DAY_OF_MONTH).toString()
                    
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onDateSelected(dateMillis) }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dayNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(m1: Long, m2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealItem(
    mealWithItems: MealWithItems,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val totalCarbs = mealWithItems.items.sumOf { item ->
        if (item.foodId != null) {
            val food = availableFood.find { it.id == item.foodId }
            if (food != null) (food.carbsPer100g / 100.0) * item.weightGrams else 0.0
        } else if (item.recipeId != null) {
            val recipe = availableRecipes.find { it.recipe.id == item.recipeId }
            if (recipe != null) {
                val rTotalWeight = recipe.components.sumOf { it.weightGrams }
                val rTotalCarbs = recipe.components.sumOf { comp ->
                    val food = availableFood.find { it.id == comp.foodId }
                    if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
                }
                val rCarbsPer100g = if (rTotalWeight > 0) (rTotalCarbs / rTotalWeight) * 100.0 else 0.0
                (rCarbsPer100g / 100.0) * item.weightGrams
            } else 0.0
        } else 0.0
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(mealWithItems.meal.dateTimestamp))

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
                onClick = { expanded = !expanded },
                onLongClick = onEdit
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
                    Text(text = mealWithItems.meal.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = stringResource(id = com.example.glucocalculateur.R.string.total_carbs_prefix, Formatter.formatDouble(totalCarbs)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = com.example.glucocalculateur.R.string.delete)
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(id = com.example.glucocalculateur.R.string.meal_details_title), style = MaterialTheme.typography.titleSmall)
                mealWithItems.items.forEach { item ->
                    val label = if (item.foodId != null) {
                        availableFood.find { it.id == item.foodId }?.name ?: stringResource(id = com.example.glucocalculateur.R.string.unknown_food)
                    } else {
                        availableRecipes.find { it.recipe.id == item.recipeId }?.recipe?.name ?: stringResource(id = com.example.glucocalculateur.R.string.unknown_recipe)
                    }
                    
                    val itemCarbs = if (item.foodId != null) {
                        val food = availableFood.find { it.id == item.foodId }
                        if (food != null) (food.carbsPer100g / 100.0) * item.weightGrams else 0.0
                    } else if (item.recipeId != null) {
                        val recipe = availableRecipes.find { it.recipe.id == item.recipeId }
                        if (recipe != null) {
                            val rTotalWeight = recipe.components.sumOf { it.weightGrams }
                            val rTotalCarbs = recipe.components.sumOf { comp ->
                                val food = availableFood.find { it.id == comp.foodId }
                                if (food != null) (food.carbsPer100g / 100.0) * comp.weightGrams else 0.0
                            }
                            val rCarbsPer100g = if (rTotalWeight > 0) (rTotalCarbs / rTotalWeight) * 100.0 else 0.0
                            (rCarbsPer100g / 100.0) * item.weightGrams
                        } else 0.0
                    } else 0.0

                    Text(
                        text = "• $label : ${Formatter.formatDouble(item.weightGrams)}g (${Formatter.formatDouble(itemCarbs)} ${stringResource(id = com.example.glucocalculateur.R.string.carbs_unit)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
