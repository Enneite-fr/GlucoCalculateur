package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MealScreen(
    meals: List<MealWithItems>,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDeleteMeal: (MealWithItems) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
        // Moitié supérieure : Calendrier
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            tonalElevation = 2.dp
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        HorizontalDivider()

        // Moitié inférieure : Liste des repas
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp), // Pour ne pas être caché par la bottom bar
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
                        MealItem(meal, availableFood, availableRecipes, onDeleteMeal)
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
                            text = "Aucun repas enregistré",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MealItem(
    mealWithItems: MealWithItems,
    availableFood: List<FoodEntity>,
    availableRecipes: List<RecipeWithComponents>,
    onDeleteMeal: (MealWithItems) -> Unit
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
                    Text(text = mealWithItems.meal.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Total : ${Formatter.formatDouble(totalCarbs)} g glucides",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                TextButton(onClick = { onDeleteMeal(mealWithItems) }) {
                    Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Détail du repas :", style = MaterialTheme.typography.titleSmall)
                mealWithItems.items.forEach { item ->
                    val label = if (item.foodId != null) {
                        availableFood.find { it.id == item.foodId }?.name ?: "Inconnu"
                    } else {
                        availableRecipes.find { it.recipe.id == item.recipeId }?.recipe?.name ?: "Inconnu"
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
                        text = "• $label : ${item.weightGrams}g (${Formatter.formatDouble(itemCarbs)} g glucides)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
