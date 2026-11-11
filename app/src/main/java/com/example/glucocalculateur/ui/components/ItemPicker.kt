package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.MealItemDraft
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.ui.formatCarbsPer100g
import com.example.glucocalculateur.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Sélecteur plein écran d'aliments (et, si [recipes] est fourni, de recettes).
 * Il reste ouvert après chaque ajout : on compose un repas sans le rouvrir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPicker(
    title: String,
    foods: List<FoodEntity>,
    recipes: List<RecipeWithComponents>?,
    foodMap: Map<Long, FoodEntity>,
    onAdd: (MealItemDraft) -> Unit,
    onDone: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var foodQuery by rememberSaveable { mutableStateOf("") }
    var foodSort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var recipeQuery by rememberSaveable { mutableStateOf("") }
    var recipeSort by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var addedCount by rememberSaveable { mutableIntStateOf(0) }
    var foodToWeigh by remember { mutableStateOf<FoodEntity?>(null) }
    var recipeToWeigh by remember { mutableStateOf<RecipeWithComponents?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val recipeCarbs = remember(recipes, foodMap) {
        recipes.orEmpty().associate { it.recipe.id to CarbCalculator.recipeCarbsPer100g(it.components, foodMap) }
    }

    fun added(draft: MealItemDraft, name: String) {
        onAdd(draft)
        addedCount++
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(resources.getString(R.string.item_added, name))
        }
    }

    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(title)
                            if (addedCount > 0) {
                                Text(
                                    pluralStringResource(R.plurals.items_added, addedCount, addedCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        Button(onClick = onDone, modifier = Modifier.padding(end = Dimens.spaceS)) {
                            Text(stringResource(R.string.picker_done))
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (recipes != null) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_food)) })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_recipes)) })
                    }
                }
                val listPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Dimens.spaceS,
                    bottom = Dimens.spaceXl
                )
                val addLabel = stringResource(R.string.add)
                if (tab == 0 || recipes == null) {
                    SearchableList(
                        items = foods,
                        key = { it.id },
                        nameOf = { it.name },
                        carbsOf = { it.carbsPer100g },
                        query = foodQuery,
                        onQueryChange = { foodQuery = it },
                        sort = foodSort,
                        onSortChange = { foodSort = it },
                        contentPadding = listPadding,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        emptyContent = { EmptyState(stringResource(R.string.no_foods)) }
                    ) { food ->
                        EntityListItem(
                            title = food.name,
                            value = formatCarbsPer100g(food.carbsPer100g),
                            onClick = { foodToWeigh = food },
                            onClickLabel = addLabel
                        )
                    }
                } else {
                    SearchableList(
                        items = recipes,
                        key = { it.recipe.id },
                        nameOf = { it.recipe.name },
                        carbsOf = { recipeCarbs[it.recipe.id] ?: 0.0 },
                        query = recipeQuery,
                        onQueryChange = { recipeQuery = it },
                        sort = recipeSort,
                        onSortChange = { recipeSort = it },
                        contentPadding = listPadding,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        emptyContent = { EmptyState(stringResource(R.string.no_recipes)) }
                    ) { recipe ->
                        EntityListItem(
                            title = recipe.recipe.name,
                            value = formatCarbsPer100g(recipeCarbs[recipe.recipe.id] ?: 0.0),
                            onClick = { recipeToWeigh = recipe },
                            onClickLabel = addLabel
                        )
                    }
                }
            }
        }

        foodToWeigh?.let { food ->
            WeightInputDialog(
                title = food.name,
                carbsPer100g = food.carbsPer100g,
                onDismiss = { foodToWeigh = null },
                onConfirm = { weight ->
                    added(MealItemDraft.Food(food.id, weight), food.name)
                    foodToWeigh = null
                }
            )
        }
        recipeToWeigh?.let { recipe ->
            WeightInputDialog(
                title = recipe.recipe.name,
                carbsPer100g = recipeCarbs[recipe.recipe.id],
                onDismiss = { recipeToWeigh = null },
                onConfirm = { weight ->
                    added(MealItemDraft.Recipe(recipe.recipe.id, weight), recipe.recipe.name)
                    recipeToWeigh = null
                }
            )
        }
    }
}
