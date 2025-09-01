package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pdm.vczap_o.core.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun FileMessage(
    message: ChatMessage,
    isFromMe: Boolean,
    fontSize: Int = 14,
    showPopUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    
    // Launcher para solicitar permissão de escrita
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                downloadFileToStorage(
                    context = context,
                    fileUrl = message.file ?: "",
                    fileName = message.fileName ?: "arquivo",
                    mimeType = message.mimeType ?: "application/octet-stream"
                ) { success ->
                    isDownloading = false
                    val messageText = if (success) {
                        "Arquivo salvo com sucesso!"
                    } else {
                        "Erro ao salvar arquivo"
                    }
                    Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permissão necessária para salvar arquivo", Toast.LENGTH_SHORT).show()
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
            // Header com ícone do arquivo e informações
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getFileIcon(message.mimeType ?: ""),
                    contentDescription = "Arquivo",
                    tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    // Nome original do arquivo
                    Text(
                        text = message.fileName ?: extractFileNameFromUrl(message.file ?: "") ?: "Arquivo",
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = fontSize.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    message.fileSize?.let { size ->
                        if (size > 0) {
                            Text(
                                text = formatFileSize(size),
                                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = (fontSize - 2).sp
                            )
                        }
                    }
                }
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
                // Botão para abrir arquivo
                Button(
                    onClick = {
                        message.file?.let { fileUrl ->
                            openFile(context, fileUrl, message.fileName ?: "arquivo")
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
                                    downloadFileToStorage(
                                        context = context,
                                        fileUrl = message.file ?: "",
                                        fileName = message.fileName ?: "arquivo",
                                        mimeType = message.mimeType ?: "application/octet-stream"
                                    ) { success ->
                                        isDownloading = false
                                        val messageText = if (success) {
                                            "Arquivo salvo com sucesso!"
                                        } else {
                                            "Erro ao salvar arquivo"
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
                                        downloadFileToStorage(
                                            context = context,
                                            fileUrl = message.file ?: "",
                                            fileName = message.fileName ?: "arquivo",
                                            mimeType = message.mimeType ?: "application/octet-stream"
                                        ) { success ->
                                            isDownloading = false
                                            val messageText = if (success) {
                                                "Arquivo salvo com sucesso!"
                                            } else {
                                                "Erro ao salvar arquivo"
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
                            contentDescription = "Baixar arquivo",
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

private fun getFileIcon(mimeType: String) = when {
    mimeType.startsWith("application/pdf") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("application/zip") || mimeType.startsWith("application/x-zip") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("application/msword") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("application/vnd.ms-excel") || mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("application/vnd.ms-powerpoint") || mimeType.startsWith("application/vnd.openxmlformats-officedocument.presentationml.presentation") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("text/") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("image/") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("audio/") -> Icons.Default.InsertDriveFile
    mimeType.startsWith("video/") -> Icons.Default.InsertDriveFile
    else -> Icons.Default.InsertDriveFile
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes bytes"
    }
}

private fun openFile(context: Context, fileUrl: String, fileName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(fileUrl), getMimeTypeFromUrl(fileUrl))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show()
        Log.e("FileMessage", "Erro ao abrir arquivo: ${e.message}")
    }
}

private fun getMimeTypeFromUrl(url: String): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(url)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

private suspend fun downloadFileToStorage(
    context: Context,
    fileUrl: String,
    fileName: String,
    mimeType: String,
    onComplete: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Verificar se a URL é válida
            if (fileUrl.isBlank() || !fileUrl.startsWith("http")) {
                Log.e("FileMessage", "URL inválida para download: $fileUrl")
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
                return@withContext
            }
            
            val url = URL(fileUrl)
            val connection = url.openConnection()
            connection.connect()

            val safeFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val finalFileName = if (safeFileName.contains(".")) safeFileName else "$safeFileName.${getExtensionFromMimeType(mimeType)}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VCZap")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { fileUri ->
                    resolver.openOutputStream(fileUri)?.use { outputStream ->
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
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val vcZapDir = File(downloadsDir, "VCZap")
                if (!vcZapDir.exists()) {
                    vcZapDir.mkdirs()
                }

                val file = File(vcZapDir, finalFileName)
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
            Log.e("FileMessage", "Erro ao fazer download do arquivo: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }
}

private fun getExtensionFromMimeType(mimeType: String): String {
    return when (mimeType) {
        "application/pdf" -> "pdf"
        "application/zip" -> "zip"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/msword" -> "doc"
        "application/vnd.ms-excel" -> "xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        "application/vnd.ms-powerpoint" -> "ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
        "text/plain" -> "txt"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "audio/mpeg" -> "mp3"
        "video/mp4" -> "mp4"
        else -> "bin"
    }
}

private fun extractFileNameFromUrl(url: String): String? {
    return try {
        if (url.isNotEmpty()) {
            val uri = Uri.parse(url)
            val path = uri.lastPathSegment
            if (!path.isNullOrEmpty()) {
                // Remove parâmetros de query se houver
                val withoutQuery = path.split("?").firstOrNull()
                withoutQuery?.split("#")?.firstOrNull()
            } else {
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
