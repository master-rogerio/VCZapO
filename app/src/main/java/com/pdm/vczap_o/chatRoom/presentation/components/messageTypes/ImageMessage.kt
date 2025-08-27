package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.pdm.vczap_o.R
import com.pdm.vczap_o.chatRoom.presentation.components.FullScreenImageViewer
import com.pdm.vczap_o.chatRoom.presentation.utils.vibrateDevice
import com.pdm.vczap_o.core.data.MediaCacheManager
import com.pdm.vczap_o.core.model.ChatMessage

@Composable
fun ImageMessage(
    message: ChatMessage, isFromMe: Boolean, fontSize: Int = 16, showPopUp: () -> Unit,
) {
    val tag = "ImageMessage"
    var isExpanded by remember { mutableStateOf(false) }
    message.image?.let { imageUrl ->
        val context = LocalContext.current
        var mediaUri by remember { mutableStateOf<Uri?>(null) }

        LaunchedEffect(imageUrl) {
            val cachedUri = MediaCacheManager.getMediaUri(context, imageUrl)
            Log.d(tag, "Retrieved cached image URI: $cachedUri")
            mediaUri = cachedUri
        }

        Column {
            AsyncImage(
                model = mediaUri ?: imageUrl,
                fallback = painterResource(id = R.drawable.person), // Placeholder
                error = painterResource(id = R.drawable.person), // Imagem de erro
                contentDescription = "Image message",
                modifier = Modifier
                    .heightIn(min = 30.dp, max = 250.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)
                    )
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            vibrateDevice(context)
                            showPopUp()
                        }, onTap = {
                            // Só permite expandir a imagem se o URI não for nulo
                            if (mediaUri != null || imageUrl.isNotBlank()) {
                                isExpanded = true
                            }
                        })
                    },
                contentScale = ContentScale.FillWidth
            )
            if (message.content.isNotEmpty()) {
                Text(
                    text = message.content,
                    color = if (isFromMe) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = fontSize.sp,
                    lineHeight = getLineHeight(fontSize).sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .padding(horizontal = 5.dp)
                )
            }
            if (isExpanded) {
                // Garante que não seja um valor nulo para o visualizador de imagem
                val imageToShow = (mediaUri ?: imageUrl.toUri()).toString()
                FullScreenImageViewer(
                    imageUri = imageToShow,
                    onDismiss = { isExpanded = false },
                )
            }
        }
    }
}