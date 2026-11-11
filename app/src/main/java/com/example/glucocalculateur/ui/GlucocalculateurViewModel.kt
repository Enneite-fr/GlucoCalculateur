package com.example.glucocalculateur.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.AppDatabase
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.data.GlucoRepository
import com.example.glucocalculateur.data.MealItemDraft
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.RecipeComponentDraft
import com.example.glucocalculateur.data.RecipeWithComponents
import com.example.glucocalculateur.data.io.CsvCodec
import com.example.glucocalculateur.data.io.ImportReport
import com.example.glucocalculateur.data.io.JsonBackupCodec
import com.example.glucocalculateur.data.io.RecipeRow
import com.example.glucocalculateur.data.io.TextDecoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class AppLanguage(val code: String, val country: String, val label: String) {
    SYSTEM("", "", "Système"),
    FRENCH_FR("fr", "FR", "Français (France)"),
    ENGLISH_US("en", "US", "English (United States)"),
    ENGLISH_GB("en", "GB", "English (United Kingdom)")
}

/** Événements ponctuels à afficher (Snackbar). */
sealed interface UiEvent {
    data class ImportFinished(val report: ImportReport) : UiEvent
    data class ExportFinished(val itemCount: Int) : UiEvent
    data class OperationFailed(val operation: FileOperation, val detail: String?) : UiEvent
    data class MealDeleted(val meal: MealWithItems) : UiEvent
}

enum class FileOperation { IMPORT, EXPORT }

class GlucoCalculateurViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GlucoRepository(AppDatabase.getDatabase(application))
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    // --- Préférences ---

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme_mode", null) ?: "") }.getOrDefault(ThemeMode.AUTO)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString("theme_mode", mode.name) }
        _themeMode.value = mode
    }

    private val _language = MutableStateFlow(
        runCatching { AppLanguage.valueOf(prefs.getString("app_language", null) ?: "") }.getOrDefault(AppLanguage.SYSTEM)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.edit { putString("app_language", lang.name) }
        _language.value = lang
    }

    // --- Données ---

    val allFood: StateFlow<List<FoodEntity>> = repository.foods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<List<RecipeWithComponents>> = repository.recipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeals: StateFlow<List<MealWithItems>> = repository.meals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Crée (`id == null`) ou modifie un aliment. */
    fun saveFood(id: Long?, name: String, carbsPer100g: Double) {
        viewModelScope.launch { repository.saveFood(id, name, carbsPer100g) }
    }

    fun deleteFood(foodId: Long) {
        viewModelScope.launch { repository.deleteFood(foodId) }
    }

    suspend fun countRecipesUsingFood(foodId: Long): Int = repository.countRecipesUsingFood(foodId)

    fun saveRecipe(id: Long?, name: String, components: List<RecipeComponentDraft>) {
        viewModelScope.launch { repository.saveRecipe(id, name, components) }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch { repository.deleteRecipe(recipeId) }
    }

    suspend fun countMealsUsingRecipe(recipeId: Long): Int = repository.countMealsUsingRecipe(recipeId)

    fun saveMeal(id: Long?, name: String, timestamp: Long, items: List<MealItemDraft>) {
        viewModelScope.launch { repository.saveMeal(id, name, timestamp, items) }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)?.let { _events.send(UiEvent.MealDeleted(it)) }
        }
    }

    fun restoreMeal(meal: MealWithItems) {
        viewModelScope.launch { runCatching { repository.restoreMeal(meal) } }
    }

    // --- Import / export ---

    fun exportFoodsCsv(uri: Uri) = export(uri) {
        val foods = repository.exportBackup().foods
        val header = listOf(string(R.string.csv_header_food), string(R.string.csv_header_carbs))
        CsvCodec.writeFoods(foods, header, decimalSeparator()) to foods.size
    }

    fun exportRecipesCsv(uri: Uri) = export(uri) {
        val recipes = repository.exportBackup().recipes
        val rows = recipes.flatMap { recipe ->
            recipe.components.map { RecipeRow(recipe.name, it.foodName, it.weightGrams) }
        }
        val header = listOf(
            string(R.string.csv_header_recipe),
            string(R.string.csv_header_ingredient),
            string(R.string.csv_header_weight)
        )
        CsvCodec.writeRecipes(rows, header, decimalSeparator()) to recipes.size
    }

    fun exportBackup(uri: Uri) = export(uri) {
        val backup = repository.exportBackup()
        JsonBackupCodec.encode(backup) to (backup.foods.size + backup.recipes.size + backup.meals.size)
    }

    fun importFoodsCsv(uri: Uri) = import(uri) { text ->
        val parsed = CsvCodec.parseFoods(text)
        repository.importFoods(parsed.rows).let { it.copy(issues = parsed.issues + it.issues) }
    }

    fun importRecipesCsv(uri: Uri) = import(uri) { text ->
        val parsed = CsvCodec.parseRecipes(text)
        repository.importRecipes(parsed.rows).let { it.copy(issues = parsed.issues + it.issues) }
    }

    fun importBackup(uri: Uri) = import(uri) { text ->
        val decoded = JsonBackupCodec.decode(text)
        repository.importBackup(decoded.backup).let { it.copy(issues = decoded.issues + it.issues) }
    }

    private fun export(uri: Uri, build: suspend () -> Pair<String, Int>) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val (text, count) = build()
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?: error("Flux d'écriture indisponible")
                    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    count
                }
            }
            _events.send(
                result.fold(
                    onSuccess = { UiEvent.ExportFinished(it) },
                    onFailure = { UiEvent.OperationFailed(FileOperation.EXPORT, it.message) }
                )
            )
        }
    }

    private fun import(uri: Uri, apply: suspend (String) -> ImportReport) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: error("Flux de lecture indisponible")
                    apply(TextDecoding.decode(bytes))
                }
            }
            _events.send(
                result.fold(
                    onSuccess = { UiEvent.ImportFinished(it) },
                    onFailure = { UiEvent.OperationFailed(FileOperation.IMPORT, it.message) }
                )
            )
        }
    }

    private fun string(id: Int): String = getApplication<Application>().getString(id)

    private fun decimalSeparator(): Char = DecimalFormatSymbols.getInstance(Locale.getDefault()).decimalSeparator
}
