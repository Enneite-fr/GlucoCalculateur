package com.example.glucocalculateur.ui.screens

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealItemDraft
import com.example.glucocalculateur.data.MealLabels
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeComponentDraft
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.domain.DateUtils
import com.example.glucocalculateur.ui.components.AppDatePickerDialog
import com.example.glucocalculateur.ui.components.AppTimePickerDialog
import com.example.glucocalculateur.ui.components.CarbTotalBar
import com.example.glucocalculateur.ui.components.CompositionRow
import com.example.glucocalculateur.ui.components.DiscardChangesDialog
import com.example.glucocalculateur.ui.components.ItemPicker
import com.example.glucocalculateur.ui.components.SectionHeader
import com.example.glucocalculateur.ui.components.WeightInputDialog
import com.example.glucocalculateur.ui.components.lineCarbs
import com.example.glucocalculateur.ui.defaultMealNameRes
import com.example.glucocalculateur.ui.rememberDateFormats
import com.example.glucocalculateur.ui.theme.Dimens
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalTime

@Parcelize
data class MealDraft(val name: String, val timestamp: Long, val items: List<MealItemDraft>) : Parcelable

@Parcelize
data class RecipeDraft(val name: String, val components: List<RecipeComponentDraft>) : Parcelable

/** Éditeur plein écran d'un repas. Toute la saisie survit à la rotation et au changement de langue. */
@Composable
fun MealEditorScreen(
    meal: MealWithItems?,
    initialDate: LocalDate,
    foods: List<FoodEntity>,
    recipes: List<RecipeWithComponents>,
    foodMap: Map<Long, FoodEntity>,
    recipeMap: Map<Long, RecipeWithComponents>,
    onSave: (name: String, timestamp: Long, items: List<MealItemDraft>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val formats = rememberDateFormats()
    val initialDraft = rememberSaveable {
        meal?.let { MealDraft(it.meal.name, it.meal.dateTimestamp, it.items.map(MealItemDraft::from)) }
            ?: LocalTime.now().withSecond(0).withNano(0).let { now ->
                MealDraft(
                    name = context.getString(defaultMealNameRes(now.hour)),
                    timestamp = DateUtils.at(initialDate, now),
                    items = emptyList()
                )
            }
    }
    var draft by rememberSaveable { mutableStateOf(initialDraft) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val unknown = stringResource(R.string.unknown_food)
    val lines = remember(draft.items, foodMap, recipeMap) {
        draft.items.map { item ->
            Line(
                label = MealLabels.of(item, foodMap, recipeMap),
                weight = item.weightGrams,
                carbsPer100g = CarbCalculator.draftCarbsPer100g(item, foodMap, recipeMap)
            )
        }
    }
    val error = when {
        draft.name.isBlank() -> stringResource(R.string.error_name_required)
        draft.items.isEmpty() -> stringResource(R.string.error_items_required)
        else -> null
    }

    EditorScaffold(
        title = stringResource(if (meal == null) R.string.new_meal_title else R.string.edit_meal_title),
        isDirty = draft != initialDraft,
        canSave = error == null,
        onSave = { onSave(draft.name.trim(), draft.timestamp, draft.items) },
        onClose = onClose,
        totalCarbs = lines.sumOf { lineCarbs(it.carbsPer100g, it.weight) ?: 0.0 },
        totalWeight = lines.sumOf { it.weight }
    ) {
        item {
            NameField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = stringResource(R.string.meal_name_label),
                showError = draft.name.isBlank()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(Dimens.spaceS))
                    Text(formats.mediumDate(DateUtils.localDate(draft.timestamp)))
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Spacer(Modifier.width(Dimens.spaceS))
                    Text(formats.time(DateUtils.localTime(draft.timestamp)))
                }
            }
        }
        compositionSection(
            title = R.string.composition_section,
            lines = lines,
            unknownLabel = unknown,
            error = error.takeIf { draft.items.isEmpty() },
            onAdd = { showPicker = true },
            onEdit = { editingIndex = it },
            onRemove = { index -> draft = draft.copy(items = draft.items.filterIndexed { i, _ -> i != index }) }
        )
    }

    if (showPicker) {
        ItemPicker(
            title = stringResource(R.string.add_to_meal_title),
            foods = foods,
            recipes = recipes,
            foodMap = foodMap,
            onAdd = { draft = draft.copy(items = draft.items + it) },
            onDone = { showPicker = false }
        )
    }
    editingIndex?.takeIf { it in draft.items.indices }?.let { index ->
        WeightInputDialog(
            title = lines[index].label ?: unknown,
            initialWeight = draft.items[index].weightGrams,
            carbsPer100g = lines[index].carbsPer100g,
            onDismiss = { editingIndex = null },
            onConfirm = { weight ->
                draft = draft.copy(items = draft.items.mapIndexed { i, item -> if (i == index) item.withWeight(weight) else item })
                editingIndex = null
            }
        )
    }
    if (showDate) {
        AppDatePickerDialog(
            initialDate = DateUtils.localDate(draft.timestamp),
            onConfirm = { date ->
                draft = draft.copy(timestamp = DateUtils.withDate(draft.timestamp, date))
                showDate = false
            },
            onDismiss = { showDate = false }
        )
    }
    if (showTime) {
        AppTimePickerDialog(
            initialTime = DateUtils.localTime(draft.timestamp),
            onConfirm = { time ->
                draft = draft.copy(timestamp = DateUtils.withTime(draft.timestamp, time.hour, time.minute))
                showTime = false
            },
            onDismiss = { showTime = false }
        )
    }
}

