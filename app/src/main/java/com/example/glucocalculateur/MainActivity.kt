package com.example.glucocalculateur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.os.ConfigurationCompat
import android.content.res.Resources
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glucocalculateur.ui.AppLanguage
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.ThemeMode
import com.example.glucocalculateur.ui.components.SpeedDialAction
import com.example.glucocalculateur.ui.components.SpeedDialFab
import com.example.glucocalculateur.ui.screens.*
import com.example.glucocalculateur.ui.theme.GlucoCalculateurTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialiser la langue avant setContent pour éviter le clignotement
        val prefs = getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val langName = prefs.getString("app_language", "SYSTEM") ?: "SYSTEM"
        try {
            val appLang = com.example.glucocalculateur.ui.AppLanguage.valueOf(langName)
            val locale = if (appLang == com.example.glucocalculateur.ui.AppLanguage.SYSTEM) {
                ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0] ?: Locale.getDefault()
            } else {
                Locale(appLang.code, appLang.country)
            }
            com.example.glucocalculateur.ui.Formatter.applyLocaleToContext(this, locale)
        } catch (e: Exception) {}

        enableEdgeToEdge()
        setContent {
            val viewModel: GlucoCalculateurViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val language by viewModel.language.collectAsState()

            val locale = remember(language) {
                if (language == AppLanguage.SYSTEM) {
                    ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0] ?: Locale.getDefault()
                } else {
                    Locale(language.code, language.country)
                }
            }
            
            val context = LocalContext.current
            val activity = context as? androidx.activity.ComponentActivity
            
            // Appliquer la locale au niveau de l'activité pour forcer le rafraîchissement global
            LaunchedEffect(locale) {
                val config = activity?.resources?.configuration
                if (config != null && config.locales[0] != locale) {
                    com.example.glucocalculateur.ui.Formatter.applyLocaleToContext(activity, locale)
                    activity.recreate()
                }
            }

            val localizedContext = remember(locale) {
                com.example.glucocalculateur.ui.Formatter.updateResourceLocale(context, locale)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            LaunchedEffect(darkTheme) {
                activity?.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                    ) { darkTheme }
                )
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalContext provides localizedContext,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides (activity ?: androidx.activity.compose.LocalActivityResultRegistryOwner.current!!)
            ) {
                GlucoCalculateurTheme(
                    darkTheme = darkTheme
                ) {
                    GlucoCalculateurApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoCalculateurApp(viewModel: GlucoCalculateurViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MEALS) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    
    val foods by viewModel.allFood.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState()
    val meals by viewModel.allMeals.collectAsState()

    // Dialogs
    if (showAddFoodDialog) {
        FoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { name, carbs ->
                viewModel.addFood(name, carbs)
                showAddFoodDialog = false
            }
        )
    }

    if (showAddRecipeDialog) {
        RecipeDialog(
            availableFood = foods,
            onDismiss = { showAddRecipeDialog = false },
            onConfirm = { name, components ->
                viewModel.addRecipe(name, components)
                showAddRecipeDialog = false
            }
        )
    }

    if (showAddMealDialog) {
        MealDialog(
            availableFood = foods,
            availableRecipes = recipes,
            onDismiss = { showAddMealDialog = false },
            onConfirm = { name, timestamp, items ->
                viewModel.addMeal(name, timestamp, items)
                showAddMealDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isSettingsOpen) {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.tab_settings)) },
                        navigationIcon = {
                            IconButton(onClick = { isSettingsOpen = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isSettingsOpen) {
                        SettingsScreen(viewModel = viewModel)
                    } else {
                        when (currentDestination) {
                            AppDestinations.MEALS -> MealScreen(
                                meals = meals,
                                availableFood = foods,
                                availableRecipes = recipes,
                                onDeleteMeal = { viewModel.deleteMeal(it.meal) },
                                onSettingsClick = { isSettingsOpen = true },
                                viewModel = viewModel
                            )
                            AppDestinations.RECIPES -> RecipeScreen(
                                recipes = recipes,
                                availableFood = foods,
                                onDeleteRecipe = { viewModel.deleteRecipe(it.recipe) },
                                onSettingsClick = { isSettingsOpen = true },
                                viewModel = viewModel
                            )
                            AppDestinations.FOOD -> FoodScreen(
                                foods = foods,
                                onDeleteFood = { viewModel.deleteFood(it) },
                                onSettingsClick = { isSettingsOpen = true },
                                viewModel = viewModel
                            )
                        }
                    }
                }

                if (!isSettingsOpen) {
                    // Dégradé plus accentué pour faire disparaître les cartes
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
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
                }
            }
        }

        // Complètement à l'extérieur du Scaffold pour un overlay parfait
        if (!isSettingsOpen) {
            SpeedDialFab(onActionClick = { action ->
                when (action) {
                    SpeedDialAction.FOOD -> showAddFoodDialog = true
                    SpeedDialAction.RECIPE -> showAddRecipeDialog = true
                    SpeedDialAction.MEAL -> showAddMealDialog = true
                }
            })
        }
    }
}

@Composable
fun FloatingBottomBar(
    currentDestination: AppDestinations,
    onNavigate: (AppDestinations) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(72.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Fond flouté (Effet verre dépoli)
        // Note: Le flou s'applique au contenu de la Surface. 
        // Pour simuler un flou d'arrière-plan, on utilise une Surface semi-transparente floutée.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .blur(15.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            shadowElevation = 12.dp
        ) {}

        // Contenu non flouté (Icônes et texte)
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
