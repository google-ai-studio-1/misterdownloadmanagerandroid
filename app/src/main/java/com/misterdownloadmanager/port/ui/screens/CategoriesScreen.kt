package com.misterdownloadmanager.port.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.model.Category
import com.misterdownloadmanager.port.ui.icons.MdmIcons

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    selectedCategoryId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.id }) { category ->
            Card(
                onClick = { onSelect(category.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(MdmIcons.Category, contentDescription = null)
                    Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                    if (category.extensions.isNotEmpty()) {
                        Text(
                            text = category.extensions.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (category.id == selectedCategoryId) {
                        Text(text = stringResource(R.string.label_selected), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
