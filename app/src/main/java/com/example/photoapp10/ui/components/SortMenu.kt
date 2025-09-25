package com.example.photoapp10.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.photoapp10.feature.photo.domain.SortMode

@Composable
fun SortMenu(
    current: SortMode,
    onChange: (SortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        IconButton(onClick = { expanded = true }) {
            // Replace with your own icon resource if available
            Icon(painterResource(android.R.drawable.arrow_down_float), contentDescription = "Sort")
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Name A–Z") },
            onClick = { expanded = false; onChange(SortMode.NAME_ASC) }
        )
        DropdownMenuItem(
            text = { Text("Name Z–A") },
            onClick = { expanded = false; onChange(SortMode.NAME_DESC) }
        )
        DropdownMenuItem(
            text = { Text("Date newest") },
            onClick = { expanded = false; onChange(SortMode.DATE_NEW) }
        )
        DropdownMenuItem(
            text = { Text("Date oldest") },
            onClick = { expanded = false; onChange(SortMode.DATE_OLD) }
        )
        DropdownMenuItem(
            text = { Text("Favorites first") },
            onClick = { expanded = false; onChange(SortMode.FAV_FIRST) }
        )
    }
}














