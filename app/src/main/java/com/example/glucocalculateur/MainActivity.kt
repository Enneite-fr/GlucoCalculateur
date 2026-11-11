package com.example.glucocalculateur

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glucocalculateur.data.io.ImportReport
import com.example.glucocalculateur.ui.AppLanguage
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.ThemeMode
import com.example.glucocalculateur.ui.components.AppDestinations
import com.example.glucocalculateur.ui.components.FloatingBottomBar
import com.example.glucocalculateur.ui.components.ImportReportDialog
import com.example.glucocalculateur.ui.components.UiEventsEffect
import com.example.glucocalculateur.ui.screens.FoodDialog
import com.example.glucocalculateur.ui.screens.FoodScreen
import com.example.glucocalculateur.ui.screens.MealEditorScreen
import com.example.glucocalculateur.ui.screens.MealScreen
import com.example.glucocalculateur.ui.screens.RecipeEditorScreen
import com.example.glucocalculateur.ui.screens.RecipeScreen
import com.example.glucocalculateur.ui.screens.SettingsScreen
import com.example.glucocalculateur.ui.theme.Dimens
import com.example.glucocalculateur.ui.theme.GlucoCalculateurTheme
import com.example.glucocalculateur.ui.theme.listBottomPadding
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguage.migrateLegacyPreference(this)

        enableEdgeToEdge()
        setContent {
            val viewModel: GlucoCalculateurViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme },
                )
            }

            GlucoCalculateurTheme(darkTheme = darkTheme) {
                GlucoCalculateurApp(viewModel)
            }
        }
    }
}

/** Écran plein affiché par-dessus les onglets. */
sealed interface Editor : Parcelable {
    @Parcelize
    data class Meal(val mealId: Long?, val epochDay: Long) : Editor

    @Parcelize
    data class Recipe(val recipeId: Long?) : Editor
}

/** Dialogue d'aliment ouvert (création si [foodId] est nul). */
@Parcelize
data class FoodDialogTarget(val foodId: Long?) : Parcelable

