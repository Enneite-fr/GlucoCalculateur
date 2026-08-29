package com.example.glucocalculateur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.components.SpeedDialAction
import com.example.glucocalculateur.ui.components.SpeedDialFab
import com.example.glucocalculateur.ui.screens.*
import com.example.glucocalculateur.ui.theme.GlucoCalculateurTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlucoCalculateurTheme {
                GlucoCalculateurApp()
            }
        }
    }
}

@Composable
fun GlucoCalculateurApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MEALS) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    
    val viewModel: GlucoCalculateurViewModel = viewModel()
    val foods by viewModel.allFood.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState()
    val meals by viewModel.allMeals.collectAsState()

    // Dialogs
    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { name, carbs ->
                viewModel.addFood(name, carbs)
                showAddFoodDialog = false
            }
        )
    }

    if (showAddRecipeDialog) {
        AddRecipeDialog(
            availableFood = foods,
            onDismiss = { showAddRecipeDialog = false },
            onConfirm = { name, components ->
                viewModel.addRecipe(name, components)
                showAddRecipeDialog = false
            }
        )
    }

    if (showAddMealDialog) {
        AddMealDialog(
            availableFood = foods,
            availableRecipes = recipes,
            onDismiss = { showAddMealDialog = false },
            onConfirm = { name, items ->
                viewModel.addMeal(name, items)
                showAddMealDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { currentDestination = it }
                )
            }
        },
        floatingActionButton = {
            SpeedDialFab(onActionClick = { action ->
                when (action) {
                    SpeedDialAction.FOOD -> showAddFoodDialog = true
                    SpeedDialAction.RECIPE -> showAddRecipeDialog = true
                    SpeedDialAction.MEAL -> showAddMealDialog = true
                }
            })
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentDestination) {
                AppDestinations.MEALS -> MealScreen(
                    meals = meals,
                    availableFood = foods,
                    availableRecipes = recipes,
                    onDeleteMeal = { viewModel.deleteMeal(it.meal) }
                )
                AppDestinations.RECIPES -> RecipeScreen(
                    recipes = recipes,
                    availableFood = foods,
                    onDeleteRecipe = { viewModel.deleteRecipe(it.recipe) }
                )
                AppDestinations.FOOD -> FoodScreen(
                    foods = foods,
                    onDeleteFood = { viewModel.deleteFood(it) }
                )
            }
        }
    }
}

@Composable
fun FloatingBottomBar(
    currentDestination: AppDestinations,
    onNavigate: (AppDestinations) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(CircleShape),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestinations.entries.forEach { destination ->
                val isSelected = currentDestination == destination
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    label = "bg"
                )
                val iconColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "icon"
                )
                
                Surface(
                    onClick = { onNavigate(destination) },
                    shape = CircleShape,
                    color = backgroundColor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (val icon = destination.icon) {
                            is IconSource.Vector -> {
                                Icon(
                                    imageVector = icon.imageVector,
                                    contentDescription = stringResource(destination.labelRes),
                                    tint = iconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            is IconSource.Resource -> {
                                Icon(
                                    painter = painterResource(id = icon.resId),
                                    contentDescription = stringResource(destination.labelRes),
                                    tint = iconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        if (isSelected) {
                            Text(
                                text = stringResource(destination.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class Resource(val resId: Int) : IconSource()
}

enum class AppDestinations(
    val labelRes: Int,
    val icon: IconSource,
) {
    MEALS(R.string.tab_meals, IconSource.Vector(Icons.Default.Restaurant)),
    RECIPES(R.string.tab_recipes, IconSource.Vector(Icons.AutoMirrored.Filled.MenuBook)),
    FOOD(R.string.tab_food, IconSource.Resource(R.drawable.ic_carotte)),
}
