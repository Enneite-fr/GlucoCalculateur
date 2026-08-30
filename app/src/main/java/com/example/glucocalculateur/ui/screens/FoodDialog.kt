package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.R
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.ui.Formatter

@Composable
fun FoodDialog(
    food: FoodEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(food?.name ?: "") }
    var carbs by rememberSaveable { mutableStateOf(food?.carbsPer100g?.let { Formatter.formatDouble(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (food == null) stringResource(id = R.string.add_food) else stringResource(id = R.string.edit_food_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.food_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() || char == ',' || char == '.' }) {
                            carbs = Formatter.validateNumericInput(it)
                        }
                    },
                    label = { Text(stringResource(id = R.string.carbs_per_100g)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val carbsValue = Formatter.parseDouble(carbs) ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, carbsValue)
                    }
                }
            ) {
                Text(if (food == null) stringResource(id = R.string.add) else stringResource(id = R.string.save_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
