package com.example.photoapp10.feature.album.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun EmojiSelectionDialog(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf(
        "📷", "🖼️", "📸", "🎨", "🌈", "⭐", "❤️", "💙", "💚", "💛", "🧡", "💜",
        "🎉", "🎊", "🎈", "🎁", "🎂", "🍰", "☕", "🍕", "🍔", "🍟", "🌮", "🌯",
        "🏖️", "🏔️", "🌊", "🌅", "🌄", "🌙", "☀️", "⭐", "🌟", "💫", "✨", "🔥",
        "🌸", "🌺", "🌻", "🌷", "🌹", "🌿", "🍀", "🌱", "🌳", "🌲", "🌴", "🌵",
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮",
        "🚗", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛",
        "✈️", "🚁", "🚀", "🛸", "🚂", "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉",
        "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬",
        "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸", "🏒", "🏑",
        "🎵", "🎶", "🎤", "🎧", "🎸", "🎹", "🥁", "🎺", "🎷", "🎻", "🎼", "🎯",
        "📱", "💻", "⌨️", "🖥️", "🖨️", "📠", "📞", "☎️", "📟", "📺", "📻", "🎙️"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Choose an Emoji",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(60.dp),
                    modifier = Modifier.height(300.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(emojis) { emoji ->
                        Card(
                            onClick = {
                                onEmojiSelected(emoji)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(50.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}













