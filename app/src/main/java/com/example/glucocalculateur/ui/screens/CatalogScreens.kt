package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.FoodNames
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.data.io.CsvCodec
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.components.AppTopBar
import com.example.glucocalculateur.ui.components.CompositionRow
import com.example.glucocalculateur.ui.components.ConfirmDeleteDialog
import com.example.glucocalculateur.ui.components.DecimalField
import com.example.glucocalculateur.ui.components.DecimalInput
import com.example.glucocalculateur.ui.components.EmptyState
import com.example.glucocalculateur.ui.components.EntityListItem
import com.example.glucocalculateur.ui.components.SearchableList
import com.example.glucocalculateur.ui.components.SortOption
import com.example.glucocalculateur.ui.components.validateDecimal
import com.example.glucocalculateur.ui.currentLocale
import com.example.glucocalculateur.ui.formatCarbsPer100g
import com.example.glucocalculateur.ui.formatGrams
import com.example.glucocalculateur.ui.theme.Dimens

// --- Recettes ---

@Composable
fun RecipeScreen(
    recipes: List<RecipeWithComponents>,
    foodMap: Map<Long, FoodEntity>,
    onCreateRecipe: () -> Unit,
    onEditRecipe: (RecipeWithComponents) -> Unit,
    onDeleteRecipe: (RecipeWithComponents) -> Unit,
    countMealsUsingRecipe: suspend (Long) -> Int,
    onSettingsClick: () -> Unit,
    bottomPadding: Dp
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var mealsUsing by remember { mutableIntStateOf(0) }

    val carbs = remember(recipes, foodMap) {
        recipes.associate { it.recipe.id to CarbCalculator.recipeCarbsPer100g(it.components, foodMap) }
    }

    recipes.firstOrNull { it.recipe.id == toDeleteId }?.let { recipe ->
        LaunchedEffect(recipe.recipe.id) { mealsUsing = countMealsUsingRecipe(recipe.recipe.id) }
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_recipe_confirm_title),
            message = stringResource(R.string.delete_confirm_msg, recipe.recipe.name),
            warning = if (mealsUsing > 0) {
                pluralStringResource(R.plurals.cascade_delete_recipe_warning, mealsUsing, mealsUsing)
            } else null,
            onConfirm = { onDeleteRecipe(recipe); toDeleteId = null },
            onDismiss = { toDeleteId = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.tab_recipes), onSettingsClick = onSettingsClick)
        val unknown = stringResource(R.string.unknown_food)
        SearchableList(
            items = recipes,
            key = { it.recipe.id },
            nameOf = { it.recipe.name },
            carbsOf = { carbs[it.recipe.id] ?: 0.0 },
            query = query,
            onQueryChange = { query = it },
            sort = sort,
            onSortChange = { sort = it },
            contentPadding = listPadding(bottomPadding),
            emptyContent = {
                EmptyState(
                    message = stringResource(R.string.no_recipes),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    actionLabel = stringResource(R.string.create_recipe),
                    onAction = onCreateRecipe
                )
            }
        ) { recipe ->
            val expanded = expandedId == recipe.recipe.id
            val count = recipe.components.size
            EntityListItem(
                title = recipe.recipe.name,
                subtitle = pluralStringResource(R.plurals.ingredients_count, count, count) + " · " +
                    formatGrams(CarbCalculator.recipeTotalWeight(recipe.components)),
                value = formatCarbsPer100g(carbs[recipe.recipe.id] ?: 0.0),
                onClick = { expandedId = if (expanded) null else recipe.recipe.id },
                onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.show_details),
                onEdit = { onEditRecipe(recipe) },
                onDelete = { toDeleteId = recipe.recipe.id },
                expanded = expanded,
                modifier = Modifier.animateItem()
            ) {
                recipe.components.forEach { comp ->
                    val food = foodMap[comp.foodId]
                    CompositionRow(
                        label = food?.name ?: unknown,
                        weightGrams = comp.weightGrams,
                        carbs = food?.let { CarbCalculator.componentCarbs(it.carbsPer100g, comp.weightGrams) }
                    )
                }
            }
        }
    }
}

// --- Aliments ---

