package com.example.photoapp10.core.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selection indicator badge that shows the selection number
 */
@Composable
fun SelectionBadge(
    selectionNumber: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = selectionNumber.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Selection border modifier for selected items
 */
@Composable
fun Modifier.selectionBorder(isSelected: Boolean): Modifier {
    return if (isSelected) {
        this.border(
            width = 3.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
    } else {
        this
    }
}