/** Éditeur plein écran d'une recette. */
@Composable
fun RecipeEditorScreen(
    recipe: RecipeWithComponents?,
    foods: List<FoodEntity>,
    foodMap: Map<Long, FoodEntity>,
    onSave: (name: String, components: List<RecipeComponentDraft>) -> Unit,
    onClose: () -> Unit
) {
    val initialDraft = rememberSaveable {
        RecipeDraft(
            name = recipe?.recipe?.name.orEmpty(),
            components = recipe?.components.orEmpty().map { RecipeComponentDraft(it.foodId, it.weightGrams) }
        )
    }
    var draft by rememberSaveable { mutableStateOf(initialDraft) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editingIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var nameTouched by rememberSaveable { mutableStateOf(recipe != null) }

    val unknown = stringResource(R.string.unknown_food)
    val lines = remember(draft.components, foodMap) {
        draft.components.map { comp ->
            val food = foodMap[comp.foodId]
            Line(label = food?.name, weight = comp.weightGrams, carbsPer100g = food?.carbsPer100g)
        }
    }
    val error = when {
        draft.name.isBlank() -> stringResource(R.string.error_name_required)
        draft.components.isEmpty() -> stringResource(R.string.error_ingredients_required)
        else -> null
    }

    EditorScaffold(
        title = stringResource(if (recipe == null) R.string.new_recipe_title else R.string.edit_recipe_title),
        isDirty = draft != initialDraft,
        canSave = error == null,
        onSave = { onSave(draft.name.trim(), draft.components) },
        onClose = onClose,
        totalCarbs = lines.sumOf { lineCarbs(it.carbsPer100g, it.weight) ?: 0.0 },
        totalWeight = lines.sumOf { it.weight }
    ) {
        item {
            NameField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it); nameTouched = true },
                label = stringResource(R.string.recipe_name_label),
                showError = nameTouched && draft.name.isBlank()
            )
        }
        compositionSection(
            title = R.string.ingredients_title,
            lines = lines,
            unknownLabel = unknown,
            error = error.takeIf { draft.components.isEmpty() },
            onAdd = { showPicker = true },
            onEdit = { editingIndex = it },
            onRemove = { index ->
                draft = draft.copy(components = draft.components.filterIndexed { i, _ -> i != index })
            }
        )
    }

    if (showPicker) {
        ItemPicker(
            title = stringResource(R.string.add_ingredient_dialog_title),
            foods = foods,
            recipes = null,
            foodMap = foodMap,
            onAdd = { item ->
                if (item is MealItemDraft.Food) {
                    draft = draft.copy(components = draft.components + RecipeComponentDraft(item.foodId, item.weightGrams))
                }
            },
            onDone = { showPicker = false }
        )
    }
    editingIndex?.takeIf { it in draft.components.indices }?.let { index ->
        WeightInputDialog(
            title = lines[index].label ?: unknown,
            initialWeight = draft.components[index].weightGrams,
            carbsPer100g = lines[index].carbsPer100g,
            onDismiss = { editingIndex = null },
            onConfirm = { weight ->
                draft = draft.copy(components = draft.components.mapIndexed { i, c -> if (i == index) c.copy(weightGrams = weight) else c })
                editingIndex = null
            }
        )
    }
}

private data class Line(val label: String?, val weight: Double, val carbsPer100g: Double?)

/** Cadre commun des éditeurs : barre d'actions, total toujours visible, retour protégé. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffold(
    title: String,
    isDirty: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    totalCarbs: Double,
    totalWeight: Double,
    content: LazyListScope.() -> Unit
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val requestClose = { if (isDirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = requestClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save_btn)) }
                }
            )
        },
        bottomBar = { CarbTotalBar(totalCarbs = totalCarbs, totalWeight = totalWeight) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)
        ) {
            content()
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = { confirmDiscard = false; onClose() },
            onKeepEditing = { confirmDiscard = false }
        )
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, label: String, showError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = showError,
        supportingText = if (showError) {
            { Text(stringResource(R.string.error_name_required)) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun LazyListScope.compositionSection(
    title: Int,
    lines: List<Line>,
    unknownLabel: String,
    error: String?,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    item {
        SectionHeader(
            title = stringResource(title),
            modifier = Modifier.padding(top = Dimens.spaceS),
            trailing = {
                FilledTonalButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(Dimens.spaceXs))
                    Text(stringResource(R.string.add))
                }
            }
        )
    }
    if (lines.isEmpty()) {
        item {
            Text(
                text = error ?: stringResource(R.string.composition_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Dimens.spaceL)
            )
        }
    }
    itemsIndexed(lines) { index, line ->
        val label = line.label ?: unknownLabel
        CompositionRow(
            label = label,
            weightGrams = line.weight,
            carbs = lineCarbs(line.carbsPer100g, line.weight),
            onClick = { onEdit(index) },
            onRemove = { onRemove(index) }
        )
    }
    item { Spacer(Modifier.padding(bottom = Dimens.spaceS)) }
}

