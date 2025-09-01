package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.pdm.vczap_o.R
import com.pdm.vczap_o.chatRoom.presentation.components.FullScreenImageViewer
import com.pdm.vczap_o.chatRoom.presentation.utils.vibrateDevice
import com.pdm.vczap_o.core.data.MediaCacheManager
import com.pdm.vczap_o.core.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun ImageMessage(
    message: ChatMessage, isFromMe: Boolean, fontSize: Int = 16, showPopUp: () -> Unit,
) {
    val tag = "ImageMessage"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    
    // Launcher para solicitar permissão de escrita
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                downloadImageToGallery(context, message.image ?: "", message.id) { success ->
                    isDownloading = false
                    val messageText = if (success) {
                        "Imagem salva na galeria com sucesso!"
                    } else {
                        "Erro ao salvar imagem na galeria"
                    }
                    Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permissão necessária para salvar na galeria", Toast.LENGTH_SHORT).show()
        }
    }
    
    message.image?.let { imageUrl ->
        var mediaUri by remember { mutableStateOf<Uri?>(null) }

        LaunchedEffect(imageUrl) {
            try {
                // ALTERAÇÃO 28/08/2025 R - Carregamento robusto de imagem com fallback
                val cachedUri = MediaCacheManager.getMediaUri(context, imageUrl)
                mediaUri = cachedUri
                Log.d(tag, "Imagem carregada do cache: $cachedUri")
            } catch (e: Exception) {
                // ALTERAÇÃO 28/08/2025 R - Fallback robusto para URL original
                Log.w(tag, "Falha ao carregar imagem do cache: ${e.message}")
                Log.w(tag, "Usando URL original como fallback: $imageUrl")
                mediaUri = imageUrl.toUri()
                // FIM ALTERAÇÃO 28/08/2025 R
            }
        }

        Box {
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
        
        // Botão de download flutuante
        FloatingActionButton(
            onClick = {
                if (!isDownloading) {
                    isDownloading = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ não precisa de permissão para MediaStore
                        scope.launch {
                            downloadImageToGallery(context, imageUrl, message.id) { success ->
                                isDownloading = false
                                val messageText = if (success) {
                                    "Imagem salva na galeria com sucesso!"
                                } else {
                                    "Erro ao salvar imagem na galeria"
                                }
                                Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Android 9 e anterior precisam de permissão
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            scope.launch {
                                downloadImageToGallery(context, imageUrl, message.id) { success ->
                                    isDownloading = false
                                    val messageText = if (success) {
                                        "Imagem salva na galeria com sucesso!"
                                    } else {
                                        "Erro ao salvar imagem na galeria"
                                    }
                                    Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Baixar imagem",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    }
}

private suspend fun downloadImageToGallery(
    context: Context,
    imageUrl: String,
    messageId: String,
    onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val fileName = "image_${messageId}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VCZap")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { imageUri ->
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        connection.getInputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            } else {
                // Android 9 e anterior - usar diretório externo
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val vcZapDir = File(picturesDir, "VCZap")
                if (!vcZapDir.exists()) {
                    vcZapDir.mkdirs()
                }

                val file = File(vcZapDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    connection.getInputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Notificar o MediaScanner sobre o novo arquivo
                val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            }
        } catch (e: Exception) {
            Log.e("ImageMessage", "Erro ao fazer download da imagem: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }
}