package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.ui.CarbCalculator
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(
    meals: List<MealWithItems>,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDeleteMeal: (MealWithItems) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GlucoCalculateurViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val language by viewModel.language.collectAsState()
    
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
    viewModel: GlucoCalculateurViewModel
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var mealToDelete by remember { mutableStateOf<MealWithItems?>(null) }
    var mealToEdit by remember { mutableStateOf<MealWithItems?>(null) }
    var showFullCalendar by rememberSaveable { mutableStateOf(false) }

    val foodMap = remember(availableFood) { availableFood.associateBy { it.id } }
    val recipeMap = remember(availableRecipes) { availableRecipes.associateBy { it.recipe.id } }

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
                    Text(stringResource(id = R.string.validate_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullCalendar = false }) {
                    Text(stringResource(id = R.string.cancel))
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
            title = { Text(stringResource(id = R.string.delete_meal_confirm_title)) },
            text = { Text(stringResource(id = R.string.delete_confirm_msg, mealToDelete?.meal?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        mealToDelete?.let { onDeleteMeal(it) }
                        mealToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
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
            
            val targetDateMillis = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.tab_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(id = R.string.my_meals_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
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

                        items(mealsForDate, key = { it.meal.id }) { meal ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                MealItem(
                                    mealWithItems = meal,
                                    foodMap = foodMap,
                                    recipeMap = recipeMap,
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
                                    text = stringResource(id = R.string.no_meals_saved),
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
    
    // Stabilise la vue calendrier autour de la date
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
                    val isSelected = isSameDay(dateCal, calendar)
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

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealItem(
    mealWithItems: MealWithItems,
    foodMap: Map<Long, FoodEntity>,
    recipeMap: Map<Long, RecipeWithComponents>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val totalCarbs = remember(mealWithItems.items, foodMap, recipeMap) {
        CarbCalculator.calculateMealTotalCarbs(mealWithItems.items, foodMap, recipeMap)
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
                        text = stringResource(id = R.string.total_carbs_prefix, Formatter.formatDouble(totalCarbs)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(id = R.string.meal_details_title), style = MaterialTheme.typography.titleSmall)
                mealWithItems.items.forEach { item ->
                    val label = if (item.foodId != null) {
                        foodMap[item.foodId]?.name ?: stringResource(id = R.string.unknown_food)
                    } else if (item.recipeId != null) {
                        recipeMap[item.recipeId]?.recipe?.name ?: stringResource(id = R.string.unknown_recipe)
                    } else {
                        stringResource(id = R.string.unknown_food)
                    }
                    
                    val itemCarbs = CarbCalculator.calculateMealItemCarbs(item, foodMap, recipeMap)

                    Text(
                        text = "• $label : ${Formatter.formatDouble(item.weightGrams)}g (${Formatter.formatDouble(itemCarbs)} ${stringResource(id = R.string.carbs_unit)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