@Composable
fun GlucoCalculateurApp(viewModel: GlucoCalculateurViewModel) {
    var destination by rememberSaveable { mutableStateOf(AppDestinations.MEALS) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var foodDialog by rememberSaveable { mutableStateOf<FoodDialogTarget?>(null) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val selectedDay = LocalDate.ofEpochDay(selectedEpochDay)

    val foods by viewModel.allFood.collectAsStateWithLifecycle()
    val recipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val meals by viewModel.allMeals.collectAsStateWithLifecycle()
    val foodMap = remember(foods) { foods.associateBy { it.id } }
    val recipeMap = remember(recipes) { recipes.associateBy { it.recipe.id } }

    val snackbarHostState = remember { SnackbarHostState() }
    var importReport by remember { mutableStateOf<ImportReport?>(null) }
    UiEventsEffect(
        events = viewModel.events,
        snackbarHostState = snackbarHostState,
        onUndoMealDeletion = viewModel::restoreMeal,
        onShowImportReport = { importReport = it }
    )

    // Retour : depuis un autre onglet, revient aux repas (les éditeurs et les paramètres gèrent le leur).
    BackHandler(enabled = editor == null && !settingsOpen && destination != AppDestinations.MEALS) {
        destination = AppDestinations.MEALS
    }

    val createForCurrentTab = {
        when (destination) {
            AppDestinations.MEALS -> editor = Editor.Meal(null, selectedEpochDay)
            AppDestinations.RECIPES -> editor = Editor.Recipe(null)
            AppDestinations.FOOD -> foodDialog = FoodDialogTarget(null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    ) {
        when (val current = editor) {
            is Editor.Meal -> {
                val meal = current.mealId?.let { id -> meals.firstOrNull { it.meal.id == id } }
                if (current.mealId == null || meal != null) {
                    MealEditorScreen(
                        meal = meal,
                        initialDate = LocalDate.ofEpochDay(current.epochDay),
                        foods = foods,
                        recipes = recipes,
                        foodMap = foodMap,
                        recipeMap = recipeMap,
                        onSave = { name, timestamp, items ->
                            viewModel.saveMeal(current.mealId, name, timestamp, items)
                            editor = null
                        },
                        onClose = { editor = null }
                    )
                } else {
                    BackHandler { editor = null }
                }
            }
            is Editor.Recipe -> {
                val recipe = current.recipeId?.let { recipeMap[it] }
                if (current.recipeId == null || recipe != null) {
                    RecipeEditorScreen(
                        recipe = recipe,
                        foods = foods,
                        foodMap = foodMap,
                        onSave = { name, components ->
                            viewModel.saveRecipe(current.recipeId, name, components)
                            editor = null
                        },
                        onClose = { editor = null }
                    )
                } else {
                    BackHandler { editor = null }
                }
            }
            null -> if (settingsOpen) {
                SettingsScreen(viewModel = viewModel, onBack = { settingsOpen = false })
            } else {
                MainTabs(
                    destination = destination,
                    onNavigate = { destination = it },
                    onCreate = createForCurrentTab
                ) { bottomPadding ->
                    when (destination) {
                        AppDestinations.MEALS -> MealScreen(
                            meals = meals,
                            foodMap = foodMap,
                            recipeMap = recipeMap,
                            selectedDay = selectedDay,
                            onSelectedDayChange = { selectedEpochDay = it.toEpochDay() },
                            onAddMeal = { editor = Editor.Meal(null, it.toEpochDay()) },
                            onEditMeal = { editor = Editor.Meal(it.meal.id, selectedEpochDay) },
                            onDeleteMeal = { viewModel.deleteMeal(it.meal.id) },
                            onSettingsClick = { settingsOpen = true },
                            bottomPadding = bottomPadding
                        )
                        AppDestinations.RECIPES -> RecipeScreen(
                            recipes = recipes,
                            foodMap = foodMap,
                            onCreateRecipe = { editor = Editor.Recipe(null) },
                            onEditRecipe = { editor = Editor.Recipe(it.recipe.id) },
                            onDeleteRecipe = { viewModel.deleteRecipe(it.recipe.id) },
                            countMealsUsingRecipe = viewModel::countMealsUsingRecipe,
                            onSettingsClick = { settingsOpen = true },
                            bottomPadding = bottomPadding
                        )
                        AppDestinations.FOOD -> FoodScreen(
                            foods = foods,
                            onCreateFood = { foodDialog = FoodDialogTarget(null) },
                            onEditFood = { foodDialog = FoodDialogTarget(it.id) },
                            onDeleteFood = { viewModel.deleteFood(it.id) },
                            countRecipesUsingFood = viewModel::countRecipesUsingFood,
                            onSettingsClick = { settingsOpen = true },
                            bottomPadding = bottomPadding
                        )
                    }
                }
            }
        }

        val onTabs = editor == null && !settingsOpen
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (onTabs) Dimens.listBottomSpace else 96.dp)
        )
    }

    foodDialog?.let { target ->
        val food = target.foodId?.let { foodMap[it] }
        if (target.foodId == null || food != null) {
            FoodDialog(
                food = food,
                foods = foods,
                onSave = { id, name, carbs ->
                    viewModel.saveFood(id, name, carbs)
                    foodDialog = null
                },
                onDismiss = { foodDialog = null }
            )
        }
    }
    importReport?.let { ImportReportDialog(it, onDismiss = { importReport = null }) }
}

/** Onglets : contenu, dégradé de bas de liste, barre flottante et bouton d'action contextuel. */
@Composable
private fun MainTabs(
    destination: AppDestinations,
    onNavigate: (AppDestinations) -> Unit,
    onCreate: () -> Unit,
    content: @Composable (bottomPadding: androidx.compose.ui.unit.Dp) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content(listBottomPadding())

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenPadding)
                .padding(bottom = Dimens.bottomBarMargin)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingBottomBar(current = destination, onNavigate = onNavigate, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Dimens.spaceM))
            FloatingActionButton(
                onClick = onCreate,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimens.bottomBarHeight)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(destination.createLabelRes),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
