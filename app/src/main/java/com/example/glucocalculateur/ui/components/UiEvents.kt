package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.MealWithItems
import com.example.glucocalculateur.data.io.ImportIssue
import com.example.glucocalculateur.data.io.ImportReport
import com.example.glucocalculateur.ui.FileOperation
import com.example.glucocalculateur.ui.UiEvent
import kotlinx.coroutines.flow.Flow

/**
 * Affiche les événements du ViewModel en Snackbar : bilans d'import/export, erreurs,
 * suppression de repas avec « Annuler ».
 */
@Composable
fun UiEventsEffect(
    events: Flow<UiEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoMealDeletion: (MealWithItems) -> Unit,
    onShowImportReport: (ImportReport) -> Unit
) {
    val context = LocalContext.current
    val undo by rememberUpdatedState(onUndoMealDeletion)
    val showReport by rememberUpdatedState(onShowImportReport)

    LaunchedEffect(events) {
        events.collect { event ->
            val res = context.resources
            when (event) {
                is UiEvent.ImportFinished -> {
                    val report = event.report
                    val parts = buildList {
                        add(res.getString(R.string.import_done_summary, report.added, report.updated, report.unchanged))
                        if (report.alreadyPresent > 0) {
                            add(res.getQuantityString(R.plurals.import_already_present, report.alreadyPresent, report.alreadyPresent))
                        }
                        if (report.issues.isNotEmpty()) {
                            add(res.getQuantityString(R.plurals.import_issues_count, report.issues.size, report.issues.size))
                        }
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = parts.joinToString("\n"),
                        actionLabel = if (report.issues.isNotEmpty()) res.getString(R.string.details) else null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) showReport(report)
                }
                is UiEvent.ExportFinished -> snackbarHostState.showSnackbar(
                    res.getQuantityString(R.plurals.export_done, event.itemCount, event.itemCount)
                )
                is UiEvent.OperationFailed -> snackbarHostState.showSnackbar(
                    message = res.getString(
                        if (event.operation == FileOperation.IMPORT) R.string.import_failed else R.string.export_failed
                    ),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                is UiEvent.MealDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = res.getString(R.string.meal_deleted),
                        actionLabel = res.getString(R.string.undo),
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) undo(event.meal)
                }
            }
        }
    }
}

/** Détail des éléments ignorés lors d'un import. */
@Composable
fun ImportReportDialog(report: ImportReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_report_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(report.issues) { issue ->
                    Text(issueText(issue), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun issueText(issue: ImportIssue): String = when (issue.type) {
    ImportIssue.Type.MALFORMED_LINE ->
        if (issue.line != null) stringResource(R.string.issue_malformed_line, issue.line, issue.subject)
        else stringResource(R.string.issue_malformed_entry, issue.subject)
    ImportIssue.Type.INVALID_NUMBER ->
        if (issue.line != null) stringResource(R.string.issue_invalid_number, issue.line, issue.subject)
        else stringResource(R.string.issue_invalid_number_entry, issue.subject)
    ImportIssue.Type.UNKNOWN_INGREDIENTS ->
        if (issue.detail.isEmpty()) stringResource(R.string.issue_recipe_without_ingredients, issue.subject)
        else stringResource(R.string.issue_unknown_ingredients, issue.subject, issue.detail)
    ImportIssue.Type.EMPTY_MEAL -> stringResource(R.string.issue_empty_meal, issue.subject)
    ImportIssue.Type.PARTIAL_MEAL -> stringResource(R.string.issue_partial_meal, issue.subject)
}
