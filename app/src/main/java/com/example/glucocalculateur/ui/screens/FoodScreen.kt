package com.example.glucocalculateur.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.glucocalculateur.data.FoodEntity
import com.example.glucocalculateur.ui.Formatter

@Composable
fun FoodScreen(
    foods: List<FoodEntity>,
    onDeleteFood: (FoodEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(foods) { food ->
            FoodItem(food, onDeleteFood)
        }
    }
}

@Composable
fun FoodItem(
    food: FoodEntity,
    onDeleteFood: (FoodEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = food.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${Formatter.formatDouble(food.carbsPer100g)}g " + stringResource(id = com.example.glucocalculateur.R.string.carbs_per_100g),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onDeleteFood(food) }) {
                Text(stringResource(id = com.example.glucocalculateur.R.string.delete))
            }
        }
    }
}