@Composable
fun FoodScreen(
    foods: List<FoodEntity>,
    onCreateFood: () -> Unit,
    onEditFood: (FoodEntity) -> Unit,
    onDeleteFood: (FoodEntity) -> Unit,
    countRecipesUsingFood: suspend (Long) -> Int,
    onSettingsClick: () -> Unit,
    bottomPadding: Dp
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var toDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recipesUsing by remember { mutableIntStateOf(0) }

    foods.firstOrNull { it.id == toDeleteId }?.let { food ->
        LaunchedEffect(food.id) { recipesUsing = countRecipesUsingFood(food.id) }
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_food_confirm_title),
            message = stringResource(R.string.delete_confirm_msg, food.name),
            warning = if (recipesUsing > 0) {
                pluralStringResource(R.plurals.cascade_delete_warning, recipesUsing, recipesUsing)
            } else null,
            onConfirm = { onDeleteFood(food); toDeleteId = null },
            onDismiss = { toDeleteId = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.tab_food), onSettingsClick = onSettingsClick)
        val editLabel = stringResource(R.string.edit)
        SearchableList(
            items = foods,
            key = { it.id },
            nameOf = { it.name },
            carbsOf = { it.carbsPer100g },
            query = query,
            onQueryChange = { query = it },
            sort = sort,
            onSortChange = { sort = it },
            contentPadding = listPadding(bottomPadding),
            emptyContent = {
                EmptyState(
                    message = stringResource(R.string.no_foods),
                    icon = Icons.Default.Inventory2,
                    actionLabel = stringResource(R.string.add_food),
                    onAction = onCreateFood
                )
            }
        ) { food ->
            EntityListItem(
                title = food.name,
                value = formatCarbsPer100g(food.carbsPer100g),
                onClick = { onEditFood(food) },
                onClickLabel = editLabel,
                onEdit = { onEditFood(food) },
                onDelete = { toDeleteId = food.id },
                modifier = Modifier.animateItem()
            )
        }
    }
}

private fun listPadding(bottom: Dp) = PaddingValues(
    start = Dimens.screenPadding,
    end = Dimens.screenPadding,
    top = Dimens.spaceS,
    bottom = bottom
)

/**
 * Création / modification d'un aliment. Si le nom existe déjà, propose de mettre à jour
 * l'aliment existant plutôt que de créer un doublon.
 */
@Composable
fun FoodDialog(
    food: FoodEntity?,
    foods: List<FoodEntity>,
    onSave: (id: Long?, name: String, carbsPer100g: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = currentLocale()
    var name by rememberSaveable { mutableStateOf(food?.name.orEmpty()) }
    var carbsText by rememberSaveable {
        mutableStateOf(food?.carbsPer100g?.let { Formatter.formatNumber(it, locale) }.orEmpty())
    }
    var carbsTouched by rememberSaveable { mutableStateOf(food != null) }

    val existing = remember(name, foods) {
        val key = FoodNames.key(name)
        if (key.isEmpty()) null else foods.firstOrNull { it.id != food?.id && FoodNames.key(it.name) == key }
    }
    val carbs = validateDecimal(carbsText, max = CsvCodec.MAX_CARBS_PER_100G, allowZero = true, touched = carbsTouched)
    // Modification : un autre aliment porte déjà ce nom -> bloqué. Création : mise à jour proposée.
    val nameConflict = food != null && existing != null
    val canSave = name.isNotBlank() && carbs is DecimalInput.Valid && !nameConflict
    val save = {
        if (canSave && carbs is DecimalInput.Valid) {
            // Mise à jour d'un aliment existant depuis « Ajouter » : on garde son nom d'origine.
            val target = existing ?: food
            onSave(target?.id, if (food == null && existing != null) existing.name else name, carbs.value)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (food == null) R.string.add_food else R.string.edit_food_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.food_name)) },
                    isError = nameConflict,
                    supportingText = when {
                        nameConflict -> { { Text(stringResource(R.string.food_name_taken)) } }
                        existing != null -> {
                            {
                                Text(
                                    stringResource(R.string.food_exists_update, existing.name, formatCarbsPer100g(existing.carbsPer100g)),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        else -> null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Dimens.spaceS))
                DecimalField(
                    value = carbsText,
                    onValueChange = { carbsText = it; carbsTouched = true },
                    label = stringResource(R.string.carbs_label),
                    suffix = stringResource(R.string.per_100g_suffix),
                    error = (carbs as? DecimalInput.Invalid)?.message,
                    onImeAction = save
                )
            }
        },
        confirmButton = {
            Button(onClick = save, enabled = canSave) {
                Text(
                    stringResource(
                        when {
                            food == null && existing != null -> R.string.update_btn
                            food == null -> R.string.add
                            else -> R.string.save_btn
                        }
                    )
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
