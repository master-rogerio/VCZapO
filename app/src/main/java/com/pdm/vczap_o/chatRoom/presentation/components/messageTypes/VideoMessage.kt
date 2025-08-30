package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.ImageBitmap
import com.pdm.vczap_o.core.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun VideoMessage(
    message: ChatMessage,
    isFromMe: Boolean,
    fontSize: Int = 14,
    showPopUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var thumbnailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Gerar thumbnail do vídeo
    LaunchedEffect(message.video) {
        message.video?.let { videoUrl ->
            if (videoUrl.isNotEmpty()) {
                scope.launch {
                    val bitmap = generateVideoThumbnail(context, videoUrl)
                    bitmap?.let { 
                        thumbnailBitmap = it.asImageBitmap()
                    }
                }
            }
        }
    }
    
    // Launcher para solicitar permissão de escrita
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                downloadVideoToGallery(context, message.video ?: "", message.id) { success ->
                    isDownloading = false
                    val messageText = if (success) {
                        "Vídeo salvo na galeria com sucesso!"
                    } else {
                        "Erro ao salvar vídeo na galeria"
                    }
                    Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permissão necessária para salvar na galeria", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFromMe) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { showPopUp() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview do vídeo com overlay de play
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Thumbnail do vídeo (primeiro frame)
                if (thumbnailBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnailBitmap!!,
                        contentDescription = "Preview do vídeo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback: fundo escuro com ícone de vídeo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Vídeo",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Overlay com ícone de play centralizado
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproduzir vídeo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Header com informações do vídeo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Vídeo",
                    tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Vídeo",
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = fontSize.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }

            // Conteúdo da mensagem se houver
            if (!message.content.isNullOrBlank()) {
                Text(
                    text = message.content,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = fontSize.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botões de ação
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão para abrir vídeo
                Button(
                    onClick = {
                        message.video?.let { videoUrl ->
                            openVideo(context, videoUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                         else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Abrir",
                        fontSize = 12.sp,
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Botão para download
                IconButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Android 10+ não precisa de permissão para MediaStore
                                scope.launch {
                                    downloadVideoToGallery(context, message.video ?: "", message.id) { success ->
                                        isDownloading = false
                                        val messageText = if (success) {
                                            "Vídeo salvo na galeria com sucesso!"
                                        } else {
                                            "Erro ao salvar vídeo na galeria"
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
                                        downloadVideoToGallery(context, message.video ?: "", message.id) { success ->
                                            isDownloading = false
                                            val messageText = if (success) {
                                                "Vídeo salvo na galeria com sucesso!"
                                            } else {
                                                "Erro ao salvar vídeo na galeria"
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
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Baixar vídeo",
                            tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openVideo(context: Context, videoUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(videoUrl), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir vídeo", Toast.LENGTH_SHORT).show()
        Log.e("VideoMessage", "Erro ao abrir vídeo: ${e.message}")
    }
}

private suspend fun generateVideoThumbnail(context: Context, videoUrl: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            
            if (videoUrl.startsWith("http")) {
                // Para URLs remotas, baixar temporariamente para arquivo local
                val tempFile = File.createTempFile("video_thumb", ".mp4", context.cacheDir)
                try {
                    val url = URL(videoUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    
                    val inputStream = connection.getInputStream()
                    val outputStream = tempFile.outputStream()
                    
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    retriever.setDataSource(tempFile.absolutePath)
                } finally {
                    tempFile.delete() // Limpar arquivo temporário
                }
            } else {
                // Para URLs locais
                retriever.setDataSource(context, Uri.parse(videoUrl))
            }
            
            // Gerar thumbnail do primeiro frame
            val thumbnail = retriever.frameAtTime
            retriever.release()
            
            thumbnail
        } catch (e: Exception) {
            Log.e("VideoMessage", "Erro ao gerar thumbnail: ${e.message}")
            null
        }
    }
}

private suspend fun downloadVideoToGallery(
    context: Context,
    videoUrl: String,
    messageId: String,
    onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Verificar se a URL é válida
            if (videoUrl.isBlank() || !videoUrl.startsWith("http")) {
                Log.e("VideoMessage", "URL inválida para download: $videoUrl")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
                return@withContext
            }
            
            val url = URL(videoUrl)
            val connection = url.openConnection()
            connection.connect()

            val fileName = "video_${messageId}_${System.currentTimeMillis()}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VCZap")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { videoUri ->
                    resolver.openOutputStream(videoUri)?.use { outputStream ->
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
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val vcZapDir = File(moviesDir, "VCZap")
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
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            }
        } catch (e: Exception) {
            Log.e("VideoMessage", "Erro ao fazer download do vídeo: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }
}
