package com.example.glucocalculateur.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.glucocalculateur.R
import com.example.glucocalculateur.ui.AppLanguage
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.ThemeMode
import com.example.glucocalculateur.ui.components.ImportExportRow
import com.example.glucocalculateur.ui.theme.Dimens
import java.time.LocalDate

private const val GITHUB_URL = "https://github.com/Enneite-fr/GlucoCalculateur"
private val CSV_TYPES = arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream")
private val JSON_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

/** Nom de fichier d'export neutre et daté : `gluco-foods-2026-11-11.csv`. */
private fun exportFileName(kind: String, extension: String) = "gluco-$kind-${LocalDate.now()}.$extension"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: GlucoCalculateurViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language = remember { AppLanguage.current() }

    val exportFoods = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let(viewModel::exportFoodsCsv)
    }
    val importFoods = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::importFoodsCsv)
    }
    val exportRecipes = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let(viewModel::exportRecipesCsv)
    }
    val importRecipes = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::importRecipesCsv)
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let(viewModel::exportBackup)
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::importBackup)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceL)
        ) {
            SettingsCard(title = stringResource(R.string.appearance_section)) {
                Text(stringResource(R.string.display_mode), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                            ThemeMode.AUTO -> R.string.theme_auto
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.language_region_section)) {
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = lang == language,
                                    role = Role.RadioButton,
                                    onClick = { if (lang != language) lang.apply() }
                                )
                                .padding(vertical = Dimens.spaceXs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = lang == language, onClick = null)
                            Spacer(Modifier.width(Dimens.spaceM))
                            Text(
                                lang.nativeName ?: stringResource(R.string.language_system),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                Hint(stringResource(R.string.number_format_note))
            }

            SettingsCard(title = stringResource(R.string.food_csv_section)) {
                ImportExportRow(
                    onExport = { exportFoods.launch(exportFileName("foods", "csv")) },
                    onImport = { importFoods.launch(CSV_TYPES) }
                )
                Hint(stringResource(R.string.food_csv_format_hint))
            }

            SettingsCard(title = stringResource(R.string.recipe_csv_section)) {
                ImportExportRow(
                    onExport = { exportRecipes.launch(exportFileName("recipes", "csv")) },
                    onImport = { importRecipes.launch(CSV_TYPES) }
                )
                Hint(stringResource(R.string.recipe_csv_format_hint))
            }

            SettingsCard(title = stringResource(R.string.backup_json_section)) {
                ImportExportRow(
                    onExport = { exportBackup.launch(exportFileName("backup", "json")) },
                    onImport = { importBackup.launch(JSON_TYPES) }
                )
                Hint(stringResource(R.string.export_json_subtitle))
            }

            CreditsCard()
        }
    }
}

@Composable
private fun CreditsCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.versionName
        }.getOrNull() ?: "?"
    }
    SettingsCard(title = stringResource(R.string.credits_section)) {
        Text(stringResource(R.string.version_label, versionName), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.ai_designed_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        HorizontalDivider()
        Text(
            stringResource(R.string.technologies_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(stringResource(R.string.technologies_list), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { runCatching { uriHandler.openUri(GITHUB_URL) } }) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(Dimens.spaceS))
            Text(stringResource(R.string.github_link))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
