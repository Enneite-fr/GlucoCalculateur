package com.example.glucocalculateur.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.glucocalculateur.R
import com.example.glucocalculateur.domain.TextSearch
import com.example.glucocalculateur.ui.currentLocale
import com.example.glucocalculateur.ui.theme.Dimens

enum class SortOption(val labelRes: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    CARBS_ASC(R.string.sort_carbs_asc),
    CARBS_DESC(R.string.sort_carbs_desc)
}

/**
 * Liste filtrable et triable : barre de recherche (sans accents ni casse), tri, état vide
 * et « aucun résultat ».
 */
@Composable
fun <T> SearchableList(
    items: List<T>,
    key: (T) -> Any,
    nameOf: (T) -> String,
    carbsOf: (T) -> Double,
    query: String,
    onQueryChange: (String) -> Unit,
    sort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    emptyContent: @Composable () -> Unit = {},
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val locale = currentLocale()
    val indexed = remember(items) { items.map { it to TextSearch.normalize(nameOf(it)) } }
    val visible = remember(indexed, query, sort, locale) {
        val filtered = if (query.isBlank()) indexed.map { it.first }
        else indexed.filter { TextSearch.matches(it.second, query) }.map { it.first }
        val byName = compareBy(TextSearch.collator(locale), nameOf)
        when (sort) {
            SortOption.NAME_ASC -> filtered.sortedWith(byName)
            SortOption.NAME_DESC -> filtered.sortedWith(byName.reversed())
            SortOption.CARBS_ASC -> filtered.sortedWith(compareBy(carbsOf).then(byName))
            SortOption.CARBS_DESC -> filtered.sortedWith(compareByDescending(carbsOf).then(byName))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(query = query, onQueryChange = onQueryChange, sort = sort, onSortChange = onSortChange)
        when {
            items.isEmpty() -> emptyContent()
            visible.isEmpty() -> EmptyState(
                message = stringResource(R.string.no_results, query.trim()),
                icon = Icons.Default.SearchOff
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceS)
            ) {
                items(visible, key = key) { itemContent(it) }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.sort_btn_desc))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortOption.entries.forEach { option ->
                    val selected = option == sort
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(option.labelRes),
                                fontWeight = if (selected) FontWeight.Bold else null
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (option == SortOption.NAME_ASC || option == SortOption.NAME_DESC) {
                                    Icons.Default.SortByAlpha
                                } else {
                                    Icons.Default.SwapVert
                                },
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            onSortChange(option)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}
