package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.glucocalculateur.ui.Formatter

@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = com.example.glucocalculateur.R.string.add_food)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = com.example.glucocalculateur.R.string.food_name)) }
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() || char == ',' || char == '.' }) {
                            carbs = Formatter.validateNumericInput(it)
                        }
                    },
                    label = { Text(stringResource(id = com.example.glucocalculateur.R.string.carbs_per_100g)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                Text(stringResource(id = com.example.glucocalculateur.R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.example.glucocalculateur.R.string.cancel))
            }
        }
    )
}
