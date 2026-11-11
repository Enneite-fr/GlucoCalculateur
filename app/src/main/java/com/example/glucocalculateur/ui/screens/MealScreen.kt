package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealLabels
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.domain.DateUtils
import com.example.glucocalculateur.ui.DateFormats
import com.example.glucocalculateur.ui.components.AppDatePickerDialog
import com.example.glucocalculateur.ui.components.AppTopBar
import com.example.glucocalculateur.ui.components.CompositionRow
import com.example.glucocalculateur.ui.components.EmptyState
import com.example.glucocalculateur.ui.components.EntityListItem
import com.example.glucocalculateur.ui.formatCarbs
import com.example.glucocalculateur.ui.formatGrams
import com.example.glucocalculateur.ui.rememberDateFormats
import com.example.glucocalculateur.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

/** Repas d'un jour, avec leur total de glucides. */
private data class DayGroup(val date: LocalDate, val meals: List<MealWithItems>, val totalCarbs: Double)

@Composable
fun MealScreen(
    meals: List<MealWithItems>,
    foodMap: Map<Long, FoodEntity>,
    recipeMap: Map<Long, RecipeWithComponents>,
    selectedDay: LocalDate,
    onSelectedDayChange: (LocalDate) -> Unit,
    onAddMeal: (LocalDate) -> Unit,
    onEditMeal: (MealWithItems) -> Unit,
    onDeleteMeal: (MealWithItems) -> Unit,
    onSettingsClick: () -> Unit,
    bottomPadding: Dp
) {
    val formats = rememberDateFormats()
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var expandedMealId by rememberSaveable { mutableStateOf<Long?>(null) }

    val mealCarbs = remember(meals, foodMap, recipeMap) {
        meals.associate { it.meal.id to CarbCalculator.mealTotalCarbs(it.items, foodMap, recipeMap) }
    }
    val dayGroups = remember(meals, mealCarbs) {
        meals.groupBy { DateUtils.localDate(it.meal.dateTimestamp) }
            .map { (date, list) -> DayGroup(date, list, list.sumOf { mealCarbs[it.meal.id] ?: 0.0 }) }
    }
    // Le jour sélectionné figure toujours dans la liste, éventuellement vide.
    val groups = remember(dayGroups, selectedDay) {
        val withSelected = if (dayGroups.any { it.date == selectedDay }) dayGroups
        else dayGroups + DayGroup(selectedDay, emptyList(), 0.0)
        withSelected.sortedByDescending { it.date }
    }
    // Position de l'en-tête de chaque jour, et jour de chaque élément de la liste.
    val headerIndex = remember(groups) {
        var index = 0
        groups.associate { group -> (group.date to index).also { index += 1 + maxOf(group.meals.size, 1) } }
    }
    val dayAtIndex = remember(groups) {
        groups.flatMap { group -> List(1 + maxOf(group.meals.size, 1)) { group.date } }
    }

    val listState = rememberLazyListState()
    var scrollRequest by remember { mutableStateOf<LocalDate?>(null) }
    var autoScrolling by remember { mutableStateOf(false) }
    val currentDayAtIndex by rememberUpdatedState(dayAtIndex)
    val currentOnDayChange by rememberUpdatedState(onSelectedDayChange)

    val selectDay: (LocalDate) -> Unit = { date ->
        onSelectedDayChange(date)
        scrollRequest = date
    }

    // Jour choisi dans le bandeau ou le calendrier -> défilement vers ce jour.
    LaunchedEffect(scrollRequest, headerIndex) {
        val target = scrollRequest ?: return@LaunchedEffect
        val index = headerIndex[target] ?: return@LaunchedEffect
        autoScrolling = true
        try {
            listState.animateScrollToItem(index)
        } finally {
            autoScrolling = false
            scrollRequest = null
        }
    }
    // Défilement par l'utilisateur -> le bandeau suit le jour affiché en haut de la liste.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (!autoScrolling && listState.isScrollInProgress) {
                    currentDayAtIndex.getOrNull(index)?.let(currentOnDayChange)
                }
            }
    }

    if (showCalendar) {
        AppDatePickerDialog(
            initialDate = selectedDay,
            onConfirm = { selectDay(it); showCalendar = false },
            onDismiss = { showCalendar = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.my_meals_title), onSettingsClick = onSettingsClick)
        WeekStrip(
            selectedDay = selectedDay,
            formats = formats,
            onDaySelected = selectDay,
            onOpenCalendar = { showCalendar = true }
        )
        val emptyMessage = stringResource(R.string.no_meals_day)
        val addLabel = stringResource(R.string.add_meal_btn)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Dimens.spaceS, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)
        ) {
            groups.forEach { group ->
                dayHeader(group, formats)
                if (group.meals.isEmpty()) {
                    item(key = "empty-${group.date.toEpochDay()}") {
                        EmptyState(
                            message = emptyMessage,
                            icon = Icons.Default.Restaurant,
                            actionLabel = addLabel,
                            onAction = { onAddMeal(group.date) }
                        )
                    }
                }
                items(group.meals, key = { it.meal.id }) { meal ->
                    MealCard(
                        meal = meal,
                        totalCarbs = mealCarbs[meal.meal.id] ?: 0.0,
                        foodMap = foodMap,
                        recipeMap = recipeMap,
                        formats = formats,
                        expanded = expandedMealId == meal.meal.id,
                        onToggle = { expandedMealId = if (expandedMealId == meal.meal.id) null else meal.meal.id },
                        onEdit = { onEditMeal(meal) },
                        onDelete = { onDeleteMeal(meal) },
                        modifier = Modifier.padding(horizontal = Dimens.screenPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.dayHeader(group: DayGroup, formats: DateFormats) {
    stickyHeader(key = "day-${group.date.toEpochDay()}") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceS)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formats.fullDate(group.date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (group.meals.isNotEmpty()) {
                Text(
                    text = formatCarbs(group.totalCarbs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: MealWithItems,
    totalCarbs: Double,
    foodMap: Map<Long, FoodEntity>,
    recipeMap: Map<Long, RecipeWithComponents>,
    formats: DateFormats,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val time = formats.time(DateUtils.localTime(meal.meal.dateTimestamp))
    val count = pluralStringResource(R.plurals.meal_items_count, meal.items.size, meal.items.size)
    val unknown = stringResource(R.string.unknown_food)
    EntityListItem(
        title = meal.meal.name,
        subtitle = "$time · $count",
        value = formatGrams(totalCarbs),
        valueLabel = stringResource(R.string.carbs_value_label),
        onClick = onToggle,
        onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.show_details),
        onEdit = onEdit,
        onDelete = onDelete,
        expanded = expanded,
        modifier = modifier
    ) {
        meal.items.forEach { item ->
            CompositionRow(
                label = MealLabels.of(item, foodMap, recipeMap) ?: unknown,
                weightGrams = item.weightGrams,
                carbs = CarbCalculator.mealItemCarbsPer100g(item, foodMap, recipeMap)
                    ?.let { CarbCalculator.componentCarbs(it, item.weightGrams) }
            )
        }
    }
}

/** Bandeau de sept jours centré sur le jour sélectionné. */
@Composable
private fun WeekStrip(
    selectedDay: LocalDate,
    formats: DateFormats,
    onDaySelected: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val days = remember(selectedDay) { (-3L..3L).map { selectedDay.plusDays(it) } }
    val today = LocalDate.now()
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .padding(horizontal = Dimens.screenPadding)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceS)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formats.monthYear(selectedDay),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Dimens.spaceS)
                    )
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.open_calendar),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    days.forEach { day ->
                        val selected = day == selectedDay
                        val description = formats.fullDate(day)
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .selectable(selected = selected, role = Role.Tab) { onDaySelected(day) }
                                .sizeIn(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                .padding(vertical = Dimens.spaceS, horizontal = Dimens.spaceXs)
                                .semantics(mergeDescendants = true) { contentDescription = description },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formats.narrowDayOfWeek(day),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    day == today -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
