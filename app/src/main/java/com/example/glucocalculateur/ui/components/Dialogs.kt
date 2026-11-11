package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.glucocalculateur.R
import com.example.glucocalculateur.domain.CarbCalculator
import com.example.glucocalculateur.domain.DateUtils
import com.example.glucocalculateur.ui.Formatter
import com.example.glucocalculateur.ui.currentLocale
import com.example.glucocalculateur.ui.formatCarbs
import com.example.glucocalculateur.ui.formatNumber
import com.example.glucocalculateur.ui.theme.Dimens
import java.time.LocalDate
import java.time.LocalTime

/** Confirmation de suppression, avec avertissement éventuel. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    warning: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                if (warning != null) {
                    Spacer(Modifier.height(Dimens.spaceS))
                    Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Champ numérique décimal localisé, avec unité et message d'erreur. */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    error: String? = null,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val locale = currentLocale()
    OutlinedTextField(
        value = value,
        onValueChange = { input -> Formatter.sanitizeDecimalInput(input, locale)?.let(onValueChange) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

/** Valeur décimale bornée : renvoie la valeur, ou le message d'erreur à afficher. */
sealed interface DecimalInput {
    data class Valid(val value: Double) : DecimalInput
    data class Invalid(val message: String?) : DecimalInput
}

@Composable
fun validateDecimal(text: String, max: Double, allowZero: Boolean, touched: Boolean = true): DecimalInput {
    val value = Formatter.parseDouble(text)
    val message = when {
        text.isBlank() || value == null -> if (touched) stringResource(R.string.error_value_required) else null
        value > max -> stringResource(R.string.error_max_value, formatNumber(max))
        value < 0.0 || (!allowZero && value == 0.0) -> stringResource(R.string.error_positive)
        else -> return DecimalInput.Valid(value)
    }
    return DecimalInput.Invalid(message)
}

private const val MAX_WEIGHT_GRAMS = 10_000.0

/** Saisie du poids d'un élément, avec aperçu des glucides correspondants. */
@Composable
fun WeightInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    initialWeight: Double? = null,
    carbsPer100g: Double? = null
) {
    val locale = currentLocale()
    var text by rememberSaveable {
        mutableStateOf(initialWeight?.takeIf { it > 0.0 }?.let { Formatter.formatNumber(it, locale) } ?: "")
    }
    var touched by rememberSaveable { mutableStateOf(false) }
    val input = validateDecimal(text, max = MAX_WEIGHT_GRAMS, allowZero = false, touched = touched)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val preview = (input as? DecimalInput.Valid)?.let { valid ->
        carbsPer100g?.let { formatCarbs(CarbCalculator.componentCarbs(it, valid.value)) }
    }
    val confirm = { if (input is DecimalInput.Valid) onConfirm(input.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DecimalField(
                value = text,
                onValueChange = { text = it; touched = true },
                label = stringResource(R.string.weight_label),
                suffix = stringResource(R.string.grams_unit),
                error = (input as? DecimalInput.Invalid)?.message,
                supportingText = preview?.let { stringResource(R.string.carbs_preview, it) },
                onImeAction = confirm,
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = confirm, enabled = input is DecimalInput.Valid) {
                Text(stringResource(R.string.validate_btn))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Sélecteur de date : le jour choisi est un [LocalDate], sans décalage de fuseau. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = DateUtils.dateToPickerMillis(initialDate))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(DateUtils.pickerMillisToDate(it)) } ?: onDismiss() }
            ) { Text(stringResource(R.string.validate_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    ) {
        DatePicker(state = state)
    }
}

/** Sélecteur d'heure, au format 12/24 h du système. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.validate_btn))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) }
    )
}

/** Confirmation avant d'abandonner une saisie modifiée. */
@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.keep_editing)) } }
    )
}
