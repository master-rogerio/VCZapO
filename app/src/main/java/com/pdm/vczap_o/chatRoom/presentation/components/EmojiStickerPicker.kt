package com.pdm.vczap_o.chatRoom.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun EmojiStickerPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Emojis", "Stickers")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Selecionar Emoji ou Sticker",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tab Row para alternar entre Emojis e Stickers
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Conteúdo baseado na tab selecionada
                when (selectedTab) {
                    0 -> EmojiContent(onEmojiSelected = onEmojiSelected)
                    1 -> StickerContent(onStickerSelected = onStickerSelected)
                }
            }
        }
    }
}

@Composable
private fun EmojiContent(onEmojiSelected: (String) -> Unit) {
    val categories = emojiCategories.keys.toList()
    val pagerState = rememberPagerState(
        pageCount = { categories.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = category.take(8) + if (category.length > 8) "..." else "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

        HorizontalPager(pagerState) { page ->
            val categoryName = categories[page]
            val emojis = emojiCategories[categoryName] ?: emptyList()

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 40.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(emojis.size) { index ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .clickable { onEmojiSelected(emojis[index]) }
                    ) {
                        Text(
                            text = emojis[index],
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerContent(onStickerSelected: (String) -> Unit) {
    val categories = stickerCategories.keys.toList()
    val pagerState = rememberPagerState(
        pageCount = { categories.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = category.take(8) + if (category.length > 8) "..." else "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

        HorizontalPager(pagerState) { page ->
            val categoryName = categories[page]
            val stickers = stickerCategories[categoryName] ?: emptyList()

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 60.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stickers.size) { index ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onStickerSelected(stickers[index]) }
                    ) {
                        Text(
                            text = stickers[index],
                            fontSize = 32.sp
                        )
                    }
                }
            }
        }
    }
}