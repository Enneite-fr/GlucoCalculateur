package com.example.glucocalculateur.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.net.toUri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.ui.AppLanguage
import com.example.glucocalculateur.ui.GlucoCalculateurViewModel
import com.example.glucocalculateur.ui.ThemeMode

@Composable
fun SettingsScreen(viewModel: GlucoCalculateurViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    
    val exportFoodLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportFoodsToCsv(context, it) }
    }

    val importFoodLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFoodsFromCsv(context, it) }
    }

    val exportRecipeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportRecipesToCsv(context, it) }
    }

    val importRecipeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importRecipesFromCsv(context, it) }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportDataToUri(context, it) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importDataFromUri(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section Apparence
        SettingsSection(title = stringResource(id = com.example.glucocalculateur.R.string.appearance_section)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(id = com.example.glucocalculateur.R.string.display_mode), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                            label = {
                                Text(when (mode) {
                                    ThemeMode.LIGHT -> stringResource(id = com.example.glucocalculateur.R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(id = com.example.glucocalculateur.R.string.theme_dark)
                                    ThemeMode.AUTO -> stringResource(id = com.example.glucocalculateur.R.string.theme_auto)
                                })
                            }
                        )
                    }
                }
            }
        }

        // Section Langue
        SettingsSection(title = stringResource(id = com.example.glucocalculateur.R.string.language_region_section)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    Surface(
                        onClick = { viewModel.setLanguage(lang) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (language == lang) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang.label, style = MaterialTheme.typography.bodyLarge)
                            if (language == lang) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(id = com.example.glucocalculateur.R.string.number_format_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Section Aliments (CSV)
        SettingsSection(title = "Gestion des Aliments (CSV)") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportFoodLauncher.launch("aliments.csv") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter")
                    }

                    OutlinedButton(
                        onClick = { importFoodLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importer")
                    }
                }
                Text("Format : Nom ; Glucides (pour 100g)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Section Recettes (CSV)
        SettingsSection(title = "Gestion des Recettes (CSV)") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportRecipeLauncher.launch("recettes.csv") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter")
                    }

                    OutlinedButton(
                        onClick = { importRecipeLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importer")
                    }
                }
                Text("Format : Nom Recette ; Ingrédient ; Poids (g)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Section Sauvegarde complète (JSON)
        SettingsSection(title = "Sauvegarde complète (JSON)") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { exportJsonLauncher.launch("gluco_data.json") },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Exporter JSON", style = MaterialTheme.typography.bodyLarge)
                        Text("Sauvegarde complète des aliments et recettes", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedButton(
                    onClick = { importJsonLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Importer JSON", style = MaterialTheme.typography.bodyLarge)
                        Text("Restaurer depuis un fichier JSON", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Section Crédits
        val packageInfo = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }
        }
        val versionName = packageInfo?.versionName ?: "1.0"

        SettingsSection(title = stringResource(id = com.example.glucocalculateur.R.string.credits_section)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(id = com.example.glucocalculateur.R.string.version_label, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Text(
                    text = stringResource(id = com.example.glucocalculateur.R.string.ai_designed_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Column {
                    Text(
                        text = stringResource(id = com.example.glucocalculateur.R.string.technologies_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = com.example.glucocalculateur.R.string.technologies_list),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Micyn/GlucoCalculateur".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(start = 0.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = com.example.glucocalculateur.R.string.github_link),
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
